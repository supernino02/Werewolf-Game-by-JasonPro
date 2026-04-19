//---------------
// ROLES DEFINITIONS
//---------------
players(Players)     :- .findall(P, role(P,_), Players).
wolves(Wolves)       :- .findall(P, role(P,wolf), Wolves).
villagers(Villagers) :- .findall(P, role(P,villager), Villagers).

//---------------
// UTILITY TO SAMPLE RANDOM
//---------------
random_target(Candidates, Target) 
    :-  Candidates \== []
    &   .shuffle(Candidates, [Target | _]).

//fallback
random_target([], Target) 
    :-  not iam(narrator) 
    & .my_name(Target).

//retrieve all players except self
other_players(Candidates) 
    :-  .my_name(Me) 
    &   players(Players)
    &   .delete(Me, Players, Candidates).