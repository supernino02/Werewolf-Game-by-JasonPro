package llm;

import jason.asSemantics.TransitionSystem;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;

public class LLMUtils {

    //class used to build modular prompts with optional blocks
    //avoid code duplication and ensure correct parsing and formatting
    public static class PromptBuilder {
        private StringBuilder sb;

        //initialize prompt builder with world setting
        public PromptBuilder() {
            sb = new StringBuilder();
            sb.append("""
                ### WORLD SETTING
                You are participating in a multi-agent simulation of the game Werewolf/Mafia.

                """);
        }

        //append style of speech block if valid
        public PromptBuilder addStyle(String style) {
            if (isValid(style)) {
                sb.append("""
                    ### STYLE OF SPEECH
                    %s

                    """.formatted(clean(style)));
            }
            return this;
        }

        //append identity block if valid
        public PromptBuilder addIdentity(String identityContext) {
            if (isValid(identityContext)) {
                sb.append("""
                    ### YOUR IDENTITY
                    %s

                    """.formatted(clean(identityContext)));
            }
            return this;
        }

        //append latest events block if valid
        public PromptBuilder addLogs(String logs) {
            if (isValid(logs)) {
                sb.append("""
                    ### LATEST EVENTS
                    %s
                    
                    """.formatted(clean(logs)));
            }
            return this;
        }

        //append current goal block if valid
        public PromptBuilder addGoal(String goal) {
            if (isValid(goal)) {
                sb.append("""
                    ### YOUR CURRENT GOAL
                    %s

                    """.formatted(clean(goal)));
            }
            return this;
        }

        //append constraints block if valid
        public PromptBuilder addConstraints(String constraints) {
            if (isValid(constraints)) {
                sb.append("""
                    ### RESPONSE CONSTRAINTS (MANDATORY)
                    %s 

                    """.formatted(clean(constraints)));
            }
            return this;
        }

        //append secret wolf knowledge block if valid
        public PromptBuilder addSecretWolfKnowledge(String wolfPackStr) {
            if (isValid(wolfPackStr)) {
                sb.append("""
                    ### SECRET KNOWLEDGE ABOUT THE PACK
                    Your Wolf Pack: %s.
                    You MUST pretend to be a normal villager in public.
                    NEVER reveal your pack.

                    """.formatted(clean(wolfPackStr)));
            }
            return this;
        }

        //append custom opinions block handling empty data
        public PromptBuilder addOpinionsBlock(String definitions, String currentData) {
            String data = (currentData == null || currentData.trim().isEmpty()) ? "[none]" : currentData;

            String content = """
                    HOW TO READ THESE VALUES:
                    %s

                    CURRENT EPISTEMIC OPINIONS TOWARDS OTHER PLAYERS:
                    NOTE: Any traits not listed in a player's profile is currently 0.0 (Unbiased/Neutral opinion).
                    %s
                    """.formatted(definitions, clean(data));

            return addCustomBlock("YOUR CURRENT OPINIONS OF OTHERS (TRAITS)", content);
        }

        //append generic custom block with uppercase title
        public PromptBuilder addCustomBlock(String title, String content) {
            if (isValid(content)) {
                sb.append("""
                    ### %s
                    %s

                    """.formatted(title.toUpperCase(), clean(content)));
            }
            return this;
        }

        //add a final instruction to generate response and build the prompt
        public String build() {
            sb.append("GENERATE RESPONSE NOW:");
            return sb.toString();
        }

        //check if string is not null and not empty
        private boolean isValid(String input) {
            return input != null && !input.trim().isEmpty();
        }

        //remove double quotes from input
        private String clean(String input) {
            return input.replace("\"", "");
        }
    }

    //remove leading and trailing quotes
    public static String cleanQuotes(String s) {
        return s.replaceAll("^\"|\"$", "");
    }

    //trim input or return empty string if null
    public static String preprocess(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.trim();
    }

    //escape characters and format as json string, to ensure correct parsing and prevent injection issues
    public static String postprocess(String llmOutput) {
        if (llmOutput == null) {
            return "\"error: returned null from ollama call\"";
        }
        
        //clean unprintable chars
        String cleaned = llmOutput.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        //escape special chars for safe parsing in Jason literals
        String escaped = cleaned
                .replace("\\", "\\\\")    
                .replace("\"", "\\\"")    
                .replace("\n", "\\n")     
                .replace("\r", "")        
                .replace("\t", "\\t")     
                .trim();
                
        return "\"" + escaped + "\"";
    }

    //wrapper to execute async without uid
    public static void executeAsync(TransitionSystem ts, String taskPrompt, String beliefFunctor) {
        executeAsync(ts, taskPrompt, beliefFunctor, null);
    }

    //execute llm call asynchronously and inject result belief
    public static void executeAsync(TransitionSystem ts, String taskPrompt, String beliefFunctor, String uid) {
        String agent_name = ts.getAgArch().getAgName();        
        
        //fetch phase uid from agent belief base
        String phaseUid = "unknown_phase";
        try {
            jason.asSyntax.Literal pattern = ASSyntax.parseLiteral("current_phase_uid(_)");
            jason.asSyntax.Literal matchedBelief = ts.getAg().findBel(pattern, new jason.asSemantics.Unifier());
            if (matchedBelief != null) {
                phaseUid = matchedBelief.getTerm(0).toString().replaceAll("\"", "");
            }
        } catch (Exception e) {
            ts.getLogger().warning("Could not find current_phase_uid for " + agent_name);
        }
        final String finalPhaseUid = phaseUid;

        //start async thread for network call
        new Thread(() -> {
            try {

                //preprecess, call the llm, postprocess
                String cleanedPrompt = preprocess(taskPrompt);
                String llmResponse = OllamaAPI.generate(agent_name, beliefFunctor, cleanedPrompt, finalPhaseUid);                
                String formattedResponse = postprocess(llmResponse);

                //attempt native jason term parsing or fallback to string
                Term responseTerm;
                try {
                    responseTerm = ASSyntax.parseTerm(formattedResponse);
                } catch (Exception parseEx) {
                    responseTerm = ASSyntax.createString(formattedResponse);
                }

                //create literal with dynamic functor
                Literal resultLiteral = createResultLiteral(beliefFunctor, uid, responseTerm);

                //strictly synchronize belief base update
                synchronized (ts.getAg()) {
                    ts.getAg().addBel(resultLiteral);
                }
                
                //wake the agent reasoning engine
                if (ts.getAgArch() != null) {
                    ts.getAgArch().wakeUpSense();
                }

            } catch (Exception e) {
                //log the failure
                ts.getLogger().severe("LLM failure [" + beliefFunctor + "]: " + e.getMessage());
                try {
                    //inject error fallback belief
                    String fallback = postprocess("error");
                    Literal fallbackLiteral = createResultLiteral(beliefFunctor, uid, ASSyntax.createString(fallback));
                    
                    synchronized (ts.getAg()) {
                        ts.getAg().addBel(fallbackLiteral);
                    }
                    if (ts.getAgArch() != null) ts.getAgArch().wakeUpSense();
                    
                } catch (Exception ex) {
                    ts.getLogger().severe("Critical parsing failure in fallback injection.");
                }
            }
        }).start();
    }

    //create literal with or without uid parameter
    private static Literal createResultLiteral(String beliefFunctor, String uid, Term responseTerm) {
        if (uid != null && !uid.trim().isEmpty()) {
            return ASSyntax.createLiteral(beliefFunctor, ASSyntax.createString(uid.trim()), responseTerm);
        }
        return ASSyntax.createLiteral(beliefFunctor, responseTerm);
    }
}