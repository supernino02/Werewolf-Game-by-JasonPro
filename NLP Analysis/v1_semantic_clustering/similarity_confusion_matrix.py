import os
import argparse
import pandas as pd
import numpy as np
import torch
import seaborn as sns
import matplotlib.pyplot as plt
from transformers import BertTokenizer, BertModel
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.metrics import confusion_matrix, accuracy_score
import warnings

warnings.filterwarnings('ignore')

# 1. Define Data
HARDCODED_PLAYERS = [
    'alice', 'bob', 'charlie', 'arthur', 'bella', 'caleb', 'david', 
    'eve', 'frank', 'grace', 'heidi', 'ivan', 'judy', 'kevin', 'leo'
]

PERFORMATIVES = ["accuse", "defend", "suspect", "agree", "interrogate", "deflect"]

TEMPLATES = {
    "accuse": "aggressively accuse {target} of being a wolf and push the village to execute them.",
    "defend": "defend {target} against current suspicions and argue for their innocence.",
    "suspect": "express heavy suspicion towards {target} without fully committing to an accusation.",
    "agree": "strongly agree with the recent statements or tactical direction of {target}.",
    "interrogate": "ask a direct, pressuring question to {target} to force them into a mistake or contradiction.",
    "deflect": "deflect any current suspicion away and firmly redirect the village's attention onto {target}."
}

def plot_academic_confusion_matrix(y_true, y_pred, labels, title, filename):
    """Generates a clean, publication-ready normalized confusion matrix."""
    # Compute normalized confusion matrix (Row-wise / Recall)
    cm = confusion_matrix(y_true, y_pred, labels=labels, normalize='true')
    
    # Setup seaborn academic theme
    sns.set_theme(style="white", font_scale=1.1)
    fig, ax = plt.subplots(figsize=(10, 8))
    
    # Plot heatmap
    sns.heatmap(
        cm, annot=True, fmt=".2f", cmap="Blues",
        xticklabels=labels, yticklabels=labels,
        vmin=0.0, vmax=1.0, 
        cbar_kws={'label': 'Probability (normalized row-wise)'},
        linewidths=0.5, linecolor='gray'
    )
    
    # Formatting
    plt.title(title, fontsize=16, fontweight='bold', pad=15)
    plt.ylabel('Ground Truth', fontsize=14, fontweight='bold')
    plt.xlabel('Predicted', fontsize=14, fontweight='bold')
    
    # Rotate labels for targets to prevent overlap
    plt.xticks(rotation=45, ha='right')
    plt.yticks(rotation=0)
    plt.tight_layout()
    
    # Save high resolution
    plt.savefig(filename, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Saved: {filename}")


def run_baseline_evaluation(test_csv: str):
    # 2. Generate the 90 Anchor combinations
    anchor_data = []
    for perf, template in TEMPLATES.items():
        for target in HARDCODED_PLAYERS:
            sentence = template.format(target=target)
            anchor_data.append({
                "sentence": sentence, 
                "performative": perf, 
                "target": target
            })

    anchors_df = pd.DataFrame(anchor_data)

    # 3. Load Test Data
    print(f"Loading test dataset from {test_csv}...")
    try:
        test_df = pd.read_csv(test_csv)
        # Ensure correct schema
        test_df = test_df.dropna(subset=['message', 'performative', 'target'])
        test_df = test_df[test_df['target'].isin(HARDCODED_PLAYERS)]
        test_df = test_df[test_df['performative'].isin(PERFORMATIVES)]
    except FileNotFoundError:
        print(f"ERROR: File {test_csv} not found.")
        return

    print(f"Test Set: {len(test_df)} valid messages.")

    # 4. Embed using EXACTLY the base model (bert-base-uncased)
    print("Loading bert-base-uncased...")
    tokenizer = BertTokenizer.from_pretrained('bert-base-uncased')
    model = BertModel.from_pretrained('bert-base-uncased')
    model.eval()

    def embed_sentences(sentences):
        embeddings = []
        with torch.no_grad():
            for sentence in sentences:
                inputs = tokenizer(sentence, return_tensors='pt', padding=True, truncation=True, max_length=128)
                outputs = model(**inputs)
                embeddings.append(outputs.pooler_output.squeeze().cpu().numpy())
        return np.array(embeddings)

    print("Embedding Anchors...")
    anchor_embeddings = embed_sentences(anchors_df['sentence'].tolist())
    
    print("Embedding Test Set...")
    test_embeddings = embed_sentences(test_df['message'].tolist())

    # 5. K-Nearest Neighbor Matching (Cosine Similarity)
    print("Computing cosine similarities and matching nearest anchors...")
    sim_matrix = cosine_similarity(test_embeddings, anchor_embeddings)
    
    # Get index of the highest similarity anchor for each test sentence
    best_anchor_indices = np.argmax(sim_matrix, axis=1)
    
    # 6. Extract Predictions
    predicted_perfs = [anchors_df.iloc[idx]['performative'] for idx in best_anchor_indices]
    predicted_targets = [anchors_df.iloc[idx]['target'] for idx in best_anchor_indices]
    
    true_perfs = test_df['performative'].tolist()
    true_targets = test_df['target'].tolist()

    # 7. Print Accuracies
    perf_acc = accuracy_score(true_perfs, predicted_perfs)
    target_acc = accuracy_score(true_targets, predicted_targets)
    
    print("-" * 50)
    print("Cosine Similarity KNN (Raw bert-base-uncased)")
    print(f"Performative Accuracy: {perf_acc * 100:.2f}%")
    print(f"Target Accuracy:       {target_acc * 100:.2f}%")
    print("-" * 50)

    # 8. Generate Paper-Ready Confusion Matrices
    print("Generating Academic Confusion Matrices...")
    
    plot_academic_confusion_matrix(
        y_true=true_perfs,
        y_pred=predicted_perfs,
        labels=PERFORMATIVES,
        title=f"Performative Prediction with cosine similarity (Raw bert-base-uncased)\nAccuracy: {perf_acc * 100:.1f}%",
        filename="cm_performative_baseline.png"
    )
    
    plot_academic_confusion_matrix(
        y_true=true_targets,
        y_pred=predicted_targets,
        labels=HARDCODED_PLAYERS,
        title=f"Target Prediction with cosine similarity (Raw bert-base-uncased)\nAccuracy: {target_acc * 100:.1f}%",
        filename="cm_target_baseline.png"
    )

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Evaluate Base BERT Nearest-Anchor Baseline.")
    parser.add_argument("--test_csv", type=str, required=True, help="Path to the test CSV file.")
    args = parser.parse_args()
    
    run_baseline_evaluation(args.test_csv)