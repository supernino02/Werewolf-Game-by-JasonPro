package ui.actions;

import ui.core.AgentGUI;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import java.util.ArrayList;
import java.util.List;

public class UIUtils {

    public static boolean isUIActive(TransitionSystem ts) {
        return ts.getAg().getBB().contains(Literal.parseLiteral("ui(active)")) != null;
    }

    public static boolean isUIPassive(TransitionSystem ts) {
        return ts.getAg().getBB().contains(Literal.parseLiteral("ui(passive)")) != null;
    }

    public static boolean hasUI(TransitionSystem ts) {
        return isUIActive(ts) || isUIPassive(ts);
    }

    // Safely retrieves the GUI, assuming the invariant that it is already initialized
    public static AgentGUI getGUI(TransitionSystem ts) {
        return AgentGUI.getGUI(ts.getUserAgArch().getAgName());
    }

    // Strips the surrounding quotes from Jason string terms
    public static String cleanStr(Term t) {
        return t.toString().replaceAll("^\"|\"$", "");
    }

    // Converts a Jason ListTerm into a standard Java List<String>
    public static List<String> getList(Term t) {
        List<String> list = new ArrayList<>();
        if (t.isList()) {
            for (Term item : (ListTerm) t) {
                list.add(cleanStr(item));
            }
        }
        return list;
    }

    // Fetches the dynamic writing delay from the agent's Belief Base
    public static double getDelay(TransitionSystem ts) {
        for (jason.asSyntax.Literal bel : ts.getAg().getBB()) {
            // 1. Match outer wrapper: setting(...) -> Arity is 1
            if (bel.getFunctor().equals("setting") && bel.getArity() == 1) {

                jason.asSyntax.Term innerTerm = bel.getTerm(0);

                // 2. Ensure the inner term is a structure (e.g., writing_delay_ms(30))
                if (innerTerm.isStructure()) {
                    jason.asSyntax.Structure innerStruct = (jason.asSyntax.Structure) innerTerm;

                    // 3. Match the inner key and extract its value
                    if (innerStruct.getFunctor().equals("writing_delay_ms") && innerStruct.getArity() == 1) {
                        try {
                            return Double.parseDouble(innerStruct.getTerm(0).toString());
                        } catch (Exception e) {
                            ts.getLogger().warning("Failed to parse writing_delay_ms: " + e.getMessage());
                        }
                        break;
                    }
                }
            }
        }
        return 0.0;
    }
}