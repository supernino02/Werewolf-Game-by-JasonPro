# JasonPro Codebase Map

This document explains every directory and file currently present in `JasonPro`.

## Root

- `build.gradle`: Gradle build configuration (JaCaMo dependency, JavaFX modules, UI theme dependency, JSON dependency, and `run` task that launches `werewolf.jcm`).
- `werewolf.jcm`: JaCaMo MAS descriptor; defines narrator and player agents, role beliefs, temper values, selected reasoners, and runtime settings overrides.
- `CODEBASE_MAP.md`: This file; directory and file purpose map for the JasonPro codebase.
- `SETUP_AND_JCM_SETTINGS_GUIDE.md`: Setup and startup guide, `.jcm` definition guide, and settings behavior reference.

## BERT_API/

Purpose: Python inference service used by Jason internal actions to classify speech performatives.

- `BERT_API/inference_API.py`: FastAPI server that loads a BERT-based model and exposes POST `/predict` for performative and target inference.
- `BERT_API/model.pth`: Trained PyTorch weights loaded by `inference_API.py` at startup.

## agents/

Purpose: AgentSpeak (.asl) logic for narrator, players, and shared helper modules.

### agents/narrator/

Purpose: Narrator (game master) orchestration, transitions, and configuration.

- `agents/narrator/narrator.asl`: Main narrator behavior; starts/ends game, resolves votes/hunts, synchronization, dispatch, and logging export orchestration.
- `agents/narrator/init_narrator.asl`: Settings schema/defaults, override handling, setting validation/constraints, service ping checks, and startup setup.

#### agents/narrator/core/

Purpose: Core game model and static narration templates.

- `agents/narrator/core/game_graph.asl`: State-machine graph (nodes/edges) for game flow, transitions, and win conditions.
- `agents/narrator/core/game_narration.asl`: Static base messages and narrator style templates used for phase/action narration.

#### agents/narrator/log_manager/

Purpose: Shared log creation and filtered retrieval rules.

- `agents/narrator/log_manager/logger.asl`: Annotated event log, audience compression, access control, and chunk extraction for context consumers.

### agents/players/

Purpose: Player initialization, wrappers, reasoner routing, and behavior modules.

- `agents/players/init_player.asl`: Player boot process, reasoner resolution, dynamic module include routing, UI mode selection, and boot cleanup.
- `agents/players/player_wrapper.asl`: Common player action handlers (phase/action processing, submit helpers, UI synchronization, and delay logic).
- `agents/players/guess_performative.asl`: BERT-based speech-act guess workflow for linguistic-relativity mode.

#### agents/players/first_order_beliefs/

Purpose: Mutable belief state and trait updates.

- `agents/players/first_order_beliefs/traits.asl`: Trait update primitives and trait-history logging hooks.

#### agents/players/log_getter/

Purpose: Fetch/caching adapter for narrator logs.

- `agents/players/log_getter/getter.asl`: Cached and uncached log retrieval plans, pending-request synchronization, and debug traces.

#### agents/players/reasoners/human/

Purpose: Human-in-the-loop decision path.

- `agents/players/reasoners/human/interface.asl`: UI-driven actions for vote/hunt/speech inputs and no-op opinion updates.

#### agents/players/reasoners/llm/

Purpose: LLM-driven decision plans.

- `agents/players/reasoners/llm/choose_victim.asl`: Wolf hunt target selection with retries and fallback to random valid target.
- `agents/players/reasoners/llm/choose_vote.asl`: Village vote target selection with retries and fallback behavior.
- `agents/players/reasoners/llm/decide_to_speak.asl`: LLM-based intent-to-speak decision plan.
- `agents/players/reasoners/llm/time_to_speak.asl`: LLM performative selection and message generation for speaking turn.
- `agents/players/reasoners/llm/update_opinions.asl`: LLM-driven trait/mood update extraction and application.

#### agents/players/reasoners/symbolic/

Purpose: Rule-based (non-LLM) decision path.

- `agents/players/reasoners/symbolic/interface.asl`: Symbolic action interface for vote/hunt/speech with optional LLM message generation toggle.
- `agents/players/reasoners/symbolic/utils.asl`: Shared helper predicates for symbolic modules.

##### agents/players/reasoners/symbolic/strategies/

Purpose: Role-specific symbolic strategy sets.

- `agents/players/reasoners/symbolic/strategies/villager.asl`: Villager symbolic plans (speech, performative choice, votes, and opinion updates).
- `agents/players/reasoners/symbolic/strategies/wolf.asl`: Wolf symbolic plans (deception, hunt/vote behavior, and opinion updates).

### agents/shared/

Purpose: Generic predicates and prompt-context translation modules used by narrator/players.

- `agents/shared/utils.asl`: Common predicates for role/player list derivation and random target helpers.

#### agents/shared/llm_context/

Purpose: Convert game state into prompt-friendly text blocks.

- `agents/shared/llm_context/log_2_context.asl`: Converts log chunks into LLM-readable context strings.
- `agents/shared/llm_context/performatives_2_context.asl`: Natural-language descriptions for performatives and intents.
- `agents/shared/llm_context/temper_2_context.asl`: Temper/personality to text conversion rules.
- `agents/shared/llm_context/traits_2_context.asl`: Trait/opinion structures to context text conversion.

## src/

Purpose: Java internal actions, service adapters, UI framework, and agent core classes.

### src/bert/

Purpose: Java bridge for BERT API integration.

- `src/bert/guess_performative.java`: Internal action that requests performative prediction and injects result belief.
- `src/bert/ping_bert.java`: Internal action wrapper around BERT API health/warmup ping.
- `src/bert/python_BERT_API.java`: HTTP client for BERT API, optional auto-start of local Python server, and call logging.

### src/llm/

Purpose: Java bridge for Ollama LLM calls and related internal actions.

- `src/llm/choose_performative.java`: Internal action to ask LLM for speech performative output.
- `src/llm/choose_victim.java`: Internal action to ask LLM for hunt victim output.
- `src/llm/choose_vote.java`: Internal action to ask LLM for vote target output.
- `src/llm/decide_to_speak.java`: Internal action to ask LLM whether to speak.
- `src/llm/LLMUtils.java`: Prompt-builder and helper utilities shared by LLM actions.
- `src/llm/narrator_message.java`: Internal action to generate narrator-style phase text via LLM.
- `src/llm/OllamaAPI.java`: HTTP client for Ollama `/api/generate`, model warmup ping, and optional per-call JSON logging.
- `src/llm/ping_llm.java`: Internal action wrapper around LLM ping/model load check.
- `src/llm/player_message.java`: Internal action to generate player utterance text via LLM.
- `src/llm/update_opinions.java`: Internal action to generate structured opinion/mood updates via LLM.

### src/ui/

Purpose: JavaFX UI core, UI widgets, and AgentSpeak internal actions used to control UI state.

#### src/ui/core/

- `src/ui/core/AgentGUI.java`: Main GUI controller; window lifecycle, interaction state, chat rendering, vote/intent recap behavior, and settings-aware display toggles.

#### src/ui/components/

- `src/ui/components/ChatPanel.java`: Chat/feed panel for phase messages, player messages, and action recaps.
- `src/ui/components/CirclePanel.java`: Circular player layout widget with icons, arrows, and vote overlays.
- `src/ui/components/HeatmapPanel.java`: Matrix-style view for opinions/traits with interactive highlighting.
- `src/ui/components/InteractionPanel.java`: Input/action panel for speaking, voting, and status controls.

#### src/ui/actions/

- `src/ui/actions/ask_speak.java`: Internal action to open the "speak?" prompt for active human UI.
- `src/ui/actions/init_gui.java`: Internal action that initializes GUI with role/player list and setting-derived bounds/delays.
- `src/ui/actions/intent_to_speak.java`: Internal action to render player speaking-intent markers.
- `src/ui/actions/mark_dead.java`: Internal action to mark a player as dead in UI widgets.
- `src/ui/actions/phase_message.java`: Internal action to push phase title/body messages to chat panel.
- `src/ui/actions/player_message.java`: Internal action to render player utterances and performative annotations.
- `src/ui/actions/request_speech.java`: Internal action to open speech performative + target + message input flow.
- `src/ui/actions/set_status.java`: Internal action to set local outcome state (dead/victory/defeat).
- `src/ui/actions/show_vote.java`: Internal action to render vote/hunt action updates.
- `src/ui/actions/start_vote.java`: Internal action to begin vote UI mode with correct phase context.
- `src/ui/actions/typing.java`: Internal action to show typing indicator for speaking agents.
- `src/ui/actions/UIUtils.java`: Utility helpers for UI presence checks, string/list conversion, and dynamic delay lookup.
- `src/ui/actions/updateOwnTrait.java`: Internal action to refresh local trait/mood display values.
- `src/ui/actions/update_icons.java`: Internal action to update role/death icons for one or more players.
- `src/ui/actions/update_trait.java`: Internal action to refresh displayed trait values for target players.

### src/utils/

Purpose: Cross-cutting utility internal actions for timestamping and persistence.

- `src/utils/ms_timestamp.java`: Internal action returning current timestamp in milliseconds.
- `src/utils/save_narrator_log.java`: Internal action to serialize narrator log JSON.
- `src/utils/save_player_log.java`: Internal action to serialize per-player log JSON.

### src/vesna/

Purpose: Core custom agent architecture and temperament abstractions.

- `src/vesna/apply_mood_effect.java`: Internal action that applies mood delta effects to an agent.
- `src/vesna/IntentionWrapper.java`: Wrapper for intentions with temperament-aware scoring/comparison support.
- `src/vesna/OptionWrapper.java`: Wrapper for candidate options/plans with temperament-aware selection metadata.
- `src/vesna/ProAgent.java`: Custom agent class extending Jason Agent with temperament-aware deliberation.
- `src/vesna/Temper.java`: Value object for personality/mood traits (paranoia, individualism, stress, exposure).
- `src/vesna/TemperSelectable.java`: Interface marker used by temperament-aware wrappers.

## Notes

- The current folder contains source/config assets and BERT model assets; generated build output directories are created by Gradle when you build/run.
- Runtime game logs are written under the path configured by settings (`path_log_directory` and `path_game_stats`) when stats logging is enabled.
