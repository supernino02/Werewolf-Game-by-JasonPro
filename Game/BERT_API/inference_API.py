import os
import torch
import torch.nn as nn
from transformers import BertTokenizerFast, BertModel
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn
import warnings
warnings.filterwarnings("ignore")


# 1. Configuration and Mappings
MODEL_PATH = "model.pth"
MAX_LEN = 128

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

# Global variables to hold model and tokenizer in memory
device = None
model = None
tokenizer = None

@app.on_event("startup")
def load_model():
    global device, model, tokenizer
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Loading model onto {device}...")
    
    tokenizer = BertTokenizerFast.from_pretrained('bert-base-uncased')
    model = SimpleJointBERT(len(PERFORMATIVES), len(PLAYERS)).to(device)
    
    if not os.path.exists(MODEL_PATH):
        raise RuntimeError(f"Weights not found at {MODEL_PATH}")
        
    model.load_state_dict(torch.load(MODEL_PATH, map_location=device))
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
        # Tokenize
        encoding = tokenizer(
            query.message, 
            max_length=MAX_LEN, 
            padding='max_length',
            truncation=True, 
            return_tensors='pt'
        )

        input_ids = encoding['input_ids'].to(device)
        attention_mask = encoding['attention_mask'].to(device)

        # Forward Pass
        with torch.no_grad():
            p_logits, t_logits = model(input_ids, attention_mask)

        # Argmax to get classes
        p_idx = torch.argmax(p_logits, dim=1).item()
        t_idx = torch.argmax(t_logits, dim=1).item()

        # Map back to strings
        predicted_perf = ID_TO_PERF[p_idx]
        predicted_target = ID_TO_TARGET[t_idx]

        return {
            "performative": predicted_perf,
            "target": predicted_target,
            "message": query.message
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    # Run the server on localhost:81818
    uvicorn.run(app, host="127.0.0.1", port=41818)