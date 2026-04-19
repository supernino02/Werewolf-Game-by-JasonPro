//---------------
// DEFAULT NARRATOR STYLE (for LLM prompt construction)
//---------------
narrator_style(Desc) :- 
    .concat("- TONE: Conversational, engaging, slightly dramatic but clear.\n",
            "- VOCABULARY: Natural tabletop language (e.g., 'You guys', 'The village', 'The pack'). Use contractions (you're, let's, it's).\n",
            "- FOCUS: Setting the mood while delivering phase mechanics.\n",
            "- PROHIBITION: DO NOT sound like a robotic terminal. DO NOT use overly formal words like 'Furthermore' or 'Therefore'.", 
            Desc).

//---------------
// EDGES MESSAGES
//---------------
narrator_message(setup_wolves, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Role Assignment - wolf" &
    PhaseIntent = "Inform the player that their role is Wolf and they must hunt villagers at night." &
    Message = "You are a Wolf. Coordinate with your pack tonight to hunt villagers and survive.".

narrator_message(setup_villagers, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Role Assignment - villager" &
    PhaseIntent = "Inform the player that their role is Villager and they must survive and find the wolves." &
    Message = "You are a Villager. Rely on discussion to find the wolves and survive the night.".

narrator_message(start_wolves, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Game Start" &
    PhaseIntent = "Announce the start of the game. Prompt the wolves to hunt down the villagers." &
    Message = "The game has begun. You are the wolf pack, hunt down the villagers.".

narrator_message(start_villagers, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Game Start" &
    PhaseIntent = "Announce the start of the game. Prompt the innocent villagers to find the wolves among them." &
    Message = "The game has begun. You are innocent villagers, find the wolves among you.".

narrator_message(night_falls_wolves, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Night Phase" &
    PhaseIntent = "Announce that night has fallen. Instruct the wolves to choose a victim from the village." &
    Message = "Night has fallen. The village sleeps, so choose your victim.".

narrator_message(night_falls_villagers, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Night Phase" &
    PhaseIntent = "Announce that night has fallen and the village sleeps while the wolves prowl." &
    Message = "Night has fallen. Sleep safely while the wolves prowl.".

narrator_message(hunt_success_wolves, [Victim, villager], PhaseTitle, PhaseIntent, Message) :-
    .term2string(Victim, VStr) &
    PhaseTitle = "Hunt Success" &
    .concat("Inform the wolves that they successfully hunted and killed the innocent player ", VStr, ".", PhaseIntent) &
    .concat("Success! The pack has hunted and killed the innocent villager ", VStr, ".", Message).

narrator_message(hunt_success_wolves, [Victim, wolf], PhaseTitle, PhaseIntent, Message) :-
    .term2string(Victim, VStr) &
    PhaseTitle = "Hunt Backfired" &
    .concat("Inform the wolves that their hunt backfired and they accidentally killed their own packmate ", VStr, ".", PhaseIntent) &
    .concat("Friendly fire! In the chaos, you accidentally killed your packmate, ", VStr, ".", Message).

narrator_message(hunt_success_villagers, [Victim, villager], PhaseTitle, PhaseIntent, Message) :-
    .term2string(Victim, VStr) &
    PhaseTitle = "Morning Tragedy" &
    .concat("Announce to the village that the innocent player ", VStr, " was killed in the night.", PhaseIntent) &
    .concat("A morning tragedy. The innocent villager ", VStr, " was killed in the night.", Message).

narrator_message(hunt_success_villagers, [Victim, wolf], PhaseTitle, PhaseIntent, Message) :-
    .term2string(Victim, VStr) &
    PhaseTitle = "Morning Surprise" &
    .concat("Announce to the village that the player ", VStr, " was killed in the night, and reveal that they were a wolf.", PhaseIntent) &
    .concat("A bizarre morning. ", VStr, " is dead, and their corpse reveals they were a wolf!", Message).

narrator_message(hunt_victim, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "You Died" &
    PhaseIntent = "Inform the player that they were attacked in the night and did not survive." &
    Message = "You were attacked by the wolves in the dead of night. You are dead.".

narrator_message(hunt_tie_retry_wolves, K, PhaseTitle, PhaseIntent, Message) :-
    .term2string(K, KStr) &
    PhaseTitle = "Hunt Tied" &
    .concat("Inform the wolves that their vote tied. They must vote again and have ", KStr, " attempts remaining.", PhaseIntent) &
    .concat("The pack is divided and tied. Vote again, you have ", KStr, " attempts remaining.", Message).

narrator_message(hunt_tie_wolves, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Hunt Failed" &
    PhaseIntent = "Inform the wolves that they failed to agree on a target, so the night ends without a kill." &
    Message = "You failed to reach a consensus. The night ends without a kill.".

narrator_message(hunt_tie_villagers, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Peaceful Morning" &
    PhaseIntent = "Announce to the village that morning has come and no one was killed during the night." &
    Message = "Morning has broken. The night was quiet and no one was killed.".

narrator_message(discussion_start_wolves, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Discussion" &
    PhaseIntent = "Announce the start of the discussion round. Instruct the wolves to deceive the village." &
    Message = "Discussion has started. Blend in and deceive the villagers.".

narrator_message(discussion_start_villagers, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Discussion" &
    PhaseIntent = "Announce the start of the discussion round. Instruct the villagers to find the wolves." &
    Message = "Discussion has started. Share suspicions and find the wolves.".

narrator_message(peasant_vote_early, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Vote Started" &
    PhaseIntent = "Announce that the discussion ended early because no one wanted to speak, and the execution vote is now starting." &
    Message = "Discussion ended early. The execution vote begins now.".

narrator_message(peasant_vote_timeout, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Vote Started" &
    PhaseIntent = "Announce that the discussion time is over and the execution vote has started." &
    Message = "Time is up. The peasant execution vote has officially started.".

narrator_message(exec_result, [wolves, wolf, Victim], PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Execution Result - Failure" &
    .concat("Inform the wolves that the village executed their packmate ", Victim, ".", PhaseIntent) &
    .concat("Disaster! The village executed your brother, ", Victim, ". Stay hidden.", Message).

narrator_message(exec_result, [villagers, wolf, Victim], PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Execution Result - Success" &
    .concat("Announce to the village that the executed player ", Victim, " was a wolf.", PhaseIntent) &
    .concat("Justice! The village executed ", Victim, ", who was secretly a wolf.", Message).

narrator_message(exec_result, [wolves, villager, Victim], PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Execution Result - Success" &
    .concat("Inform the wolves that the village foolishly executed the innocent player ", Victim, ".", PhaseIntent) &
    .concat("Excellent. The village foolishly executed the innocent ", Victim, ".", Message).

narrator_message(exec_result, [villagers, villager, Victim], PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Execution Result - Failure" &
    .concat("Announce to the village that the executed player ", Victim, " was an innocent villager.", PhaseIntent) &
    .concat("A tragic mistake. The town executed ", Victim, ", an innocent villager.", Message).

narrator_message(exec_victim, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Executed" &
    PhaseIntent = "Inform the player that the town voted against them and they have been executed." &
    Message = "The village voted against you. You have been executed.".

narrator_message(exec_tie_wolves, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Execution Tied" &
    PhaseIntent = "Inform the wolves that the village execution vote tied, so no one is executed today." &
    Message = "The village execution vote tied. No one dies today, a perfect outcome for the pack.".

narrator_message(exec_tie_villagers, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Execution Tied" &
    PhaseIntent = "Announce to the village that the execution vote tied, so no one is executed today." &
    Message = "The execution vote tied. Nobody is executed today.".

narrator_message(villagers_win, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Game Over" &
    PhaseIntent = "Announce that all wolves have been eliminated and the village wins the game." &
    Message = "THE VILLAGE WINS! All wolves have been eliminated.".

narrator_message(wolves_win, no_parameters, PhaseTitle, PhaseIntent, Message) :-
    PhaseTitle = "Game Over" &
    PhaseIntent = "Announce that the wolves have reached parity and the wolves win the game." &
    Message = "THE WOLVES WIN! All villagers have been eliminated.".