package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class phase_message extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (!UIUtils.hasUI(ts)) return true;
        
        UIUtils.getGUI(ts).phaseMessage(
            UIUtils.cleanStr(args[0]), // Phase Title
            UIUtils.cleanStr(args[1]), // Message
            UIUtils.getDelay(ts)
        );
        return true;
    }
}