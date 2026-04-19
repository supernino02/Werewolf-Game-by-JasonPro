//---------------
// UTILITIES BELIEFS 
//---------------

//strategies:  min,max,random
pick_by_strategy(min, [val(_, T) | _], T).
pick_by_strategy(max, Sorted, T) :- Sorted \== [] & .reverse(Sorted, [val(_, T) | _]).
pick_by_strategy(random, Candidates, Target) :- random_target(Candidates, Target).
//fallback for empty list: return self (is the only safe  option)
pick_by_strategy(_, [], T) :- .my_name(T).

+?select_by_trait(Op, Attr, Candidates, Target)
    <-  //lazy initialization to ensure traits are loaded before we try to access them
        for ( .member(P, Candidates) ) { ?trait(P, Attr, _); };        
        //now is possible to  retrieve them safely
        .findall(val(V, P), (.member(P, Candidates) & trait(P, Attr, V)), List);
        //sort them 
        .sort(List, Sorted);
        //pick by strategy
        ?pick_by_strategy(Op, Sorted, Target).
