package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class show_vote extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (!UIUtils.hasUI(ts)) return true;
        UIUtils.getGUI(ts).registerVote(UIUtils.cleanStr(args[0]), UIUtils.cleanStr(args[1]), UIUtils.cleanStr(args[2]));
        return true;
    }
}