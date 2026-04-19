// ========================================================
// --- SETTING TO OVERRIDE AND SHARE WITH OTHER AGENTS ---
// ========================================================
// local -> kept to the narrator itself
// share -> broadcast to all others
default_settings([
    random_seed(none)[local],                    //deterministic seed for reproducibility

    debug_log(true)[share],                      //show the log entries added and cached in agentspeak console

    max_log_entries_retrieved(100)[local],       //maximum chunk of log entries to retrieve for the LLM context )upper limit for performance reasons)

    trait_bounds(-1.0, 1.0, 2)[share],           //trait bounds definition
    trait_init_noise(0.05)[share],               //random noise added at the initialization of traits.

    responsive_delay_range_ms(500, 3000)[share], //delay to simulate "thinking"
    writing_delay_ms(30)[share],                 //delay to simulate the typing of the messages
    phase_transition_delay_ms(500)[local],       //delay between the phases (to let the players process the info and prepare for the next phase)

    avoid_ties_in_votes(false)[local],           //if true, th nerrator force a kill in case of tie
    max_hunt_rounds(3)[local],                   //retry limit for the hunt phase        
    max_discussion_turns(7)[local],              //retry limit for the discussion phase

    llm_model("gemma4:31b")[local],              //llm model to use (in ollama)
    max_llm_retry(3)[share],                     //number of retries of LLM calls
    log_llm_calls(false)[local],                 //save in files all the calls to the LLM

    llm_narrator(true)[local],                   //use the LLM to generate the narration
    narrator_chunk_log_size(5)[local],           //how many recent log entries to include in the context for the narration generation
    set_narrator_style(none)[local],             //custom style for the narrator: if none, will use the default one defined in the code

    llm_player_message(true)[share],             //use the LLM to generate the messages of the players
    default_player_message("Sadly :( the player is mute. ***gesticulates dramatically***")[share],     

    allow_speech_act_misalignment(true)[share],  //the players must infer the speech act of the messages.
    show_correct_performative(true)[share],      //for debug purposes, show the correct performative of the messages in the UI (if the allow_speech_act_misalignment is enabled)

    disable_delay(false)[share],                 //disable the llm to allow for the fast mode (for testing)
    llm_disabled(false)[share],                  //internal flag to track the effective status of the LLM

    ui_show_vote_recap(true)[share],             //show the recap of the vote phase
    ui_show_hunt_recap(true)[share],             //show the recap of the hunt phase
    ui_show_intent_to_speak_recap(true)[share],  //show the recap of the intent to speak phase

    enable_stats_log(false)[share],              //enable the log of the game stats and llm calls for later analysis
    path_log_directory("GAME_STATS")[local],     //base directory for the logs of the game stats and llm calls (if enabled)
    path_game_stats(none)[share],                //directory that contains the game stats: if none, will save in the current directory with a timestamped filename
    save_game_stats(true)[share],                //whether to save the game stats in a file at the end of the game
    save_trait_evolution(true)[share],           //whether to save the evolution of the traits of the players during the game (for analysis and debugging)
    save_mood_evolution(true)[share],            //whether to save the evolution of the moods of the players during the game (for analysis and debugging)
    save_llm_calls(false)[share],                //whether to save the calls to the LLM in files (for debugging and analysis)
    save_bert_calls(false)[share],               //whether to save the calls to BERT in files (for debugging and analysis)

    exit_on_game_over(false)[local]
]).


//remove settings not in whitelist
@remove_invalid_settings
+!remove_invalid_settings 
    :   default_settings(Defaults)
    <-  //define a whitelist
        .findall(SettingName, (.member(D, Defaults) & D =.. [SettingName, _, _]), AllowedSettings);

        //iterate over the defined one
        .findall(S, setting(S), ExistingSettings);        
        for (.member(CurrentSetting, ExistingSettings)) {
            //check the name
            CurrentSetting =.. [CurrentSettingName, _, _];
            //if not in whitelist remove and notify it
            if (not .member(CurrentSettingName, AllowedSettings)) {
                .print("[WARNING] Invalid setting (removed): ", CurrentSetting);
                .abolish(setting(CurrentSetting));
            }
        }.

//initialize missing settings
@apply_default_settings
+!apply_default_settings 
    :   default_settings(Defaults)
    <-  //get the existing settings
        .findall(S, setting(S), ValidSettings);
        //get the names of them
        .findall(Name, (.member(E, ValidSettings) & E =.. [Name, _, _]), ExistingSettingNames);

        //use the info from the default_settings
        for (.member(DefaultItem, Defaults)) {
            //univ it to get the name and args
            DefaultItem =.. [SettingName, DefArgs, Annots];

            //if not defined, simply declare it
            if (not .member(SettingName, ExistingSettingNames)) {
                +setting(DefaultItem);
            } else {
                //safely locate the existing setting
                .findall(S, (setting(S) & S =.. [SettingName, _, _]), [RuntimeOverride]);
                
                //extract the user's arguments
                RuntimeOverride =.. [SettingName, OverrideArgs, _];
                
                //destroy the old one
                .abolish(setting(RuntimeOverride));
                
                //rebuild it with the schema's annotations
                Reconstructed =.. [SettingName, OverrideArgs, Annots];
                +setting(Reconstructed);
            }
        }.

//setting manipulation to ensure internal consistency and apply constraints
@apply_setting_constraints
+!apply_setting_constraints
    <-  if (setting(disable_delay(true))) {
        // if disable_delay is set, remove all the slowing-down settings 
            .print("[INFO] Fast mode enabled.");
            
            //abolish and set new values
            .abolish(setting(responsive_delay_range_ms(_, _))); 
            +setting(responsive_delay_range_ms(0, 0)[share]);
            
            //abolish and set new setting
            .abolish(setting(writing_delay_ms(_))); 
            +setting(writing_delay_ms(0)[share]);
            
            //abolish and set new setting
            .abolish(setting(phase_transition_delay_ms(_))); 
            +setting(phase_transition_delay_ms(0)[local]);
            
            //remove the use of the LLM
            .abolish(setting(llm_disabled(_)));
            +setting(llm_disabled(true)[share]); 
        };

        //if needed, prepare the logging directory for the game stats and llm calls (if enabled, and if not already set by the user)
        if (setting(enable_stats_log(true))) {
            ?setting(path_game_stats(CurrentPath));
            ?setting(path_log_directory(LogDir));
            if (CurrentPath == none) {
                .date(Y, Mo, D); 
                .time(H, M, S);
                .concat(LogDir,"/", Y, "-", Mo, "-", D, "_", H, "-", M, "-", S, NewPath);
                
                // Update the global setting with the new computed path
                .abolish(setting(path_game_stats(_)));
                +setting(path_game_stats(NewPath)[share]);
                .print("[INFO] Generated new logging directory: ", NewPath);
            }
        } else {
            //disable all the logging related to game stats and llm calls
            .abolish(setting(path_game_stats(_)));
            +setting(path_game_stats(none)[share]);

            .abolish(setting(save_game_stats(_)));
            +setting(save_game_stats(false)[local]);

            .abolish(setting(save_trait_evolution(_)));
            +setting(save_trait_evolution(false)[share]);

            .abolish(setting(save_mood_evolution(_)));
            +setting(save_mood_evolution(false)[share]);

            .abolish(setting(save_llm_calls(_)));
            +setting(save_llm_calls(false)[share]);
        };

        //check that the LLM is abilitated: pingo to warm up the model 
        if (not setting(llm_disabled(true))) {
            ?setting(llm_model(ModelName));
            
            // Determine the LLM logging directory based on save_llm_calls
            if (setting(save_llm_calls(true))) {
                ?setting(path_game_stats(LogConfigDir));
            } else {
                LogConfigDir = "none";
            }
            
            .print("[INFO] Pinging the LLM (", ModelName, ") to check availability.");
            
            // Call the 3-arity action (Model, ConfigDir, Result)
            llm.ping_llm(ModelName, LogConfigDir, PingResult);

            if (PingResult \== "ok") {
                .print("[ERROR] LLM ping failed (", PingResult, ").");
                
                // Disable the LLM mathematically for the rest of the run
                .abolish(setting(llm_disabled(_))); 
                +setting(llm_disabled(true)[share]);
            } else {
                .print("[INFO] LLM is available. Call logging directory set to: ", LogConfigDir);
            }
        };

        //the llm is disabled, from the setting or the ping's fail
        if (setting(llm_disabled(true))) {
            .print("[INFO] LLM will not be used.");
            
            .abolish(setting(llm_narrator(_))); 
            +setting(llm_narrator(false)[local]);

            .abolish(setting(llm_player_message(_))); 
            +setting(llm_player_message(false)[share]);

            //makes no sense to  infer the speech act on a default, raw message, so disable it too
            .abolish(setting(allow_speech_act_misalignment(_))); 
            +setting(allow_speech_act_misalignment(false)[share]);
            
            //for clarity, remove unuseful settings
            .abolish(setting(llm_model(_)));
        }

        if (setting(allow_speech_act_misalignment(true)))   {
            if (setting(save_bert_calls(true))) {
                ?setting(path_game_stats(LogConfigDir));
            } else {
                LogConfigDir = "none";
            }

            bert.ping_bert(LogConfigDir, BertPingResult);

            if (BertPingResult \== "ok") {
                .print("[ERROR] BERT ping failed (", BertPingResult, ").");
                
                //disable bert
                .abolish(setting(allow_speech_act_misalignment(_))); 
                +setting(allow_speech_act_misalignment(false)[share]);
            } else {
                .print("[INFO] BERT is available. Call logging directory set to: ", LogConfigDir);
            }
        }.

//send some settings to the players (the one with [share])
@broadcast_shared_settings
+!broadcast_shared_settings
    <-  //get all settings beliefs
        for (setting(Item)) {
            //explode them
            Item =.. [Functor, Args, Annots];
            if (.member(share, Annots)) {
                //remove the [share] and send to everyone
                CleanItem =.. [Functor, Args, []];
                .broadcast(tell, setting(CleanItem));
            }
        }.

@setup
+!setup 
    <-  +iam(narrator);

        .print("========================================");
        .print("         NARRATOR CONFIGURATION         ");
        .print("========================================");

        !remove_invalid_settings;   //clean up
        !apply_default_settings;    //initialize
        !apply_setting_constraints; //ensure consistency
        !broadcast_shared_settings; //share settings

        //notify the active setting
        for(setting(Final)) { .print(Final); };
        .print("========================================");
        ?setting(random_seed(Seed));
        if (Seed \== none) {
            .set_random_seed(Seed);
        }
        
        //if the LLM is enabled, load the log-to-context module to provide the narration context
        if (setting(llm_narrator(true))) {
            if (setting(set_narrator_style(none))) {
                .print("[INFO] No custom narrator style defined. The default one will be used.");
            } else {
                ?setting(set_narrator_style(Style));
                .print("[INFO] Custom narrator style defined: ", Style);
                .abolish(narrator_style(_));
                +narrator_style(Style);
            }

            .include("shared/llm_context/log_2_context.asl");
        }

        //setup the barrier: wait for all players (except the narrator itself)
        +current_node(setup);
        +current_phase_uid(0);
        .broadcast(tell, current_phase_uid(0)); //share the phase info for synchronization

        .all_names(AllAgents); .my_name(Me); .delete(Me, AllAgents, OtherAgents);
        -+waiting_on(OtherAgents);

        //explicitly initialize the players
        .broadcast(achieve, configure_player);

        //cleanup: remove the setup plans
        .remove_plan(setup);
        .remove_plan(remove_invalid_settings);
        .remove_plan(apply_default_settings);
        .remove_plan(apply_setting_constraints);
        .remove_plan(broadcast_shared_settings);
        //no more needed after the setup
        .abolish(default_settings(_)).
