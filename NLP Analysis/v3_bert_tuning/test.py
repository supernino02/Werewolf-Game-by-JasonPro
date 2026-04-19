import os
import argparse
import pandas as pd
import numpy as np
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader
from transformers import BertTokenizerFast, BertModel
from sklearn.metrics import classification_report, accuracy_score
from sklearn.manifold import TSNE
import matplotlib.pyplot as plt
import seaborn as sns
import warnings

warnings.filterwarnings("ignore")

# 1. Fixed Closed Sets
PERFORMATIVES = ['accuse', 'defend', 'suspect', 'agree', 'interrogate', 'deflect']
PERF_TO_ID = {perf: i for i, perf in enumerate(PERFORMATIVES)}
ID_TO_PERF = {i: perf for i, perf in enumerate(PERFORMATIVES)}

PLAYERS = ['alice', 'bob', 'charlie', 'arthur', 'bella', 'caleb', 'david', 
           'eve', 'frank', 'grace', 'heidi', 'ivan', 'judy', 'kevin', 'leo']
TARGET_TO_ID = {t: i for i, t in enumerate(PLAYERS)}
ID_TO_TARGET = {i: t for i, t in enumerate(PLAYERS)}

class SimpleDualDataset(Dataset):
    def __init__(self, df, tokenizer, max_len=128):
        self.data = df
        self.tokenizer = tokenizer
        self.max_len = max_len

    def __len__(self):
        return len(self.data)

    def __getitem__(self, index):
        row = self.data.iloc[index]
        text = str(row['message'])
        
        encoding = self.tokenizer(
            text, max_length=self.max_len, padding='max_length',
            truncation=True, return_tensors='pt'
        )

        perf_label = PERF_TO_ID[row['performative']]
        target_label = TARGET_TO_ID[row['target']]

        return {
            'input_ids': encoding['input_ids'].flatten(),
            'attention_mask': encoding['attention_mask'].flatten(),
            'perf_label': torch.tensor(perf_label, dtype=torch.long),
            'target_label': torch.tensor(target_label, dtype=torch.long)
        }

class SimpleJointBERT(nn.Module):
    def __init__(self, num_performatives, num_targets, model_name='bert-base-uncased'):
        super(SimpleJointBERT, self).__init__()
        self.bert = BertModel.from_pretrained(model_name)
        self.dropout = nn.Dropout(0.1)
        hidden_dim = self.bert.config.hidden_size
        
        # Structure: [0] Linear -> [1] ReLU -> [2] Dropout -> [3] Linear
        self.perf_classifier = nn.Sequential(
            nn.Linear(hidden_dim, hidden_dim // 2),
            nn.ReLU(),
            nn.Dropout(0.1),
            nn.Linear(hidden_dim // 2, num_performatives)
        )
        
        self.target_classifier = nn.Sequential(
            nn.Linear(hidden_dim, hidden_dim // 2),
            nn.ReLU(),
            nn.Dropout(0.1),
            nn.Linear(hidden_dim // 2, num_targets)
        )

    def forward(self, input_ids, attention_mask):
        outputs = self.bert(input_ids=input_ids, attention_mask=attention_mask)
        pooled_output = self.dropout(outputs.pooler_output)
        
        # Intercept intermediate representations (slicing through Dropout)
        perf_hidden = self.perf_classifier[:3](pooled_output)
        target_hidden = self.target_classifier[:3](pooled_output)
        
        # Compute final logits
        perf_logits = self.perf_classifier[3](perf_hidden)
        target_logits = self.target_classifier[3](target_hidden)
        
        return perf_logits, target_logits, perf_hidden, target_hidden

def evaluate_and_plot(args):
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Hardware utilized: {device}")

    print(f"Loading test dataset from {args.test_csv}...")
    test_df = pd.read_csv(args.test_csv).dropna(subset=['message', 'performative', 'target'])
    test_df['target'] = test_df['target'].str.lower().str.strip()
    test_df = test_df[test_df['target'].isin(PLAYERS) & test_df['performative'].isin(PERFORMATIVES)]
    
    if len(test_df) == 0:
        print("ERROR: Test dataset is empty after filtering for valid labels.")
        return

    tokenizer = BertTokenizerFast.from_pretrained('bert-base-uncased')
    test_loader = DataLoader(SimpleDualDataset(test_df, tokenizer), batch_size=args.batch_size, shuffle=False)

    print(f"Initializing architecture and loading weights from {args.model_path}...")
    model = SimpleJointBERT(len(PERFORMATIVES), len(PLAYERS)).to(device)
    
    try:
        model.load_state_dict(torch.load(args.model_path, map_location=device))
    except FileNotFoundError:
        print(f"ERROR: Model file {args.model_path} not found.")
        return
        
    model.eval()

    p_preds, p_trues = [], []
    t_preds, t_trues = [], []
    perf_embeddings = []
    target_embeddings = []

    print("Running inference and extracting task-specific latent vectors...")
    with torch.no_grad():
        for batch in test_loader:
            input_ids = batch['input_ids'].to(device)
            attention_mask = batch['attention_mask'].to(device)
            
            p_logits, t_logits, p_hidden, t_hidden = model(input_ids, attention_mask)
            
            p_preds.extend(torch.argmax(p_logits, dim=1).cpu().numpy())
            p_trues.extend(batch['perf_label'].numpy())
            
            t_preds.extend(torch.argmax(t_logits, dim=1).cpu().numpy())
            t_trues.extend(batch['target_label'].numpy())
            
            perf_embeddings.append(p_hidden.cpu().numpy())
            target_embeddings.append(t_hidden.cpu().numpy())

    perf_embeddings = np.vstack(perf_embeddings)
    target_embeddings = np.vstack(target_embeddings)

    print("\n" + "="*50)
    print("FINAL TEST REPORT")
    print("="*50)
    
    print("\n--- Performative Classification ---")
    perf_acc = accuracy_score(p_trues, p_preds)
    print(f"Accuracy: {perf_acc:.4f}")
    print(classification_report(p_trues, p_preds, target_names=PERFORMATIVES, zero_division=0))
    
    print("\n--- Target Classification ---")
    target_acc = accuracy_score(t_trues, t_preds)
    print(f"Accuracy: {target_acc:.4f}")
    print(classification_report(t_trues, t_preds, target_names=PLAYERS, zero_division=0))
    print("="*50 + "\n")

    # t-SNE Computation
    print("Computing separate t-SNE projections for each head...")
    perplexity_val = min(30, len(perf_embeddings) - 1)
    
    tsne_perf = TSNE(n_components=2, perplexity=perplexity_val, random_state=42)
    tsne_perf_results = tsne_perf.fit_transform(perf_embeddings)
    
    tsne_target = TSNE(n_components=2, perplexity=perplexity_val, random_state=42)
    tsne_target_results = tsne_target.fit_transform(target_embeddings)

    true_perf_labels = [ID_TO_PERF[idx] for idx in p_trues]
    true_target_labels = [ID_TO_TARGET[idx] for idx in t_trues]

    print("Rendering specialized plots...")
    sns.set_theme(style="whitegrid", rc={"axes.edgecolor": "black", "axes.linewidth": 1})
    fig, axes = plt.subplots(1, 2, figsize=(12, 6), dpi=600)

    # Left Plot: Performative Head Space
    sns.scatterplot(
        x=tsne_perf_results[:, 0], y=tsne_perf_results[:, 1],
        hue=true_perf_labels, hue_order=PERFORMATIVES,
        palette=sns.color_palette("Set2", n_colors=len(PERFORMATIVES)),
        s=40, alpha=0.8, edgecolor='w', ax=axes[0],
        legend=False
    )
    axes[0].set_title(f"Performative Head: Hidden Layer Space\n(Accuracy: {perf_acc:.1%})", fontsize=14, fontweight='bold', pad=15)
    axes[0].set_xlabel("t-SNE Dim 1", fontweight='bold')
    axes[0].set_ylabel("t-SNE Dim 2", fontweight='bold')
    #axes[0].legend(title="Performative", loc='best', frameon=True, edgecolor='black')

    # Right Plot: Target Head Space
    sns.scatterplot(
        x=tsne_target_results[:, 0], y=tsne_target_results[:, 1],
        hue=true_target_labels, hue_order=PLAYERS,
        palette=sns.color_palette("tab20", n_colors=len(PLAYERS)),
        s=40, alpha=0.8, edgecolor='w', ax=axes[1],
        legend=False
    )
    axes[1].set_title(f"Target Head: Hidden Layer Space\n(Accuracy: {target_acc:.1%})", fontsize=14, fontweight='bold', pad=15)
    axes[1].set_xlabel("t-SNE Dim 1", fontweight='bold')
    axes[1].set_ylabel("t-SNE Dim 2", fontweight='bold')
    #axes[1].legend(title="Target", loc='center left', bbox_to_anchor=(1, 0.5), ncol=1, frameon=True, edgecolor='black')

    plt.tight_layout()
    plt.savefig(args.output_plot, dpi=300, bbox_inches='tight')
    plt.close()
    
    print(f"Success. Dual-head latent space projections securely saved to: {args.output_plot}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Test JointBERT and visualize specialized head latent spaces.")
    parser.add_argument("--test-csv", type=str, required=True, help="Path to the untouched test CSV file.")
    parser.add_argument("--model-path", type=str, required=True, help="Path to the specific .pth weight file.")
    parser.add_argument("--batch-size", type=int, default=64, help="Batch size for inference.")
    parser.add_argument("--output-plot", type=str, default="tsne_head_spaces.png", help="Path to save the dual t-SNE plot.")
    args = parser.parse_args()

    evaluate_and_plot(args)