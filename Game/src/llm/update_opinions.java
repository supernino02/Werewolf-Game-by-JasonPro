package llm;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class update_opinions extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {

        //contextual data extraction and cleaning
        String uid = LLMUtils.cleanQuotes(args[0].toString());        
        String identityContext = LLMUtils.cleanQuotes(args[1].toString());
        String traitsOpinionsStr = LLMUtils.cleanQuotes(args[2].toString());
        String logs = LLMUtils.cleanQuotes(args[3].toString());
        String wolfPackStr = LLMUtils.cleanQuotes(args[4].toString());
        String action = LLMUtils.cleanQuotes(args[5].toString());
        String traitsListStr = LLMUtils.cleanQuotes(args[6].toString());
        String moodsListStr = LLMUtils.cleanQuotes(args[7].toString());

        String goal = """
                TASK: Evaluate the psychological and strategic impact of the MOST RECENT ACTION on your internal opinions and mood.
                EFFECT: The output will directly update your traits (opinions of others) and mood (internal state) which will influence your future decisions and interactions in the game.
                MOTIVATION: This is a critical step for adapting your strategy as the game evolves, since it directly affects your ability to navigate the social dynamics of the game.
                """;

        String constraints = """
                1. OUTPUT FORMAT: You MUST output exactly a Jason list of literals. Do NOT output markdown, backticks, or any explanation. No preambles.
                2. LITERAL FORMATS:
                   - To update a player's trait: trait_update(player_name, attribute, delta)
                   - To update your own mood: mood_update(attribute, delta)
                3. VALID ATTRIBUTES: Use ONLY the traits and moods explicitly defined in the blocks above (YOUR CURRENT INTERNAL STATE (MOOD) and YOUR CURRENT OPINIONS OF OTHERS (TRAITS)).
                4. SCALE BOUNDARIES (CRITICAL): All traits and moods exist on a hard scale from -1.0 to 1.0.
                5. DELTA VALUES: Use float values between -0.5 and 0.5 to shift the current values. You MUST use NEGATIVE values for relief, lost influence, or reduced threat, and POSITIVE values for increases.
                6. ZERO-IMPACT FALLBACK: If the action has no logical impact, output: []
                7. SYNTAX EXAMPLE: [trait_update(AGENT_NAME, ATTRIBUTE, FLOAT), mood_update(ATTRIBUTE, FLOAT)]
                """;

        //build the final prompt using the modular builder
        String finalPrompt = new LLMUtils.PromptBuilder()
                .addIdentity(identityContext)
                .addCustomBlock("YOUR CURRENT INTERNAL STATE (MOOD)", moodsListStr)
                .addOpinionsBlock(traitsListStr, traitsOpinionsStr)
                .addSecretWolfKnowledge(wolfPackStr)
                .addLogs(logs)
                .addCustomBlock("MOST RECENT ACTION (TO BE EVALUATED)", action)
                .addGoal(goal)
                .addConstraints(constraints)
                .build();

        //execute the prompt asynchronously  and save the result in a belief, with a specified UID
        LLMUtils.executeAsync(ts, finalPrompt, "opinions_result", uid);

        return true;
    }
}