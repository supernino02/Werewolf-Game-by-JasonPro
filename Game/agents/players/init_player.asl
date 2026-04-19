{ include("players/player_wrapper.asl") }
{ include("players/log_getter/getter.asl") }
{ include("shared/utils.asl") }

//avoid the settings to  be overridden by the narrator's shared settings
+setting(Incoming)[source(narrator)]
    :   Incoming =.. [SettingName, _, _]
    &   setting(LocalValue)[source(self)]
    &   LocalValue =.. [SettingName, _, _]
    <-  .abolish(setting(Incoming));
        .print("[INFO] Local setting override kept: ", LocalValue, " (ignored shared ", Incoming, ").").

//---------------
// GENERIC PLAYER INITIALIZATION AND CONFIGURATION
//---------------
@configure_player
+!configure_player 
    :   .my_name(Me)
    &   iam(Role) 
    &   reasoner(OriginalReasoner)
    <-  //resolve the reasoner and handle llm fallbacks
        !resolve_reasoner(OriginalReasoner, ActiveReasoner);

        //delegate all ui logic
        !configure_ui(Role, ActiveReasoner);
        
        //inject the correct files based on the role and reasoner
        !load_modules(Role, ActiveReasoner);
        
        //debug info
        .print("[INFO] Player ", Me, " configured : \"", Role, "\" with reasoner \"", ActiveReasoner, "\".");
        
        //cleanup beliefs used for configuration
        !cleanup_boot_memory;
        
        //start the game 
        !init.

//something went wrong during the configuration
@configure_player_error
-!configure_player
    <-  if (not iam(_)) { .print("[ERROR] Missing iam(Role) belief."); }
        if (not reasoner(_)) { .print("[ERROR] Missing reasoner(Type) belief."); }
        if (iam(_) & reasoner(_)) { .print("[ERROR] Unknown error during player configuration."); }.


//---------------
// REASONER RESOLUTION
//---------------

//fallback triggered if llm is requested but disabled
@resolve_reasoner
+!resolve_reasoner(llm, symbolic)
    :   setting(llm_disabled(true))
    <-  .print("[WARNING] LLM not available (fallback to symbolic).");
        -+reasoner(symbolic).

//default case: return the originally requested reasoner
@resolve_reasoner_default
+!resolve_reasoner(R, R).


//---------------
// MODULE INJECTION ROUTING
//---------------

// 1. human reasoner
@load_modules_human
+!load_modules(_, human)
    <-  // core interface
        .include("players/reasoners/human/interface.asl");
        
        // llm context translation (if active)
        if (setting(llm_disabled(false))) {
            .include("shared/llm_context/log_2_context.asl");
            .include("shared/llm_context/performatives_2_context.asl");
        }.

// 2. llm reasoner
@load_modules_llm
+!load_modules(_, llm)
    <-  // core interface and intentions
        .include("players/reasoners/llm/choose_victim.asl");
        .include("players/reasoners/llm/choose_vote.asl");
        .include("players/reasoners/llm/decide_to_speak.asl");
        .include("players/reasoners/llm/time_to_speak.asl");
        .include("players/reasoners/llm/update_opinions.asl");
        
        // cognitive logic
        .include("players/first_order_beliefs/traits.asl");
        
        // llm context translation
        .include("shared/llm_context/log_2_context.asl");
        .include("shared/llm_context/performatives_2_context.asl");
        .include("shared/llm_context/temper_2_context.asl");
        .include("shared/llm_context/traits_2_context.asl");

        //only  if needed
        if (setting(allow_speech_act_misalignment(true)))  {
            .include("players/guess_performative.asl");
        }.

// 3. symbolic reasoner
@load_modules_symbolic
+!load_modules(Role, symbolic)
    <-  // core interface and strategy
        .include("players/reasoners/symbolic/interface.asl");
        .include("players/reasoners/symbolic/utils.asl");
        .concat("players/reasoners/symbolic/strategies/", Role, ".asl", SymbolicModule);
        .include(SymbolicModule);
        
        // cognitive logic
        .include("players/first_order_beliefs/traits.asl");

        if (setting(allow_speech_act_misalignment(true)))  {
            .include("players/guess_performative.asl");
        }
        
        // llm context translation (if active)
        if (setting(llm_disabled(false))) {
            .include("shared/llm_context/log_2_context.asl");
            .include("shared/llm_context/performatives_2_context.asl");
            .include("shared/llm_context/temper_2_context.asl");
            .include("shared/llm_context/traits_2_context.asl");
        }.

// strict error handling for unmapped reasoners
@load_modules_fallback
-!load_modules(_, Unknown)
    <-  .print("[ERROR] Unknown reasoner core type: ", Unknown).

//---------------
// BOOT MEMORY CLEANUP
//---------------

@cleanup_boot_memory
+!cleanup_boot_memory
    <-  //clean up one-shot boot plans from memory
        .remove_plan(configure_player);
        .remove_plan(configure_player_error);
        .remove_plan(configure_ui_active);
        .remove_plan(configure_ui_spectator);
        .remove_plan(configure_ui_none);

        .remove_plan(resolve_reasoner);
        .remove_plan(resolve_reasoner_default);
        .remove_plan(load_modules_human);
        .remove_plan(load_modules_llm);
        .remove_plan(load_modules_symbolic);
        .remove_plan(load_modules_fallback);

        .remove_plan(cleanup_boot_memory);

        //cleanup temporary settings
        .abolish(setting(spectator_mode(_)));

        if (setting(save_mood_evolution(false))) {
            .abolish(mood_log(_));
            .remove_plan(log_mood_update_active);
        };

        if (setting(save_trait_evolution(false))) {
            .abolish(trait_log(_));
            .remove_plan(log_trait_update_active);
        };

        if (setting(allow_speech_act_misalignment(false))) {
            .remove_plan(submit_performative_guess);
            .remove_plan(guess_performative); //if defined (human reasoner only)
        }

        if (not ui(_)) {
            .remove_plan(personality_ui_update);
        }.


//---------------
// UI DEFINITION AND INITIALIZATION
//---------------

//human reasoner uses the active ui
@configure_ui_active
+!configure_ui(Role, human)
    <-  +ui(active).

//others reasoners can use the spectator ui
@configure_ui_spectator
+!configure_ui(Role, _) 
    :   setting(spectator_mode(true))
    <-  +ui(passive);
        .print("[INFO] UI set to spectator mode.").

//fallback: no ui
@configure_ui_none
+!configure_ui(_, _).

//removed if no ui is used
@personality_ui_update
+my_personality(Trait, Value) 
    :   ui(_)
    <-  ui.actions.updateOwnTrait(Trait, Value, false). //not mood

@mood_ui_update
+my_mood(Trait, Value) 
    <-  !log_mood_update(Trait, Value);
        if (ui(_)) {ui.actions.updateOwnTrait(Trait, Value, true);}.

//---------------
// PLANS TO LOG THE STATS AT THE END OF THE GAME
//---------------

//save the historic of mood changes for statistical purposes, if the setting is active
mood_log([]).
@log_mood_update_active[atomic]
+!log_mood_update(Trait, Value)
    :   setting(save_mood_evolution(true))
    &   current_phase_uid(PhaseUID)
    &   mood_log(CurrentLog)
    <-  -+mood_log([ mood(Trait, Value)[phase_id(PhaseUID)] | CurrentLog ]).

// Fallback: If the setting is disabled or context is missing, do nothing safely
@log_mood_update_disabled
+!log_mood_update(_, _).

//save the files
+!export_player_log[source(narrator)]
    :   setting(path_game_stats(Dir))
    &   .my_name(Me)
    &   iam(Role)
    &   reasoner(Reasoner)
    <-  
        // 1. Safely fetch logs (default to empty list if the belief does not exist)
        if (setting(save_trait_evolution(true))) {
            ?trait_log(TL); 
            RawTraitLog = TL; 
        } else { RawTraitLog = []; };

        if (setting(save_mood_evolution(true))) {
            ?mood_log(ML); RawMoodLog = ML; 
        } else { RawMoodLog = []; };
        
        // 2. Reverse logs to chronological order
        .reverse(RawTraitLog, ChronoTraitLog);
        .reverse(RawMoodLog, ChronoMoodLog);
        
        // 3. Fetch base starting traits (returns [] automatically if none exist)
        .findall(trait(T, V), my_personality(T, V), BaseTraits);
        
        // 4. Determine File Path
        if (Dir == none) {
            .concat(Me, ".json", FilePath);
        } else {
            .concat(Dir, "/", Me, ".json", FilePath);
        };
        
        // 5. Execute Java Writer
        utils.save_player_log(Me, Role, Reasoner, BaseTraits, ChronoMoodLog, ChronoTraitLog, FilePath);
        .send(narrator, tell, end_game_logging_finished(Me)).

// Fallback if settings are missing or disabled
+!export_player_log[source(narrator)]
    :   .my_name(Me)
    <-  .send(narrator, tell, end_game_logging_finished(Me)).