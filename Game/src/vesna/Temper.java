package vesna;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import static jason.asSyntax.ASSyntax.*;
import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;
import jason.NoValueException;

/** This class implements the temper of the agent
 * <p>
 * The temper of an agent is subdivided into:
 * <ul>
 * <li> <b>personality:</b> for the moment it does never change. <i>In the future</i>, it could change based on mood but very slowly;
 * <li> <b>mood:</b> it changes applying plan post-actions if provided.
 * </ul>
 * The agent can apply two decision strategies:
 * <ul>
 * <li> <b>Most similar:</b> deterministic, it chooses always the plan with personality and mood more similar to the current ones;
 * <li> <b>Random:</b> undeterministic, it chooses with a weighted random based on the similarity between the plan annotations and the current temper.
 * </ul>
 */
public class Temper {

    /** Decision Strategy is an enumerable between most similar and random */
    private enum DecisionStrategy { MOST_SIMILAR, RANDOM };

    /** Personality is the persistent part of the agent temper */
    private Map<String, Double> personality;
    /** Mood is the mutable part of the agent temper */
    private Map<String, Double> mood;
    /** The agent decision strategy */
    private DecisionStrategy strategy;
    /** A dice necessary to generate random numbers */
    private Random dice = new Random();

    public Temper( String temper, String strategy ) throws IllegalArgumentException {

        // The temper should always be set at this point
        if ( temper == null )
            throw new IllegalArgumentException( "Temper cannot be null" );

        // Initialize the new personality
        personality = new HashMap<>();
        mood = new HashMap<>();

        try {
            // Load the personality into the Map
            Literal listLit = parseLiteral( temper );
            for ( Term term : listLit.getTerms() ) {
                Literal trait = ( Literal ) term;
                double value = ( double ) ( ( NumberTerm ) trait.getTerm( 0 ) ).solve();
                if ( trait.hasAnnot( createLiteral( "mood" ) ) ) {
                    if ( value < -1.0 || value > 1.0 )
                        throw new IllegalArgumentException( "Trait value for mood must be between -1.0 and 1.0, found:" + trait );
                    mood.put( trait.getFunctor().toString(), value );
                    continue;
                } else {
                    if ( value < -1.0 || value > 1.0 )
                        throw new IllegalArgumentException( "Trait value for personality must be between -1.0 and 1.0, found:" + trait );
                    personality.put( trait.getFunctor().toString(), value );
                }
            }
        } catch ( ParseException pe ) {
            throw new IllegalArgumentException( pe.getMessage() + " Maybe one of the terms of personality is mispelled" );
        } catch ( NoValueException nve ) {
            throw new IllegalArgumentException( nve.getMessage() + " Maybe one of the terms is mispelled and does not contain a number" );
        }

        // Load the strategy
        if ( strategy == null )
            this.strategy = DecisionStrategy.MOST_SIMILAR;
        if ( strategy.equals( "most_similar" ) )
            this.strategy = DecisionStrategy.MOST_SIMILAR;
        else if ( strategy.equals( "random" ) )
            this.strategy = DecisionStrategy.RANDOM;
        else
            throw new IllegalArgumentException( "Decision Strategy Unknown: " + strategy );
    }

    public <T extends TemperSelectable> T select( List<T> choices, ProAgent agent ) throws NoValueException {
        List<Double> weights = new ArrayList<>();

        for ( T choice : choices ) {

            double choiceWeight = 0;
            Pred label = choice.getLabel();

            Literal temperAnnot = label.getAnnot( "temper" );
            if ( temperAnnot == null )
                continue;

            ListTerm choiceTemper = ( ListTerm ) temperAnnot.getTerm( 0 );
            for ( Term traitTerm : choiceTemper ) {
                Atom trait = ( Atom ) traitTerm;
                if ( ! mood.keySet().contains( trait.getFunctor().toString() ) && ! personality.keySet().contains( trait.getFunctor().toString() ) )
                    continue;
                double traitTemper;
                if ( mood.keySet().contains( trait.getFunctor().toString() ) )
                    traitTemper = mood.get( trait.getFunctor().toString() );
                else
                    traitTemper = personality.get( trait.getFunctor().toString() );
                try {
                    double traitValue = ( double ) ( (NumberTerm ) trait.getTerm( 0 ) ).solve();
                    if ( traitValue < -1.0 || traitValue > 1.0 )
                        throw new IllegalArgumentException("Trait value out of range, found: " + trait + ". The value should be inside [-1.0, 1.0].");
                    if ( strategy == DecisionStrategy.RANDOM )
                        choiceWeight += traitTemper * traitValue;
                    else if ( strategy == DecisionStrategy.MOST_SIMILAR )
                        choiceWeight += Math.abs( traitTemper - traitValue );
                } catch ( NoValueException nve ) {
                    throw new NoValueException( "One of the plans has a mispelled annotation" );
                }
            }
            weights.add( choiceWeight );
        }

        T chosen = null;
        if ( strategy == DecisionStrategy.RANDOM )
            chosen = choices.get( getWeightedRandomIdx( weights ) );
        else if ( strategy == DecisionStrategy.MOST_SIMILAR )
            chosen = choices.get( getMostSimilarIdx( weights ) );
        if ( chosen == null )
            chosen = choices.get( 0 );

        // Only evaluate and apply the 'effects' annotation if we passed the agent
        if ( agent != null ) {
            Literal effectList = chosen.getLabel().getAnnot( "effects" );
            if ( effectList != null )
                updateDynTemper( effectList, agent );
        }

        return chosen;
    }

    private int getWeightedRandomIdx( List<Double> weights ) {
        // double sum = weights.stream().reduce( 0.0, Double::sum );
        double min_bound = 0.0;
        double max_bound = 0.0;
        for ( double weight : weights ) {
            if ( weight < 0.0 )
                min_bound += weight;
            else
                max_bound += weight;
        }
        double roll = dice.nextDouble( min_bound, max_bound );
        int currentMin = 0;
        for ( int i = 0; i < weights.size(); i++ ) {
            if ( roll > currentMin && roll < weights.get( i ) + currentMin )
                return i;
            currentMin += weights.get( i );
        }
        return 0;
    }

    private int getMostSimilarIdx( List<Double> weights ) {
        double min = Double.MAX_VALUE;
        int minIdx = -1;
        for ( int i = 0; i < weights.size(); i++ ) {
            if ( weights.get( i ) < min ) {
                min = weights.get( i );
                minIdx = i;
            }
        }
        return minIdx;
    }

    private void updateDynTemper( Literal effectList, ProAgent agent ) throws NoValueException {
        ListTerm effects = ( ListTerm ) effectList.getTerm( 0 );
        for ( Term effectTerm : effects ) {
            Literal effect = ( Literal ) effectTerm;
            String traitName = effect.getFunctor().toString();

            if ( personality.keySet().contains( traitName ) && !effect.hasAnnot( createLiteral( "mood" ) ) )
                throw new IllegalArgumentException( "You used a Personality trait in the post-effects! Use only mood traits. In case of ambigous name use the annotation [mood]." );
            if ( mood.get( traitName ) == null )
                continue;
                
            try {
                double effectValue = ( double ) ( ( NumberTerm ) effect.getTerm( 0 ) ).solve();
                // DELEGATE natively back to ProAgent
                agent.applyMoodEffect( traitName, effectValue );
            } catch ( NoValueException nve ) {
                throw new NoValueException( "One of the plans has a mispelled annotation" );
            }
        }
    }

    public Map<String, Double> getPersonality() {
        return personality;
    }

    public Map<String, Double> getMood() {
        return mood;
    }

}