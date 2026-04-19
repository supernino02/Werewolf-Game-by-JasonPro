//---------------
// PLAYER INITIALIZATION
//---------------
@initialize_player
+!init 
    :   iam(Role) 
    &   .my_name(Me)
    <-  .remove_plan(initialize_player); //remove it from the plan library

        //get the list of all the players (except the narrator)
        .all_names(AllAgents); .delete(narrator, AllAgents, Players);
        
        if (ui(_)) { 
            ui.actions.init_gui(Role, Players); 
            //set the current traits and moods in the UI
            for ( my_personality(Trait, Value) ) { ui.actions.updateOwnTrait(Trait, Value, false); };
            for ( my_mood(Trait, Value) )        { ui.actions.updateOwnTrait(Trait, Value, true);  };
        };

        //everyone is a villager (the actual roles will be updated later by the narrator)
        for ( .member(P, Players)) { +role(P, villager); };
        
        //boot completed: send the role to the narrator
        .send(narrator, achieve, sync_reply(role(Me, Role))).


//the narrator told the  wolves who they are, update the belief base and the UI accordingly
@acknowledge_pack[atomic]
+!acknowledge_pack(PackList) 
    :   iam(wolf)
    <-  .remove_plan(acknowledge_pack);
        
        //update the belief base (and ui) with the actual wolves
        for ( .member(P, PackList) ) { -role(P, _); +role(P, wolf); };
        if (ui(_)) { ui.actions.update_icons(PackList, "wolf"); } ;
        
        //notify the narrator to go ahead
        .send(narrator, achieve, sync_reply(pack_acknowledged)).

//---------------
// HELPERS TO SUBMIT ACTIONS TO THE NARRATOR
//---------------
+!submit_vote(Target) 
    :   .my_name(Me)
    <-  .send(narrator, achieve, sync_reply(vote(Me, Target))).

+!submit_hunt(Victim) 
    :   .my_name(Me)
    <-  .send(narrator, achieve, sync_reply(hunt(Me, Victim))).

+!submit_intent_to_speak(Intent) 
    :   .my_name(Me)
    <-  .send(narrator, achieve, sync_reply(intent_to_speak(Me, Intent))).

+!submit_speech(Msg, Performative) 
    :   .my_name(Me)
    <-  .send(narrator, achieve, sync_reply(speech(Me, Msg, Performative))).

@submit_performative_guess
+!submit_performative_guess(GuessedSpeech) //GuessedSpeech=Speech(Speaker, Msg, GuessedPerformative)
    :   .my_name(Me)
    <-  .send(narrator, achieve, sync_reply(performative_guessed(Me, GuessedSpeech))).

//---------------
// PERFORMATIVES IN THE SPEECH 
//---------------
speech_performative(accuse).
speech_performative(defend).
speech_performative(suspect).
speech_performative(agree).
speech_performative(interrogate).
speech_performative(deflect).


//---------------
// NEW PHASE MANAGEMENT
//---------------

//start the hunting vote for wolves
@manage_night_vote_atomic[atomic]
+!manage_phase(night_phase, Phase_title, Msg) 
    :   iam(wolf)
    &   ui(_)
    <-  ui.actions.start_vote(Phase_title, Msg, "hunt").

//start the execution vote for the village
@manage_peasant_vote_atomic[atomic]
+!manage_phase(peasant_vote, Phase_title, Msg) 
    :   ui(_)
    <-  ui.actions.start_vote(Phase_title, Msg, "vote");
        //reset the skip speaking flag (the turn is ended)
        .abolish(skip_speaking_turn).

//add the phase notification in the UI
@manage_general_phase_atomic[atomic]
+!manage_phase(Phase_name, Phase_title, Msg) 
    :   ui(_)
    <-  ui.actions.phase_message(Phase_title, Msg).

//safe fallback  (no ui)
@manage_phase_fallback_atomic[atomic]
+!manage_phase(_,_,_).

//---------------
// RECEIVED ACTION MANAGEMENT
//---------------

//process a hunt vote
+!manage_action(hunt_vote(Wolf, Target)) 
    :   iam(wolf)
    <-  if (ui(_)) { ui.actions.show_vote(Wolf, Target, "hunt"); };
        !upd_opinions(Wolf, hunt_vote(Target)).

//process a daily execution vote
+!manage_action(vote(Voter, Target))
    <-  if (ui(_)) { ui.actions.show_vote(Voter, Target, "vote"); };
        !upd_opinions(Voter, vote(Target)).

//process intent to speak
+!manage_action(intent_to_speak(Speaker, Intent)) : ui(_)
    <-  ui.actions.intent_to_speak(Speaker, Intent);
        !upd_opinions(Speaker, intent_to_speak(Intent)).

//show typing indicator when someone else is speaking
+!manage_action(time_to_speak(Speaker))  
    :   ui(passive) 
    |   (ui(active) & .my_name(Me) & Speaker \== Me)
    <-  ui.actions.typing(Speaker).

//standard speech processing
+!manage_action(speech(Speaker, Msg, Performative)) 
    <-  if (ui(_)) { ui.actions.player_message(Speaker, Msg, Performative); };
        !upd_opinions(Speaker, Performative).

//process a player's death: remove the beliefs about their role and traits  (and update the UI)
+!manage_action(victim(Cause, Victim, Role))
    <-  .abolish(role(Victim, _));
        .abolish(trait(Victim, _, _));
        +victim(Cause, Victim, Role);
        if (ui(_)) { 
            ui.actions.mark_dead(Victim);
            ui.actions.update_icons([Victim], Role);
            .my_name(Me);
            if (Victim == Me) { ui.actions.set_status("dead"); }
        }.

//reveal a role (to the UI)
+!manage_action(role(Player, Role)) 
    :   ui(_)
    <-  ui.actions.update_icons([Player], Role).

//handle game end conditions
+!manage_action(winner(Role)) 
    :   iam(Role) 
    &   ui(_) 
    <-  ui.actions.set_status("victory").
    
+!manage_action(winner(Role)) 
    :   not iam(Role) 
    &   ui(_) 
    <-  ui.actions.set_status("defeat").

//safe fallback
+!manage_action(_).


//---------------
// THE DECISON ARE DELAYED TO MAKE THE GAME FEEL MORE REALISTIC (AND NOT LIKE A SIMULATION)
//---------------
+!delay_for_realism : setting(disable_delay(true)).
+!delay_for_realism
    :   setting(responsive_delay_range_ms(Min,Max))
    <-  .random(R);
        Delay = math.round(Min + (R * (Max - Min)));
        .wait(Delay).