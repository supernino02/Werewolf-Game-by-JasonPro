package utils;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;

public class ms_timestamp extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        //simply return the current timestamp in milliseconds as a number term
        return un.unifies(args[0], new NumberTermImpl(System.currentTimeMillis()));
    }
}