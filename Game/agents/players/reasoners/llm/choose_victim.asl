//---------------
// HUNT (wolf only)
//---------------
+!choose_victim 
    :   iam(wolf)
    &   setting(max_llm_retry(MaxRetries))
    <-  !delay_for_realism;
        //call a subgoal (to allow retries)
        !get_valid_victim(MaxRetries, Target);
        !submit_hunt(Target).

+!get_valid_victim(RetriesLeft, Target)
    :   RetriesLeft > 0
    <-  //get the context for the LLM
        ?player_description(IdentityContext);
        ?valid_targets(ValidTargetsStr);
        ?all_players_traits_str(TraitsOpinionsString);
        ?wolf_pack_str(WolfPackStr);
        ?available_traits_str(TraitsListStr);
        ?available_moods_str(MoodsListStr);
        
        //get the log from the narrator
        !fetch_log_block(night_phase, 1, CroppedLog);
        ?translate_log(CroppedLog, LLMFriendlyLog);

        //trigger the call
        llm.choose_victim(IdentityContext, LLMFriendlyLog, ValidTargetsStr, TraitsOpinionsString, WolfPackStr, TraitsListStr, MoodsListStr);
        .wait(victim_result(TargetString));
        -victim_result(_);

        //check for correctness
        .term2string(Target, TargetString);
        ?role(Target, _). //check if is  a  player

//failure: retry
-!get_valid_victim(RetriesLeft, Target)
    :   RetriesLeft > 0
    <-  .print("[WARNING] LLM generated an invalid hunt target. Retries left: ", RetriesLeft - 1);
        -victim_result(_); 
        
        //restart with a decremented counter
        !get_valid_victim(RetriesLeft - 1, Target).

//failure: no retry
-!get_valid_victim(RetriesLeft, Target)
    :   other_players(Candidates)
    &   random_target(Candidates, Target)
    <-  .print("[ERROR] LLM failed to provide a valid hunt target. Falling back to random selection.");
        -victim_result(_).