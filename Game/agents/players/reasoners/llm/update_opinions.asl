//---------------
// UPDATE OPINIONS AND MOODS
//---------------

//do not compute opinions for the intent to speak, will be WAY too many calls  
+!upd_opinions(Speaker, intent_to_speak(Intent)).

//entry point: simply call the primary plan with a retry mechanism
+!upd_opinions(Speaker, Action)
    :   setting(max_llm_retry(MaxRetries))
    <-  !invoke_llm_opinions(Speaker, Action, MaxRetries).

//primary llm execution plan (Concurrency enabled, [atomic] removed)
+!invoke_llm_opinions(Speaker, Action, RetriesLeft)
    :   RetriesLeft > 0
    <-  //get the context for the LLM
        ?player_description(IdentityContext);
        ?all_players_traits_str(TraitsOpinionsString);
        ?wolf_pack_str(WolfPackStr);
        ?available_traits_str(TraitsListStr);
        ?available_moods_str(MoodsListStr);
        ?action_2_text(Speaker, Action, ActionStr);
        
        //get the log from the narrator
        !fetch_log_block(any, 3, CroppedLog);
        ?translate_log(CroppedLog, LLMFriendlyLog);

        //create a unique identifier for this call: since the plan is concurrent,
        //we cannot rely on a single global variable to store the result, so id needed 
        //to tag it with an ID to ensure we match the result to the correct call.
        .random(RandNum);
        .concat("UID_", RandNum, UID);

        //trigger the call to obtain the opinions updates
        llm.update_opinions(UID, IdentityContext, TraitsOpinionsString, LLMFriendlyLog, WolfPackStr, ActionStr, TraitsListStr, MoodsListStr);
        .wait(opinions_result(UID, UpdatesString));
        -opinions_result(UID, _);
        
        //convert the output string back to a list and apply the updates
        .term2string(UpdatesList, UpdatesString);
        .list(UpdatesList);
        for ( .member(Update, UpdatesList) ) {
            if (Update = trait_update(Target, Attr, Delta)) {
                !upd_trait(Target, Attr, Delta);
            }
            if (Update = mood_update(MoodAttr, Delta)) {
                vesna.apply_mood_effect(MoodAttr, Delta);
            }
        }.

//failure: retry
-!invoke_llm_opinions(Speaker, Action, RetriesLeft)
    :   RetriesLeft > 0
    <-  .print("[WARNING] LLM generated invalid update syntax. Retries left: ", RetriesLeft - 1);
        
        //restart with a decremented counter
        !invoke_llm_opinions(Speaker, Action, RetriesLeft - 1).

//failure: no retry, nothing happens
-!invoke_llm_opinions(Speaker, Action, RetriesLeft)
    <-  .print("[ERROR] LLM failed to provide valid opinion updates. Falling back to skipping updates.").