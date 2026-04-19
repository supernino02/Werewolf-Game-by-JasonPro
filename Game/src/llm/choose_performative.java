package llm;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class choose_performative extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        
        //contextual data extraction and cleaning
        String identityContext  = LLMUtils.cleanQuotes(args[0].toString());
        String logs             = LLMUtils.cleanQuotes(args[1].toString());
        String performativesStr = LLMUtils.cleanQuotes(args[2].toString());
        String playersStr       = LLMUtils.cleanQuotes(args[3].toString());
        String opinionsStr      = LLMUtils.cleanQuotes(args[4].toString());
        String wolfPackStr      = LLMUtils.cleanQuotes(args[5].toString());
        String traitsListStr    = LLMUtils.cleanQuotes(args[6].toString());
        String moodsListStr     = LLMUtils.cleanQuotes(args[7].toString());

        String options = """
            PERFORMATIVES:
            %s
            
            PLAYERS TO TARGET:
            %s
            """.formatted(performativesStr, playersStr);

        String goal = """
            TASK: Choose the most strategic communication action (performative) to take right now.
            MOTIVATION: Base your choice on your Personality Profile, your current Mood, and your Opinions of others. Protect yourself and advance your faction's win condition.
            """;


        StringBuilder constraints = new StringBuilder("""
            1. OUTPUT FORMAT: You MUST output exactly ONE Jason literal. Do NOT output markdown, backticks, or any explanation. No preambles. NO JSON. ONLY the literal.
            2. LITERAL FORMAT: performative(target_player_name)
            3. VALID PERFORMATIVES: Choose ONLY from the exact performative names provided in the PERFORMATIVES list.
            4. VALID TARGETS: Choose ONLY from the exact names provided in the PLAYERS TO TARGET list. You CANNOT target yourself. Do not invent names or target dead players.
            5. LATERAL STRATEGY: Be unpredictable. Do not tunnel-vision on the last person who spoke. Shift the spotlight, pressure quiet players, bait reactions, or change the subject to control the narrative.
            """);

        //additional strategic advice for wolves with pack knowledge
        if (wolfPackStr != null && !wolfPackStr.isEmpty()) {
            constraints.append("6. STRATEGIC DECEPTION (WOLVES ONLY): Protect your packmates. Do NOT accuse or suspect your packmates unless forced to by extreme village suspicion. Act like a paranoid villager toward the innocent players.\n");
        }
        
        //build the final prompt using the modular builder
        String finalPrompt = new LLMUtils.PromptBuilder()
                .addIdentity(identityContext)
                .addCustomBlock("YOUR CURRENT INTERNAL STATE (MOOD)", moodsListStr)
                .addOpinionsBlock(traitsListStr, opinionsStr)
                .addSecretWolfKnowledge(wolfPackStr)
                .addLogs(logs)
                .addCustomBlock("AVAILABLE OPTIONS", options)
                .addGoal(goal)
                .addConstraints(constraints.toString())
                .build();

        //execute the prompt asynchronously  and save the result in a belief
        LLMUtils.executeAsync(ts, finalPrompt, "performative_result");

        return true;
    }
}