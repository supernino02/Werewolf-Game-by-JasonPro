package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class request_speech extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (!UIUtils.isUIActive(ts)) return true;
        UIUtils.getGUI(ts).requestSpeechSelection(UIUtils.getList(args[0]));
        return true;
    }
}