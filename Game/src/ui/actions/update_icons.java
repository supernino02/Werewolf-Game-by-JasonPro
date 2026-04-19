package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class update_icons extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        UIUtils.getGUI(ts).updateRoleIcons(UIUtils.getList(args[0]), UIUtils.cleanStr(args[1]));
        return true;
    }
}