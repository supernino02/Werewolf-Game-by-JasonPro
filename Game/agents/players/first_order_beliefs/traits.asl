//---------------
// TRAIT INITIALIZATION
//---------------

//ensure value in range [Min, Max] and rounded to DP decimal places
clamp(Val, Clamped)
    :-  setting(trait_bounds(Min, Max, DP))
    &   Factor  = 10**DP
    &   TempVal = math.max(Min, math.min(Max, Val))
    &   Clamped = math.round(TempVal * Factor) / Factor.

//MAGIC NUMBERS to add gaussian noise to and initialize a clamped trait
//ask GEMINI for further info :(
add_noise(Mean, StdDev, ClampedResult)
    :-  .random(U1) 
    &   .random(U2)
    &   RealU1 = math.max(0.0001, U1)
    &   Z0 = math.sqrt(-2.0 * math.log(RealU1)) * math.cos(6.28318530718 * U2)
    &   RawVal = Mean + (Z0 * StdDev)
    &   clamp(RawVal, ClampedResult).

//initialize a trait if does not exist yet, 
+?trait(Player, Attr, Value) 
    :   not trait(Player, Attr, _)
    &   setting(trait_bounds(Min, Max, _))
    &   setting(trait_init_noise(StdDev))
    &   Mean = (Min + Max) / 2.0
    &   add_noise(Mean, StdDev, Value) // Evaluated mathematically, no yield
    <-  +trait(Player, Attr, Value).

//else extract it
+?trait(Player, Attr, Value) 
    :   trait(Player, Attr, Value).

//---------------
// TRAIT MANAGEMENT
//---------------

+!upd_trait(Player, _, _) : .my_name(Player).       //no traits about yourself
+!upd_trait(Player, _, _) : not role(Player, _).    //no traits about dead players

//valid update: get current value, add delta, clamp and update belief base (and ui)
@upd_trait[atomic]
+!upd_trait(Player, Attr, Delta)
    <-  ?trait(Player, Attr, CurrentVal);          //get current value
        ?clamp(CurrentVal + Delta, NewVal);        //clamp it
        .abolish(trait(Player, Attr, _));
        +trait(Player, Attr, NewVal).


//whan updated, update the ui and log the change
+trait(Player, Attr, Value)
    <-  if (ui(_)) { ui.actions.update_trait(Player, Attr, Value); };
        !log_trait_update(Player, Attr, Value).


//---------------
// LOGGING PURPOSE
//---------------
trait_log([]).


//if requested, add to it
@log_trait_update_active[atomic]
+!log_trait_update(Player, Attr, Value)
    :   setting(save_trait_evolution(true))
    &   current_phase_uid(PhaseUID)
    &   trait_log(CurrentLog)
    <-  // Prepend the updated trait to the agent's internal log with the phase annotation
        -+trait_log([ trait(Player, Attr, Value)[phase_id(PhaseUID)] | CurrentLog ]).

//fallback
+!log_trait_update(_, _, _).