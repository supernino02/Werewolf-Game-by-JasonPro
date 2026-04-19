package llm;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class player_message extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {

        //contextual data extraction and cleaning
        String identityContext =  LLMUtils.cleanQuotes(args[0].toString());
        String styleDesc =        LLMUtils.cleanQuotes(args[1].toString());        
        String translatedIntent = LLMUtils.cleanQuotes(args[2].toString()); 
        String logs =             LLMUtils.cleanQuotes(args[3].toString());
        String wolfPackStr =      LLMUtils.cleanQuotes(args[4].toString());
        String playerStr =        LLMUtils.cleanQuotes(args[5].toString());

        String goal = """
                TASK: Write a chat message to the other players.
                MESSAGE TO CONVEY: %s
                """.formatted(translatedIntent);
        
        String constraints = """
                1. CONTEXTUAL ANCHORING: Read the RECENT CHAT LOG. You must naturally transition from the current topic of conversation into your INTENT TO CONVEY so it flows like a real human discussion.
                2. EXACT TARGETING: You must type the exact name of your target (e.g., 'delta'). Do not use nicknames or vague pronouns to refer to them.
                3. PUBLIC CHANNEL DECEPTION: This is a public chat. NEVER reveal your Hidden Role if you are a Wolf. Always speak as if you are a concerned Villager.
                4. FIRST-PERSON IMMERSION: Your name is already attached to the UI. Start speaking immediately. Do not introduce yourself, do not sign off, and use standard pronouns (I, me, my).
                5. ORIGINALITY: Write entirely new dialogue. You are strictly forbidden from copying phrases or vocabulary from the RECENT CHAT LOG.
                6. NO PARROTING: Do not use the same sentence structures, rhetorical questions, or opening hooks as the messages in the RECENT CHAT LOG.
                7. STRICT LIMIT: Limit your spoken message to 1 to 3 sentences maximum.
                8. FORMAT: Output ONLY the raw response string. No meta-commentary, no JSON, no quotation marks, no markdown.
                """;

        //build the final prompt using the modular builder
        String finalPrompt = new LLMUtils.PromptBuilder()
                .addIdentity(identityContext)
                .addSecretWolfKnowledge(wolfPackStr)
                .addLogs(logs)
                .addCustomBlock("PLAYERS IN THE GAME", playerStr)
                .addGoal(goal)
                .addStyle(styleDesc)
                .addConstraints(constraints)
                .build();

        //execute the prompt asynchronously  and save the result in a belief
        LLMUtils.executeAsync(ts, finalPrompt, "message_result");

        return true;
    }
}