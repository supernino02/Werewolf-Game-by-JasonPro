import os
import torch
import torch.nn as nn
from transformers import BertTokenizerFast, BertModel
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn
import warnings
from huggingface_hub import hf_hub_download

warnings.filterwarnings("ignore")

# 1. Configuration and Mappings
REPO_ID = "supernino02/WerewolfBert"
FILENAME = "custom_model.pth"
LOCAL_DIR = "./model_storage"  # Folder where the model will be persisted
MAX_LEN = 128

# If your repo is Private, you can set an environment variable before running the script:
HF_TOKEN = os.environ.get("HF_TOKEN") 

PERFORMATIVES = ['accuse', 'defend', 'suspect', 'agree', 'interrogate', 'deflect']
ID_TO_PERF = {i: perf for i, perf in enumerate(PERFORMATIVES)}

PLAYERS = ['alice', 'bob', 'charlie', 'arthur', 'bella', 'caleb', 'david', 
           'eve', 'frank', 'grace', 'heidi', 'ivan', 'judy', 'kevin', 'leo']
ID_TO_TARGET = {i: t for i, t in enumerate(PLAYERS)}

# 2. Re-declare the Architecture
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

# 3. Setup FastAPI and Global State
app = FastAPI(title="Game NLP Inference Engine")

device = None
model = None
tokenizer = None

@app.on_event("startup")
def load_model():
    global device, model, tokenizer
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Loading model onto {device}...")
    
    # Ensure local directory exists
    os.makedirs(LOCAL_DIR, exist_ok=True)
    local_path = os.path.join(LOCAL_DIR, FILENAME)
    
    # 1. Download only if it doesn't exist locally
    if not os.path.exists(local_path):
        print(f"Model not found locally. Downloading from {REPO_ID}...")
        try:
            hf_hub_download(
                repo_id=REPO_ID, 
                filename=FILENAME, 
                token=HF_TOKEN,
                local_dir=LOCAL_DIR,
                local_dir_use_symlinks=False
            )
            print("Download complete.")
        except Exception as e:
            raise RuntimeError(f"Failed to download weights: {e}")
    else:
        print("Model found locally. Loading immediately...")

    print(f"Downloading/Loading tokenizer from {REPO_ID}...")
    tokenizer = BertTokenizerFast.from_pretrained(REPO_ID, token=HF_TOKEN)
    
    model = SimpleJointBERT(len(PERFORMATIVES), len(PLAYERS)).to(device)
    
    print(f"Applying weights from {local_path}...")
    model.load_state_dict(torch.load(local_path, map_location=device, weights_only=True))
    model.eval()
    print("Model successfully loaded and ready for inference.")

# 4. Define API Schema
class Query(BaseModel):
    message: str

# 5. Define Inference Endpoint
@app.post("/predict")
def predict(query: Query):
    if not query.message:
        raise HTTPException(status_code=400, detail="Message cannot be empty")

    try:
        encoding = tokenizer(
            query.message, 
            max_length=MAX_LEN, 
            padding='max_length',
            truncation=True, 
            return_tensors='pt'
        )

        input_ids = encoding['input_ids'].to(device)
        attention_mask = encoding['attention_mask'].to(device)

        with torch.no_grad():
            p_logits, t_logits = model(input_ids, attention_mask)

        p_idx = torch.argmax(p_logits, dim=1).item()
        t_idx = torch.argmax(t_logits, dim=1).item()

        return {
            "performative": ID_TO_PERF[p_idx],
            "target": ID_TO_TARGET[t_idx],
            "message": query.message
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=41818)