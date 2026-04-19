import os
import argparse
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from matplotlib.ticker import MaxNLocator

def plot_step_logs(csv_path: str, output_path: str, x_axis_col: str):
    if not os.path.exists(csv_path):
        print(f"ERROR: Log file not found at {csv_path}")
        return

    print(f"Loading training logs from {csv_path}...")
    df = pd.read_csv(csv_path)

    # Verify the requested X-axis exists
    if x_axis_col not in df.columns:
        print(f"ERROR: X-axis column '{x_axis_col}' not found. Available columns: {list(df.columns)}")
        return

    required_cols = [
        'train_loss_total', 'val_loss_total', 
        'val_perf_acc', 'val_target_acc'
    ]
    for col in required_cols:
        if col not in df.columns:
            print(f"ERROR: Missing expected column '{col}' in the CSV.")
            return

    # Dynamic visual formatting
    num_points = len(df)
    use_markers = num_points <= 50  # Prevent marker clutter on high-frequency logs
    m_train = 'o' if use_markers else None
    m_val = 's' if use_markers else None
    line_w = 2 if use_markers else 1.5

    sns.set_theme(style="whitegrid", rc={"axes.edgecolor": "black", "axes.linewidth": 1})
    fig, axes = plt.subplots(1, 2, figsize=(12, 5))

    # X-Axis label formatting
    x_label = "Global Step (Optimizer Updates)" if x_axis_col == 'global_step' else "Total Samples Seen"

    # --- LEFT PLOT: Loss Curves ---
    axes[0].plot(df[x_axis_col], df['train_loss_total'], label='Train Loss (Rolling)', marker=m_train, color='#1f77b4', linewidth=line_w)
    axes[0].plot(df[x_axis_col], df['val_loss_total'], label='Validation Loss', marker=m_val, color='#d62728', linewidth=line_w)
    
    axes[0].set_title('Learning Curves (Loss)', fontsize=14, fontweight='bold', pad=15)
    axes[0].set_xlabel(x_label, fontsize=12, fontweight='bold')
    axes[0].set_ylabel('Cross-Entropy Loss', fontsize=12, fontweight='bold')
    axes[0].legend(loc='upper right', frameon=True, edgecolor='black')
    
    # Auto-format X-axis for clean scaling
    axes[0].xaxis.set_major_locator(MaxNLocator(integer=True, nbins=8))
    axes[0].set_xlim(df[x_axis_col].min(), df[x_axis_col].max())

    # --- RIGHT PLOT: Accuracy Curves ---
    axes[1].plot(df[x_axis_col], df['val_perf_acc'], label='Performative Accuracy', marker=m_train, color='#2ca02c', linewidth=line_w)
    axes[1].plot(df[x_axis_col], df['val_target_acc'], label='Target Accuracy', marker=m_val, color='#9467bd', linewidth=line_w)
    
    axes[1].set_title('Validation Performance', fontsize=14, fontweight='bold', pad=15)
    axes[1].set_xlabel(x_label, fontsize=12, fontweight='bold')
    axes[1].set_ylabel('Accuracy', fontsize=12, fontweight='bold')
    axes[1].legend(loc='lower right', frameon=True, edgecolor='black')
    
    ymin = min(df['val_perf_acc'].min(), df['val_target_acc'].min()) - 0.05
    axes[1].set_ylim(max(0.0, ymin), 1.05)
    axes[1].xaxis.set_major_locator(MaxNLocator(integer=True, nbins=8))
    axes[1].set_xlim(df[x_axis_col].min(), df[x_axis_col].max())

    # Finalize and Save
    plt.tight_layout()
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Success. Learning curves securely saved to: {output_path}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Plot training and validation curves from step-based logs.")
    parser.add_argument("--csv", type=str, default="JOINT_BERT_MODELS/step_logs.csv", help="Path to the training log CSV.")
    parser.add_argument("--output", type=str, default="training_step_curves.png", help="Path to save the output plot.")
    parser.add_argument("--x-axis", type=str, choices=['global_step', 'total_samples'], default='global_step', help="Metric to use for the X-axis.")
    args = parser.parse_args()

    plot_step_logs(args.csv, args.output, args.x_axis)