//---------------
// TRANSLATION FROM RAW ACTION TO LMM-FRIENDLY CONTEXT
// used in the llm/update_opinions.asl
//---------------

action_2_text(Speaker, intent_to_speak(yes), Text) :- .concat(Speaker, " wants to speak in this round.", Text).
action_2_text(Speaker, intent_to_speak(no), Text) :- .concat(Speaker, " does not want to speak in this round.", Text).

action_2_text(Speaker, Action, Text)
    :-  performative_description(Action, Desc)
    &   .concat(Speaker, " ", Desc, Text).

action_2_text(Speaker, vote(Target), Text) :- .concat(Speaker, " casts a vote to eliminate ", Target, Text).
action_2_text(Speaker, hunt_vote(Target), Text) :- .concat(Speaker, " casts a vote to hunt ", Target, Text).



//---------------
// TRANSLATION FROM RAW LOG TO LMM-FRIENDLY CONTEXT
//---------------


log_to_text(phase(_, Title, Msg), Speech)
    :-  .concat("- [NARRATOR]: ", Msg, Speech).

// Format Player speech actions
log_to_text(action(speech(Src, Msg, Int)), S) :- not iam(narrator)
    &   .term2string(Int, IntStr)
    &   .concat("- [", Src, " -> ", IntStr, "]: ", Msg, S).
//log the actions
log_to_text(action(victim(hunt, Vic, R)), S)  :- not iam(narrator) & .concat("    - ", Vic, ", a ", R, ", was killed by the hunt of the wolves.", S).
log_to_text(action(victim(vote, Vic, R)), S)  :- not iam(narrator) & .concat("    - ", Vic, ", a ", R, ", was eliminated by the vote from the village.", S).
log_to_text(action(hunt_vote(Src, Tgt)), S)   :- iam(wolf) &         .concat("    - ", Src, " hunted ", Tgt, S).
log_to_text(action(vote(Src, Tgt)), S)        :- not iam(narrator) & .concat("    - ", Src, " voted for ", Tgt, S).

//---------------
// ITERATIVE TRANSLATION
//---------------

//base case:  empty log
translate_log([], "").

//if log_to_text is defined for the head of the log, use it and continue with the tail
translate_log([Head | Tail], FinalString) 
    :-  log_to_text(Head, LineString) 
    &   translate_log(Tail, RestString) 
    &   .concat(LineString, "\n", RestString, FinalString).

//if log_to_text is not defined for the head, ignore it and recurse
translate_log([_ | Tail], RestString) 
    :-  translate_log(Tail, RestString).