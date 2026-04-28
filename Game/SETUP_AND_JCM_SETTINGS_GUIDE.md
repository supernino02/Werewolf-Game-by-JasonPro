# JasonPro Setup, Startup, and .jcm Settings Guide

This guide explains how to set up and start the game, how `werewolf.jcm` is defined, and how `setting(...)` is used by narrator and players.

## 1) Prerequisites

## Java and Gradle

- Java JDK 17 or 21 installed (JavaFX 21 is configured in `build.gradle`).
- Gradle installed and available in PATH (this project does not include a Gradle wrapper script).

## Ollama (required for LLM features)

- Ollama must be installed and running.
- JasonPro expects Ollama at:
  - `http://localhost:11434/api/generate`
- The selected model in settings (for example `gemma4:31b`) must already be available in Ollama.

If Ollama is not reachable, narrator startup marks `llm_disabled(true)` and the system falls back to non-LLM behavior.

## Python + BERT dependencies (required for speech-act misalignment mode)

- Python installed and available in PATH.
- Required Python libraries for `BERT_API/inference_API.py`:
  - `torch`
  - `transformers`
  - `fastapi`
  - `pydantic`
  - `uvicorn`
- `BERT_API/model.pth` must exist.

Suggested install example:

```bash
pip install torch transformers fastapi pydantic uvicorn "huggingface_hub[hf_xet]"
```

## 2) How to Start a Game

From `JasonPro`:

```bash
gradle run
```

`build.gradle` defines task `run` that launches JaCaMo with:

```text
jacamo.infra.JaCaMoLauncher werewolf.jcm
```

## Startup sequence at runtime

1. Narrator agent starts with goal `setup`.
2. Narrator validates and applies settings defaults/overrides.
3. Narrator applies constraints:
   - fast mode (`disable_delay`) handling,
   - logging enable/disable handling,
   - optional LLM ping (`llm.ping_llm(...)`),
   - optional BERT ping (`bert.ping_bert(...)`).
4. Narrator broadcasts shared settings to all players.
5. Each player resolves its reasoner and loads corresponding modules.
6. Game graph transitions begin (`setup -> inform_pack -> start_game -> ...`).

## 3) How to Define `werewolf.jcm`

`werewolf.jcm` is the MAS deployment descriptor.

Basic structure:

```jcm
mas vesna {
    agent narrator : narrator.asl {
        goals  : setup
        beliefs: setting(...), setting(...)
    }

    agent alice : init_player.asl {
        instances : 1
        ag-class: vesna.ProAgent
        temper: temper(paranoia(...), individualism(...), stress(...)[mood], exposure(...)[mood])
        strategy: most_similar
        beliefs: iam(wolf), reasoner(symbolic)
    }

    asl-path: "agents", "agents/narrator", "agents/players"
}
```

## What each player definition controls

- `agent <name> : init_player.asl`: chooses player bootstrap script.
- `ag-class: vesna.ProAgent`: uses the custom ProAgent architecture.
- `temper: temper(...)`: base personality and mood values.
- `strategy: most_similar`: Jason option selection strategy.
- `beliefs: iam(...)`: initial role belief (`wolf` or `villager`).
- `beliefs: reasoner(...)`: reasoning mode (`symbolic`, `llm`, or `human`).

## Narrator settings placement

Narrator settings are provided in narrator `beliefs` as `setting(...)` terms. These override defaults from `agents/narrator/init_narrator.asl`.

## 4) `setting(...)` Semantics and Scope

In narrator defaults, settings are annotated as:

- `[local]`: narrator-only, not broadcast.
- `[share]`: broadcast to all players.

Players also protect local overrides:

- If a player already has a local setting (`source(self)`), incoming narrator shared setting with same functor is ignored.

This allows per-player custom override behavior in `.jcm`.

## 5) Complete Settings Reference

Below are settings defined in narrator defaults plus player-specific setting hooks.

## Core execution and timing

- `random_seed(none)` [local]
  - Purpose: deterministic run when set to a value.
  - Used by: narrator setup.

- `max_log_entries_retrieved(100)` [local]
  - Purpose: hard upper bound for narrator log chunk extraction.
  - Used by: narrator log manager when creating context windows for players/LLM.

- `responsive_delay_range_ms(500, 3000)` [share]
  - Purpose: think-time delay for player decisions.
  - Used by: `players/player_wrapper.asl` in `!delay_for_realism`.

- `writing_delay_ms(30)` [share]
  - Purpose: typing/message animation delay factor.
  - Used by: narrator transition pacing and UI rendering.

- `phase_transition_delay_ms(500)` [local]
  - Purpose: minimum pause between graph transitions.
  - Used by: narrator transition execution.

- `disable_delay(false)` [share]
  - Purpose: fast mode toggle.
  - Effect when true: all delays set to zero and LLM is force-disabled.

## Trait and personality calibration

- `trait_bounds(-1.0, 1.0, 2)` [share]
  - Purpose: min/max and precision for trait display/normalization.
  - Used by: UI initialization and trait-processing modules.

- `trait_init_noise(0.05)` [share]
  - Purpose: initial noise factor applied to personality trait setup.
  - Used by: player trait initialization logic.

## Game-flow controls

- `avoid_ties_in_votes(false)` [local]
  - Purpose: optional random tie breaker in tally logic.

- `max_hunt_rounds(3)` [local]
  - Purpose: max retry rounds for wolf hunt tie loops.

- `max_discussion_turns(7)` [local]
  - Purpose: max speaking rounds before forced vote.

## LLM controls

- `llm_model("gemma4:31b")` [local]
  - Purpose: Ollama model id for LLM calls.

- `max_llm_retry(3)` [share]
  - Purpose: retry budget for invalid LLM outputs in player plans.

- `llm_narrator(true)` [local]
  - Purpose: narrator message generation by LLM (else static templates).

- `narrator_chunk_log_size(5)` [local]
  - Purpose: number of recent log entries fed into narrator LLM context.

- `set_narrator_style(none)` [local]
  - Purpose: optional custom narrator style prompt block.

- `llm_player_message(true)` [share]
  - Purpose: whether player speech message text is generated by LLM.

- `default_player_message("...")` [share]
  - Purpose: fallback text when player LLM messaging is disabled.

- `llm_disabled(false)` [share]
  - Purpose: internal runtime gate for all LLM-dependent features.
  - Set automatically on failure conditions.

## Speech-act / BERT controls

- `allow_speech_act_misalignment(true)` [share]
  - Purpose: enable performative guessing pipeline (linguistic relativity mode).
  - When enabled, players may need BERT to classify performatives.

- `show_correct_performative(true)` [share]
  - Purpose: UI debug display of true performative.

- `save_bert_calls(false)` [share]
  - Purpose: save BERT API calls to logs when stats logging is active.

## UI recap controls

- `ui_show_vote_recap(true)` [share]
  - Purpose: chat recap of vote actions.

- `ui_show_hunt_recap(true)` [share]
  - Purpose: chat recap of hunt actions.

- `ui_show_intent_to_speak_recap(true)` [share]
  - Purpose: chat recap of speaking-intent actions.

## Logging and export controls

- `enable_stats_log(false)` [share]
  - Purpose: master switch for game stats export pipeline.

- `path_log_directory("GAME_STATS")` [local]
  - Purpose: base folder for generated game log run directories.

- `path_game_stats(none)` [share]
  - Purpose: active run output path; auto-generated when needed.

- `save_game_stats(true)` [share]
  - Purpose: save narrator and player JSON at game end.

- `save_trait_evolution(true)` [share]
  - Purpose: persist per-player trait timeline.

- `save_mood_evolution(true)` [share]
  - Purpose: persist per-player mood timeline.

- `save_llm_calls(false)` [share]
  - Purpose: save Ollama request/response JSON files.

- `exit_on_game_over(false)` [local]
  - Purpose: stop MAS automatically when winner is decided.

## Debug setting

- `debug_log(true)` [share]
  - Purpose: verbose runtime debug output in AgentSpeak console.

## Legacy/inconsistent key to be aware of

- `log_llm_calls(false)` [local] appears in defaults but is not used by current logging flow.
- Current effective key for call logging is `save_llm_calls(...)`.

## Player-specific optional setting

- `spectator_mode(true)` (optional, typically per-player in `.jcm` beliefs)
  - Purpose: enable passive UI mode for non-human/non-driving agents.

## 6) Why and When Settings Get Disabled

This is important for understanding "why something is off even if I enabled it in `.jcm`".

## A) Fast mode forces LLM off

If `disable_delay(true)`:

- `responsive_delay_range_ms -> (0,0)`
- `writing_delay_ms -> 0`
- `phase_transition_delay_ms -> 0`
- `llm_disabled -> true`

Reason: fast simulation mode is designed to skip realism delays and disable LLM path.

## B) LLM ping failure forces non-LLM mode

On narrator setup, if `llm.ping_llm(...)` fails:

- `llm_disabled -> true`
- then system enforces:
  - `llm_narrator(false)`
  - `llm_player_message(false)`
  - `allow_speech_act_misalignment(false)`
  - `llm_model` removed

Reason: avoid runtime blocking/failures when Ollama/model is unavailable.

## C) BERT ping failure disables speech-act misalignment

If `allow_speech_act_misalignment(true)` but `bert.ping_bert(...)` fails:

- `allow_speech_act_misalignment -> false`

Reason: performative-guess path depends on BERT API availability.

## D) Stats logging off disables related saves

If `enable_stats_log(false)`:

- `path_game_stats(none)`
- `save_game_stats(false)`
- `save_trait_evolution(false)`
- `save_mood_evolution(false)`
- `save_llm_calls(false)`

Reason: master switch keeps run lightweight and avoids file output.

## E) Player local overrides win

If a player already has local `setting(X(...))` and narrator broadcasts shared `setting(X(...))`:

- player keeps local value.

Reason: `init_player.asl` explicitly preserves local per-player overrides.

## 7) Ollama and BERT: Local vs Hosted API Alternative

## Local default mode

- Ollama endpoint is hardcoded in Java to localhost (`src/llm/OllamaAPI.java`).
- BERT endpoint is hardcoded in Java to localhost (`src/bert/python_BERT_API.java`).
- BERT client can auto-start local Python server (`python inference_API.py`) if unreachable.

## Hosted API alternative (without starting local server)

You can host APIs elsewhere and communicate with them instead of starting local services:

1. Host an Ollama-compatible endpoint and/or BERT `/predict` endpoint remotely.
2. Make JasonPro reach that endpoint either by:
   - reverse-proxy/port-forward to expected local URLs, or
   - updating Java URL constants to remote host and rebuilding.
3. If ping succeeds, local auto-start is not needed.

This matches the architecture: Jason only needs reachable HTTP endpoints; local process startup is just a convenience fallback.

## 9) Minimal Quickstart Checklist

1. Start Ollama and ensure model exists.
2. Ensure Python + BERT dependencies are installed.
3. From `JasonPro`, run `gradle run`.
4. If enabling speech-act misalignment, verify BERT ping succeeds.
5. Tune `werewolf.jcm` `setting(...)` and per-player `reasoner(...)` as needed.
