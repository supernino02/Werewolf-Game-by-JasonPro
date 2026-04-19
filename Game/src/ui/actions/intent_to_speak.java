package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class intent_to_speak extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (!UIUtils.hasUI(ts)) return true;
        UIUtils.getGUI(ts).updatePlayerIntent(UIUtils.cleanStr(args[0]), UIUtils.cleanStr(args[1]));
        return true;
    }
}