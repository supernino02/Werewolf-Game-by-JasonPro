//---------------
// SPEAKING INTENT
//--------------

@v_speak_panic_shout[ temper([stress(0.8)[mood], paranoia(0.8)]), effects([stress(-0.3)[mood], exposure(0.2)[mood]]) ]
+!evaluate_speaking_intent(yes).

@v_speak_terrified_silence[ temper([exposure(0.8)[mood], stress(0.6)[mood]]), effects([exposure(-0.2)[mood]]) ]
+!evaluate_speaking_intent(no).

@v_speak_maverick_lead[ temper([individualism(0.8), stress(0.2)[mood]]), effects([exposure(0.1)[mood]]) ]
+!evaluate_speaking_intent(yes).

@v_speak_analytical_observe[ temper([individualism(0.7), paranoia(0.5)]), effects([stress(-0.1)[mood]]) ]
+!evaluate_speaking_intent(no).

@v_speak_martyr_confess[ temper([exposure(0.9)[mood], paranoia(0.2)]), effects([stress(-0.5)[mood]]) ]
+!evaluate_speaking_intent(yes).

@v_speak_sheep_hide[ temper([individualism(-0.5), stress(0.3)[mood]]), effects([exposure(-0.1)[mood]]) ]
+!evaluate_speaking_intent(no).

//---------------
// SPEECH ACT
//--------------

@v_perf_deflect_suspect[ temper([stress(0.8)[mood], exposure(0.7)[mood]]), effects([stress(-0.4)[mood]]) ]
+!choose_speech_performative(deflect(Target)) 
    :   other_players(Others) 
    <-  ?select_by_trait(max, suspicion, Others, Target).

@v_perf_accuse_suspect[ temper([paranoia(0.8), individualism(0.6)]), effects([exposure(0.2)[mood]]) ]
+!choose_speech_performative(accuse(Target)) 
    :   other_players(Others) 
    <-  ?select_by_trait(max, suspicion, Others, Target).

@v_perf_interrogate_quiet[ temper([individualism(0.8), stress(0.3)[mood]]), effects([exposure(0.1)[mood]]) ]
+!choose_speech_performative(interrogate(Target)) 
    :   other_players(Others) 
    <-  ?select_by_trait(min, influence, Others, Target).

@v_perf_defend_ally[ temper([paranoia(-0.5), individualism(-0.5)]), effects([stress(-0.1)[mood]]) ]
+!choose_speech_performative(defend(Target)) 
    :   other_players(Others) 
    <-  ?select_by_trait(max, utility, Others, Target).

@v_perf_agree_leader[ temper([individualism(-0.8), paranoia(-0.2)]), effects([exposure(-0.2)[mood]]) ]
+!choose_speech_performative(agree(Leader)) 
    :   other_players(Others) 
    <-  ?select_by_trait(max, influence, Others, Leader).

@v_perf_suspect_random[ temper([stress(0.6)[mood], individualism(0.4)]), effects([exposure(0.1)[mood]]) ]
+!choose_speech_performative(suspect(Target)) 
    :   other_players(Others) 
    <-  ?random_target(Others, Target).


//---------------
// WHO TO VOTE
//--------------

@v_vote_panic[ temper([stress(0.8)[mood], paranoia(0.7)]), effects([stress(-0.5)[mood]]) ]
+!evaluate_vote(Target) 
    :   other_players(Others) 
    <-  ?select_by_trait(max, suspicion, Others, Target).

@v_vote_logical[ temper([individualism(0.8), stress(-0.2)[mood]]), effects([stress(-0.1)[mood]]) ]
+!evaluate_vote(Target) 
    :   other_players(Others) 
    <-  ?select_by_trait(max, suspicion, Others, Target).

@v_vote_purge_useless[ temper([paranoia(0.7), stress(0.3)[mood]]), effects([exposure(-0.1)[mood]]) ]
+!evaluate_vote(Target) 
    :   other_players(Others) 
    <-  ?select_by_trait(min, utility, Others, Target).

@v_vote_scapegoat[ temper([exposure(0.8)[mood], individualism(0.3)]), effects([exposure(-0.4)[mood]]) ]
+!evaluate_vote(Target) 
    :   other_players(Others) 
    <-  ?select_by_trait(min, influence, Others, Target).

@v_vote_sheep[ temper([individualism(-0.8), stress(0.2)[mood]]), effects([exposure(-0.2)[mood]]) ]
+!evaluate_vote(Target) 
    :   other_players(Others) 
    <-  ?select_by_trait(max, influence, Others, Target).


//---------------
// BELIEF UPDATE
//--------------

@v_op_speak_yes_relieved[ temper([exposure(0.7)[mood], stress(-0.2)[mood]]), effects([exposure(-0.1)[mood]]) ]
+!upd_opinions(Speaker, intent_to_speak(yes))
    <-  !upd_trait(Speaker, influence, 0.1).

@v_op_speak_yes_paranoid[ temper([paranoia(0.7), stress(0.4)[mood]]), effects([stress(0.05)[mood]]) ]
+!upd_opinions(Speaker, intent_to_speak(yes))
    <-  !upd_trait(Speaker, influence, 0.05);
        !upd_trait(Speaker, suspicion, 0.05).

@v_op_speak_no_paranoid[ temper([paranoia(0.6), stress(0.5)[mood]]), effects([stress(0.1)[mood]]) ]
+!upd_opinions(Speaker, intent_to_speak(no))
    <-  !upd_trait(Speaker, influence, -0.1);
        !upd_trait(Speaker, suspicion, 0.1).

@v_op_speak_no_dismissive[ temper([individualism(0.7), paranoia(-0.4)]), effects([]) ]
+!upd_opinions(Speaker, intent_to_speak(no))
    <-  !upd_trait(Speaker, influence, -0.15).

@v_op_interrogate_analytical[ temper([individualism(0.6), paranoia(-0.3)]), effects([stress(0.1)[mood]]) ]
+!upd_opinions(Speaker, interrogate(Target))
    <-  !upd_trait(Speaker, influence, 0.15);
        !upd_trait(Target, suspicion, 0.1).

@v_op_interrogate_paranoid[ temper([paranoia(0.8), stress(0.6)[mood]]), effects([stress(0.15)[mood]]) ]
+!upd_opinions(Speaker, interrogate(Target))
    <-  !upd_trait(Speaker, suspicion, 0.1);
        !upd_trait(Target, suspicion, 0.05).

@v_op_deflect_stressed[ temper([stress(0.6)[mood], exposure(0.5)[mood]]), effects([stress(0.15)[mood]]) ]
+!upd_opinions(Speaker, deflect(Target))
    <-  !upd_trait(Speaker, suspicion, 0.2);
        !upd_trait(Target, suspicion, 0.1).

@v_op_deflect_analytical[ temper([individualism(0.8), paranoia(0.5)]), effects([stress(0.05)[mood]]) ]
+!upd_opinions(Speaker, deflect(Target))
    <-  !upd_trait(Speaker, suspicion, 0.25).

@v_op_voted_me_panic[ temper([stress(0.6)[mood], exposure(0.6)[mood]]), effects([stress(0.3)[mood], exposure(0.4)[mood]]) ]
+!upd_opinions(Voter, vote(Target)) : .my_name(Target)
    <-  !upd_trait(Voter, suspicion, 0.3);
        !upd_trait(Voter, utility, -0.3).

@v_op_voted_me_angry[ temper([individualism(0.8), paranoia(0.5)]), effects([stress(0.1)[mood], exposure(0.2)[mood]]) ]
+!upd_opinions(Voter, vote(Target)) : .my_name(Target)
    <-  !upd_trait(Voter, suspicion, 0.2);
        !upd_trait(Voter, utility, -0.4).

@v_op_trigger_gotcha[ temper([paranoia(0.0), stress(0.0)[mood]]), effects([]) ]
+!upd_opinions(Voter, vote(Target))
    <-  !fetch_log_block(start_discussion_round, 1, RecentLog, cache_if_possible);
        !process_gotcha(Voter, Target, RecentLog).

@v_op_fallback[ temper([paranoia(0.0), stress(0.0)[mood]]), effects([]) ]
+!upd_opinions(_, _).


//---------------
// CHECK OLD LOGS
//--------------
@v_gotcha_contradiction_paranoid[ temper([paranoia(0.7), stress(0.5)[mood]]), effects([stress(-0.2)[mood]]) ]
+!process_gotcha(Voter, Target, RecentLog)
    :   .member(action(speech(Voter, _, defend(Target))), RecentLog)
    <-  !upd_trait(Voter, suspicion, 0.4);
        !upd_trait(Voter, influence, -0.2);
        !upd_trait(Target, suspicion, -0.1).

@v_gotcha_contradiction_analytical[ temper([individualism(0.7), paranoia(-0.2)]), effects([stress(-0.1)[mood]]) ]
+!process_gotcha(Voter, Target, RecentLog)
    :   .member(action(speech(Voter, _, defend(Target))), RecentLog)
    <-  !upd_trait(Voter, suspicion, 0.25);
        !upd_trait(Voter, influence, -0.3).

@v_gotcha_fickle_paranoid[ temper([paranoia(0.6), stress(0.4)[mood]]), effects([stress(-0.1)[mood]]) ]
+!process_gotcha(Voter, Target, RecentLog)
    :   .member(action(speech(Voter, _, accuse(Other))), RecentLog) & Other \== Target
    <-  !upd_trait(Voter, suspicion, 0.15);
        !upd_trait(Voter, influence, -0.1).

@v_gotcha_fickle_arrogant[ temper([individualism(0.8), exposure(-0.3)[mood]]), effects([]) ]
+!process_gotcha(Voter, Target, RecentLog)
    :   .member(action(speech(Voter, _, accuse(Other))), RecentLog) & Other \== Target
    <-  !upd_trait(Voter, influence, -0.2);
        !upd_trait(Voter, utility, -0.1).

@v_gotcha_standard[ temper([paranoia(0.0), stress(0.0)[mood]]), effects([]) ]
+!process_gotcha(Voter, Target, RecentLog)
    <-  !upd_trait(Target, suspicion, 0.1).

@v_gotcha_fallback[ temper([paranoia(0.0), stress(0.0)[mood]]), effects([]) ]
+!process_gotcha(_, _, _).