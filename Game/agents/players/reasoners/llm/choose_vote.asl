//---------------
// EXECUTION VOTE (daytime)
//---------------
+!choose_vote
    :   setting(max_llm_retry(MaxRetries))
    <-  !delay_for_realism;
        //call a subgoal (to allow retries)
        !get_valid_vote(MaxRetries, Target);
        !submit_vote(Target).

+!get_valid_vote(RetriesLeft, Target)
    :   RetriesLeft > 0
    <-  //get the context for the LLM
        ?player_description(IdentityContext);
        ?valid_targets(ValidTargetsStr);
        ?all_players_traits_str(TraitsOpinionsString);
        ?wolf_pack_str(WolfPackStr);
        ?available_traits_str(TraitsListStr);
        ?available_moods_str(MoodsListStr);
        
        //get the log from the narrator
        !fetch_log_block(any, 1, CroppedLog);
        ?translate_log(CroppedLog, LLMFriendlyLog);

        //trigger the call
        llm.choose_vote(IdentityContext, LLMFriendlyLog, ValidTargetsStr, TraitsOpinionsString, WolfPackStr, TraitsListStr, MoodsListStr);
        .wait(vote_result(TargetString));
        -vote_result(_);

        //check for correctness
        .term2string(Target, TargetString);
        ?role(Target, _). //check if is  a  player

//failure: retry
-!get_valid_vote(RetriesLeft, Target)
    :   RetriesLeft > 0
    <-  .print("[WARNING] LLM generated an invalid vote target. Retries left: ", RetriesLeft - 1);
        -vote_result(_); 
        
        //restart with a decremented counter
        !get_valid_vote(RetriesLeft - 1, Target).

//failure: no retry
-!get_valid_vote(RetriesLeft, Target)
    :   other_players(Candidates)
    &   random_target(Candidates, Target)
    <-  .print("[ERROR] LLM failed to provide a valid vote target. Falling back to random selection.");
        -vote_result(_).