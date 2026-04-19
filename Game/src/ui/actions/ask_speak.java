package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class ask_speak extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (!UIUtils.isUIActive(ts)) return true;
        UIUtils.getGUI(ts).askSpeak();
        return true;
    }
}