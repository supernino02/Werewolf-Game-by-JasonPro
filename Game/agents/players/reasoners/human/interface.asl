+!choose_vote
    <-  .wait(user_input(RawTarget)); 
        -user_input(RawTarget); 
        .term2string(Target, RawTarget);
        !submit_vote(Target).


+!choose_victim
    <-  .wait(user_input(RawTarget)); 
        -user_input(RawTarget); 
        .term2string(Target, RawTarget);
        !submit_hunt(Target).

//if the intent to  remain silent for the entire turn wass choosen
+!decide_to_speak 
    :   skip_speaking_turn
    <-  !submit_intent_to_speak(no).

+!decide_to_speak
    <-  ui.actions.ask_speak;
        .wait(user_input(RawString)); 
        -user_input(RawString);
        if (RawString == "end_turn") { Intent = no; } 
        else { .term2string(Intent, RawString); };        
        !submit_intent_to_speak(Intent).

+!time_to_speak
    <-  .findall(Act, speech_performative(Act), Options);        
        ui.actions.request_speech(Options);
        
        .wait(user_speech(MessageString, Performative)); 
        -user_speech(MessageString, Performative); 
        
        !submit_speech(MessageString, Performative).

//does not matter the performative, the human player cannot guess it, so we just submit a none guess
@guess_performative
+!guess_performative(speech(Speaker, Msg, Performative))
    <-  !submit_performative_guess(speech(Speaker, Msg, none)).

//the human reasoner does not hold opinions about others
+!upd_opinions(_, _).