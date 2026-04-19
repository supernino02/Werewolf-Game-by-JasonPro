//---------------
// ACTION INTERFACES
//--------------
+!choose_vote
    <-  !delay_for_realism;
        !evaluate_vote(Target);
        !submit_vote(Target).

+!choose_victim
    <-  !delay_for_realism;
        !evaluate_prey(Target);
        !submit_hunt(Target).

+!decide_to_speak
    <-  !delay_for_realism;
        !evaluate_speaking_intent(Intent);
        !submit_intent_to_speak(Intent).


+!time_to_speak
    :   setting(default_player_message(DefaultMessage))
    <-  !delay_for_realism;
        !choose_speech_performative(Performative);

        if (setting(llm_player_message(true))) {
            //get the context of the speaker
            ?player_description(IdentityContext);

            ?player_style(StyleDesc);
            

            //convert the performative into a description of the intent behind it
            ?performative_description(Performative, PerformativeIntentDesc);
            
            //get the context from the log
            !fetch_log_block(peasant_vote, 1, CroppedLog);
            ?translate_log(CroppedLog, LLMFriendlyLog);
            
            //get the wolf pack and the players list as strings
            ?wolf_pack_str(WolfPackStr);
            ?players_list_str(PlayersStr);
            
            llm.player_message(IdentityContext, StyleDesc, PerformativeIntentDesc, LLMFriendlyLog, WolfPackStr ,PlayersStr);
            .wait(message_result(MessageString));
            -message_result(_);
        } else {
            MessageString = DefaultMessage;
        }
        
        !submit_speech(MessageString, Performative).

