//---------------
// TIME TO SPEAK (choose performative and generate message)
//---------------
+!time_to_speak
    :   setting(max_llm_retry(MaxRetries))
    <-  //call a subgoal (to allow retries) to choose the  performative
        !get_valid_performative(MaxRetries, Performative, PerformativeIntentDesc);

        if (setting(llm_player_message(true))) { 
            //get the context for the LLM
            ?player_description(IdentityContext);
            ?player_style(StyleDesc);
            ?wolf_pack_str(WolfPackStr);
            ?players_list_str(PlayersStr);
            !fetch_log_block(peasant_vote, 1, CroppedLog);
            ?translate_log(CroppedLog, LLMFriendlyLog);

            //trigger the call to obtain the message
            llm.player_message(IdentityContext, StyleDesc, PerformativeIntentDesc, LLMFriendlyLog, WolfPackStr, PlayersStr);
            .wait(message_result(MessageString));
            -message_result(_);
        } else {
            //if disabled, use the default message
            ?setting(default_player_message(DefaultMessage));
            MessageString = DefaultMessage;
        }
        //no correctness check for the message, whatever is good.
        !submit_speech(MessageString, Performative).

//get the performative
+!get_valid_performative(RetriesLeft, Performative, PerformativeIntentDesc)
    :   RetriesLeft > 0
    <-  //get the context for the LLM
        ?player_description(IdentityContext);
        ?available_performatives(PerfListStr);
        ?valid_targets(ValidTargetsStr);
        ?all_players_traits_str(TraitsOpinionsString);
        ?wolf_pack_str(WolfPackStr);
        ?available_traits_str(TraitsListStr);
        ?available_moods_str(MoodsListStr);
        
        //get the log from the narrator
        !fetch_log_block(peasant_vote, 1, CroppedLog);
        ?translate_log(CroppedLog, LLMFriendlyLog);

        //trigger the call
        llm.choose_performative(IdentityContext, LLMFriendlyLog, PerfListStr, ValidTargetsStr, TraitsOpinionsString, WolfPackStr, TraitsListStr, MoodsListStr);
        .wait(performative_result(PerfString)); 
        -performative_result(_);

        //check for correctness
        .term2string(Performative, PerfString);
        Performative =.. [PerfName, [Target], _];
        
        //check the performative is valid and the target is a real player
        ?speech_performative(PerfName);
        ?role(Target, _);         
        
        //get the textual description of the performative intent
        ?performative_description(Performative, PerformativeIntentDesc).

//failure: retry
-!get_valid_performative(RetriesLeft, Performative, PerformativeIntentDesc)
    :   RetriesLeft > 0
    <-  .print("[WARNING] LLM generated an invalid performative. Retries left: ", RetriesLeft - 1);
        -performative_result(_); 
        
        //restart with a decremented counter
        !get_valid_performative(RetriesLeft - 1, Performative, PerformativeIntentDesc).

//failure: no retry
-!get_valid_performative(RetriesLeft, Performative, PerformativeIntentDesc)
    :   other_players(Candidates)
    &   random_target(Candidates, Target)
    &   Performative = suspect(Target)
    &   performative_description(Performative, PerformativeIntentDesc)
    <-  .print("[ERROR] LLM failed to provide a valid performative. Falling back to random suspect().");
        -performative_result(_).