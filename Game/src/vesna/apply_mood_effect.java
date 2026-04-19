package vesna;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;

public class apply_mood_effect extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        // 1. Extract the trait name and the numerical effect
        String trait = args[0].toString().replaceAll("^\"|\"$", "");
        double effectValue = ((NumberTerm) args[1]).solve();
        
        // 2. Safely cast the agent to ProAgent and apply the effect
        if (ts.getAg() instanceof ProAgent) {
            ((ProAgent) ts.getAg()).applyMoodEffect(trait, effectValue);
        } else {
            ts.getLogger().warning("Agent is not a ProAgent. Mood effect ignored.");
        }
        
        return true;
    }
}