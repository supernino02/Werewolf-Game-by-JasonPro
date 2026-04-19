package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class set_status extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (!UIUtils.hasUI(ts)) return true;
        UIUtils.getGUI(ts).setPlayerStatus(UIUtils.cleanStr(args[0]));
        return true;
    }
}