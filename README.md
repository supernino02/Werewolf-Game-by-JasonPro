# Werewolf
``No friends? No problem!''

Werewolf is a multi-agent social deduction project built on Jason/JaCaMo, with support for:
- symbolic reasoning,
- local LLM-driven reasoning through Ollama,
- human-in-the-loop interaction through a JavaFX UI,
- NLP-based speech-act reconstruction using a BERT service.

<p align="center">
  <img src="https://raw.githubusercontent.com/supernino02/Werewolf-Game-by-JasonPro/main/Report/images/UI.PNG" alt="Werewolf UI" width="600">
  <br>
  <em>The JavaFX interface facilitating human-agent interaction during gameplay.</em>
</p>

This repository also includes the full NLP experimentation pipeline, simulation archives, and the LaTeX report used to document results.

## Start Here

To avoid duplicating concepts, this README stays high-level and points to the detailed Game documentation:

- Setup, startup, `.jcm` settings, and runtime behavior:
	[Game/SETUP_AND_JCM_SETTINGS_GUIDE.md](Game/SETUP_AND_JCM_SETTINGS_GUIDE.md)
- Full architecture and file-by-file map of the game runtime:
	[Game/CODEBASE_MAP.md](Game/CODEBASE_MAP.md)

## Repository Overview

| Area | Purpose | Main Entry Points |
|---|---|---|
| [Game](Game) | Runtime multi-agent Werewolf system (Jason/JaCaMo + Java + UI + LLM + BERT bridge). | [Game/build.gradle](Game/build.gradle), [Game/werewolf.jcm](Game/werewolf.jcm), [Game/BERT_API/inference_API.py](Game/BERT_API/inference_API.py) |
| [NLP Analysis](NLP%20Analysis) | Dataset creation, baseline analyses, and BERT tuning for performative/target prediction. | [NLP Analysis/datasets/create_speech_dataset.py](NLP%20Analysis/datasets/create_speech_dataset.py), [NLP Analysis/v3_bert_tuning/train.py](NLP%20Analysis/v3_bert_tuning/train.py), [NLP Analysis/v3_bert_tuning/test.py](NLP%20Analysis/v3_bert_tuning/test.py) |
| [Simulations](Simulations) | Archived game runs and post-processing scripts for plots/statistics. | [Simulations/images.py](Simulations/images.py), [Simulations/LLM](Simulations/LLM), [Simulations/SYMBOLIC](Simulations/SYMBOLIC) |
| [Report](Report) | Full technical write-up and figures (LaTeX project). | [Report/main.tex](Report/main.tex), [Report/NLP.tex](Report/NLP.tex), [Report/LLM.tex](Report/LLM.tex), [Report/RESULTS.tex](Report/RESULTS.tex) |

## How The Pieces Fit Together

1. The game runtime in [Game](Game) generates interaction logs from symbolic/LLM/human-driven play.
2. Those logs can be transformed into datasets with [NLP Analysis/datasets/create_speech_dataset.py](NLP%20Analysis/datasets/create_speech_dataset.py).
3. BERT models are trained/evaluated in [NLP Analysis/v3_bert_tuning](NLP%20Analysis/v3_bert_tuning).
4. Runtime speech-act inference is served by [Game/BERT_API/inference_API.py](Game/BERT_API/inference_API.py), which loads automatically a trained `model.pth` in `Game/BERT_API` from huggingface.
5. Simulation batches are analyzed via [Simulations/images.py](Simulations/images.py), and final documentation is maintained in [Report](Report).

## Quick Start (Run The Game)

Detailed requirements are in [Game/SETUP_AND_JCM_SETTINGS_GUIDE.md](Game/SETUP_AND_JCM_SETTINGS_GUIDE.md). Minimal checklist:

- Java JDK 17 or 21
- Gradle available in PATH (no wrapper is included)
- Ollama running locally for LLM features
- Python + required packages for BERT features (`torch`, `transformers`, `fastapi`, `pydantic`, `uvicorn`)

Run the game:

```bash
cd Game
gradle run
```

Optional: manually start BERT inference service (the Java bridge also attempts auto-start if needed):

```bash
cd Game/BERT_API
python inference_API.py
```

## Typical NLP Workflow

Build dataset from game logs:

```bash
cd "NLP Analysis/datasets"
python create_speech_dataset.py --input-dir ../RAW_GAMES --output-csv merged.csv --split 0.2 --balance-perf --balance-target --save-stats --at-least 5
```

Train a joint BERT model:

```bash
cd "NLP Analysis/v3_bert_tuning"
python train.py --train-csv ../datasets/train.csv --test-csv ../datasets/test.csv --model_dir TRAIN/run_01
```

Evaluate and visualize latent space:

```bash
cd "NLP Analysis/v3_bert_tuning"
python test.py --test-csv ../datasets/test.csv --model-path TRAIN/run_01/model_step_XXXX.pth --output-plot tsne_head_spaces.png
```

Alternative baseline/kernel experiments are in:

- [NLP Analysis/v1_semantic_clustering](NLP%20Analysis/v1_semantic_clustering)
- [NLP Analysis/v2_bert_kernel](NLP%20Analysis/v2_bert_kernel)

## Typical Simulation Workflow

Generate plots from archived runs:

```bash
cd Simulations
python images.py --data-dir LLM --output-dir LLM_IMAGES --bin-days 2
python images.py --data-dir SYMBOLIC --output-dir SYMBOLIC_IMAGES --bin-days 2
```

## Report Workflow

The final write-up is in [Report](Report). Build from [Report/main.tex](Report/main.tex), for example:

```bash
cd Report
latexmk -pdf main.tex
```

## First Files To Edit

- [Game/werewolf.jcm](Game/werewolf.jcm): player roster, roles, reasoners, and runtime settings.
- [Game/agents](Game/agents): AgentSpeak logic for narrator and players.
- [Game/src](Game/src): Java internal actions (LLM, BERT, UI, utilities).
- [NLP Analysis](NLP%20Analysis): data and model experimentation.

## Notes

- Generated artifacts such as build output, raw game exports, and some model/log files are ignored by [.gitignore](.gitignore).
- For complete setting semantics and architecture internals, use the two linked Game docs as the canonical reference.
