package llm;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class choose_victim extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        
        //contextual data extraction and cleaning
        String identityContext = LLMUtils.cleanQuotes(args[0].toString());
        String logs            = LLMUtils.cleanQuotes(args[1].toString());
        String targetsStr      = LLMUtils.cleanQuotes(args[2].toString());
        String opinionsStr     = LLMUtils.cleanQuotes(args[3].toString());
        String wolfPackStr     = LLMUtils.cleanQuotes(args[4].toString());
        String traitsListStr   = LLMUtils.cleanQuotes(args[5].toString());
        String moodsListStr    = LLMUtils.cleanQuotes(args[6].toString());

        String goal = """
            TASK: It is the night phase. Cast your vote for which player the wolf pack should hunt and kill.
            MOTIVATION: Base your choice on your Personality Profile, your current Mood, and your Opinions of others. Eliminate threats or highly influential villagers.
            """;

        String constraints = """
            1. HUNT VALIDITY: You may vote for anyone in the AVAILABLE TARGETS list. 
            2. STRATEGY: You can hunt your own packmates if you are executing a high-level deception, but normally you should target the most influential villagers, those with high utility, or those onto your trail.
            3. LATERAL THINKING: Consider the wolf pack's collective strategy based on the recent logs, but you are not obligated to vote in line with them. You can be a maverick if it serves your goals.
            4. LIST ADHERENCE: Choose the target ONLY from the EXACT names provided in the AVAILABLE TARGETS list. Do not invent names or target dead players.
            5. FORMAT: Output ONLY the raw response string (no meta-commentary, no JSON, no markdown). ONLY the raw target's name.
            """;

        //build the final prompt using the modular builder
        String finalPrompt = new LLMUtils.PromptBuilder()
                .addIdentity(identityContext)
                .addCustomBlock("YOUR CURRENT INTERNAL STATE (MOOD)", moodsListStr)
                .addOpinionsBlock(traitsListStr, opinionsStr)
                .addSecretWolfKnowledge(wolfPackStr)
                .addLogs(logs)
                .addCustomBlock("AVAILABLE TARGETS", targetsStr)
                .addGoal(goal)
                .addConstraints(constraints)
                .build();
        
        //execute the prompt asynchronously  and save the result in a belief
        LLMUtils.executeAsync(ts, finalPrompt, "victim_result");

        return true;
    }

}