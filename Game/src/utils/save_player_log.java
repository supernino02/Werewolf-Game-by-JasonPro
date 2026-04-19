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

public class save_player_log extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (args.length < 7) {
            ts.getLogger().severe("save_player_log requires 7 args: (Name, Role, Reasoner, BaseTraits, MoodLog, TraitLog, FilePath)");
            return false;
        }

        String name = args[0].toString().replace("\"", "");
        String role = args[1].toString().replace("\"", "");
        String reasoner = args[2].toString().replace("\"", "");
        ListTerm baseTraits = (ListTerm) args[3];
        ListTerm moodLog = (ListTerm) args[4];
        ListTerm traitLog = (ListTerm) args[5];
        String filename = ((StringTerm) args[6]).getString();

        // 1. METADATA
        List<String> baseTraitJsons = new ArrayList<>();
        for (Term t : baseTraits) {
            if (t.isStructure()) {
                Structure s = (Structure) t;
                baseTraitJsons.add("\"" + s.getTerm(0).toString() + "\": " + s.getTerm(1).toString());
            }
        }
        String metaBlock = String.format(
            "  \"metadata\": {\n    \"name\": \"%s\",\n    \"role\": \"%s\",\n    \"reasoner\": \"%s\",\n    \"base_traits\": { %s }\n  }",
            name, role, reasoner, String.join(", ", baseTraitJsons)
        );

        // 2. MOOD LOG
        List<String> moodJsons = new ArrayList<>();
        for (Term t : moodLog) {
            if (t.isStructure()) {
                Structure s = (Structure) t;
                String phaseId = getAnnot(s, "phase_id");
                if (s.getArity() == 2) {
                    moodJsons.add(String.format("    { \"phase_id\": \"%s\", \"mood\": \"%s\", \"value\": %s }", 
                        phaseId, s.getTerm(0).toString(), s.getTerm(1).toString()));
                } else if (s.getArity() == 1) {
                    moodJsons.add(String.format("    { \"phase_id\": \"%s\", \"mood\": \"%s\" }", 
                        phaseId, s.getTerm(0).toString()));
                }
            }
        }
        String moodBlock = "  \"mood_log\": [\n" + String.join(",\n", moodJsons) + "\n  ]";

        // 3. TRAIT LOG
        List<String> traitJsons = new ArrayList<>();
        for (Term t : traitLog) {
            if (t.isStructure()) {
                Structure s = (Structure) t;
                String phaseId = getAnnot(s, "phase_id");
                traitJsons.add(String.format("    { \"phase_id\": \"%s\", \"target\": \"%s\", \"trait\": \"%s\", \"value\": %s }",
                    phaseId, s.getTerm(0).toString(), s.getTerm(1).toString(), s.getTerm(2).toString()));
            }
        }
        String traitBlock = "  \"trait_log\": [\n" + String.join(",\n", traitJsons) + "\n  ]";

        // 4. ASSEMBLE
        String finalJson = "{\n" + metaBlock + ",\n" + moodBlock + ",\n" + traitBlock + "\n}";

        // --- DIRECTORY CREATION LOGIC ---
        File outputFile = new File(filename);
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, false))) {
            writer.print(finalJson);
        } catch (Exception e) {
            ts.getLogger().severe("Failed to write player log: " + e.getMessage());
            return false;
        }
        return true;
    }

    private String getAnnot(Structure s, String annotName) {
        Term annot = s.getAnnots(annotName).isEmpty() ? null : s.getAnnots(annotName).get(0);
        if (annot != null && annot.isStructure()) return ((Structure) annot).getTerm(0).toString().replace("\"", "");
        return "unknown";
    }
}