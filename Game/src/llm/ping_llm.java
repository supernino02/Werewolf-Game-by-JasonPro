package llm;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.ASSyntax;

public class ping_llm extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        //ensure maximum arity of 3
        if (args.length < 3) {
            ts.getLogger().severe("ping_llm requires 3 arguments: (ModelName, LogConfig, ResultVar)");
            return false;
        }

        //get the model name 
        String modelName = ((StringTerm) args[0]).getString();
        
        //get the booolean logConfig (true/false) or the log directory path as a string
        String logConfig = args[1].isString() ? ((StringTerm) args[1]).getString() : args[1].toString();

        //explicitly ping and initialize the model
        String pingResponse = OllamaAPI.ping(modelName, logConfig);

        //bind the response as a last element in the unifier to the provided variable
        return un.unifies(args[2], ASSyntax.createString(pingResponse));
    }
}