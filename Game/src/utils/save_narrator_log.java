package utils;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class save_narrator_log extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (args.length < 2 || !args[0].isList() || !args[1].isString()) {
            ts.getLogger().severe("save_narrator_log requires 2 args: (ChronoLog, FilePath)");
            return false;
        }

        ListTerm logList = (ListTerm) args[0];
        String filename = ((StringTerm) args[1]).getString();

        List<PhaseGroup> phases = new ArrayList<>();
        PhaseGroup currentPhase = null;

        for (Term t : logList) {
            if (t.isStructure()) {
                Structure s = (Structure) t;
                String functor = s.getFunctor();

                if (functor.equals("phase")) {
                    currentPhase = new PhaseGroup();
                    currentPhase.name = s.getTerm(0).toString();
                    currentPhase.title = s.getTerm(1).toString();
                    currentPhase.message = s.getTerm(2).toString();
                    currentPhase.uid = getAnnot(s, "phase_id");
                    phases.add(currentPhase);
                } else if (functor.equals("action") && currentPhase != null) {
                    ActionEntry ae = new ActionEntry();
                    ae.content = s.getTerm(0).toString();
                    ae.uid = getAnnot(s, "phase_id");
                    currentPhase.actions.add(ae);
                }
            }
        }

        List<String> phaseJsons = new ArrayList<>();
        for (PhaseGroup pg : phases) {
            List<String> actionJsons = new ArrayList<>();
            for (ActionEntry ae : pg.actions) {
                actionJsons.add(String.format("        { \"phase_id\": \"%s\", \"content\": \"%s\" }",
                    escapeJson(ae.uid), escapeJson(ae.content)
                ));
            }
            String phaseObj = String.format(
                "    {\n      \"phase_id\": \"%s\",\n      \"phase\": { \"name\": \"%s\", \"title\": \"%s\", \"message\": \"%s\" },\n" +
                "      \"actions\": [\n%s\n      ]\n    }",
                escapeJson(pg.uid), escapeJson(pg.name), escapeJson(pg.title), escapeJson(pg.message),
                String.join(",\n", actionJsons)
            );
            phaseJsons.add(phaseObj);
        }

        String finalJson = "{\n  \"narrator_log\": [\n" + String.join(",\n", phaseJsons) + "\n  ]\n}";

        // --- DIRECTORY CREATION LOGIC ---
        File outputFile = new File(filename);
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, false))) {
            writer.print(finalJson);
        } catch (Exception e) {
            ts.getLogger().severe("Failed to write narrator log: " + e.getMessage());
            return false;
        }
        return true;
    }

    private String getAnnot(Structure s, String annotName) {
        Term annot = s.getAnnots(annotName).isEmpty() ? null : s.getAnnots(annotName).get(0);
        if (annot != null && annot.isStructure()) return ((Structure) annot).getTerm(0).toString().replace("\"", "");
        return "unknown";
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static class PhaseGroup {
        String name, title, message, uid;
        List<ActionEntry> actions = new ArrayList<>();
    }
    private static class ActionEntry {
        String content, uid;
    }
}