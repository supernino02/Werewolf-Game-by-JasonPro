//---------------
// LLM DECIDE TO SPEAK
//---------------
// helper rules defining the only valid outputs
valid_speaking_intent(yes).
valid_speaking_intent(no).

+!decide_to_speak
    :   setting(max_llm_retry(MaxRetries))
    <-  !delay_for_realism;
        //call a subgoal (to allow retries)
        !get_valid_speaking_intent(MaxRetries, Intent);
        !submit_intent_to_speak(Intent).

+!get_valid_speaking_intent(RetriesLeft, Intent)
    :   RetriesLeft > 0
    <-  //get the context for the LLM
        ?player_description(IdentityContext);
        ?all_players_traits_str(TraitsOpinionsString);
        ?wolf_pack_str(WolfPackStr);
        ?available_traits_str(TraitsListStr);
        ?available_moods_str(MoodsListStr);
        
        //get the log from the narrator
        !fetch_log_block(any, 1, CroppedLog);
        ?translate_log(CroppedLog, LLMFriendlyLog);

        //trigger the call
        llm.decide_to_speak(IdentityContext, LLMFriendlyLog, TraitsOpinionsString, WolfPackStr, TraitsListStr, MoodsListStr);
        .wait(intent_result(IntentString));
        -intent_result(_);

        //check for correctness 
        .term2string(Intent, IntentString);
        ?valid_speaking_intent(Intent).

//failure: retry
-!get_valid_speaking_intent(RetriesLeft, Intent)
    :   RetriesLeft > 0
    <-  .print("[WARNING] LLM generated an invalid speaking intent. Retries left: ", RetriesLeft - 1);
        -intent_result(_); 
        
        //restart with a decremented counter
        !get_valid_speaking_intent(RetriesLeft - 1, Intent).

// Failure: no retry
-!get_valid_speaking_intent(RetriesLeft, Intent)
    :   .findall(I, valid_speaking_intent(I), IntentList)
    &   random_target(IntentList, Intent)
    <-  .print("[ERROR] LLM failed to provide a valid speaking intent. Falling back to random selection: ", Intent);
        -intent_result(_).