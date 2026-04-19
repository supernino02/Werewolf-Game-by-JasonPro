{ include("init_narrator.asl") }
{ include("core/game_narration.asl") }
{ include("core/game_graph.asl") }

{ include("log_manager/logger.asl") }

{ include("shared/utils.asl") }


//---------------
// START GAME
//---------------
+!start_game 
    :   setting(max_hunt_rounds(MHR))
    <-  //setting needed for the hunt retry logic
        +hunt_turns(MHR);
        !execute_transition.

@end_game_plan
+!end_game
    <-  // 1. Unified Cleanup
        for ( node(_, _, C) ) { for ( .member(B, C) ) { .abolish(B); } };
        
        // 2. Narrator Export & Broadcast
        if (setting(save_game_stats(true)) & setting(path_game_stats(Dir))) {
            ?shared_log(FL);
            .reverse(FL, Chrono);

            .concat(Dir, "/narrator.json",FP);

            utils.save_narrator_log(Chrono, FP);

            
            // Mark narrator as finished locally
            .my_name(Me); +end_game_logging_finished(Me);
            .broadcast(achieve, export_player_log);

            // 3. Polling: Count finished agents vs total agents
            .all_names(All);
            .length(All, TotalCount);
            
            // Loop until the count of finished beliefs matches TotalCount
            while (.findall(Player,end_game_logging_finished(Player), FinishedList) & .length(FinishedList, FinishedCount) & FinishedCount < TotalCount) {
                .wait(100); 
            };
            .print("[INFO] All ", TotalCount, " agents finished logging.");
        };

        // 4. Termination
        if (setting(exit_on_game_over(true))) { .print("[INFO] Stopping MAS."); .stopMAS; }.
//---------------
// COUNTING VOTES LOGIC
//---------------

//! harcoded rule, just to avoid infinite games (eg when harvesting data for llm training)
tally_votes(VoteList, 1, MaxVictim) :-  setting(avoid_ties_in_votes(true)) & .random(R) & R < 0.25
    &   .print("[INFO] Avoiding a tie by randomly selecting a victim...")
    &   .setof(V, .member(V, VoteList), UniqueVictims)           
    &   .findall(vote_count(C, V), (.member(V, UniqueVictims) & .count(.member(V, VoteList), C)), Tally)
    &   .max(Tally, vote_count(MaxCount, MaxVictim)).

//get the winner and the number of ties
tally_votes(VoteList, TieCount, MaxVictim) 
    :-  .setof(V, .member(V, VoteList), UniqueVictims)           //get unique targets
    &   .findall(vote_count(C, V), (.member(V, UniqueVictims) & .count(.member(V, VoteList), C)), Tally)
    &   .max(Tally, vote_count(MaxCount, MaxVictim))             //find the highest vote count and who got it
    &   .count(.member(vote_count(MaxCount, _), Tally), TieCount). //check how many players got that max count

+!count_hunt_votes
    <-  .findall(V, hunt(_, V), AllHunts);           //get all hunt votes
        ?tally_votes(AllHunts, TieCount, MaxVictim); //calculate the results
        
        //save results as beliefs
        +tie_count_hunt(TieCount); 
        +max_victim_hunt(MaxVictim);
        
        //if there is a tie, decrement the retry counter
        if (TieCount > 1) {
            ?hunt_turns(K);
            if (K > 0) { -+hunt_turns(K - 1); };
        };
        //move to the next phase (if possible, will retry the hunt)
        !execute_transition.

+!count_peasant_votes
    <-  .findall(V, vote(_, V), AllVotes);           //get all peasant votes
        ?tally_votes(AllVotes, TieCount, MaxVictim); //calculate the results
        
        //save results as beliefs
        +tie_count_village(TieCount); 
        +max_victim_village(MaxVictim);
        
        //move to the next phase (no retry possible, if there is a tie just move on)
        !execute_transition.

//---------------
// VOTING RESULTS
//---------------

//check if the game is over after the hunt
+!resolve_hunt <- !execute_transition.

+!resolve_vote 
    :   setting(max_hunt_rounds(MHR))
    <-  -+hunt_turns(MHR); //reset the hunt turns for the upcoming night
        !execute_transition.

+!morning_phase 
    :   setting(max_discussion_turns(Max))
    <-  //initialize the discussion turns counter
        -+discussion_turns(Max);        
        
        //move to the discussion phase
        !execute_transition.


//---------------
// DISCUSSION PHASE LOGIC
//---------------

//simple hook to start a discussion turn
+!start_discussion_round <- !execute_transition.

+!finish_speaking_round 
    :   discussion_turns(K)
    <-  -+discussion_turns(K - 1); 
        //trigger the next node
        !execute_transition.


//if there is a a last speaker, remove it from the candidates for this round ( to avoid immediate repetition )
+!evaluate_intents_to_speak 
    :   last_speaker(LS)
    <-  .findall(P, intent_to_speak(P, yes), RawNorm);
        .findall(P, intent_to_speak(P, privileged), RawPriv);
        .findall(P, intent_to_speak(P, targeted), RawTarg); 
        
        .delete(LS, RawNorm, OkNorm);
        .delete(LS, RawPriv, OkPriv);
        .delete(LS, RawTarg, OkTarg);
        
        !perform_selection(OkPriv, OkTarg, OkNorm).

//no last speaker, no filtering
+!evaluate_intents_to_speak
    <-  .findall(P, intent_to_speak(P, yes), OkNorm);
        .findall(P, intent_to_speak(P, privileged), OkPriv);
        .findall(P, intent_to_speak(P, targeted), OkTarg); 
        
        !perform_selection(OkPriv, OkTarg, OkNorm).

//---------------
// CHOOSING WHO SPEAKS NEXT
//---------------

//if no one wants to speak (targeted alone is not enough), skip
+!perform_selection([], _, [])
    <-  +speaker_selected(no); 
        !execute_transition.

//if someone wants to speak, select them
+!perform_selection(Privileged, Targeted, Normal) 
    :   players(Players)
    <-  //delegate selection to the priority cascade
        !pick_candidate(Privileged, Targeted, Normal, Speaker);
        
        //update the last speaker to avoid immediate repetition
        -+last_speaker(Speaker); 
        +speaker_selected(yes);
        
        //notify all players about the chosen speaker
        !dispatch_action(Players, time_to_speak(Speaker));
        
        //wait for the speaker to send their message
        -+waiting_on([Speaker]);
        .send(Speaker, achieve, time_to_speak).


//pick a privileged volunteer (i.e a active player who wants to speak)
+!pick_candidate(Privileged, _, _, Speaker) 
    :   Privileged \== [] 
    <-  .shuffle(Privileged, [Speaker|_]).

//25% chance to let the targeted player respond
+!pick_candidate(_, [Speaker], _, Speaker) 
    :   .random(R) & R <= 0.25.

//pick a normal volunteer
+!pick_candidate(_, _, Normal, Speaker) 
    :   Normal \== [] 
    <-  .shuffle(Normal, [Speaker|_]).

//fallback to the targeted player if no normal volunteers exist
+!pick_candidate(_, [Speaker], _, Speaker).

//fallback case: if no one wants to speak
-!perform_selection(_, _, _)
    <-  +speaker_selected(no); 
        !execute_transition.
//---------------
// ACTION MANAGEMENT (when logged)
//---------------

//process a player's elimination from the game
@manage_victim[atomic]
+!manage_action(victim(Cause, Victim, Role))
    <-  //remove the player's active role from the belief base
        .abolish(role(Victim, _));        
        //log their death, the cause, and their revealed role
        +victim(Cause, Victim, Role).

//fallback: do nothing (just log them)
+!manage_action(_).