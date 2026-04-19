package bert;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;

public class guess_performative extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        //get the message from the argument
        String message = ((StringTerm) args[0]).getString();
        //get the agent (for logging purposes)
        String agentName = ts.getAgArch().getAgName(); 

        //call the BERT API to get the predicted belief
        String beliefStr = python_BERT_API.predict(agentName, message);

        //inject back 
        ts.getAg().addBel(Literal.parseLiteral(beliefStr));

        return true;
    }
}