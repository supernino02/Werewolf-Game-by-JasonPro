package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class player_message extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (!UIUtils.hasUI(ts)) return true;
        
        UIUtils.getGUI(ts).playerMessage(
            UIUtils.cleanStr(args[0]), //sender
            UIUtils.cleanStr(args[1]), //string
            UIUtils.cleanStr(args[2]), // The Performative
            UIUtils.getDelay(ts)
        );
        return true;
    }
}