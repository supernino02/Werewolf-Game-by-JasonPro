package ui.components;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.application.Platform;

public class ChatPanel extends VBox {
    private VBox chatBox;
    private boolean autoScroll = true;

    public ChatPanel() {
        chatBox = new VBox(15);
        chatBox.setPadding(new Insets(15));
        
        ScrollPane scrollPane = new ScrollPane(chatBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        
        chatBox.heightProperty().addListener((obs, oldV, newV) -> {
            if (autoScroll) {
                scrollPane.layout();
                scrollPane.setVvalue(1.0);
                Platform.runLater(() -> scrollPane.setVvalue(1.0));
            }
        });
        
        scrollPane.setOnScroll(event -> {
            autoScroll = false;
        });
        
        scrollPane.setOnMousePressed(event -> {
            autoScroll = false;
        });
        
        scrollPane.setOnMouseReleased(event -> {
            if (scrollPane.getVvalue() >= 0.99) {
                autoScroll = true;
            }
        });
        
        scrollPane.vvalueProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() >= 0.99) {
                autoScroll = true;
            }
        });
        
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.getChildren().add(scrollPane);
        this.setStyle("-fx-background-color: #f4f5f7; -fx-border-color: #d1d5da; -fx-border-width: 0 1 0 0;");
    }

    // --- NEW: updated signature with showCorrect and correctPerformative ---
    public void addPlayerMessage(String sender, String msg, String performative, boolean isMe, String iconStr, double delayMs, boolean showCorrect, String correctPerformative) {
        VBox container = new VBox(2);
        
        Label nameLabel = new Label(isMe ? "You" : sender);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6a737d; -fx-font-weight: bold;");

        Label iconLabel = new Label(iconStr); 
        iconLabel.setStyle("-fx-font-size: 13px;");

        HBox headerRow = new HBox(5); 
        if (isMe) {
            headerRow.setAlignment(Pos.CENTER_RIGHT);
            headerRow.getChildren().addAll(nameLabel, iconLabel); 
        } else {
            headerRow.setAlignment(Pos.CENTER_LEFT);
            headerRow.getChildren().addAll(iconLabel, nameLabel); 
        }

        Label bubble = new Label(""); 
        bubble.setWrapText(true);
        bubble.maxWidthProperty().bind(this.widthProperty().multiply(0.75));
        bubble.setStyle(isMe ? 
            "-fx-font-size: 16px; -fx-background-color: #DCF8C6; -fx-padding: 12; -fx-background-radius: 18 0 18 18;" : 
            "-fx-font-size: 16px; -fx-background-color: #ffffff; -fx-border-color: #e1e4e8; -fx-padding: 12; -fx-background-radius: 0 18 18 18;");

        container.setAlignment(isMe ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        container.getChildren().addAll(headerRow, bubble);

// --- Performative Color Correction & Strikethrough Logic ---
            if (performative != null && !performative.isEmpty()) {
                String displayPerf = performative.replace("(", " → ").replace(")", "");
                Label perfLabel = new Label(displayPerf);
                
                String color = "#959da5"; // default gray
                String strikethrough = ""; // default no strikethrough
                HBox footerRow = new HBox(8); // spacing for items inside the footer
                
                if (showCorrect && correctPerformative != null) {
                    if (performative.equals(correctPerformative)) {
                        // If it's correct, keep it default gray. 
                        color = "#959da5"; 
                    } else {
                        // If it's wrong, make the wrong one red with strikethrough
                        color = "#d98282"; // Dimmed out red
                        strikethrough = " -fx-strikethrough: true;";
                        
                        // And show the correct one in green
                        String displayCorrect = correctPerformative.replace("(", " → ").replace(")", "");
                        Label correctLabel = new Label(displayCorrect);
                        correctLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8db891; -fx-font-weight: bold;");
                        
                        if (!isMe) {
                            footerRow.getChildren().add(correctLabel); 
                        }
                    }
                }
                
                perfLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + "; -fx-font-weight: bold;" + strikethrough);
                footerRow.getChildren().add(perfLabel); 
                
                if (isMe) {
                    footerRow.setAlignment(Pos.CENTER_LEFT);
                    HBox.setMargin(footerRow, new Insets(0, 0, 0, 5)); 
                } else {
                    footerRow.setAlignment(Pos.CENTER_RIGHT);
                    HBox.setMargin(footerRow, new Insets(0, 5, 0, 0)); 
                }
                
                container.getChildren().add(footerRow); 
            }
        
        HBox row = new HBox(container);
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatBox.getChildren().add(row);

        animateText(bubble, msg, delayMs);    
    }
    
    public void addPhaseMessage(String phaseTitle, String msg, double delayMs) {
        VBox phaseBox = new VBox(8);
        phaseBox.setPadding(new Insets(15));
        phaseBox.setStyle("-fx-background-color: #e1e4e8; -fx-background-radius: 12; -fx-border-color: #d1d5da; -fx-border-radius: 12;");

        Label titleLabel = new Label(phaseTitle.toUpperCase());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: black; -fx-font-weight: bold;");

        HBox header = new HBox(titleLabel);
        header.setAlignment(Pos.TOP_RIGHT);

        Label msgLabel = new Label(""); 
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #24292e;");
        msgLabel.setAlignment(Pos.CENTER);

        phaseBox.getChildren().addAll(header, msgLabel);
        HBox row = new HBox(phaseBox);
        HBox.setHgrow(phaseBox, Priority.ALWAYS);
        chatBox.getChildren().add(row);

        animateText(msgLabel, msg, delayMs);
    }
    
    private void animateText(Label label, String text, double delayMs) {
        label.setText("");
        if (text == null || text.isEmpty()) return;
        
        String formattedText = text.replace(". ", ".\n")
                                   .replace("! ", "!\n")
                                   .replace("? ", "?\n");

        if (delayMs <= 0) {
            label.setText(formattedText);
            return;
        }

        final int[] index = {0};
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(delayMs), event -> {
            if (index[0] < formattedText.length()) {
                label.setText(label.getText() + formattedText.charAt(index[0]));
                index[0]++;
            }
        }));
        
        timeline.setCycleCount(formattedText.length());
        timeline.play();
    }

    public void removeNode(javafx.scene.Node node) { chatBox.getChildren().remove(node); }
    public void addNode(javafx.scene.Node node) { chatBox.getChildren().add(node); }

    // --- NEW: INTENT SUMMARY RENDERER ---
    public void addIntentSummary(java.util.Map<String, String> intents) {
        if (intents == null || intents.isEmpty()) return;

        java.util.List<String> wants = new java.util.ArrayList<>();
        java.util.List<String> refuses = new java.util.ArrayList<>();
        java.util.List<String> targeted = new java.util.ArrayList<>();
        java.util.List<String> privileged = new java.util.ArrayList<>();

        for (java.util.Map.Entry<String, String> entry : intents.entrySet()) {
            String p = entry.getKey();
            switch (entry.getValue().toLowerCase()) {
                case "yes": wants.add(p); break;
                case "no":
                case "end_turn": refuses.add(p); break;
                case "targeted": targeted.add(p); break;
                case "privileged": privileged.add(p); break;
            }
        }

        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #d1d5da; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label title = new Label("--- INTENT TO SPEAK SUMMARY ---");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #586069; -fx-font-size: 12px;");
        box.getChildren().add(title);

        if (!privileged.isEmpty()) box.getChildren().add(createSummaryRow("👑 Privileged: ", privileged));
        if (!wants.isEmpty()) box.getChildren().add(createSummaryRow("💬 Want to speak: ", wants));
        if (!targeted.isEmpty()) box.getChildren().add(createSummaryRow("🎯 Targeted: ", targeted));
        if (!refuses.isEmpty()) box.getChildren().add(createSummaryRow("❌ Refuse to speak: ", refuses));

        HBox row = new HBox(box);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(10, 0, 10, 0));
        chatBox.getChildren().add(row);
        
        Platform.runLater(() -> {
            if (autoScroll && this.getChildren().get(0) instanceof ScrollPane) {
                ((ScrollPane) this.getChildren().get(0)).setVvalue(1.0);
            }
        });
    }

    private Label createSummaryRow(String prefix, java.util.List<String> players) {
        Label l = new Label(prefix + String.join(", ", players));
        l.setStyle("-fx-font-size: 14px; -fx-text-fill: #24292e;");
        return l;
    }

    // --- NEW: EXPLICIT ACTION/VOTE MESSAGE RENDERER ---
    public void addActionMessage(String actor, String target, String actionType) {
        Platform.runLater(() -> {
            VBox actionBox = new VBox(5);
            actionBox.setPadding(new Insets(10, 15, 10, 15));
            // Slight off-white background with a subtle border to distinguish it from chat bubbles
            actionBox.setStyle("-fx-background-color: #fdfdfe; -fx-border-color: #e1e4e8; -fx-border-radius: 8; -fx-background-radius: 8;");

            // Use the appropriate icon based on the action type (hunt vs vote)
            String iconStr = actionType.equalsIgnoreCase("hunt") ? "🐾" : "🎯";
            String actionColor = actionType.equalsIgnoreCase("hunt") ? "#d73a49" : "#0366d6";

            Label actionLabel = new Label(iconStr + " " + actor.toUpperCase() + " " + actionType.toUpperCase() + " " + target.toUpperCase());
            actionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + actionColor + "; -fx-font-weight: bold;");
            actionBox.getChildren().add(actionLabel);

            // Wrap in an HBox to center it in the chat feed like the phase/summary messages
            HBox row = new HBox(actionBox);
            row.setAlignment(Pos.CENTER);
            row.setPadding(new Insets(5, 0, 5, 0));
            chatBox.getChildren().add(row);

            // Maintain autoscroll
            if (autoScroll && this.getChildren().get(0) instanceof ScrollPane) {
                ((ScrollPane) this.getChildren().get(0)).setVvalue(1.0);
            }
        });
    }
}