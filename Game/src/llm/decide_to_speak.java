package llm;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class decide_to_speak extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        
        //contextual data extraction and cleaning
        String identityContext = LLMUtils.cleanQuotes(args[0].toString());
        String logs            = LLMUtils.cleanQuotes(args[1].toString());
        String opinionsStr     = LLMUtils.cleanQuotes(args[2].toString());
        String wolfPackStr     = LLMUtils.cleanQuotes(args[3].toString());
        String traitsListStr   = LLMUtils.cleanQuotes(args[4].toString());
        String moodsListStr    = LLMUtils.cleanQuotes(args[5].toString());

        String goal = """
            TASK: Decide whether you want to volunteer to speak up in the current discussion round, or stay silent.
            MOTIVATION: Base your decision on your Personality Profile, your current Mood, and the LATEST EVENTS.
            """;

        String constraints = """
            1. STRATEGIC EVALUATION: Read the LATEST EVENTS and your internal state.
               - Output 'yes' if you are being accused and need to defend yourself, if you have valuable deductions to share based on your traits, or if you want to actively manipulate the village.
               - Output 'no' if you feel perfectly safe, want to stay hidden in the shadows (e.g., low exposure), or have nothing useful to add.
            2. SYNTAX (CRITICAL): You MUST output exactly the word yes or no.
            3. NO BAGGAGE: Do NOT output any other text, explanation, punctuation, markdown, or backticks. ONLY the raw word.
            """;

        //build the final prompt using the modular builder
        String finalPrompt = new LLMUtils.PromptBuilder()
                .addIdentity(identityContext)
                .addCustomBlock("YOUR CURRENT INTERNAL STATE (MOOD)", moodsListStr)
                .addOpinionsBlock(traitsListStr, opinionsStr)
                .addSecretWolfKnowledge(wolfPackStr)
                .addLogs(logs)
                .addGoal(goal)
                .addConstraints(constraints)
                .build();

        //execute the prompt asynchronously  and save the result in a belief
        LLMUtils.executeAsync(ts, finalPrompt, "intent_result");

        return true;
    }
}