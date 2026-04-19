package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class typing extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (!UIUtils.hasUI(ts)) return true;
        
        String speaker = UIUtils.cleanStr(args[0]);
        boolean isMe = speaker.equalsIgnoreCase(ts.getUserAgArch().getAgName());
        UIUtils.getGUI(ts).showTypingIndicator(speaker, isMe);
        
        return true;
    }
}