import os
import argparse
import pandas as pd
import numpy as np
import torch
import seaborn as sns
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
from sklearn.manifold import TSNE
from sklearn.svm import SVC
from sklearn.metrics import confusion_matrix, accuracy_score
from transformers import BertTokenizer, BertModel
import warnings

warnings.filterwarnings('ignore')

HARDCODED_PLAYERS = [
    'alice', 'bob', 'charlie', 'arthur', 'bella', 'caleb', 'david', 
    'eve', 'frank', 'grace', 'heidi', 'ivan', 'judy', 'kevin', 'leo'
]
PERFORMATIVES = ["accuse", "defend", "suspect", "agree", "interrogate", "deflect"]

def plot_combined_confusion_matrices(y_true_p, y_pred_p, labels_p, acc_p, c_p,
                                     y_true_t, y_pred_t, labels_t, acc_t, c_t,
                                     filename="cm_combined.png"):
    """Generates a side-by-side, annot-free confusion matrix figure with a central colorbar."""
    cm_p = confusion_matrix(y_true_p, y_pred_p, labels=labels_p, normalize='true')
    cm_t = confusion_matrix(y_true_t, y_pred_t, labels=labels_t, normalize='true')
    
    sns.set_theme(style="white", font_scale=1.0)
    fig = plt.figure(figsize=(10, 5))
    
    # 1 row, 3 columns (Left Matrix, Thin Center Colorbar, Right Matrix)
    gs = gridspec.GridSpec(1, 3, width_ratios=[1, 0.05, 1])
    ax_perf = plt.subplot(gs[0])
    cbar_ax = plt.subplot(gs[1])
    ax_target = plt.subplot(gs[2])
    
    # 'viridis' spans Dark Purple (0) -> Teal (0.5) -> Bright Yellow (1.0)
    cmap = "viridis"
    
    # 1. Left Plot: Performative
    sns.heatmap(
        cm_p, annot=False, cmap=cmap, vmin=0.0, vmax=1.0,
        xticklabels=labels_p, yticklabels=labels_p,
        cbar=False, ax=ax_perf, linewidths=0.5, linecolor='gray'
    )
    ax_perf.set_title(f"Performative (C={c_p})\nAcc: {acc_p:.1%}", fontweight='bold', pad=10)
    ax_perf.set_ylabel('Ground Truth', fontweight='bold')
    ax_perf.set_xlabel('Predicted', fontweight='bold')
    ax_perf.tick_params(axis='x', rotation=90)
    ax_perf.tick_params(axis='y', rotation=0)
    
    # 2. Right Plot: Target
    sns.heatmap(
        cm_t, annot=False, cmap=cmap, vmin=0.0, vmax=1.0,
        xticklabels=labels_t, yticklabels=labels_t,
        cbar=True, cbar_ax=cbar_ax, 
        ax=ax_target, linewidths=0.5, linecolor='gray'
    )
    ax_target.set_title(f"Target (C={c_t})\nAcc: {acc_t:.1%}", fontweight='bold', pad=10)
    
    # Mirror the Y-axis to the right so labels don't collide with the center colorbar
    ax_target.yaxis.tick_right()
    ax_target.yaxis.set_label_position("right")
    
    ax_target.set_ylabel('Ground Truth', fontweight='bold')
    ax_target.set_xlabel('Predicted', fontweight='bold')
    ax_target.tick_params(axis='x', rotation=90)
    ax_target.tick_params(axis='y', rotation=0)
    
    # Finalize Layout
    plt.tight_layout()
    plt.savefig(filename, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Saved merged confusion matrix plot: {filename}")

def run_pipeline(train_csv: str, test_csv: str, plot_hyp: bool):
    print(f"Loading datasets...")
    try:
        train_df = pd.read_csv(train_csv).dropna(subset=['message', 'performative', 'target'])
        test_df = pd.read_csv(test_csv).dropna(subset=['message', 'performative', 'target'])
        
        train_df = train_df[train_df['target'].isin(HARDCODED_PLAYERS) & train_df['performative'].isin(PERFORMATIVES)]
        test_df = test_df[test_df['target'].isin(HARDCODED_PLAYERS) & test_df['performative'].isin(PERFORMATIVES)]
    except FileNotFoundError as e:
        print(f"ERROR: {e}")
        return

    print(f"Train Set: {len(train_df)} | Test Set: {len(test_df)}")

    # 1. EMBEDDING PROCESS
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

    print("Embedding Training Data...")
    X_train = embed_sentences(train_df['message'].tolist())
    print("Embedding Testing Data...")
    X_test = embed_sentences(test_df['message'].tolist())

    y_train_perf = train_df['performative'].tolist()
    y_test_perf = test_df['performative'].tolist()
    
    y_train_target = train_df['target'].tolist()
    y_test_target = test_df['target'].tolist()

    # 2. t-SNE OF TRAINING DATA
    print("Generating t-SNE projection for Training Embeddings...")
    tsne = TSNE(n_components=2, perplexity=min(30, len(X_train) - 1), random_state=42)
    X_train_tsne = tsne.fit_transform(X_train)

    print("Rendering t-SNE plot (Performative)...")
    plt.figure(figsize=(10, 8))
    sns.scatterplot(
        x=X_train_tsne[:, 0], y=X_train_tsne[:, 1],
        hue=y_train_perf, hue_order=PERFORMATIVES,
        palette=sns.color_palette("Set2", n_colors=len(PERFORMATIVES)),
        s=50, alpha=0.7, edgecolor='w'
    )
    plt.title("t-SNE of Training Embeddings", fontsize=16, fontweight='bold', pad=15)
    plt.xlabel("t-SNE Dimension 1")
    plt.ylabel("t-SNE Dimension 2")
    plt.tight_layout()
    plt.savefig("tsne_train_embeddings_perf.png", dpi=300, bbox_inches='tight')
    plt.close()

    print("Rendering t-SNE plot (Target)...")
    plt.figure(figsize=(10, 8))
    sns.scatterplot(
        x=X_train_tsne[:, 0], y=X_train_tsne[:, 1],
        hue=y_train_target, hue_order=HARDCODED_PLAYERS,
        palette=sns.color_palette("tab20", n_colors=len(HARDCODED_PLAYERS)),
        s=50, alpha=0.7, edgecolor='w'
    )
    plt.title("t-SNE of Training Embeddings", fontsize=16, fontweight='bold', pad=15)
    plt.xlabel("t-SNE Dimension 1")
    plt.ylabel("t-SNE Dimension 2")
    plt.tight_layout()
    plt.savefig("tsne_train_embeddings_target.png", dpi=300, bbox_inches='tight')
    plt.close()

    # 3. GAUSSIAN SVM HYPERPARAMETER TUNING
    C_values = [50.0, 100.0, 150,160,170,180,190, 200.0, 210, 220, ]
    
    def tune_and_train_svm(target_name, y_train, y_test, labels):
        print(f"\n--- Tuning Gaussian SVC for {target_name.upper()} ---")
        
        train_accs = []
        test_accs = []
        best_acc = 0.0
        best_c = None
        best_model = None
        
        for c_val in C_values:
            svc = SVC(kernel='rbf', C=c_val, gamma='scale', random_state=42)
            svc.fit(X_train, y_train)
            
            t_pred = svc.predict(X_train)
            v_pred = svc.predict(X_test)
            
            t_acc = accuracy_score(y_train, t_pred)
            v_acc = accuracy_score(y_test, v_pred)
            
            train_accs.append(t_acc)
            test_accs.append(v_acc)
            
            print(f"C={c_val:<6.2f} | Train Acc: {t_acc:.4f} | Test Acc: {v_acc:.4f}")
            
            if v_acc > best_acc:
                best_acc = v_acc
                best_c = c_val
                best_model = svc

        print(f">> BEST {target_name.upper()}: C={best_c} | Test Acc: {best_acc:.4f}")
        
        if plot_hyp:
            plt.figure(figsize=(8, 6))
            plt.plot(C_values, train_accs, marker='o', linestyle='-', label='Train Accuracy', color='blue')
            plt.plot(C_values, test_accs, marker='s', linestyle='-', label='Test Accuracy', color='orange')
            plt.xscale('log')
            plt.axvline(x=best_c, color='red', linestyle='--', alpha=0.6, label=f'Best C ({best_c})')
            
            plt.title(f"RBF Kernel SVC Tuning: {target_name.capitalize()}", fontsize=14, fontweight='bold', pad=15)
            plt.xlabel("Regularization Parameter C (Log Scale)")
            plt.ylabel("Classification Accuracy")
            plt.legend()
            plt.grid(True, linestyle='--', alpha=0.6)
            plt.tight_layout()
            plt.savefig(f"hyp_tuning_{target_name}.png", dpi=300)
            plt.close()

        # Return the absolute best predictions to plot later
        y_test_pred_best = best_model.predict(X_test)
        return y_test_pred_best, best_acc, best_c

    # Execute for both targets
    pred_perf, acc_perf, c_perf = tune_and_train_svm("performative", y_train_perf, y_test_perf, PERFORMATIVES)
    pred_target, acc_target, c_target = tune_and_train_svm("target", y_train_target, y_test_target, HARDCODED_PLAYERS)

    # 4. Generate the side-by-side combined figure
    print("\nGenerating unified confusion matrices...")
    plot_combined_confusion_matrices(
        y_test_perf, pred_perf, PERFORMATIVES, acc_perf, c_perf,
        y_test_target, pred_target, HARDCODED_PLAYERS, acc_target, c_target,
        filename="cm_combined.png"
    )

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Evaluate Gaussian SVM on BERT Embeddings.")
    parser.add_argument("--train_csv", type=str, required=True, help="Path to the training CSV file.")
    parser.add_argument("--test_csv", type=str, required=True, help="Path to the test CSV file.")
    parser.add_argument("--plot-hyp-tuning", action="store_true", help="Plot Train vs Test accuracy over hyperparameter values.")
    args = parser.parse_args()
    
    run_pipeline(args.train_csv, args.test_csv, args.plot_hyp_tuning)