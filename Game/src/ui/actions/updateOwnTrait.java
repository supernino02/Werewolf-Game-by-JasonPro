package ui.actions;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import jason.asSyntax.NumberTerm;
import ui.core.AgentGUI;

/**
 * Jason Internal Action: ui.actions.updateOwnTrait(Trait, Value, IsMood)
 */
public class updateOwnTrait extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        try {
            // 1. Extract the Trait name
            String trait = args[0].toString();
            if (trait.startsWith("\"") && trait.endsWith("\"")) {
                trait = trait.substring(1, trait.length() - 1);
            }

            // 2. Extract the numeric Value
            double value = ((NumberTerm) args[1]).solve();

            // 3. Extract the IsMood boolean flag
            boolean isMood = args[2].toString().equals("true");

            // 4. Get the name of the agent executing this action (e.g., "alice")
            String agentName = ts.getAgArch().getAgName();
            
            // 5. Fetch the specific GUI instance for this agent
            AgentGUI gui = AgentGUI.getGUI(agentName);

            if (gui != null) {
                // 6. Push the update to the GUI (Platform.runLater is handled inside AgentGUI)
                gui.updateOwnTrait(trait, value, isMood);
            } else {
                ts.getLogger().warning("GUI not found for agent: " + agentName);
            }

            return true;

        } catch (Exception e) {
            ts.getLogger().severe("Error in ui.actions.updateOwnTrait: " + e.getMessage());
            return false;
        }
    }
}