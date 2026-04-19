package ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class HeatmapPanel extends VBox {
    private GridPane grid;
    private Map<String, Integer> rowMap = new HashMap<>();
    private Map<String, Integer> colMap = new HashMap<>();
    private Map<String, Rectangle> cellRects = new HashMap<>();
    private Map<String, Label> cellLabels = new HashMap<>();
    private Map<String, Double> cellValues = new HashMap<>();
    
    private Map<String, Label> roleIcons = new HashMap<>();
    private Map<String, Label> nameLabels = new HashMap<>();
    private List<String> currentTraits = new ArrayList<>();

    private Set<String> deadPlayers = new HashSet<>();
    private Map<String, List<Node>> clickNodes = new HashMap<>();
    private boolean isInteractive = false;
    
    private double minBound;
    private double maxBound;
    private String valFormat;

    // --- SELF TEMPER COMPONENTS ---
    private VBox selfStateBox;
    private VBox personalityBox;
    private VBox moodBox;
    private Map<String, TraitRow> selfTraitRows = new HashMap<>();

    private HBox legendBox;
    private ScrollPane scroll;
    private Label noTraitsLabel;
    private int nextCol = 1;
    
    private Consumer<String> onPlayerClick;
    private Consumer<String> onPlayerHoverEnter;
    private Consumer<String> onPlayerHoverExit;

    public HeatmapPanel(List<String> players, String agentName, double minBound, double maxBound, int sigDigits) {
        this.minBound = minBound;
        this.maxBound = maxBound;
        this.valFormat = "%." + sigDigits + "f";
        
        this.setPadding(new Insets(15));
        this.setSpacing(15);
        this.setStyle("-fx-background-color: #f4f5f7;");
        this.setAlignment(Pos.TOP_CENTER);

        // --- HORIZONTAL COLORMAP LEGEND (COOLWARM) ---
        legendBox = new HBox(8);
        legendBox.setAlignment(Pos.CENTER);
        legendBox.setPadding(new Insets(0, 0, 5, 0));
        
        Rectangle legendRect = new Rectangle(150, 15);
        LinearGradient gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#3b4cc0")),   // Deep Blue
                new Stop(0.5, Color.web("#f2f2f2")),   // Neutral Light Gray/White
                new Stop(1.0, Color.web("#b40426"))    // Deep Red
        );
        legendRect.setFill(gradient);
        legendRect.setStroke(Color.web("#d1d5da"));
        
        Label lLow = new Label(String.format(java.util.Locale.US, valFormat, minBound));
        lLow.setStyle("-fx-font-size: 11px; -fx-text-fill: #586069; font-weight: bold;");
        Label lHigh = new Label(String.format(java.util.Locale.US, valFormat, maxBound));
        lHigh.setStyle("-fx-font-size: 11px; -fx-text-fill: #586069; font-weight: bold;");
        
        legendBox.getChildren().addAll(lLow, legendRect, lHigh);
        legendBox.setVisible(false);
        legendBox.setManaged(false);

        // --- SELF TEMPER PANEL INITIALIZATION ---
        selfStateBox = new VBox(15);
        selfStateBox.setAlignment(Pos.CENTER);
        selfStateBox.setStyle("-fx-background-color: white; -fx-border-color: #e1e4e8; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 15;");
        
        Label stateTitle = new Label(agentName.toUpperCase(java.util.Locale.US));
        stateTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #24292e; -fx-font-size: 13px;");
        
        personalityBox = new VBox(8);
        personalityBox.setAlignment(Pos.CENTER);
        
        moodBox = new VBox(8);
        moodBox.setAlignment(Pos.CENTER);
        
        selfStateBox.getChildren().addAll(stateTitle, personalityBox, moodBox);
        selfStateBox.setVisible(false); // Hidden until traits are added
        selfStateBox.setManaged(false);

        // --- GRID INITIALIZATION ---
        grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setAlignment(Pos.TOP_CENTER);

        int row = 1;
        for (String p : players) {
            if (p.equalsIgnoreCase(agentName)) continue;
            clickNodes.put(p, new ArrayList<>());

            // 1. Name Label
            Label pLabel = new Label(p);
            pLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 10 0 0; -fx-text-fill: #586069;");
            pLabel.setCursor(Cursor.DEFAULT);
            pLabel.setOnMouseClicked(e -> fireClick(p));
            
            pLabel.setOnMouseEntered(e -> fireHover(p, true));
            pLabel.setOnMouseExited(e -> fireHover(p, false));
            
            grid.add(pLabel, 0, row);
            
            rowMap.put(p, row);
            nameLabels.put(p, pLabel);
            clickNodes.get(p).add(pLabel);

            // 2. Role Icon
            Label iconLabel = new Label("👤");
            iconLabel.setStyle("-fx-font-size: 16px;");
            iconLabel.setCursor(Cursor.DEFAULT);
            iconLabel.setOnMouseClicked(e -> fireClick(p));
            
            iconLabel.setOnMouseEntered(e -> fireHover(p, true));
            iconLabel.setOnMouseExited(e -> fireHover(p, false));
            
            roleIcons.put(p, iconLabel);
            grid.add(iconLabel, nextCol, row);
            clickNodes.get(p).add(iconLabel);

            row++;
        }

        scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #f4f5f7;");
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVisible(false);
        scroll.setManaged(false);

        noTraitsLabel = new Label("No external traits to display yet.");
        noTraitsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #959da5; -fx-font-style: italic; -fx-padding: 20 0 0 0;");
        noTraitsLabel.setAlignment(Pos.CENTER);
        noTraitsLabel.setMaxWidth(Double.MAX_VALUE);

        // Added legendBox as the first element
        this.getChildren().addAll(legendBox, selfStateBox, noTraitsLabel, scroll);
    }

    //---------------
    // AGENT TEMPER STATE
    //---------------
    
    // Inner class to construct and manage the bar chart layout for each trait
    private class TraitRow extends HBox {
        private HBox negBox;
        private HBox posBox;
        private Label valLabel;
        private Rectangle bar;

        public TraitRow(String trait, boolean isMood) {
            this.setSpacing(15);
            this.setAlignment(Pos.CENTER);

            // Left-side Label
            Label nameLabel = new Label(trait.toUpperCase());
            nameLabel.setPrefWidth(90);
            nameLabel.setAlignment(Pos.CENTER_RIGHT);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

            Color styleColor = isMood ? Color.GRAY : Color.BLACK;
            nameLabel.setTextFill(styleColor);

            // Negative axis (Draws Left)
            negBox = new HBox(5);
            negBox.setPrefWidth(135);
            negBox.setAlignment(Pos.CENTER_RIGHT);

            // Center Origin Line
            Rectangle divider = new Rectangle(2, 16, Color.web("#e1e4e8"));

            // Positive axis (Draws Right)
            posBox = new HBox(5);
            posBox.setPrefWidth(135);
            posBox.setAlignment(Pos.CENTER_LEFT);

            // Value label - hidden by default
            valLabel = new Label();
            valLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
            valLabel.setTextFill(styleColor);
            valLabel.setVisible(false);

            bar = new Rectangle(0, 14);
            bar.setStroke(styleColor);
            bar.setStrokeType(StrokeType.INSIDE);
            bar.setStrokeWidth(1.5);
            
            this.getChildren().addAll(nameLabel, negBox, divider, posBox);

            // Hover interactions trigger label visibility
            this.setOnMouseEntered(e -> valLabel.setVisible(true));
            this.setOnMouseExited(e -> valLabel.setVisible(false));
        }

        public void update(double value, double minB, double maxB, String format) {
            negBox.getChildren().clear();
            posBox.getChildren().clear();

            valLabel.setText(String.format(java.util.Locale.US, format, value));
            
            // Mathematically establish maximum width based on existing bounds
            double maxScale = Math.max(Math.abs(minB), Math.abs(maxB));
            if (maxScale == 0) maxScale = 1.0; 
            
            double barWidth = (Math.abs(value) / maxScale) * 100.0;
            barWidth = Math.min(barWidth, 115.0); // Clip physical width slightly beyond axis if needed
            
            bar.setWidth(barWidth);

            // Set up the dynamic colormap gradient (from 0/neutral to the numerical value color)
            Color neutralColor = getColorForValue(0.0);
            Color targetColor = getColorForValue(value);

            if (value < 0) {
                // Negative draws right to left. Left edge (0%) is target, right edge (100%) is neutral.
                LinearGradient grad = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, targetColor),
                    new Stop(1, neutralColor));
                bar.setFill(grad);
                negBox.getChildren().addAll(valLabel, bar); // Label on the outside
            } else {
                // Positive draws left to right. Left edge (0%) is neutral, right edge (100%) is target.
                LinearGradient grad = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, neutralColor),
                    new Stop(1, targetColor));
                bar.setFill(grad);
                posBox.getChildren().addAll(bar, valLabel); // Label on the outside
            }
        }
    }

    public void updateOwnTrait(String trait, double value, boolean isMood) {
        // Ensure the colorbar and self box are shown when temper traits arrive
        legendBox.setVisible(true);
        legendBox.setManaged(true);

        selfStateBox.setVisible(true);
        selfStateBox.setManaged(true);
        
        TraitRow row = selfTraitRows.get(trait);
        if (row == null) {
            row = new TraitRow(trait, isMood);
            selfTraitRows.put(trait, row);
            
            if (isMood) {
                moodBox.getChildren().add(row);
            } else {
                personalityBox.getChildren().add(row);
            }
        }
        
        row.update(value, minBound, maxBound, valFormat);
    }

    //---------------
    // EXISTING EVENT / HEATMAP LOGIC 
    //---------------

    public void setOnPlayerClicked(Consumer<String> cb) { this.onPlayerClick = cb; }
    public void setOnPlayerHoverEnter(Consumer<String> cb) { this.onPlayerHoverEnter = cb; }
    public void setOnPlayerHoverExit(Consumer<String> cb) { this.onPlayerHoverExit = cb; }

    private void fireClick(String player) {
        if (!isInteractive || deadPlayers.contains(player)) return;
        if (onPlayerClick != null) onPlayerClick.accept(player);
    }

    private void fireHover(String player, boolean hover) {
        if (deadPlayers.contains(player)) return;
        highlightRow(player, hover);
        if (hover && onPlayerHoverEnter != null) onPlayerHoverEnter.accept(player);
        if (!hover && onPlayerHoverExit != null) onPlayerHoverExit.accept(player);
    }

    public void highlightRow(String player, boolean hover) {
        if (deadPlayers.contains(player)) return;
        Label pLabel = nameLabels.get(player);
        Label iconLabel = roleIcons.get(player);
        
        if (pLabel != null && iconLabel != null) {
            if (hover) {
                pLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 10 0 0; -fx-text-fill: #0389d6;");
                iconLabel.setScaleX(1.4);
                iconLabel.setScaleY(1.4);
            } else {
                pLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 10 0 0; -fx-text-fill: #586069;");
                iconLabel.setScaleX(1.0);
                iconLabel.setScaleY(1.0);
            }
        }
    }

    public void setInteractive(boolean interactive) {
        this.isInteractive = interactive;
        for (String p : rowMap.keySet()) {
            if (!deadPlayers.contains(p)) {
                Cursor c = interactive ? Cursor.HAND : Cursor.DEFAULT;
                for (Node n : clickNodes.get(p)) {
                    n.setCursor(c);
                }
            }
        }
    }

    private void addTraitColumn(String trait) {
        if (colMap.containsKey(trait)) return;
        int c = nextCol;
        colMap.put(trait, c);
        currentTraits.add(trait);
        nextCol++;

        for (Map.Entry<String, Label> entry : roleIcons.entrySet()) {
            GridPane.setColumnIndex(entry.getValue(), nextCol);
        }

        Label tLabel = new Label(trait.toUpperCase());
        tLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #24292e;");
        tLabel.setRotate(-90);
        Group textGroup = new Group(tLabel);
        VBox headerBox = new VBox(textGroup);
        headerBox.setAlignment(Pos.BOTTOM_CENTER);
        headerBox.setMinHeight(85); 
        grid.add(headerBox, c, 0);

        for (String p : rowMap.keySet()) {
            StackPane stack = new StackPane();
            stack.setCursor(Cursor.DEFAULT);
            
            Rectangle cell = new Rectangle(35, 25);
            cell.setFill(Color.TRANSPARENT); 
            cell.setStroke(Color.TRANSPARENT);
            cell.setStrokeType(StrokeType.INSIDE);
            cell.setStrokeWidth(1);
            cell.setArcWidth(4); 
            cell.setArcHeight(4);
            
            Label valLabel = new Label("");
            valLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: black;");
            valLabel.setVisible(false);

            stack.getChildren().addAll(cell, valLabel);
            grid.add(stack, c, rowMap.get(p));
            clickNodes.get(p).add(stack);

            String key = p + "_" + trait;
            cellRects.put(key, cell);
            cellLabels.put(key, valLabel);
            cellValues.put(key, Double.NaN);

            stack.setOnMouseEntered(e -> {
                if (!deadPlayers.contains(p)) highlightColumn(trait, true);
            });
            stack.setOnMouseExited(e -> {
                if (!deadPlayers.contains(p)) highlightColumn(trait, false);
            });
            stack.setOnMouseClicked(e -> fireClick(p));
        }
    }

    private void highlightColumn(String trait, boolean hover) {
        if (hover) {
            double max = Double.NEGATIVE_INFINITY;
            double min = Double.POSITIVE_INFINITY;
            for (String p : rowMap.keySet()) {
                if (deadPlayers.contains(p)) continue;
                String key = p + "_" + trait;
                double val = cellValues.getOrDefault(key, Double.NaN);
                if (!Double.isNaN(val)) {
                    if (val > max) max = val;
                    if (val < min) min = val;
                }
            }

            for (String p : rowMap.keySet()) {
                String key = p + "_" + trait;
                Rectangle cell = cellRects.get(key);
                Label lbl = cellLabels.get(key);

                if (cell != null && lbl != null) {
                    double val = cellValues.getOrDefault(key, Double.NaN);
                    if (deadPlayers.contains(p) || Double.isNaN(val)) {
                        lbl.setVisible(false);
                        if (Double.isNaN(val)) cell.setStroke(Color.TRANSPARENT); 
                        continue;
                    }

                    lbl.setText(String.format(java.util.Locale.US, valFormat, val));
                    lbl.setVisible(true);

                    if (!Double.isInfinite(max) && !Double.isInfinite(min) && (val == max || val == min)) {
                        cell.setStroke(Color.BLACK);
                        cell.setStrokeWidth(2);
                    } else {
                        cell.setStroke(Color.web("#d1d5da"));
                        cell.setStrokeWidth(1);
                    }
                }
            }
        } else {
            for (String p : rowMap.keySet()) {
                String key = p + "_" + trait;
                Rectangle cell = cellRects.get(key);
                Label lbl = cellLabels.get(key);
                if (cell != null && lbl != null) {
                    double val = cellValues.getOrDefault(key, Double.NaN);
                    if (Double.isNaN(val)) {
                        cell.setStroke(Color.TRANSPARENT);
                    } else {
                        cell.setStroke(Color.web("#d1d5da"));
                        cell.setStrokeWidth(1);
                    }
                    lbl.setVisible(false);
                }
            }
        }
    }

    /**
     * Mathematically scaled to ensure exactly 0.0 is the neutral (#f2f2f2) point.
     * All values > 0 scale perfectly towards Red.
     * All values < 0 scale perfectly towards Blue.
     */
    private Color getColorForValue(double value) {
        if (Double.isNaN(value)) return Color.TRANSPARENT;
        
        if (value < 0.0) {
            // Negative values map towards Blue
            double factor = (minBound >= 0) ? 0 : Math.max(0.0, Math.min(1.0, value / minBound));
            return Color.web("#f2f2f2").interpolate(Color.web("#3b4cc0"), factor);
        } else {
            // Positive values map towards Red
            double factor = (maxBound <= 0) ? 0 : Math.max(0.0, Math.min(1.0, value / maxBound));
            return Color.web("#f2f2f2").interpolate(Color.web("#b40426"), factor);
        }
    }

    public void updateCell(String player, String trait, double value) {
        if (!rowMap.containsKey(player)) return;

        if (currentTraits.isEmpty()) {
            noTraitsLabel.setVisible(false);
            noTraitsLabel.setManaged(false);
            legendBox.setVisible(true);
            legendBox.setManaged(true);
            scroll.setVisible(true);
            scroll.setManaged(true);
        }

        if (!colMap.containsKey(trait)) {
            addTraitColumn(trait);
        }

        String key = player + "_" + trait;
        cellValues.put(key, value);
        Rectangle cell = cellRects.get(key);
        if (cell != null) {
            if (Double.isNaN(value)) {
                cell.setFill(Color.TRANSPARENT);
                cell.setStroke(Color.TRANSPARENT);
            } else {
                cell.setFill(getColorForValue(value));
                cell.setStroke(Color.web("#d1d5da")); 
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
        deadPlayers.add(p);
        if (nameLabels.containsKey(p)) {
            nameLabels.get(p).setOpacity(0.3);
            roleIcons.get(p).setOpacity(0.3);
            for (String trait : currentTraits) {
                Rectangle cell = cellRects.get(p + "_" + trait);
                if (cell != null) cell.setOpacity(0.2);
            }
            
            for (Node n : clickNodes.get(p)) {
                n.setCursor(Cursor.DEFAULT);
            }
        }
    }
}