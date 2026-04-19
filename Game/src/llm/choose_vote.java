package llm;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class choose_vote extends DefaultInternalAction {

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
            TASK: Cast your vote for the daytime execution. Choose the player you want to eliminate.
            MOTIVATION: Base your choice on your Personality Profile, your current Mood, and your Opinions of others. Protect yourself and eliminate threats or highly suspicious targets.
            """;

        StringBuilder constraints = new StringBuilder("""
            1. VOTE VALIDITY: You may vote for anyone in the AVAILABLE TARGETS list.
            2. STRATEGY: Vote for your enemies, the most suspicious player, or follow your consensus based on your Personality Profile.
            3. LIST ADHERENCE: Choose the target ONLY from the EXACT names provided in the AVAILABLE TARGETS list. Do not invent names or target dead players.
            4. SYNTAX (CRITICAL): You MUST output exactly the raw player name.
            5. NO BAGGAGE: Do NOT output any other text, punctuation, markdown, or backticks. ONLY the target's name.
            """);

        //additional strategic advice for wolves with pack knowledge
        if (wolfPackStr != null && !wolfPackStr.isEmpty()) {
            constraints.append("6. STRATEGIC DECEPTION (WOLVES ONLY): Protect your packmates. Do NOT vote for your packmates unless forced to by extreme village suspicion.\n");
        }

        //build the final prompt using the modular builder
        String finalPrompt = new LLMUtils.PromptBuilder()
                .addIdentity(identityContext)
                .addCustomBlock("YOUR CURRENT INTERNAL STATE (MOOD)", moodsListStr)
                .addOpinionsBlock(traitsListStr, opinionsStr) // Uses the modular block
                .addSecretWolfKnowledge(wolfPackStr)          // Uses the modular block
                .addLogs(logs)
                .addCustomBlock("AVAILABLE TARGETS", targetsStr)
                .addGoal(goal)
                .addConstraints(constraints.toString())
                .build();
        
        //execute the prompt asynchronously  and save the result in a belief
        LLMUtils.executeAsync(ts, finalPrompt, "vote_result");

        return true;
    }
}