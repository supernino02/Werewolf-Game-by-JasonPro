import pandas as pd
import numpy as np
import torch
import seaborn as sns
import matplotlib.pyplot as plt
from transformers import BertTokenizer, BertModel
from sklearn.metrics.pairwise import cosine_similarity
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

# 2. Generate the 90 combinations
data = []
for perf, template in TEMPLATES.items():
    for player in HARDCODED_PLAYERS:
        sentence = template.format(target=player)
        data.append({
            "sentence": sentence, 
            "performative": perf, 
            "player": player
        })

df = pd.DataFrame(data)

# 3. Embed using EXACTLY your base model (bert-base-uncased)
print("Loading bert-base-uncased...")
tokenizer = BertTokenizer.from_pretrained('bert-base-uncased')
model = BertModel.from_pretrained('bert-base-uncased')
model.eval()

print("Extracting pooler_outputs...")
embeddings = []

with torch.no_grad():
    for sentence in df['sentence']:
        inputs = tokenizer(sentence, return_tensors='pt', padding=True, truncation=True, max_length=128)
        outputs = model(**inputs)
        pooler_output = outputs.pooler_output.squeeze().cpu().numpy()
        embeddings.append(pooler_output)

embeddings = np.array(embeddings)

# 4. Compute the 90x90 Cosine Similarity Matrix
print("Computing pairwise cosine similarities...")
sim_matrix = cosine_similarity(embeddings)

# 5. Extract Intra-class and Inter-class similarities
intra_class_sims = []
inter_class_sims = []

n = len(df)
for i in range(n):
    for j in range(i + 1, n): 
        sim = sim_matrix[i, j]
        if df['performative'].iloc[i] == df['performative'].iloc[j]:
            intra_class_sims.append(sim)
        else:
            inter_class_sims.append(sim)

# Package into a DataFrame for Seaborn
plot_data = pd.DataFrame({
    'Cosine Similarity': intra_class_sims + inter_class_sims,
    'Relationship': ['Intra-class (Same Performative)'] * len(intra_class_sims) + 
                    ['Inter-class (Different Performative)'] * len(inter_class_sims)
})

# 6. Visualization: Clean, Minimalist KDE Plot
print("Generating KDE distribution plot...")

# Use a clean, academic style
sns.set_theme(style="whitegrid", rc={"axes.edgecolor": "black", "axes.linewidth": 1})
plt.figure(figsize=(5,4))

# Plot the Kernel Density Estimate
sns.kdeplot(
    data=plot_data, 
    x='Cosine Similarity', 
    hue='Relationship', 
    fill=True, 
    common_norm=False, 
    palette=['#1f77b4', '#d62728'], # High-contrast Blue and Red
    alpha=0.5,
    linewidth=2
)

# Minimalist Formatting
plt.title("Density of Pairwise Cosine Similarities", fontsize=14, fontweight='bold', pad=15)
plt.ylabel("Density", fontsize=12, fontweight='bold')
plt.xlabel("Cosine Similarity", fontsize=12, fontweight='bold')

# Remove the automatic legend title for a cleaner look
plt.legend(title=None, labels=['Inter-class (Different Performative)', 'Intra-class (Same Performative)'], 
           loc='upper left', fontsize=11, frameon=True, edgecolor='black')

plt.xlim(0.85, 1.0) # Adjust this if your similarities fall lower, but BERT usually sits high
plt.tight_layout()

# 7. Save output
output_filename = "kde.png"
plt.savefig(output_filename, dpi=300, bbox_inches='tight')
print(f"Success. Plot saved to: {output_filename}")