package bert;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.ASSyntax;

public class ping_bert extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {

        //the arg 1 is the name of the log file
        String logConfig = args[0].isString() ? ((StringTerm) args[0]).getString() : args[0].toString();

        //call teh main driver 
        String pingResponse = python_BERT_API.ping(logConfig);

        //return the value
        return un.unifies(args[1], ASSyntax.createString(pingResponse));
    }
}