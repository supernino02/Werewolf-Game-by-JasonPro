package vesna;

import jason.asSemantics.*;
import jason.asSyntax.*;
import jason.runtime.Settings;
import jason.NoValueException;

import static jason.asSyntax.ASSyntax.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.Map; // <--- ADD THIS LINE

import java.util.logging.Logger;

/**
 * <p>
 * ProAgent extends Agent with temper-based plan/intention selection,
 * without requiring an embodied body (no WebSocket connection).
 * </p>
 * <p>
 * Parameters:
 * <ul>
 * <li> {@code temper( [ LIST OF PROPENSIONS ] )} for personality and mood traits.</li>
 * <li> {@code strategy( most_similar | random )} for the plan temper choice.</li>
 * </ul>
 * <p>
 * In order to use it you should add to your .jcm:
 * <pre>
 * agent alice:alice.asl {
 * ag-class: 		vesna.ProAgent
 * temper:			temper( prop1(0.5), prop2(0.3) )
 * strategy: 		most_similar
 * }
 * </pre>
 * @author Andrea Gatti
 */
public class ProAgent extends Agent{

	/** The temper of the agent */
	private Temper temper;
	// // private Random dice = new Random();
	/** The logger necessary to print on the JaCaMo log */
	protected transient Logger logger;

	/** Initialize the agent with body and temper
	 * <p>
	 * Override initAg method in order to:
	 * <ul>
	 *	<li> connect to the body if needed; </li>
	 *	<li> initialize the temper if needed. </li>
	 * </ul>
	 */
	public void initAg() {

		super.initAg();

		// Initialize the global variables
		Settings stts = getTS().getSettings();
		String temperStr 	= stts.getUserParameter( "temper" );
		String strategy 	= stts.getUserParameter( "strategy" );
		logger = getTS().getLogger();

		// Initialize the agent temper and strategy
		temper = new Temper( temperStr, strategy );

		syncTemperBeliefs();

	}

	/** Stops the agent: prints a message and kills the agent
	 * @param reason The reason why the agent is stopping
	 */
	private void stop( String reason ) {
		logger.severe( reason );
		kill_agent();
	}

	/** Kills the agent
	 * <p>
	 * It calls the internal actions to drop all desires, intentions and events and then kill the agent;
	 * This is necessary to avoid the agent to keep running after the kill_agent call ( that otherwise is simply enqueued ).
	 * </p>
	 */
	private void kill_agent() {
		logger.severe( "Killing agent" );
		try {
			InternalAction drop_all_desires = getIA( ".drop_all_desires" );
			InternalAction drop_all_intentions = getIA( ".drop_all_intentions" );
			InternalAction drop_all_events = getIA( ".drop_all_events" );
			InternalAction action = getIA( ".kill_agent" );

			drop_all_desires.execute( getTS(), new Unifier(), new Term[] {} );
			drop_all_intentions.execute( getTS(), new Unifier(), new Term[] {} );
			drop_all_events.execute( getTS(), new Unifier(), new Term[] {} );
			action.execute( getTS(), new Unifier(), new Term[] { createString( getTS().getAgArch().getAgName() ) } );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	/** Overrides the selectOption in order to consider Temper if needed
	 * <p>
	 * If there is only one option or the options are without temper it goes with the default selection;
	 * Otherwise it calls the temper select method.
	 * </p>
	 * @param options The list of options to choose from
	 * @return The selected option
	 * @see vesna.Temper#select(List) Temper.select(List)
	 */
	public Option selectOption( List<Option> options ) {

		// If there is only one options or the options are without temper go with the default
		if ( options.size() == 1 || !areOptionsWithTemper( options ) )
			return super.selectOption( options );

		// Wrap the options inside an object Temper Selectable
		List<OptionWrapper> wrappedOptions = options.stream()
			.map( OptionWrapper::new )
			.collect( Collectors.toList() );

		// Select with temper
		try {
			Option selected = temper.select( wrappedOptions, this ).getOption();
			return selected;
		} catch ( NoValueException nve ) {
			stop( nve.getMessage() );
		}
		return null;
	}

	/** Overrides the selectIntention in order to consider Temper if added
	 * <p>
	 * If there is only one intention or the intentions are without temper it goes with the default selection;
	 * Otherwise it calls the temper select method.
	 * </p>
	 * @param intentions The queue of intentions to choose from
	 * @return The selected intention
	 * @see vesna.Temper#select(List) Temper.select(List)
	 */
	public Intention selectIntention( Queue<Intention> intentions ) {

		// logger.info( "I have " + intentions.size() + " intentions" );

		// If there is only one intention or the intentions are without temper go with the default
		if ( intentions.size() == 1 || !areIntentionsWithTemper(intentions ) )
			return super.selectIntention( intentions );

		// Wrap the intentions inside an object Temper Selectable
		List<IntentionWrapper> wrappedIntentions = new ArrayList<>( intentions ).stream()
			.map( IntentionWrapper::new )
			.collect( Collectors.toList() );

		// Select with temper and remove the Intention from the queue
		try {
			Intention selected = temper.select( wrappedIntentions, null ).getIntention();
			Iterator<Intention> it = intentions.iterator();
			while( it.hasNext() ) {
				if ( it.next() == selected ) {
					it.remove();
					break;
				}
			}
			return selected;
		} catch ( NoValueException nve ) {
			stop( nve.getMessage() );
		}
		return null;
	}

	/** Check if there is at least one option with temper annotation
	 * @param options The list of options to check
	 * @return true if at least one option has temper annotation, false otherwise
	 */
	private boolean areOptionsWithTemper( List<Option> options ) {
		Literal propension = createLiteral( "temper", new VarTerm( "X" ) );
		for ( Option option : options ) {
			Plan p = option.getPlan();
			Pred l = p.getLabel();
			if ( l.hasAnnot() ) {
				for ( Term t : l.getAnnots() )
					if ( new Unifier().unifies( propension, t ) )
						return true;
			}
		}
		return false;
	}

	/** Check if there is at least one intention with temper annotation
	 * @param intentions The queue of intentions to check
	 * @return true if at least one intention has temper annotation, false otherwise
	 */
	private boolean areIntentionsWithTemper( Queue<Intention> intentions ) {
		Literal propension = createLiteral( "propensions", new VarTerm( "X" ) );
		for ( Intention intention : intentions ) {
			Plan p = intention.peek().getPlan();
			Pred l = p.getLabel();
			if ( l.hasAnnot() ) {
				for ( Term t : l.getAnnots() )
					if ( new Unifier().unifies( propension, t ) )
						return true;
			}
		}
		return false;
	}

	//METHODS TO ALLOW FOR JASON BELIEFS TO BE IN SYNC WITH THE TEMPER

	private void syncTemperBeliefs() {
		try {
			if (temper == null) return;
			
			// Sync constant personality traits
			for (Map.Entry<String, Double> entry : temper.getPersonality().entrySet()) {
				getTS().getAg().delBel(parseLiteral("my_personality(" + entry.getKey() + ",_)"));
				getTS().getAg().addBel(parseLiteral("my_personality(" + entry.getKey() + "," + entry.getValue() + ")"));
			}
			
			// Sync dynamic mood traits
			for (Map.Entry<String, Double> entry : temper.getMood().entrySet()) {
				getTS().getAg().delBel(parseLiteral("my_mood(" + entry.getKey() + ",_)"));
				getTS().getAg().addBel(parseLiteral("my_mood(" + entry.getKey() + "," + entry.getValue() + ")"));
			}
		} catch (Exception e) {
			logger.severe("Failed to sync temper beliefs: " + e.getMessage());
		}
	}

	public void applyMoodEffect(String trait, double effectValue) {
		if (temper == null || !temper.getMood().containsKey(trait)) {
			logger.warning("Attempted to modify non-existent mood trait: " + trait);
			return;
		}

		double currentMood = temper.getMood().get(trait);
		double newMood = currentMood + effectValue;

		if (newMood > 1.0) newMood = 1.0;
		else if (newMood < -1.0) newMood = -1.0;

		temper.getMood().put(trait, newMood);
		syncTemperBeliefs(); // Instantly pushes the change to the Jason BB and GUI
	}

}