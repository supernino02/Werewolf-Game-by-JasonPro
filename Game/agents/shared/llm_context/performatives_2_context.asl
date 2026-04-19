
//---------------
// SPEECH PERFORMATIVES DESCRIPTIONS (for LLM prompt construction)
//---------------
// --- PERFORMATIVE TO INTENT MAPPING ---
performative_description(accuse(Target), Msg)      :- .term2string(Target, TargetStr) &
    .concat("aggressively accuse ", TargetStr, " of being a wolf and push the village to execute them.", Msg).

performative_description(defend(Target), Msg)      :- .term2string(Target, TargetStr) &
    .concat("defend ", TargetStr, " against current suspicions and argue for their innocence.", Msg).

performative_description(suspect(Target), Msg)     :- .term2string(Target, TargetStr) &
    .concat("express heavy suspicion towards ", TargetStr, " without fully committing to an accusation.", Msg).

performative_description(agree(Target), Msg)       :- .term2string(Target, TargetStr) &
    .concat("strongly agree with the recent statements or tactical direction of ", TargetStr, ".", Msg).

performative_description(interrogate(Target), Msg) :- .term2string(Target, TargetStr) &
    .concat("ask a direct, pressuring question to ", TargetStr, " to force them into a mistake or contradiction.", Msg).

performative_description(deflect(Target), Msg)     :- .term2string(Target, TargetStr) & 
    .concat("deflect any current suspicion away and firmly redirect the village's attention onto ", TargetStr, ".", Msg).


//define a block of text listing all the performatives and their descriptions
available_performatives(PerfListStr)
    :-  .findall(FullDesc, (
            speech_performative(Perf)                //get the performative functor
        &    Action =.. [Perf, ["{PLAYER_NAME}"]]    //use a dummy target 
        &    performative_description(Action, Desc)  //get the description
        &   .concat(Perf, ": ", Desc, FullDesc)      //"PerformativeName: Description"
        ), PerfList) 
    &   join_strings(PerfList, PerfListStr, "\n").   //make a string: each element in a newline


//define the list of valid targets as all the players except yourself
valid_targets(TargetsStr)
    :-  other_players(Candidates) 
    &   .shuffle(Candidates, Shuffled)  //randomize the order each time for more varied LLM responses
    &   join_strings(Shuffled, TargetsStr, ", ").