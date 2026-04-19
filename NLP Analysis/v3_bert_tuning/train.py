import os
import argparse
import csv
import pandas as pd
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader
from transformers import BertTokenizerFast, BertModel
from sklearn.metrics import accuracy_score
import warnings

warnings.filterwarnings("ignore")
os.environ["CUDA_VISIBLE_DEVICES"] = "1"

# 1. Fixed Closed Sets
PERFORMATIVES = ['accuse', 'defend', 'suspect', 'agree', 'interrogate', 'deflect']
PERF_TO_ID = {perf: i for i, perf in enumerate(PERFORMATIVES)}

PLAYERS = ['alice', 'bob', 'charlie', 'arthur', 'bella', 'caleb', 'david', 
           'eve', 'frank', 'grace', 'heidi', 'ivan', 'judy', 'kevin', 'leo']
TARGET_TO_ID = {t: i for i, t in enumerate(PLAYERS)}

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
        
        perf_logits = self.perf_classifier(pooled_output)
        target_logits = self.target_classifier(pooled_output)
        
        return perf_logits, target_logits

def train_and_evaluate(args):
    os.makedirs(args.model_dir, exist_ok=True)
    log_csv_path = os.path.join(args.model_dir, "step_logs.csv")
    
    with open(log_csv_path, mode='w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow([
            'global_step', 'total_samples', 'epoch',
            'train_loss_perf', 'train_loss_target', 'train_loss_total',
            'val_loss_perf', 'val_loss_target', 'val_loss_total',
            'val_perf_acc', 'val_target_acc'
        ])

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Hardware utilized: {device}")

    tokenizer = BertTokenizerFast.from_pretrained('bert-base-uncased')
    
    train_df = pd.read_csv(args.train_csv).dropna(subset=['message', 'performative', 'target'])
    train_df['target'] = train_df['target'].str.lower().str.strip()
    
    val_df = pd.read_csv(args.test_csv).dropna(subset=['message', 'performative', 'target'])
    val_df['target'] = val_df['target'].str.lower().str.strip()
    
    train_loader = DataLoader(SimpleDualDataset(train_df, tokenizer), batch_size=args.batch_size, shuffle=True)
    val_loader = DataLoader(SimpleDualDataset(val_df, tokenizer), batch_size=args.batch_size)
    
    model = SimpleJointBERT(len(PERFORMATIVES), len(PLAYERS)).to(device)
    loss_fn = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=2e-5)

    best_val_loss = float('inf')
    steps_no_improve = 0
    
    global_step = 0
    total_samples = 0
    
    running_train_loss_p = 0.0
    running_train_loss_t = 0.0

    print(f"Training: Batch Size = {args.batch_size} | Eval Interval = {args.eval_steps} steps")
    print("-" * 100)
    
    model.train()
    for epoch in range(args.epochs):
        for batch in train_loader:
            optimizer.zero_grad()
            
            input_ids = batch['input_ids'].to(device)
            attention_mask = batch['attention_mask'].to(device)
            p_labels = batch['perf_label'].to(device)
            t_labels = batch['target_label'].to(device)
            
            p_logits, t_logits = model(input_ids, attention_mask)
            
            loss_p = loss_fn(p_logits, p_labels)
            loss_t = loss_fn(t_logits, t_labels)
            
            total_loss = loss_p + loss_t
            total_loss.backward()
            optimizer.step()
            
            running_train_loss_p += loss_p.item()
            running_train_loss_t += loss_t.item()
            
            global_step += 1
            total_samples += input_ids.size(0)
            
            # --- EVALUATION BLOCK ---
            if global_step % args.eval_steps == 0:
                avg_train_loss_p = running_train_loss_p / args.eval_steps
                avg_train_loss_t = running_train_loss_t / args.eval_steps
                avg_train_loss_tot = avg_train_loss_p + avg_train_loss_t
                
                # Reset rolling loss
                running_train_loss_p = 0.0
                running_train_loss_t = 0.0
                
                model.eval()
                val_loss_p = 0
                val_loss_t = 0
                p_preds, p_trues = [], []
                t_preds, t_trues = [], []
                
                with torch.no_grad():
                    for val_batch in val_loader:
                        v_input_ids = val_batch['input_ids'].to(device)
                        v_attention_mask = val_batch['attention_mask'].to(device)
                        v_p_labels = val_batch['perf_label'].to(device)
                        v_t_labels = val_batch['target_label'].to(device)
                        
                        v_p_logits, v_t_logits = model(v_input_ids, v_attention_mask)
                        
                        l_p = loss_fn(v_p_logits, v_p_labels)
                        l_t = loss_fn(v_t_logits, v_t_labels)
                        
                        val_loss_p += l_p.item()
                        val_loss_t += l_t.item()
                        
                        p_preds.extend(torch.argmax(v_p_logits, dim=1).cpu().numpy())
                        p_trues.extend(v_p_labels.cpu().numpy())
                        t_preds.extend(torch.argmax(v_t_logits, dim=1).cpu().numpy())
                        t_trues.extend(v_t_labels.cpu().numpy())

                avg_val_loss_p = val_loss_p / len(val_loader)
                avg_val_loss_t = val_loss_t / len(val_loader)
                avg_val_loss_tot = avg_val_loss_p + avg_val_loss_t
                
                val_perf_acc = accuracy_score(p_trues, p_preds)
                val_target_acc = accuracy_score(t_trues, t_preds)

                print(f"Step {global_step:05d} (Epoch {epoch+1}) | Samples: {total_samples} | "
                      f"Train Loss: {avg_train_loss_tot:.4f} | Val Loss: {avg_val_loss_tot:.4f} | "
                      f"P-Acc: {val_perf_acc:.4f} | T-Acc: {val_target_acc:.4f}")

                with open(log_csv_path, mode='a', newline='') as f:
                    csv.writer(f).writerow([
                        global_step, total_samples, epoch+1,
                        avg_train_loss_p, avg_train_loss_t, avg_train_loss_tot,
                        avg_val_loss_p, avg_val_loss_t, avg_val_loss_tot,
                        val_perf_acc, val_target_acc
                    ])

                step_model_path = os.path.join(args.model_dir, f"model_step_{global_step}.pth")
                torch.save(model.state_dict(), step_model_path)

                if avg_val_loss_tot < best_val_loss:
                    best_val_loss = avg_val_loss_tot
                    steps_no_improve = 0
                else:
                    steps_no_improve += args.eval_steps
                    # Patience is now evaluated in total steps, not epochs
                    if steps_no_improve >= (args.patience_steps):
                        print(f"\nEarly Stopping triggered at Step {global_step}.")
                        return
                
                # Re-enter train mode after validation
                model.train()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--train-csv", type=str, default="train.csv")
    parser.add_argument("--test-csv", type=str, default="test.csv")
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--batch_size", type=int, default=256)
    parser.add_argument("--eval_steps", type=int, default=10, help="Run validation every N steps.")
    parser.add_argument("--patience_steps", type=int, default=1000, help="Stop if no improvement for N steps.")
    parser.add_argument("--model_dir", type=str, default="TRAIN_DEFAULT")
    args = parser.parse_args()
    
    train_and_evaluate(args)