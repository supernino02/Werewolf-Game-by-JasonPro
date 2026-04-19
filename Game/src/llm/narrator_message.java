package llm;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class narrator_message extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {

        //contextual data extraction and cleaning
        String intent =       LLMUtils.cleanQuotes(args[0].toString());
        String referenceMsg = LLMUtils.cleanQuotes(args[1].toString());
        String style =        LLMUtils.cleanQuotes(args[2].toString());
        String logChunk =     LLMUtils.cleanQuotes(args[3].toString());
        
        String goal = """
        INTENT TO CONVEY: %s
        """.formatted(intent);
        
        String constraints = """
                1. STRICT LIMIT: Output 1 to 3 sentences maximum.
                2. NO PARROTING: Use the REFERENCE MESSAGE strictly for factual pacing. Write entirely new dialogue.
                3. VOCABULARY VARIATION: You are strictly forbidden from using opening greetings, phrases, or idioms present in the RECENT DIALOGUE HISTORY. 
                4. NO REPETITION: Do not reuse any specific wording, phrasing, or sentence structure from the RECENT DIALOGUE HISTORY nor the REFERENCE MESSAGE or phase title.
                5. ASSUMED CONTEXT: Focus strictly on the current INTENT TO CONVEY. Assume players possess full knowledge of past events.
                6. FORMAT: Output ONLY the raw response string. No meta-commentary, no JSON, no quotation marks, no markdown.
                7. ONE-LINE: The output must be a single line of text, without line breaks or paragraphs. Punctuation is allowed, but no newlines.
                """;

        //build the final prompt using the modular builder
        String finalPrompt = new LLMUtils.PromptBuilder()
                .addIdentity("ROLE: TABLETOP GAME MASTER / NARRATOR") //role definition
                .addStyle(style)                                                       //tone definition
                .addCustomBlock("REFERENCE MESSAGE", "'" + referenceMsg + "'")  //one-shot example
                .addGoal(goal)                                                        //task definition
                .addCustomBlock("RECENT DIALOGUE HISTORY ", logChunk)          //context definition to avoid repetition 
                .addConstraints(constraints)                                    
                .build();
        
        //execute the prompt asynchronously  and save the result in a belief
        LLMUtils.executeAsync(ts, finalPrompt, "narrator_result");

        return true;
    }
}