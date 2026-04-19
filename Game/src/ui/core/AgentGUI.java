package ui.core;

import atlantafx.base.theme.PrimerLight;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.scene.control.SplitPane;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import ui.components.ChatPanel;
import ui.components.CirclePanel;
import ui.components.InteractionPanel;
import ui.components.HeatmapPanel;

public class AgentGUI {
    private static final ConcurrentHashMap<String, AgentGUI> guis = new ConcurrentHashMap<>();
    private static volatile boolean fxInitialized = false;
    private static final Object initLock = new Object();

    private String agentName;
    private TransitionSystem ts;
    private Logger logger;
    private boolean isActive;

    private ChatPanel chatPanel;
    private CirclePanel circlePanel;
    private InteractionPanel interactionPanel;
    private HeatmapPanel heatmapPanel;
    private HBox banner;

    private HBox typingBox;
    private Timeline typingTimeline;

    // Bounds configured from Jason
    private double minBound;
    private double maxBound;
    private int sigDigits;
    private double writingDelayMs;

    // --- CONFINED UI STATE ---
    private String selectedPerformative = null;
    private String selectedTarget = null;
    private boolean isVotingPhase = false;
    private boolean isSpeakingPhase = false;
    private boolean localVoteSubmitted = false;
    private String voteType = "vote";
    private final Map<String, String> currentVotes = new HashMap<>();
    private final Map<String, String> currentIntents = new HashMap<>();

    private final Map<String, String> knownIcons = new ConcurrentHashMap<>();

    public static void initGUI(String agentName, TransitionSystem ts, String role, List<String> players, double min, double max, int sig, double delay) {
        synchronized (initLock) {
            if (!fxInitialized) {
                try {
                    CountDownLatch latch = new CountDownLatch(1);
                    Platform.startup(() -> {
                        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
                        latch.countDown();
                    });
                    latch.await();
                    fxInitialized = true;
                } catch (Exception e) { fxInitialized = true; }
            }
        }
        guis.computeIfAbsent(agentName, name -> new AgentGUI(agentName, ts, role, players, min, max, sig, delay));
    }

    public static AgentGUI getGUI(String agentName) { return guis.get(agentName); }

    private AgentGUI(String agentName, TransitionSystem ts, String role, List<String> players, double min, double max, int sig, double delay) {
        this.agentName = agentName;
        this.ts = ts;
        this.logger = Logger.getLogger("AgentGUI." + agentName);
        this.minBound = min;
        this.maxBound = max;
        this.sigDigits = sig;
        this.writingDelayMs = delay;

        for (String p : players) {
            this.knownIcons.put(p, "👤");
        }
        
        this.isActive = ts.getAg().getBB().contains(Literal.parseLiteral("ui(active)")) != null;
        Platform.runLater(() -> createAndShowWindow(role, players));
    }

    public double getWritingDelay() {
        return writingDelayMs;
    }

    private void createAndShowWindow(String role, List<String> players) {
        Stage stage = new Stage();
        stage.setTitle((isActive ? "Impersonating Agent: " : "Spectating Agent: ") + agentName);

        stage.setOnCloseRequest(event -> {
            try {
                if (isActive) {
                    logger.info("Active UI window closed. Terminating the entire system...");
                    System.exit(0); 
                }
            } catch (Exception e) {
                logger.severe("Failed to check UI state on close.");
            }
        });

        String icon = role.equalsIgnoreCase("wolf") ? "🐺" : "👤";
        Label bannerLabel = new Label(icon + "  " + agentName.toUpperCase() + "  " + icon);
        bannerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        this.banner = new HBox(bannerLabel);
        banner.setAlignment(Pos.CENTER);
        banner.setStyle("-fx-background-color: #24292e; -fx-padding: 10px;");

        chatPanel = new ChatPanel();
        
        circlePanel = new CirclePanel(players, agentName);
        circlePanel.setOnPlayerClicked(this::handlePlayerClicked);
        VBox.setVgrow(circlePanel, Priority.ALWAYS);

        interactionPanel = new InteractionPanel();
        interactionPanel.setMinHeight(150);

        heatmapPanel = new HeatmapPanel(players, agentName, minBound, maxBound, sigDigits);
        heatmapPanel.setOnPlayerClicked(this::handlePlayerClicked);

        circlePanel.setOnPlayerHoverEnter(p -> {
            if (heatmapPanel != null) heatmapPanel.highlightRow(p, true);
        });
        circlePanel.setOnPlayerHoverExit(p -> {
            if (heatmapPanel != null) heatmapPanel.highlightRow(p, false);
        });
        if (heatmapPanel != null) {
            heatmapPanel.setOnPlayerHoverEnter(p -> circlePanel.setPlayerHover(p, true));
            heatmapPanel.setOnPlayerHoverExit(p -> circlePanel.setPlayerHover(p, false));
        }

        SplitPane mainSplit = new SplitPane();
        VBox centerSide = new VBox(circlePanel, interactionPanel);
        
        if (isActive) {
            mainSplit.getItems().addAll(chatPanel, centerSide);
            mainSplit.setDividerPositions(0.45); 
        } else {
            mainSplit.getItems().addAll(chatPanel, centerSide, heatmapPanel);
            mainSplit.setDividerPositions(0.35, 0.75); 
        }
        
        VBox.setVgrow(mainSplit, Priority.ALWAYS);
        VBox root = new VBox(banner, mainSplit);
        Scene scene = new Scene(root, 1920, 1080);
        stage.setWidth(Screen.getPrimary().getVisualBounds().getWidth());
        stage.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
        stage.setScene(scene);
        stage.show();
    }

private void flushIntentsSummary() {
        if (!currentIntents.isEmpty()) {
            if (checkSetting("ui_show_intent_to_speak_recap")) {
                chatPanel.addIntentSummary(new HashMap<>(currentIntents));
            }
            currentIntents.clear();
        }
        circlePanel.clearBalloons();
    }

    public void setPlayerStatus(String status) {
        Platform.runLater(() -> {
            isVotingPhase = false;
            isSpeakingPhase = false;
            circlePanel.setInteractive(false);
            if (heatmapPanel != null) heatmapPanel.setInteractive(false);
            
            flushIntentsSummary();
            circlePanel.clearArrows();

            if (status.equalsIgnoreCase("dead")) {
                banner.setStyle("-fx-background-color: #8b0000; -fx-padding: 10px;");
                interactionPanel.lockTerminalState("💀 YOU DIED 💀", "", "#8b0000");
            } else if (status.equalsIgnoreCase("victory")) {
                banner.setStyle("-fx-background-color: #d4af37; -fx-padding: 10px;");
                interactionPanel.lockTerminalState("🏆 VICTORY 🏆", "", "#d4af37");
            } else if (status.equalsIgnoreCase("defeat")) {
                banner.setStyle("-fx-background-color: #4b5320; -fx-padding: 10px;");
                interactionPanel.lockTerminalState("💔 DEFEAT 💔", "", "#4b5320");
            }
        });
    }

    private void handlePlayerClicked(String target) {
        if (!isSpeakingPhase && !isVotingPhase) return;
        if (isVotingPhase && localVoteSubmitted) return;

        this.selectedTarget = target;

        if (isSpeakingPhase) {
            interactionPanel.updateStatus(selectedPerformative, selectedTarget);
            circlePanel.drawInteractionArrow(agentName, selectedTarget, false, null); 
        } else if (isVotingPhase) {
            interactionPanel.enableVoteButton();
            circlePanel.drawVotes(currentVotes, voteType, agentName, selectedTarget);
        }
    }

    private void handleChatEnter() {
        if (!isSpeakingPhase || selectedPerformative == null || selectedTarget == null) return;
        
        String msg = interactionPanel.getSanitizedInputText();
        if (msg.isEmpty()) return;

        injectBelief("user_speech(\"" + msg + "\", " + selectedPerformative + "(" + selectedTarget + "))");
        
        isSpeakingPhase = false;
        circlePanel.setInteractive(false);
        if (heatmapPanel != null) heatmapPanel.setInteractive(false);
        
        interactionPanel.clear();
    }

    public void updateRoleIcons(List<String> players, String iconType) {
        String iconStr = iconType.equalsIgnoreCase("wolf") ? "🐺" : "👤";
        for (String t : players) {
            knownIcons.put(t, iconStr);
        }

        Platform.runLater(() -> {
            circlePanel.updateIcons(players, iconType);
            if (heatmapPanel != null) heatmapPanel.updateIcons(players, iconType);
        });
    }

    public void markDead(String player) {
        Platform.runLater(() -> {
            circlePanel.markDead(player);
            if (heatmapPanel != null) heatmapPanel.markDead(player);
        });
    }

    public void updatePlayerIntent(String player, String intent) {
        Platform.runLater(() -> {
            currentIntents.put(player, intent);
            circlePanel.setBalloon(player, intent);
        });
    }

    public void askSpeak() {
        Platform.runLater(() -> {
            removeTypingIndicator();
            circlePanel.setInteractive(false);
            if (heatmapPanel != null) heatmapPanel.setInteractive(false);
            
            interactionPanel.showAskSpeak(
                () -> { interactionPanel.clear(); injectBelief("user_input(\"privileged\")"); },
                () -> { interactionPanel.clear(); injectBelief("user_input(\"no\")"); },
                () -> { interactionPanel.clear(); injectBelief("user_input(\"end_turn\")"); injectBelief("skip_speaking_turn"); }
            );
        });
    }

    public void requestSpeechSelection(List<String> actions) {
        Platform.runLater(() -> {
            removeTypingIndicator();
            isSpeakingPhase = true;
            isVotingPhase = false;
            circlePanel.setInteractive(true);
            if (heatmapPanel != null) heatmapPanel.setInteractive(true);
            
            selectedPerformative = null;
            selectedTarget = null;
            flushIntentsSummary();
            
            interactionPanel.showSpeechSelection(actions, perf -> {
                this.selectedPerformative = perf;
                interactionPanel.updateStatus(selectedPerformative, selectedTarget);
            }, this::handleChatEnter);
        });
    }

    public void startVote(String phaseTitle, String msg, String type, double delayMs) {
        Platform.runLater(() -> {
            removeTypingIndicator();
            isVotingPhase = true;
            isSpeakingPhase = false;
            circlePanel.setInteractive(true);
            if (heatmapPanel != null) heatmapPanel.setInteractive(true);
        
            voteType = type;
            localVoteSubmitted = false;
            selectedTarget = null;
            currentVotes.clear(); 
            
            flushIntentsSummary();
            circlePanel.clearArrows();
     
            chatPanel.addPhaseMessage(phaseTitle, msg, delayMs); 
            
            interactionPanel.showVote(type, () -> {
                localVoteSubmitted = true;
                circlePanel.setInteractive(false);
                if (heatmapPanel != null) heatmapPanel.setInteractive(false);
                injectBelief("user_input(\"" + selectedTarget + "\")");
            });
        });
    }

public void registerVote(String voter, String target, String type) {
        // Check the correct setting based on the type of action
        boolean showRecap = type.equalsIgnoreCase("hunt") ? 
                            checkSetting("ui_show_hunt_recap") : 
                            checkSetting("ui_show_vote_recap");

        Platform.runLater(() -> {
            currentVotes.put(voter, target);
            circlePanel.drawVotes(currentVotes, type, agentName, isVotingPhase && !localVoteSubmitted ? selectedTarget : null);
            
            // Only log in the chat panel if the setting is true
            if (showRecap) {
                chatPanel.addActionMessage(voter, target, type);
            }
        });
    }
    // --- NEW HELPER METHOD ---
    private boolean checkSetting(String settingName) {
        try {
            return ts.getAg().believes(Literal.parseLiteral("setting(" + settingName + "(true))"), new jason.asSemantics.Unifier());
        } catch (Exception e) {
            return false;
        }
    }

    public void phaseMessage(String phaseTitle, String msg, double delayMs) {
        Platform.runLater(() -> {
            isVotingPhase = false;
            isSpeakingPhase = false;
            circlePanel.setInteractive(false);
            if (heatmapPanel != null) heatmapPanel.setInteractive(false);
            currentVotes.clear(); 
         
            interactionPanel.clear();
            flushIntentsSummary();
            circlePanel.clearArrows();
            
            chatPanel.addPhaseMessage(phaseTitle, msg, delayMs);
            removeTypingIndicator();
        });
    }

    public void playerMessage(String sender, String msg, String performative, double delayMs) {
        // --- DO NOT CALL flushIntentsSummary() HERE (Jason Thread) ---

        String iconStr = knownIcons.getOrDefault(sender, "👤");
        
        boolean showCorrect = false;
        String correctPerf = null;
        try {
            // Safely evaluate beliefs on the reasoning thread before touching the UI
            if (ts.getAg().believes(Literal.parseLiteral("setting(show_correct_performative(true))"), new jason.asSemantics.Unifier())) {
                showCorrect = true;
                
                jason.asSyntax.PredicateIndicator pi = new jason.asSyntax.PredicateIndicator("last_correct_performative", 1);
                java.util.Iterator<jason.asSyntax.Literal> it = ts.getAg().getBB().getCandidateBeliefs(pi);
                
                if (it != null && it.hasNext()) {
                    jason.asSyntax.Literal belief = it.next();
                    correctPerf = belief.getTerm(0).toString();
                    
                    // Consume the belief so it doesn't leak into the next message
                    ts.getAg().delBel(belief);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to fetch correct performative setting: " + e.getMessage());
        }

        final boolean finalShowCorrect = showCorrect;
        final String finalCorrectPerf = correctPerf;

        // --- ENTER THE STRICT JAVAFX UI THREAD ---
        Platform.runLater(() -> {
            
            // FLUSH INTENTS: Summarize gathered intents safely on the UI thread
            flushIntentsSummary();

            chatPanel.addPlayerMessage(sender, msg, performative, sender.equalsIgnoreCase(agentName), iconStr, delayMs, finalShowCorrect, finalCorrectPerf);
            removeTypingIndicator();

            if (performative != null && performative.contains("(")) {
                Pattern p = Pattern.compile(".*\\((.*?)\\)");
                Matcher m = p.matcher(performative);
                if (m.find()) {
                    String target = m.group(1);
                    circlePanel.drawInteractionArrow(sender, target, true, performative); 
                }
            }
        });
    }

    public void showTypingIndicator(String sender, boolean isMe) {
        Platform.runLater(() -> {
            if (isSpeakingPhase || isVotingPhase) return;
            removeTypingIndicator();

            VBox container = new VBox(2);
            
            String iconStr = knownIcons.getOrDefault(sender, "👤");

            Label nameLabel = new Label(isMe ? "You" : sender);
            nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6a737d; -fx-font-weight: bold;");

            Label iconLabel = new Label(iconStr); 
            iconLabel.setStyle("-fx-font-size: 13px;");

            HBox header = new HBox(5);
            header.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            
            if (isMe) {
                header.getChildren().addAll(nameLabel, iconLabel);
            } else {
                header.getChildren().addAll(iconLabel, nameLabel);
            }

            Label bubble = new Label(".");
            bubble.setStyle(isMe ? 
                "-fx-font-size: 16px; -fx-background-color: #DCF8C6; -fx-padding: 12; -fx-background-radius: 18 0 18 18;" : 
                "-fx-font-size: 16px; -fx-background-color: #ffffff; -fx-border-color: #e1e4e8; -fx-padding: 12; -fx-background-radius: 0 18 18 18;");
            container.setAlignment(isMe ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
            
            container.getChildren().addAll(header, bubble);

            typingBox = new HBox(container);
            typingBox.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            chatPanel.addNode(typingBox);

            typingTimeline = new Timeline(new KeyFrame(Duration.millis(400), e -> {
                String txt = bubble.getText();
                bubble.setText(txt.equals("...") ? "." : txt + ".");
            }));
            typingTimeline.setCycleCount(Animation.INDEFINITE);
            typingTimeline.play();
        });
    }

    private void removeTypingIndicator() {
        if (typingBox != null) chatPanel.removeNode(typingBox);
        if (typingTimeline != null) typingTimeline.stop();
        typingBox = null;
    }

    private void injectBelief(String literal) {
        new Thread(() -> {
            try { ts.getAg().addBel(Literal.parseLiteral(literal)); } 
            catch (Exception ex) { logger.severe("Failed to inject belief: " + ex.getMessage()); }
        }).start();
    }

    public void updateTrait(String player, String trait, double value) {
        Platform.runLater(() -> {
            if (heatmapPanel != null) {
                heatmapPanel.updateCell(player, trait, value);
            }
        });
    }

    public void updateOwnTrait(String trait, double value, boolean isMood) {
        Platform.runLater(() -> {
            if (heatmapPanel != null) {
                heatmapPanel.updateOwnTrait(trait, value, isMood);
            }
        });
    }
}