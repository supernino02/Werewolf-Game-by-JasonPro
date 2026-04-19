//---------------
// LOG MANAGEMENT
//---------------
shared_log([]).

//DEBUG: print the phase title and the action 
+shared_log([phase(_, Title, _)[phase_id(PhaseUID), audience(Aud)] | _]) : setting(debug_log(true))
    <- .print("Phase [", PhaseUID, "]: ", Title, " (Audience: ", Aud, ")").

+shared_log([action(Action)[phase_id(PhaseUID), audience(Aud)] | _])     : setting(debug_log(true))
    <- .print("             [", PhaseUID, "] ", Action, " (Audience: ", Aud, ")").

//do not log those actions
+!append_log(action(intent_to_speak(_, _)), _).
+!append_log(action(time_to_speak(_)), _).

//save the  log entry with  annotation to ensure no data leakagae
@append_log_atomic[atomic]
+!append_log(RawItem, Audience) 
    : current_phase_uid(PhaseUID)
    & shared_log(CurrentLog)
    & compress_audience(Audience, FinalAud)
    <-  //remove  annotations for readability and correctness
        RawItem =.. [Functor, Args, Annots];
        CleanItem =.. [Functor, Args, []];

        //update the log with the new entry as head
        -+shared_log([ CleanItem[phase_id(PhaseUID), audience(FinalAud)] | CurrentLog ]).

//---------------
// COMPRESS AUDIENCE (for readability)
//---------------
exact_set_match(ListA, ListB) :- 
    .length(ListA, L) & .length(ListB, L) & .difference(ListA, ListB, []).

compress_audience(List, players)   :- players(All) & exact_set_match(List, All).
compress_audience(List, wolves)    :- wolves(All) & exact_set_match(List, All).
compress_audience(List, villagers) :- villagers(All) & exact_set_match(List, All).

//allback: raw list or single atom
compress_audience(List, List).

//---------------
// DE-COMPRESS AUDIENCE & CHECK PERMISSIONS
//---------------
expand_to_list(players, L)   :- players(L).
expand_to_list(wolves, L)    :- wolves(L).
expand_to_list(villagers, L) :- villagers(L).
expand_to_list(List, List)   :- .list(List).
expand_to_list(Atom, [Atom]) :- not .list(Atom). //fallback for singletons

//the audience is EVERYONE, is ok 
target_is_subset(players, _).

//the audience is a Term (list or singleton) and the reader is the same, is ok
target_is_subset(X, X).

//check that the Reader (list) is a subset of the Audience (list)
target_is_subset(Audience, Reader)  
    :-  expand_to_list(Audience, AudList)
    &   expand_to_list(Reader, ReadList)
    &   .difference(ReadList, AudList, []). //empty difference: is a subset


//---------------
// PARTIAL LOG EXTRACTION (for LLM context management)
//---------------
extract_k(_, _, _, 0, _, []).   // reached maximum phase to extract (K)
extract_k(_, _, _, _, 0, []).   // reached absolute maximum elements (N)
extract_k([], _, _, _, _, []).  // reached end of log

//not authorized: skip the item and do not decrement K or N
extract_k([Item[phase_id(PhaseUID), audience(Aud)] | Tail], TargetCond, Reader, K, N, Rest)
    :-  not target_is_subset(Aud, Reader) 
    &   extract_k(Tail, TargetCond, Reader, K, N, Rest).

//authorized Phase: match the target condition, extract, and decrement BOTH K and N.
//annotations are removed, since not needed
extract_k([phase(Name, Title, Msg)[phase_id(PhaseUID), audience(Aud)] | Tail], TargetCond, Reader, K, N, [phase(Name, Title, Msg) | Rest])
    :-  (TargetCond == any | TargetCond == Name) 
    &   NextK = K - 1 
    &   NextN = N - 1 
    &   extract_k(Tail, TargetCond, Reader, NextK, NextN, Rest).

//authorized Action (or non-matching phase): extract and decrement ONLY N.
//annotations are removed, since not needed
extract_k([Item[phase_id(PhaseUID), audience(Aud)] | Tail], TargetCond, Reader, K, N, [Item | Rest])
    :-  NextN = N - 1 
    &   extract_k(Tail, TargetCond, Reader, K, NextN, Rest).

//---------------
// LOG EXTRACTION INTERFACE
//---------------
//if the requesting player is dead, return empty.
//this is caused by the asynchronous nature of the game: a player could be killed but stil try to obtain the log one last time.
get_log_chunk(_, [Reader], _, []) :- not role(Reader, _).

//if not cached, compute and cache
get_log_chunk(TargetCondition, ReaderList, K, ReversedResult)
    :-  shared_log(FullLog)
    &   compress_audience(ReaderList, FinalReader)
    &   setting(max_log_entries_retrieved(MaxN))
    &   extract_k(FullLog, TargetCondition, FinalReader, K, MaxN, Result)
    &   .reverse(Result, ReversedResult). // to have a correct chronological order
