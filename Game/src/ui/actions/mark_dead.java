package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class mark_dead extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        UIUtils.getGUI(ts).markDead(UIUtils.cleanStr(args[0]));
        return true;
    }
}