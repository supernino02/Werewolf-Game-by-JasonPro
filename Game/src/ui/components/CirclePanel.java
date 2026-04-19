package ui.components;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurve;
import javafx.scene.transform.Rotate;
import javafx.scene.Cursor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CirclePanel extends Pane {
    private Map<String, VBox> playerNodes = new HashMap<>();
    private Map<String, Label> outerLabels = new HashMap<>(); 
    private Map<String, Label> roleIcons = new HashMap<>();
    private Map<String, Double> angles = new HashMap<>();
    
    private Group arrowGroup = new Group();
    private Group currentDraftArrow = null; // Tracks the temporary arrow during selection

    // --- NEW: Custom tooltip label ---
    private Label tooltipLabel; 

    private Consumer<String> onPlayerClick;
    private Consumer<String> onPlayerHoverEnter;
    private Consumer<String> onPlayerHoverExit;

    public CirclePanel(List<String> players, String agentName) {
        this.getChildren().add(arrowGroup);

        // --- NEW: Initialize the custom floating tooltip ---
        tooltipLabel = new Label();
        tooltipLabel.setVisible(false);
        tooltipLabel.setMouseTransparent(true); // Crucial: prevents the label from blocking mouse events
        tooltipLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-text-fill: white; -fx-padding: 6 12; -fx-background-radius: 6; -fx-font-size: 14px; -fx-font-weight: bold;");
        this.getChildren().add(tooltipLabel);

        int n = players.size();

        for (int i = 0; i < n; i++) {
            String pName = players.get(i);
            double angle = 2 * Math.PI * i / n - (Math.PI / 2);
            angles.put(pName, angle);

            // 1. Icon & Name (Name hidden by default)
            Label iconLabel = new Label("👤");
            iconLabel.setStyle("-fx-font-size: 35px;");
            roleIcons.put(pName, iconLabel);

            Label textLabel = new Label(pName);
            textLabel.setOpacity(0.0); // Hidden by default

            VBox pBox = new VBox(2, iconLabel, textLabel);
            pBox.setAlignment(Pos.CENTER);
            pBox.setStyle("-fx-padding: 8;");
            pBox.setCursor(Cursor.DEFAULT);

            // Z-INDEX FIX AND HOVER TRIGGERS
            pBox.setOnMouseEntered(e -> {
                textLabel.setOpacity(1.0);
                pBox.toFront(); 
                if (!pBox.isDisabled() && onPlayerHoverEnter != null) onPlayerHoverEnter.accept(pName);
            });

            pBox.setOnMouseExited(e -> {
                textLabel.setOpacity(0.0);
                pBox.toBack(); 
                arrowGroup.toBack(); // Keep arrows at the absolute back!
                if (!pBox.isDisabled() && onPlayerHoverExit != null) onPlayerHoverExit.accept(pName);
            });

            pBox.setOnMouseClicked(e -> {
                if (!pBox.isDisabled() && onPlayerClick != null) onPlayerClick.accept(pName);
            });

            // HIGH CONTRAST PILL STYLING
            if (pName.equalsIgnoreCase(agentName)) {
                textLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #f66a0a; -fx-padding: 2 8 2 8; -fx-background-radius: 12;");
                iconLabel.setStyle("-fx-font-size: 35px; -fx-text-fill: orange;");
            } else {
                textLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #24292e; -fx-background-color: white; -fx-padding: 2 8 2 8; -fx-background-radius: 12; -fx-border-color: #d1d5da; -fx-border-radius: 12;");
            }

            playerNodes.put(pName, pBox);

            // 2. Outer Circle Label
            Label outerLabel = new Label();
            outerLabel.setVisible(false);
            outerLabel.setAlignment(Pos.CENTER);
            outerLabels.put(pName, outerLabel);

            this.getChildren().addAll(pBox, outerLabel);

            // 3. Mathematical Polar Geometry Bindings
            NumberBinding cx = widthProperty().divide(2);
            NumberBinding cy = heightProperty().divide(2);
            NumberBinding halfMin = Bindings.min(widthProperty(), heightProperty()).divide(2);
            
            NumberBinding outerR = halfMin.subtract(30);
            NumberBinding r = outerR.subtract(50);

            pBox.layoutXProperty().bind(cx.add(r.multiply(Math.cos(angle))));
            pBox.layoutYProperty().bind(cy.add(r.multiply(Math.sin(angle))));
            pBox.translateXProperty().bind(pBox.widthProperty().divide(-2));
            pBox.translateYProperty().bind(iconLabel.heightProperty().divide(-2)); 
            
            outerLabel.layoutXProperty().bind(cx.add(outerR.multiply(Math.cos(angle))));
            outerLabel.layoutYProperty().bind(cy.add(outerR.multiply(Math.sin(angle))));
            outerLabel.translateXProperty().bind(outerLabel.widthProperty().divide(-2));
            outerLabel.translateYProperty().bind(outerLabel.heightProperty().divide(-2));
        }
    }

    // Callbacks for the Controller
    public void setOnPlayerClicked(Consumer<String> cb) { this.onPlayerClick = cb; }
    public void setOnPlayerHoverEnter(Consumer<String> cb) { this.onPlayerHoverEnter = cb; }
    public void setOnPlayerHoverExit(Consumer<String> cb) { this.onPlayerHoverExit = cb; }

    // Remote trigger method (used by Heatmap)
    public void setPlayerHover(String pName, boolean hover) {
        VBox pBox = playerNodes.get(pName);
        if (pBox != null && !pBox.isDisabled()) {
            Label textLabel = (Label) pBox.getChildren().get(1);
            if (hover) {
                textLabel.setOpacity(1.0);
                pBox.toFront();
            } else {
                textLabel.setOpacity(0.0);
                pBox.toBack();
                arrowGroup.toBack();
            }
        }
    }

    public void updateIcons(List<String> targets, String iconType) {
        String iconStr = iconType.equalsIgnoreCase("wolf") ? "🐺" : "👤";
        for (String t : targets) {
            if (roleIcons.containsKey(t)) roleIcons.get(t).setText(iconStr);
        }
    }

    public void markDead(String p) {
        if (playerNodes.containsKey(p)) {
            playerNodes.get(p).setOpacity(0.2);
            playerNodes.get(p).setDisable(true);
        }
    }

    public void setBalloon(String p, String intent) {
        Label b = outerLabels.get(p);
        if (b == null) return;
        b.setVisible(true);
        String baseStyle = "-fx-font-size: 26px; -fx-background-color: white; -fx-border-color: #d1d5da; -fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 4 10;";
        
        switch (intent.toLowerCase()) {
            case "yes": b.setText("💬");
                b.setStyle("-fx-text-fill: #28a745; " + baseStyle); break;
            case "privileged": b.setText("👑"); b.setStyle("-fx-text-fill: #f66a0a; " + baseStyle); break;
            case "targeted": b.setText("🎯");
                b.setStyle("-fx-text-fill: black; " + baseStyle); break;
            case "no": 
            case "end_turn": b.setText("❌");
                b.setStyle("-fx-text-fill: #d73a49; " + baseStyle); break;
        }
    }

    public void clearBalloons() { outerLabels.values().forEach(b -> b.setVisible(false)); }

    public void drawVotes(Map<String, String> votes, String phaseType, String agentName, String tempTarget) {
        arrowGroup.getChildren().clear();
        currentDraftArrow = null;
        outerLabels.values().forEach(l -> l.setVisible(false));

        Map<String, Integer> tallies = new HashMap<>();
        String iconStr = phaseType.equalsIgnoreCase("hunt") ? "🐾" : "🎯";

        for (Map.Entry<String, String> entry : votes.entrySet()) {
            tallies.put(entry.getValue(), tallies.getOrDefault(entry.getValue(), 0) + 1);
            drawArrow(entry.getKey(), entry.getValue(), Color.web("#d73a49"), false, agentName);
        }

        if (tempTarget != null) {
            tallies.put(tempTarget, tallies.getOrDefault(tempTarget, 0) + 1);
            drawArrow(agentName, tempTarget, Color.web("#0366d6"), true, agentName);
        }

        for (Map.Entry<String, Integer> t : tallies.entrySet()) {
            Label lbl = outerLabels.get(t.getKey());
            if (lbl != null) {
                lbl.setText(iconStr + " x" + t.getValue());
                lbl.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #d73a49; -fx-background-color: transparent; -fx-border-color: transparent;");
                lbl.setVisible(true);
            }
        }
    }

    public void drawInteractionArrow(String source, String target, boolean isCommitted, String performative) {
        if (currentDraftArrow != null) {
            arrowGroup.getChildren().remove(currentDraftArrow);
            currentDraftArrow = null;
        }

        if (isCommitted) {
            for (javafx.scene.Node node : arrowGroup.getChildren()) {
                if (node instanceof Group) {
                    Group g = (Group) node;
                    g.setOpacity(0.15); 
                    for (javafx.scene.Node child : g.getChildren()) {
                        if (child instanceof QuadCurve) {
                            ((QuadCurve) child).getStrokeDashArray().clear(); 
                        }
                    }
                }
            }
            // Pass the performative to the final arrow
            drawArrow(source, target, Color.web("#0366d6"), true, source, performative);
        } else {
            // Draft arrow doesn't need a performative tooltip yet
            currentDraftArrow = drawArrow(source, target, Color.web("#0366d6"), true, source, null);
        }
    }

    // Overloaded to keep compatibility with drawVotes
    private Group drawArrow(String source, String target, Color baseColor, boolean isDashed, String agentName) {
        return drawArrow(source, target, baseColor, isDashed, agentName, null);
    }

    private Group drawArrow(String source, String target, Color baseColor, boolean isDashed, String agentName, String performative) {
        boolean isMe = source.equalsIgnoreCase(agentName);
        double strokeWidth = isMe ? 5 : 2;

        double srcAngle = angles.getOrDefault(source, 0.0);
        double tgtAngle = angles.getOrDefault(target, 0.0);

        NumberBinding cx = widthProperty().divide(2);
        NumberBinding cy = heightProperty().divide(2);
        
        NumberBinding rOut = Bindings.min(widthProperty(), heightProperty()).divide(2).subtract(80);
        NumberBinding rIn = rOut.subtract(20);

        QuadCurve curve = new QuadCurve();
        curve.setFill(Color.TRANSPARENT);
        curve.setStroke(baseColor);
        curve.setStrokeWidth(strokeWidth);
        if (isDashed) curve.getStrokeDashArray().addAll(12d, 8d);

        curve.startXProperty().bind(cx.add(rIn.multiply(Math.cos(srcAngle))));
        curve.startYProperty().bind(cy.add(rIn.multiply(Math.sin(srcAngle))));
        curve.endXProperty().bind(cx.add(rIn.multiply(Math.cos(tgtAngle))));
        curve.endYProperty().bind(cy.add(rIn.multiply(Math.sin(tgtAngle))));

        curve.controlXProperty().bind(
            curve.startXProperty().add(curve.endXProperty()).divide(2)
            .multiply(0.75).add(cx.multiply(0.25))
        );
        curve.controlYProperty().bind(
            curve.startYProperty().add(curve.endYProperty()).divide(2)
            .multiply(0.75).add(cy.multiply(0.25))
        );

        Circle startBall = new Circle(isMe ? 8 : 4, baseColor);
        startBall.centerXProperty().bind(curve.startXProperty());
        startBall.centerYProperty().bind(curve.startYProperty());

        Polygon arrowHead = new Polygon(-12, -12, 8, 0, -12, 12);
        arrowHead.setFill(baseColor);
        if (!isMe) { arrowHead.setScaleX(0.6); arrowHead.setScaleY(0.6); }
        
        arrowHead.layoutXProperty().bind(curve.endXProperty());
        arrowHead.layoutYProperty().bind(curve.endYProperty());

        Rotate rotate = new Rotate();
        rotate.setPivotX(0); rotate.setPivotY(0);
        rotate.angleProperty().bind(Bindings.createDoubleBinding(() -> {
            double tx = curve.getEndX() - curve.getControlX();
            double ty = curve.getEndY() - curve.getControlY();
            return Math.toDegrees(Math.atan2(ty, tx));
        }, curve.endXProperty(), curve.endYProperty(), curve.controlXProperty(), curve.controlYProperty()));

        arrowHead.getTransforms().add(rotate);

        Group singleArrow = new Group(curve, startBall, arrowHead);
        singleArrow.setOpacity(isMe ? 1.0 : 0.4);

        // --- UPDATED: HOVER HIGHLIGHT & CUSTOM TOOLTIP LOGIC ---
        // Only apply hover effects and tooltips if a performative exists (Discussion arrows)
        if (performative != null && !performative.isEmpty()) {
            
            singleArrow.setOnMouseEntered(e -> {
                curve.setStrokeWidth(strokeWidth + 3);
                curve.setStroke(Color.ORANGE);
                arrowHead.setFill(Color.ORANGE);
                startBall.setFill(Color.ORANGE);
                singleArrow.toFront();

                // Instantly update and show the custom tooltip
                tooltipLabel.setText(source.toUpperCase() + " " + performative);
                tooltipLabel.toFront();
                tooltipLabel.setVisible(true);
            });

            // Force the tooltip to follow the mouse dynamically
            singleArrow.setOnMouseMoved(e -> {
                tooltipLabel.setLayoutX(e.getX() + 15);
                tooltipLabel.setLayoutY(e.getY() + 15);
            });
            
            singleArrow.setOnMouseExited(e -> {
                curve.setStrokeWidth(strokeWidth);
                curve.setStroke(baseColor);
                arrowHead.setFill(baseColor);
                startBall.setFill(baseColor);

                // Hide the tooltip instantly
                tooltipLabel.setVisible(false);
            });
        }

        arrowGroup.getChildren().add(singleArrow);
        return singleArrow;
    }

    public void clearArrows() {
        arrowGroup.getChildren().clear();
        currentDraftArrow = null;
        outerLabels.values().forEach(l -> l.setVisible(false));
    }

    public void setInteractive(boolean interactive) {
        for (VBox pBox : playerNodes.values()) {
            if (!pBox.isDisabled()) {
                pBox.setCursor(interactive ? Cursor.HAND : Cursor.DEFAULT);
            }
        }
    }
}