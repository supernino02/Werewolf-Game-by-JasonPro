//---------------
// GRAPH NODES
//---------------
node(none, setup, []).
node(action(Wolves, acknowledge_pack(Wolves)), inform_pack, [pack_acknowledged]) :- wolves(Wolves).
node(none, start_game, []).
node(action(Wolves, choose_victim), night_phase, []) :- wolves(Wolves).
node(none, count_hunt_votes, [hunt(_, _)]).
node(none, resolve_hunt, [tie_count_hunt(_), max_victim_hunt(_)]).
node(none, morning_phase, [hunt_turns(_)]).
node(action(Players, decide_to_speak), start_discussion_round, []) :- players(Players) & discussion_turns(K) & K > 0.
node(none, start_discussion_round, []) :- discussion_turns(0).
node(none, evaluate_intents_to_speak, [intent_to_speak(_,_), speaker_selected(_)]).

//utility node to be used ir linguistic relativity is on
// players have to guess the performative of the last speech act before being able to vote
node(action(Listeners, guess_performative(speech(Speaker, Msg, Performative))), guess_allow_speech_act_misalignment, [last_speech(_)]) 
    :-  last_speech(speech(Speaker, Msg, Performative)) 
    &   players(AllPlayers) 
    &   .delete(Speaker, AllPlayers, Listeners).

node(none, finish_speaking_round, [speech(_, _, _)]).
node(action(Players, choose_vote), peasant_vote, [last_speaker(_)]) :- players(Players).
node(none, count_peasant_votes, [vote(_, _)]).
node(none, resolve_vote, [tie_count_village(_), max_victim_village(_), last_target(_)]).
node(none, end_game, []).

//---------------
// GRAPH EDGES
//---------------

// Notifies wolves and villagers of their respective roles at the start of the game
edge(setup, msg(Target, Message, PhaseTitle, PhaseIntent, []), inform_pack)
  :- (wolves(Target) & narrator_message(setup_wolves, no_parameters, PhaseTitle, PhaseIntent, Message))
   | (villagers(Target) & narrator_message(setup_villagers, no_parameters, PhaseTitle, PhaseIntent, Message)).

//start of the game
edge(inform_pack, msg(Targets, Message, PhaseTitle, PhaseIntent, []), start_game)
  :- (wolves(Targets) & narrator_message(start_wolves, no_parameters, PhaseTitle, PhaseIntent, Message)) 
   | (villagers(Targets) & narrator_message(start_villagers, no_parameters, PhaseTitle, PhaseIntent, Message)).

//night phase begins, wolves choose victim
edge(start_game, msg(Targets, Message, PhaseTitle, PhaseIntent, []), night_phase)
  :- (wolves(Targets) & narrator_message(night_falls_wolves, no_parameters, PhaseTitle, PhaseIntent, Message))
   | (villagers(Targets) & narrator_message(night_falls_villagers, no_parameters, PhaseTitle, PhaseIntent, Message)).

//hunt votes are alredy told to the wolves,simply go on
edge(night_phase, none, count_hunt_votes).

//HUNT ENDED
// Success
edge(count_hunt_votes, msg(Targets, Message, PhaseTitle, PhaseIntent, [victim(hunt, Victim, Role)]), resolve_hunt)
  :- tie_count_hunt(1) & max_victim_hunt(Victim) & role(Victim, Role)
   & ( (wolves(AllWolves) & .delete(Victim, AllWolves, Targets) & narrator_message(hunt_success_wolves, [Victim, Role], PhaseTitle, PhaseIntent, Message))
     | (villagers(AllVillagers) & .delete(Victim, AllVillagers, Targets) & narrator_message(hunt_success_villagers, [Victim, Role], PhaseTitle, PhaseIntent, Message))
     | (Targets = [Victim] & narrator_message(hunt_victim, no_parameters, PhaseTitle, PhaseIntent, Message)) ).
     
// Tie - Retries Available (Silent transition to resolve)
edge(count_hunt_votes, none, resolve_hunt)
  :- tie_count_hunt(Tie) & Tie > 1 & hunt_turns(K) & K > 0.

// Tie - Exhausted (Passes the Targets array to the narrative)
edge(count_hunt_votes, msg(Targets, Message, PhaseTitle, PhaseIntent, []), resolve_hunt)
  :- tie_count_hunt(Tie) & Tie > 1 & hunt_turns(0)
   & ( (wolves(Targets) & narrator_message(hunt_tie_wolves, no_parameters, PhaseTitle, PhaseIntent, Message))
     | (villagers(Targets) & narrator_message(hunt_tie_villagers, no_parameters, PhaseTitle, PhaseIntent, Message)) ).

// --- MORNING PHASE TRANSITIONS ---

// Tie - Retry Loop back to Night Phase (Passes the remaining turns 'K' to the narrative)
edge(resolve_hunt, msg(Wolves, Message, PhaseTitle, PhaseIntent, []), night_phase)
  :- wolves(Wolves) & tie_count_hunt(Tie) & Tie > 1 & hunt_turns(K) & K > 0
   & narrator_message(hunt_tie_retry_wolves, K, PhaseTitle, PhaseIntent, Message).

// Success - Move to Morning  
edge(resolve_hunt, none, morning_phase)
  :- (tie_count_hunt(1) | (tie_count_hunt(Tie) & Tie > 1 & hunt_turns(0))).

//time to speak, discussion round starts
edge(morning_phase, msg(Targets, Message, PhaseTitle, PhaseIntent, []), start_discussion_round)
  :- discussion_turns(K) & setting(max_discussion_turns(Max)) & K == Max
   & ( (wolves(Targets) & narrator_message(discussion_start_wolves, no_parameters, PhaseTitle, PhaseIntent, Message))
     | (villagers(Targets) & narrator_message(discussion_start_villagers, no_parameters, PhaseTitle, PhaseIntent, Message)) ).

edge(start_discussion_round, none, evaluate_intents_to_speak) 
  :- discussion_turns(K) & K > 0.

//message and speech_act alredy logged, simply go on
edge(evaluate_intents_to_speak, none, finish_speaking_round) 
    :- speaker_selected(yes) & setting(allow_speech_act_misalignment(false)).

//edges to route to the utility node to manage the performative guess if linguistic relativity is on
edge(evaluate_intents_to_speak, none, guess_allow_speech_act_misalignment) 
    :- speaker_selected(yes) & setting(allow_speech_act_misalignment(true)).
edge(guess_allow_speech_act_misalignment, none, finish_speaking_round).

edge(evaluate_intents_to_speak, msg(Players, Message, PhaseTitle, PhaseIntent, []), peasant_vote)
  :- speaker_selected(no) & players(Players) & discussion_turns(K) & K > 0
   & narrator_message(peasant_vote_early, no_parameters, PhaseTitle, PhaseIntent, Message).

edge(start_discussion_round, msg(Players, Message, PhaseTitle, PhaseIntent, []), peasant_vote)
  :- players(Players) & discussion_turns(0) 
   & narrator_message(peasant_vote_timeout, no_parameters, PhaseTitle, PhaseIntent, Message).

edge(finish_speaking_round, none, start_discussion_round).

edge(peasant_vote, none, count_peasant_votes).
// --- EXECUTION RESOLUTION ---
edge(count_peasant_votes, msg(Targets, Message, PhaseTitle, PhaseIntent, [victim(vote, Victim, VictimRole)]), resolve_vote)
  :- tie_count_village(1) & max_victim_village(Victim) & role(Victim, VictimRole)
   & ( (wolves(AllWolves) & .delete(Victim, AllWolves, Targets) & narrator_message(exec_result, [wolves, VictimRole, Victim], PhaseTitle, PhaseIntent, Message))
     | (villagers(AllVillagers) & .delete(Victim, AllVillagers, Targets) & narrator_message(exec_result, [villagers, VictimRole, Victim], PhaseTitle, PhaseIntent, Message))
     | (Targets = [Victim] & narrator_message(exec_victim, no_parameters, PhaseTitle, PhaseIntent, Message)) ).

edge(count_peasant_votes, msg(Targets, Message, PhaseTitle, PhaseIntent, []), resolve_vote)
  :- tie_count_village(Tie) & Tie > 1 
   & ( (wolves(Targets) & narrator_message(exec_tie_wolves, no_parameters, PhaseTitle, PhaseIntent, Message))
     | (villagers(Targets) & narrator_message(exec_tie_villagers, no_parameters, PhaseTitle, PhaseIntent, Message)) ).

edge(resolve_vote, msg(Targets, Message, PhaseTitle, PhaseIntent, []), night_phase)
  :- (wolves(Targets) & villagers(Villagers) & narrator_message(night_falls_wolves, no_parameters, PhaseTitle, PhaseIntent, Message))
   | (villagers(Targets) & narrator_message(night_falls_villagers, no_parameters, PhaseTitle, PhaseIntent, Message)).

edge(_, msg(Players, Message, PhaseTitle, PhaseIntent, Actions), end_game)
  :- win_condition(Faction, WinID)
   & players(Players) & wolves(Wolves) & villagers(Villagers)
   & .findall(role(P, Role), role(P, Role), Roles)
   & .concat(Roles, [winner(Faction)], Actions)   //the winner is the last info
   & narrator_message(WinID, no_parameters, PhaseTitle, PhaseIntent, Message).

//WIN CONDITIONS
//to make the game more enjoyable, the wolfs must kill ALL villagers
win_condition(wolf, wolves_win)        
  :- villagers([]).

win_condition(villager, villagers_win) 
  :- wolves([]).


//---------------
// NODE TRANSITION
//---------------
//at each transition, check for the endgame
+!execute_transition 
    :   current_node(CurrentNode) 
    &   edge(CurrentNode, _, end_game)
    <-  !process_transition(CurrentNode, end_game).

//now check for any other edge
+!execute_transition 
    :   current_node(CurrentNode) 
    &   edge(CurrentNode, _, NextNode)
    <-  !process_transition(CurrentNode, NextNode).

//fallback error (should not happen)
-!execute_transition
    <- .print("[ERROR] No valid transition found from current node: ", CurrentNode).

//actually  process the transition
+!process_transition(CurrentNode, NextNode) 
    :   node(_, CurrentNode, CleanupList)
    <-  .findall(OutgoingMsg, edge(CurrentNode, OutgoingMsg, NextNode), AllMessages);
        
        //cleanup the unused beliefs from the previous node
        for ( .member(StaleBelief, CleanupList) ) { .abolish(StaleBelief); };
        
        //actuallyactivate the next node
        !activate_node(NextNode, AllMessages).

//actually activate the node: send the messages, execute the actions, and manage the dynamic delay
+!activate_node(NodeName, AllMessages)
    :   current_phase_uid(PhaseUID)
    &   narrator_style(StyleDesc)
    &   setting(narrator_chunk_log_size(NarratorChunkLogSize))
    <-  -+current_node(NodeName);

        //uid for debugging purposes, to track all beliefs and messages related to this specific transition
        NewPhaseUID = PhaseUID + 1;
        -+current_phase_uid(NewPhaseUID);
        .broadcast(untell, current_phase_uid(PhaseUID));    //clear the phase info for synchronization
        .broadcast(tell,   current_phase_uid(NewPhaseUID)); //share the new phase info for synchronization

        .broadcast(untell, cached_log(_, _, _)); //clear the log cache on the players
        .broadcast(untell, pending_log_request(_, _)); //clear pending log requests on the players

        // Keep track of the absolute timestamp when the longest animation will finish
        utils.ms_timestamp(StartTime);
        -+max_target_time(StartTime);

        //iterate over the messages to send
        for ( .member(OUT, AllMessages) ) {
            if (OUT = msg(TargetList, BaseMessage, PhaseTitle, PhaseIntent, Actions_to_log)) {
                
                //generate the message using ollama
                if (setting(llm_narrator(true))) {
                    ?get_log_chunk(any, TargetList, NarratorChunkLogSize, LogChunk);
                    ?translate_log(LogChunk, LogChunkTranslated);
                    
                    llm.narrator_message(PhaseIntent, BaseMessage, StyleDesc, LogChunkTranslated);
                    .wait(narrator_result(FinalMessageString));
                    -narrator_result(_);
                } else {
                    FinalMessageString = BaseMessage;
                };

                // 1. Get the exact time THIS message starts animating
                utils.ms_timestamp(Now);
                
                // 2. Calculate how long the animation will take
                .length(FinalMessageString, Len);
                ?setting(writing_delay_ms(K));
                AnimationDuration = Len * K;
                
                // 3. Calculate absolute end time for this specific animation
                TargetTime = Now + AnimationDuration;
                
                // 4. Update the global max target time if this one finishes later
                ?max_target_time(CurrentMax);
                if (TargetTime > CurrentMax) { -+max_target_time(TargetTime); };
                
                //dispatch the message to the targets and log the action
                !dispatch_phase(TargetList, NodeName, PhaseTitle, FinalMessageString);

                //dispatch actions defined as entry effects of the node
                for ( .member(Action, Actions_to_log) ) { !dispatch_action(TargetList, Action); };
            };
        };

        //get the absolute timestamp of the latest finishing animation and clean up
        ?max_target_time(FinalTargetTime);
        -max_target_time(_);

        //if needed, wait for the remaining time
        if (setting(disable_delay(false))) {
            // Check the clock again now that all LLM calls are done
            utils.ms_timestamp(FinishedLoopTime);
            ?setting(phase_transition_delay_ms(Ms));
            
            // Wait = (When the longest animation ends) - (What time it is right now) + (Buffer)
            RemainingWait = (FinalTargetTime - FinishedLoopTime) + Ms;
            
            // If the LLM was so slow that all animations are ALREADY done, RemainingWait will be negative.
            // Only wait if there is actually time left over.
            if (RemainingWait > 0) { 
                .wait(RemainingWait); 
            };
        };

        //execute node's specific entry effects if needed
        ?node(IN, NodeName, _);
        if (IN = action(ActionTargets, Action)) {
            -+waiting_on(ActionTargets);
            .send(ActionTargets, achieve, Action);
        } else {
            !NodeName;
        }.
//---------------
// PHASE AND ACTION DISPATCH (locally logged on each agent)
//---------------
@dispatch_phase[atomic]
+!dispatch_phase(Targets, NodeName, PhaseTitle, Msg)
    <-  .send(Targets, achieve, manage_phase(NodeName, PhaseTitle, Msg));
        !append_log(phase(NodeName, PhaseTitle, Msg), Targets).

@dispatch_action[atomic]
+!dispatch_action(Targets, Action)
    <-  .send(Targets, achieve, manage_action(Action));
        !manage_action(Action);
        !append_log(action(Action), Targets).

//---------------
// SYNC MECHANICS (waiting for all agents to reply)
//---------------
@sync_handler[atomic]
+!sync_reply(Payload)[source(Sender)]
    :   waiting_on(WL) 
    &   .member(Sender, WL) //ensure the reply is from someone we're waiting on
    <-  +Payload;                            //acknowledge as a belief the payload
        !manage_sync_payload(Payload);       //explicitly manage the payload
        .delete(Sender, WL, NewWL);          //remove the senderer from the waiting list

        //if the waiting list is empty -> transition
        if (NewWL == []) {
          -waiting_on(_);
          !execute_transition;
        } else {
          //else simply wait for the others
          -+waiting_on(NewWL); 
        }.

+!sync_reply(_)[source(Sender)]
  <- .print("[ERROR] Received unexpected sync reply from ", Sender, ": ", Payload).

//---------------
// PAYLOAD MANAGEMENT -> explicitly dispatched to others to ensure synchronization and proper logging
//---------------

//the intent to speak if forced to "targeted" if the player was targeted in the prevoius speech-round
+!manage_sync_payload(intent_to_speak(Sender, _)) 
    :   last_target(Sender) 
    &   players(AllPlayers)
    <-  -+intent_to_speak(Sender, targeted);                
        !dispatch_action(AllPlayers, intent_to_speak(Sender, targeted)).

//symply forward the intent to speak
+!manage_sync_payload(intent_to_speak(Sender, Intent)) 
    :   players(AllPlayers)
    <-  !dispatch_action(AllPlayers, intent_to_speak(Sender, Intent)).

//symply forward the vote
+!manage_sync_payload(vote(Voter, Target)) 
    :   players(AllPlayers)
    <-  !dispatch_action(AllPlayers, vote(Voter, Target)).

//symply forward the hunt vote
+!manage_sync_payload(hunt(Wolf, Target)) 
    :   wolves(AllWolves)
    <-  !dispatch_action(AllWolves, hunt_vote(Wolf, Target)).

//symply forward the speech act
+!manage_sync_payload(speech(Speaker, Msg, Performative)) 
    :   setting(allow_speech_act_misalignment(false))
    &   players(AllPlayers)
    <-  Performative =.. [_, [Target] , _]; 
        -+last_target(Target);
        !dispatch_action(AllPlayers, speech(Speaker, Msg, Performative));
        
        if (setting(disable_delay(false))) {
            ?setting(writing_delay_ms(K));
            .length(Msg, Len);
            Delay = Len * K; 
            if (Delay > 0) { .wait(Delay); };
        }.


//case with  linguistic realtivity
+!manage_sync_payload(speech(Speaker, Msg, Performative)) 
    :   setting(allow_speech_act_misalignment(true))
    <-  Performative =.. [_, [Target] , _]; 
        -+last_target(Target);

        .print("[INFO] Correct speech is: ", Performative, "by ", Speaker, " with message: ", Msg);

        Speech = speech(Speaker, Msg, Performative);
        //only dispatch back to the speaker
        !dispatch_action([Speaker], Speech);

        //keep it to automatically send to the players to guess the performative
        +last_speech(Speech).

//obtain the subjective interpretation of the speech act and dispatch it only to the agent that guessed it
+!manage_sync_payload(performative_guessed(Agent, GuessedSpeech))
    <-  !dispatch_action([Agent], GuessedSpeech).


//fallback: all the others (role(_,_) , pack_acknowledged  in the set-up phase)
+!manage_sync_payload(_).