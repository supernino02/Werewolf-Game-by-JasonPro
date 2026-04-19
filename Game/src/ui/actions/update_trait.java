package ui.actions;

import jason.asSemantics.*;
import jason.asSyntax.*;

public class update_trait extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        // Fail silently if the agent is not connected to a UI (e.g., standard AI player)
        if (!UIUtils.hasUI(ts)) return true;
        
        // 1. Extract the arguments passed from the Jason plan
        String player = UIUtils.cleanStr(args[0]);
        String trait = UIUtils.cleanStr(args[1]);
        double value = ((NumberTerm) args[2]).solve();
        
        // 2. Safely pass the data to the JavaFX GUI thread
        UIUtils.getGUI(ts).updateTrait(player, trait, value);
        
        return true;
    }
}