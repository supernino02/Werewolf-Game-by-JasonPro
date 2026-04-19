package ui.components;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;

import java.util.List;
import java.util.function.Consumer;

public class InteractionPanel extends VBox {
    
    private TextField inputField;
    private Label statusLabel;
    private Button sendButton;

    // State tracking for dynamic button validation
    private String currentPerformative;
    private String currentTarget;

    public InteractionPanel() {
        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-background-color: white; -fx-border-color: #e1e4e8; -fx-border-width: 1 0 0 0; -fx-padding: 20;");
        
        // Hide panel completely upon initialization
        this.setVisible(false);
        this.setManaged(false);
    }

    public void showAskSpeak(Runnable onYes, Runnable onNo, Runnable onEnd) {
        this.setVisible(true);
        this.setManaged(true);
        this.getChildren().clear();
        
        Label q = new Label("Ask for permission to speak?");
        q.setStyle("-fx-font-size: 21px; -fx-font-weight: bold; -fx-padding: 0 0 15 0;"); 

        Button bYes = new Button("Yes");
        bYes.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 12 30;");
        Button bNo = new Button("No");
        bNo.setStyle("-fx-background-color: #d73a49; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 12 30;");
        Button bEnd = new Button("Skip phase");
        bEnd.setStyle("-fx-background-color: #6a737d; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 12 30;");

        bYes.setOnAction(e -> onYes.run());
        bNo.setOnAction(e -> onNo.run());
        bEnd.setOnAction(e -> onEnd.run());

        HBox btns = new HBox(15, bYes, bNo, bEnd);
        btns.setAlignment(Pos.CENTER);
        this.getChildren().addAll(q, btns);
    }

    public void showSpeechSelection(List<String> actions, Consumer<String> onSelect, Runnable onSend) {
        this.setVisible(true);
        this.setManaged(true);
        this.getChildren().clear();
        
        Label q = new Label("Choose an action, target, and type your message.");
        q.setStyle("-fx-font-size: 21px; -fx-font-weight: bold; -fx-padding: 0 0 15 0;");

        // Use FlowPane so buttons wrap beautifully across multiple lines
        FlowPane btns = new FlowPane();
        btns.setHgap(10);
        btns.setVgap(10);
        btns.setAlignment(Pos.CENTER);

        for (String a : actions) {
            Button btn = new Button(a.toUpperCase());
            btn.setStyle("-fx-background-color: #0366d6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 20;");
            btn.setOnAction(e -> {
                btns.getChildren().forEach(b -> b.setOpacity(0.4));
                btn.setOpacity(1.0);
                onSelect.accept(a);
            });
            btns.getChildren().add(btn);
        }

        statusLabel = new Label("Select an action and a target in the circle.");
        statusLabel.setStyle("-fx-text-fill: #d73a49; -fx-font-style: italic; -fx-padding: 15 0 5 0; -fx-font-size: 14px;");

        // Set up the input field and send button layout
        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER);
        
        inputField = new TextField();
        inputField.setPromptText("Write your message and press Enter...");
        inputField.setStyle("-fx-font-size: 18px; -fx-padding: 10px;");
        HBox.setHgrow(inputField, Priority.ALWAYS); // Make text field take up available space

        sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 10 20;");
        sendButton.setDisable(true); // Disabled by default

        // Re-evaluate button state whenever the user types
        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            checkSendButtonState();
        });

        // Button click triggers onSend
        sendButton.setOnAction(e -> onSend.run());
        
        // Enter key in text field fires the button (if it is enabled)
        inputField.setOnAction(e -> {
            if (!sendButton.isDisabled()) {
                sendButton.fire();
            }
        });

        inputBox.getChildren().addAll(inputField, sendButton);

        this.getChildren().addAll(q, btns, statusLabel, inputBox);

        // Autofocus the text field once it renders on the JavaFX thread
        Platform.runLater(() -> inputField.requestFocus());
    }

    public void updateStatus(String performative, String target) {
        if (statusLabel == null) return;
        
        this.currentPerformative = performative;
        this.currentTarget = target;
        
        checkSendButtonState();
        
        boolean isReady = (performative != null && target != null);
        
        if (!isReady) {
            if (performative == null && target == null) {
                statusLabel.setText("Select an action and a target in the circle.");
            } else if (performative == null) {
                statusLabel.setText("Select an action.");
            } else {
                statusLabel.setText("Select a target in the circle.");
            }
            statusLabel.setStyle("-fx-text-fill: #d73a49; -fx-font-style: italic;");
        } else {
            statusLabel.setText("Ready: " + performative.toUpperCase() + " " + target);
            statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
        }
    }

    // Helper to dynamically validate the Send button
    private void checkSendButtonState() {
        if (sendButton != null && inputField != null) {
            boolean isReady = (currentPerformative != null 
                            && currentTarget != null 
                            && !inputField.getText().trim().isEmpty());
            sendButton.setDisable(!isReady);
        }
    }

    public String getInputText() {
        return inputField != null ? inputField.getText().trim() : "";
    }

    public void showVote(String type, Runnable onConfirm) {
        this.setVisible(true);
        this.setManaged(true);
        this.getChildren().clear();
        
        Label q = new Label("Select a target to " + type.toUpperCase() + ":");
        q.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 0 15 0;"); 

        Button vBtn = new Button(type.toUpperCase());
        vBtn.setStyle("-fx-background-color: #d73a49; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 12 30;");
        vBtn.setDisable(true);
        vBtn.setId("voteBtn");

        vBtn.setOnAction(e -> {
            this.getChildren().remove(vBtn);
            onConfirm.run();
        });

        this.getChildren().addAll(q, vBtn);
    }

    public void enableVoteButton() {
        this.getChildren().stream()
            .filter(n -> n instanceof Button && "voteBtn".equals(n.getId()))
            .findFirst()
            .ifPresent(b -> b.setDisable(false));
    }

    // Completely collapse the panel when cleared
    public void clear() { 
        this.getChildren().clear(); 
        this.setVisible(false);
        this.setManaged(false);
        this.statusLabel = null;
        this.inputField = null;
        this.sendButton = null;
        this.currentPerformative = null;
        this.currentTarget = null;
    }

    public void lockTerminalState(String title, String subtitle, String colorHex) {
        this.setVisible(true);
        this.setManaged(true);
        this.getChildren().clear();
        
        Label mainLabel = new Label(title);
        mainLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #586069; -fx-font-style: italic;");
        this.getChildren().addAll(mainLabel, subLabel);
    }

    public String getSanitizedInputText() {
        if (inputField == null || inputField.getText() == null) {
            return "";
        }
        
        String rawText = inputField.getText().trim();
        if (rawText.isEmpty()) {
            return "";
        }
        
        // 1. Strip unprintable control characters to prevent silent parser failure
        String cleaned = rawText.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        
        // 2. Escape Jason-breaking characters
        return cleaned
            .replace("\\", "\\\\")    // 1st: Escape existing backslashes
            .replace("\"", "\\\"")    // 2nd: Escape double quotes
            .replace("\n", "\\n")     // 3rd: Convert physical newlines to string literal \n
            .replace("\r", "")        // 4th: Strip carriage returns (OS normalization)
            .replace("\t", "\\t");    // 5th: Convert physical tabs to literal \t
    }
}