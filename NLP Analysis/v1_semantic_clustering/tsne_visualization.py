import os
import argparse
import pandas as pd
import numpy as np
import torch
import seaborn as sns
import matplotlib.pyplot as plt
from sklearn.manifold import TSNE
from transformers import BertTokenizer, BertModel
import warnings

warnings.filterwarnings('ignore')

# 1. Define Data
HARDCODED_PLAYERS = [
    'alice', 'bob', 'charlie', 'arthur', 'bella', 'caleb', 'david', 
    'eve', 'frank', 'grace', 'heidi', 'ivan', 'judy', 'kevin', 'leo'
]

TEMPLATES = {
    "accuse": "aggressively accuse {target} of being a wolf and push the village to execute them.",
    "defend": "defend {target} against current suspicions and argue for their innocence.",
    "suspect": "express heavy suspicion towards {target} without fully committing to an accusation.",
    "agree": "strongly agree with the recent statements or tactical direction of {target}.",
    "interrogate": "ask a direct, pressuring question to {target} to force them into a mistake or contradiction.",
    "deflect": "deflect any current suspicion away and firmly redirect the village's attention onto {target}."
}

PERFORMATIVE_PALETTE = sns.color_palette("Set2", n_colors=len(TEMPLATES))

def generate_plot(csv_file=None, limit=None):
    # 2. Generate the 90 Anchor combinations
    anchor_data = []
    for perf, template in TEMPLATES.items():
        for player in HARDCODED_PLAYERS:
            sentence = template.format(target=player)
            anchor_data.append({
                "sentence": sentence, 
                "performative": perf, 
                "player": player,
                "source": "Anchor"
            })

    df = pd.DataFrame(anchor_data)
    print(f"Generated {len(df)} Anchor templates.")

    # 3. Process Optional CSV
    if csv_file:
        print(f"Loading dataset from {csv_file}...")
        try:
            df_csv = pd.read_csv(csv_file)
            
            # Map CSV columns to match Anchor schema
            if 'speaker' in df_csv.columns and 'message' in df_csv.columns:
                df_csv = df_csv.rename(columns={'speaker': 'player', 'message': 'sentence'})
            
            # Drop invalid rows
            df_csv = df_csv.dropna(subset=['sentence', 'performative', 'player'])
            
            # Filter to strictly known players and performatives to prevent plotting errors
            df_csv = df_csv[df_csv['player'].isin(HARDCODED_PLAYERS)]
            df_csv = df_csv[df_csv['performative'].isin(TEMPLATES.keys())]
            
            # Apply Limit
            if limit:
                df_csv = df_csv.head(limit)
                
            df_csv['source'] = 'Game Log'
            
            print(f"Adding {len(df_csv)} messages from CSV to the embedding queue.")
            df = pd.concat([df, df_csv], ignore_index=True)
            
        except FileNotFoundError:
            print(f"WARNING: File {csv_file} not found. Proceeding with Anchors only.")

    # 4. Embed using EXACTLY the base model (bert-base-uncased)
    print("Loading bert-base-uncased...")
    tokenizer = BertTokenizer.from_pretrained('bert-base-uncased')
    model = BertModel.from_pretrained('bert-base-uncased')
    model.eval() # Lock dropout layers

    print(f"Extracting pooler_outputs for {len(df)} total sentences...")
    embeddings = []

    with torch.no_grad():
        for sentence in df['sentence']:
            inputs = tokenizer(sentence, return_tensors='pt', padding=True, truncation=True, max_length=128)
            outputs = model(**inputs)
            
            # Extract the exact tensor that feeds into your Cascade classification heads
            pooler_output = outputs.pooler_output.squeeze().cpu().numpy()
            embeddings.append(pooler_output)

    embeddings = np.array(embeddings)

    # 5. Dimensionality Reduction via t-SNE
    print("Computing t-SNE...")
    # Dynamic perplexity: must be strictly less than the number of samples. Standard is min(30, N-1).
    perplexity_val = min(30, len(embeddings) - 1)
    tsne = TSNE(n_components=2, perplexity=perplexity_val, random_state=42)
    tsne_results = tsne.fit_transform(embeddings)

    df['tsne_1'] = tsne_results[:, 0]
    df['tsne_2'] = tsne_results[:, 1]

    # 6. Visualization
    plt.figure(figsize=(6, 6))

    # Plot Dataset Background Cloud (Small, transparent)
    sns.scatterplot(
        data=df[df['source'] == 'Game Log'], 
        x='tsne_1', y='tsne_2',
        hue='performative',
        hue_order=list(TEMPLATES.keys()),
        palette=PERFORMATIVE_PALETTE,
        s=40, alpha=0.25, legend=False
    )

    # Plot Anchor Templates (Large, opaque)
    sns.scatterplot(
        data=df[df['source'] == 'Anchor'], 
        x='tsne_1', y='tsne_2',
        hue='performative',
        hue_order=list(TEMPLATES.keys()),
        palette=PERFORMATIVE_PALETTE,
        s=40, alpha=1.0, edgecolor='black', linewidth=1
    )

    plt.title("t-SNE of the embedding space", fontsize=16, pad=15, fontweight='bold')
    plt.xlabel("t-SNE Dimension 1", fontsize=12)
    plt.ylabel("t-SNE Dimension 2", fontsize=12)

    # Fix Legend
    handles, labels = plt.gca().get_legend_handles_labels()
    plt.legend(handles, labels, loc='upper right', title="Performative")
    plt.tight_layout()

    # 7. Save output
    output_filename = "tsne_anchor.png"
    plt.savefig(output_filename, dpi=300, bbox_inches='tight')
    print(f"Success. Plot saved securely to: {output_filename}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Embed templates and optional CSV data using raw BERT.")
    parser.add_argument("--csv", type=str, default=None, help="Path to an optional CSV file to plot alongside anchors.")
    parser.add_argument("--limit", type=int, default=None, help="Maximum number of rows to pull from the CSV.")
    
    args = parser.parse_args()
    
    generate_plot(csv_file=args.csv, limit=args.limit)