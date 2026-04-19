//---------------
// PSYCHOLOGICAL EVALUATION ATTRIBUTES (for LLM update_opinions prompt)
//---------------


//describe the absolute scale for each trait to help the LLM understand the current values

eval_trait(suspicion, "A low level means you believe they are innocent and trustworthy, while a bigger value means you strongly suspect they are a wolf.") 
    :- iam(villager).

eval_trait(threat, "A low level means they are ignoring you or pose no danger, while a bigger value means they are actively attacking you or your pack.") 
    :- iam(wolf).

eval_trait(utility, "A low level means they are an obstacle or useless to your strategy, while a bigger value means they are actively helpful to your goals.").
eval_trait(influence, "A low level means they are losing credibility or looking foolish, while a bigger value means they are highly persuasive and leading the game.").

//create a block of text to describe all the traits to the llm
available_traits_str(TraitsStr) 
    :-  .findall(Desc, (
            eval_trait(Name, Meaning) 
        &   .concat("- ",Name, ": ", Meaning, Desc)
        ), List) 
    &   join_strings(List, TraitsStr, "\n").

//---------------
// TRAITS TO PLAYER'S BELIEFS ABOUT OTHER PLAYERS (for LLM prompt construction)
//---------------

//format all the traits about a player in a single string (e.g., "alice(suspicion: 0.45, threat: -0.1)")
player_traits_str(Player, PlayerStr) 
    :-  .findall(TraitStr, 
            (   trait(Player, TraitName, Val) 
            &   .term2string(Val, ValStr) 
            &   .concat(TraitName, ": ", ValStr, TraitStr)
            ), TraitList)         
        //delimit the traits with a comma and space, and concatenate with the player name
    &   join_strings(TraitList, JoinedTraits, ", ") 
    &   .concat(Player, "(", JoinedTraits, ")", PlayerStr).


//every (other) player is put on a new line, with their evaluated traits
all_players_traits_str(AllStr) 
    :-  .my_name(Me) 
    &   players(Players) 
    &   .shuffle(Players, Shuffled) //!important: shuffle to avoid explicit bias (nontheless is still here)
    &   .findall(PlayerStr, 
            (   .member(P, Shuffled) 
            &   P \== Me 
            &   player_traits_str(P, PlayerStr)
            ), AllPlayersList) 
            
        //newline for each player
    &   join_strings(AllPlayersList, AllStr, "\n").