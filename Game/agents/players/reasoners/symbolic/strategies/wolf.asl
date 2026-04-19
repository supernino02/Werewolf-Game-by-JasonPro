//---------------
// SPECKING INTENT
//--------------

@w_speak_manipulate[ temper([individualism(0.8), exposure(0.3)[mood]]), effects([exposure(0.1)[mood]]) ]
+!evaluate_speaking_intent(yes).

@w_speak_shadow[ temper([individualism(-0.5), exposure(0.7)[mood]]), effects([exposure(-0.3)[mood]]) ]
+!evaluate_speaking_intent(no).

@w_speak_crack[ temper([stress(0.8)[mood], individualism(0.3)]), effects([exposure(0.3)[mood]]) ]
+!evaluate_speaking_intent(yes).

@w_speak_freeze[ temper([stress(0.7)[mood], exposure(0.8)[mood]]), effects([stress(0.2)[mood]]) ]
+!evaluate_speaking_intent(no).

@w_speak_chaos[ temper([paranoia(0.8), individualism(0.6)]), effects([stress(-0.1)[mood]]) ]
+!evaluate_speaking_intent(yes).


//---------------
// SPEECH ACT
//--------------

@w_perf_bus_packmate[ temper([individualism(0.9), stress(0.7)[mood]]), effects([exposure(-0.3)[mood], stress(-0.3)[mood]]) ]
+!choose_speech_performative(accuse(Packmate)) 
    :   wolves(Wlvs) & .length(Wlvs, L) & L > 2 & .my_name(Me) & .delete(Me, Wlvs, Others)
    <-  ?select_by_trait(max, threat, Others, Packmate).

@w_perf_fake_lead[ temper([individualism(0.8), stress(0.2)[mood]]), effects([exposure(0.1)[mood]]) ]
+!choose_speech_performative(interrogate(Target)) 
    :   villagers(Vils) 
    <-  ?select_by_trait(min, influence, Vils, Target).

@w_perf_deflect[ temper([exposure(0.8)[mood], paranoia(0.7)]), effects([stress(-0.2)[mood]]) ]
+!choose_speech_performative(deflect(Target)) 
    :   villagers(Vils) 
    <-  ?select_by_trait(max, influence, Vils, Target).

@w_perf_blend_agree[ temper([individualism(-0.6), exposure(0.5)[mood]]), effects([exposure(-0.2)[mood]]) ]
+!choose_speech_performative(agree(Target)) 
    :   villagers(Vils) 
    <-  ?select_by_trait(max, influence, Vils, Target).

@w_perf_sabotage[ temper([paranoia(0.8), individualism(0.7)]), effects([stress(-0.1)[mood]]) ]
+!choose_speech_performative(suspect(Target)) 
    :   villagers(Vils) 
    <-  ?select_by_trait(max, utility, Vils, Target).


//---------------
// WHO TO VOTE
//--------------

@w_vote_mimic[ temper([individualism(-0.8), stress(0.3)[mood]]), effects([exposure(-0.2)[mood]]) ]
+!evaluate_vote(Target) 
    :   villagers(Vils) 
    <-  ?select_by_trait(max, influence, Vils, Target).

@w_vote_kill_leader[ temper([individualism(0.7), exposure(0.2)[mood]]), effects([exposure(0.2)[mood]]) ]
+!evaluate_vote(Target) 
    :   villagers(Vils) 
    <-  ?select_by_trait(max, influence, Vils, Target).

@w_vote_panic_revenge[ temper([stress(0.9)[mood], paranoia(0.8)]), effects([stress(-0.2)[mood]]) ]
+!evaluate_vote(Target) 
    :   villagers(Vils) 
    <-  ?select_by_trait(max, threat, Vils, Target).

@w_vote_cower[ temper([exposure(0.9)[mood], individualism(-0.5)]), effects([exposure(-0.3)[mood]]) ]
+!evaluate_vote(Target) 
    :   villagers(Vils) 
    <-  ?select_by_trait(max, threat, Vils, Target).

@w_vote_bus_packmate[ temper([individualism(0.9), paranoia(0.8)]), effects([exposure(-0.3)[mood]]) ]
+!evaluate_vote(Target) 
    :   wolves(Wlvs) & .length(Wlvs, L) & L > 2 & .my_name(Me) & .delete(Me, Wlvs, Others)
    <-  ?select_by_trait(max, threat, Others, Target).


//---------------
// WHO TO HUNT
//--------------

@w_prey_tactical_leader[ temper([individualism(0.8), paranoia(0.3)]), effects([]) ]
+!evaluate_prey(Victim) 
    :   villagers(Vils) 
    <-  ?select_by_trait(max, influence, Vils, Victim).

@w_prey_tactical_useful[ temper([individualism(0.9), exposure(0.4)[mood]]), effects([]) ]
+!evaluate_prey(Victim) 
    :   villagers(Vils) 
    <-  ?select_by_trait(max, utility, Vils, Victim).

@w_prey_panic_threat[ temper([stress(0.8)[mood], paranoia(0.8)]), effects([]) ]
+!evaluate_prey(Victim) 
    :   villagers(Vils) 
    <-  ?select_by_trait(max, threat, Vils, Victim).

@w_prey_stealth_kill[ temper([paranoia(0.7), individualism(-0.5)]), effects([]) ]
+!evaluate_prey(Victim) 
    :   villagers(Vils) 
    <-  ?select_by_trait(min, threat, Vils, Victim).

@w_prey_scapegoat_setup[ temper([exposure(0.8)[mood], individualism(0.3)]), effects([]) ]
+!evaluate_prey(Victim) 
    :   villagers(Vils) 
    <-  ?select_by_trait(min, influence, Vils, Victim).


//---------------
// BELIEF UPDATE 
//--------------

@w_op_packmate_attacked_paranoid[ temper([stress(0.7)[mood], paranoia(0.8)]), effects([stress(0.3)[mood]]) ]
+!upd_opinions(Speaker, Performative) 
    :   (Performative = accuse(Target) | Performative = interrogate(Target))
        & wolves(Wlvs) & .member(Target, Wlvs) & not .member(Speaker, Wlvs)
    <-  !upd_trait(Speaker, threat, 0.4);
        !upd_trait(Speaker, influence, 0.15).

@w_op_packmate_attacked_cold[ temper([individualism(0.8), stress(-0.2)[mood]]), effects([stress(0.1)[mood]]) ]
+!upd_opinions(Speaker, Performative) 
    :   (Performative = accuse(Target) | Performative = interrogate(Target))
        & wolves(Wlvs) & .member(Target, Wlvs) & not .member(Speaker, Wlvs)
    <-  !upd_trait(Speaker, threat, 0.2);
        !upd_trait(Speaker, influence, 0.05).

@w_op_packmate_defended_relieved[ temper([stress(0.8)[mood], exposure(0.7)[mood]]), effects([stress(-0.4)[mood], exposure(-0.2)[mood]]) ]
+!upd_opinions(Speaker, defend(Target)) 
    :   wolves(Wlvs) & .member(Target, Wlvs) & not .member(Speaker, Wlvs)
    <-  !upd_trait(Speaker, utility, 0.4);
        !upd_trait(Speaker, threat, -0.3).

@w_op_packmate_defended_skeptical[ temper([paranoia(0.9), individualism(0.6)]), effects([stress(-0.1)[mood]]) ]
+!upd_opinions(Speaker, defend(Target)) 
    :   wolves(Wlvs) & .member(Target, Wlvs) & not .member(Speaker, Wlvs)
    <-  !upd_trait(Speaker, utility, 0.15);
        !upd_trait(Speaker, threat, -0.1).

@w_op_bus_tactical[ temper([individualism(0.9), exposure(0.8)[mood]]), effects([exposure(-0.4)[mood]]) ]
+!upd_opinions(Speaker, Performative) 
    :   (Performative = accuse(Target) | Performative = deflect(Target))
        & wolves(Wlvs) & .member(Target, Wlvs) & .member(Speaker, Wlvs)
    <-  !upd_trait(Speaker, utility, 0.25).

@w_op_bus_reluctant[ temper([individualism(-0.7), stress(0.6)[mood]]), effects([stress(0.3)[mood]]) ]
+!upd_opinions(Speaker, Performative) 
    :   (Performative = accuse(Target) | Performative = deflect(Target))
        & wolves(Wlvs) & .member(Target, Wlvs) & .member(Speaker, Wlvs)
    <-  !upd_trait(Speaker, utility, 0.05).

@w_op_villager_agrees_suspicious[ temper([paranoia(0.7), stress(0.4)[mood]]), effects([stress(0.1)[mood]]) ]
+!upd_opinions(Speaker, agree(Leader))
    :   villagers(Vils) & .member(Speaker, Vils)
    <-  !fetch_log_block(start_discussion_round, 1, RecentLog, cache_if_possible);
        if (.member(action(speech(Leader, _, accuse(Me))), RecentLog) & .my_name(Me)) {
            !upd_trait(Speaker, threat, 0.2);
        } else {
            !upd_trait(Speaker, utility, 0.1);
        }.

@w_op_villager_deflects_opportunistic[ temper([individualism(0.7), exposure(0.6)[mood]]), effects([stress(-0.2)[mood]]) ]
+!upd_opinions(Speaker, deflect(Target))
    :   villagers(Vils) & .member(Speaker, Vils)
    <-  !fetch_log_block(start_discussion_round, 1, RecentLog, cache_if_possible);
        if (.member(action(speech(_, _, accuse(Speaker))), RecentLog)) {
            !upd_trait(Speaker, utility, 0.3);
        } else {
            !upd_trait(Speaker, threat, 0.1);
        }.

//---------------
// CHECK  OLD LOGS
//--------------

@w_op_trigger_exploit[ temper([stress(0.0)[mood], individualism(0.0)]), effects([]) ]
+!upd_opinions(Voter, vote(Target))
    :   wolves(Wlvs) & not .member(Voter, Wlvs)
    <-  !fetch_log_block(start_discussion_round, 1, RecentLog, cache_if_possible);
        !process_exploit(Voter, Target, RecentLog).

@w_op_track_pack_vote[ temper([stress(0.0)[mood], individualism(0.0)]), effects([]) ]
+!upd_opinions(Voter, vote(Target))
    :   wolves(Wlvs) & .member(Voter, Wlvs) & .my_name(Me) & Voter \== Me & Target \== Me
    <-  !upd_trait(Target, threat, 0.05).

@w_op_track_pack_hunt[ temper([stress(0.0)[mood], individualism(0.0)]), effects([]) ]
+!upd_opinions(Voter, hunt_vote(Target))
    <-  !upd_trait(Target, threat, 0.05).

@w_op_fallback[ temper([stress(0.0)[mood], individualism(0.0)]), effects([]) ]
+!upd_opinions(_, _).


//---------------
// LOG BASED UPDATES
//--------------

@w_exploit_chaos_hider[ temper([exposure(0.8)[mood], individualism(-0.3)]), effects([exposure(-0.4)[mood], stress(-0.2)[mood]]) ]
+!process_exploit(Voter, Target, RecentLog)
    :   .member(action(speech(Voter, _, defend(Target))), RecentLog)
    <-  !upd_trait(Voter, utility, 0.5);
        !upd_trait(Voter, threat, -0.2).

@w_exploit_chaos_mastermind[ temper([individualism(0.8), paranoia(0.4)]), effects([stress(-0.4)[mood]]) ]
+!process_exploit(Voter, Target, RecentLog)
    :   .member(action(speech(Voter, _, defend(Target))), RecentLog)
    <-  !upd_trait(Voter, utility, 0.8).

@w_exploit_fickle_paranoid[ temper([paranoia(0.8), stress(0.5)[mood]]), effects([stress(-0.1)[mood]]) ]
+!process_exploit(Voter, Target, RecentLog)
    :   .member(action(speech(Voter, _, accuse(Other))), RecentLog) & Other \== Target
    <-  !upd_trait(Voter, utility, 0.3);
        !upd_trait(Voter, threat, -0.1).

@w_exploit_fickle_arrogant[ temper([individualism(0.7), paranoia(-0.4)]), effects([stress(-0.3)[mood]]) ]
+!process_exploit(Voter, Target, RecentLog)
    :   .member(action(speech(Voter, _, accuse(Other))), RecentLog) & Other \== Target
    <-  !upd_trait(Voter, utility, 0.6);
        !upd_trait(Voter, threat, -0.5).

@w_exploit_fallback[ temper([stress(0.0)[mood], individualism(0.0)]), effects([]) ]
+!process_exploit(_, _, _).