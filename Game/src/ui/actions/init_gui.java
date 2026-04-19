package ui.actions;

import ui.core.AgentGUI;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class init_gui extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        
        // 1. Default fallback values updated to [-1.0, 1.0]
        double minBound = -1.0;
        double maxBound = 1.0;
        int sigDigits = 2;
        double delayMs = 0.0;

        // 2. Safely query the Belief Base for trait_bounds
        Literal boundsPattern = Literal.parseLiteral("trait_bounds(_,_,_)");
        Literal boundsBelief = ts.getAg().findBel(boundsPattern, new Unifier());
        
        if (boundsBelief != null) {
            try {
                minBound = ((NumberTerm) boundsBelief.getTerm(0)).solve();
                maxBound = ((NumberTerm) boundsBelief.getTerm(1)).solve();
                sigDigits = (int) ((NumberTerm) boundsBelief.getTerm(2)).solve();
            } catch (Exception e) {
                ts.getLogger().warning("Failed to parse trait_bounds. Using defaults.");
            }
        }

        // 3. Safely query the Belief Base for writing_delay_ms
        Literal delayPattern = Literal.parseLiteral("setting(writing_delay_ms(_))");
        Literal delayBelief = ts.getAg().findBel(delayPattern, new Unifier());
        
        if (delayBelief != null) {
            try {
                Structure innerStruct = (Structure) delayBelief.getTerm(0);
                delayMs = ((NumberTerm) innerStruct.getTerm(0)).solve();
            } catch (Exception e) {
                ts.getLogger().warning("Failed to parse writing_delay_ms. Using 0.0.");
            }
        }

        // 4. Pass everything as primitives to the GUI thread
        AgentGUI.initGUI(
            ts.getUserAgArch().getAgName(), 
            ts, 
            UIUtils.cleanStr(args[0]), 
            UIUtils.getList(args[1]),
            minBound, 
            maxBound, 
            sigDigits,
            delayMs
        );
        
        return true;
    }
}