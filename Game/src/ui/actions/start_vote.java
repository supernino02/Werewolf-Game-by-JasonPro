package ui.actions;
import jason.asSemantics.*;
import jason.asSyntax.*;

public class start_vote extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (!UIUtils.hasUI(ts)) return true;
        
        String phaseTitle = UIUtils.cleanStr(args[0]);
        String msg = UIUtils.cleanStr(args[1]);
        String type = UIUtils.cleanStr(args[2]);
        double delayMs = UIUtils.getDelay(ts);

        if (UIUtils.isUIActive(ts)) {
            UIUtils.getGUI(ts).startVote(phaseTitle, msg, type, delayMs);
        } else {
            UIUtils.getGUI(ts).phaseMessage(phaseTitle, msg, delayMs);
        }
        return true;
    }
}