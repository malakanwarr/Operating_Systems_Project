package OS;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.OutputStream;
import java.io.PrintStream;

public class OSGUI extends Application {

    private OS os;

    // UI Components
    private ListView<String> readyQueueList;
    private ListView<String> blockedQueueList;
    private ListView<String> diskList;
    private ListView<String> memoryList;
    private ComboBox<String> schedulerSelector;

    @SuppressWarnings("unchecked")
    private ListView<String>[] mlfqQueueLists = new ListView[4];

    private Label clockLabel;
    private Label runningProcessLabel;
    private Label currentInstructionLabel;

    private TextArea processOutputArea;
    private TextArea systemLogArea;

    private Button stepButton;
    private Button resetButton;

    private Label p1Badge, p2Badge, p3Badge;
    
    private BorderPane root;

    private static final String BG            = "#f4f4f9";
    private static final String PANEL_BG      = "#ffffff";
    private static final String BORDER        = "#dcdde1";
    private static final String ACCENT_BLUE   = "#0984e3";
    private static final String ACCENT_GREEN  = "#00b894";
    private static final String ACCENT_RED    = "#d63031";
    private static final String ACCENT_GOLD   = "#e17055";
    private static final String ACCENT_PURPLE = "#6c5ce7";
    private static final String TEXT_MAIN     = "#2d3436";
    private static final String TEXT_DIM      = "#b2bec3";
    private static final String P1_COLOR      = "#0984e3";
    private static final String P2_COLOR      = "#e17055";
    private static final String P3_COLOR      = "#6c5ce7";

    @Override
    public void start(Stage stage) {	
    	schedulerSelector = buildSchedulerSelector();
    	os = new OS();
    	os.setSchedulerType(schedulerSelector.getValue());
    	schedulerSelector.setOnAction(e -> {
    	    os.setSchedulerType(schedulerSelector.getValue());
    	    ((BorderPane) stepButton.getScene().getRoot()).setRight(buildRightPanel());
    	});
    	
    	os.onBeforeExecute = () -> {
    	    runningProcessLabel.setText("Process " + os.lastRunningPID);
    	    runningProcessLabel.setStyle(
    	        "-fx-font-family: 'Courier New'; -fx-font-size: 16px; " +
    	        "-fx-font-weight: bold; -fx-text-fill: " +
    	        getProcessColor(os.lastRunningPID) + ";");
    	    currentInstructionLabel.setText(os.lastExecutedInstruction);
    	};
    	
        redirectStdOut();
        stage.setTitle("CSEN 602 — OS Simulator");
        
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");
        root.setPadding(new Insets(12));

        root.setTop(buildTopBar());
        root.setLeft(buildMemoryPanel());
        root.setCenter(buildCenterPanel());
        root.setRight(buildRightPanel());

        updateGUI();

        Scene scene = new Scene(root, 1280, 800);
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildTopBar() {
    	HBox bar = new HBox(10);
    	bar.setPadding(new Insets(0, 0, 12, 0));
    	bar.setAlignment(Pos.CENTER_LEFT);
    	bar.setStyle("-fx-background-color: " + PANEL_BG + "; " +
    	             "-fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0; " +
    	             "-fx-padding: 10 12;");

    	Label title = new Label("OS SIMULATOR");
    	title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 18px; " +
    	               "-fx-font-weight: bold; -fx-text-fill: " + ACCENT_BLUE + ";");

    	clockLabel = new Label("CLOCK  0");
    	clockLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; " +
    	                    "-fx-font-weight: bold; -fx-text-fill: " + ACCENT_GREEN + "; " +
    	                    "-fx-background-color: #e8f8f5; -fx-padding: 6 14; " +
    	                    "-fx-border-color: " + ACCENT_GREEN + "; " +
    	                    "-fx-border-radius: 4; -fx-background-radius: 4;");

    	Label schedLabel = new Label("Scheduler:");
    	schedLabel.setStyle(
    	    "-fx-text-fill: " + ACCENT_PURPLE + "; " +
    	    "-fx-font-family: 'Courier New'; " +
    	    "-fx-font-size: 12px; " +
    	    "-fx-font-weight: bold;"
    	);

    	HBox schedBox = new HBox(6, schedLabel, schedulerSelector);
    	schedBox.setAlignment(Pos.CENTER_LEFT);

    	Region spacer = new Region();
    	HBox.setHgrow(spacer, Priority.ALWAYS);

    	stepButton  = makeButton("⏭  STEP",  ACCENT_BLUE);
    	resetButton = makeButton("↺  RESET", ACCENT_RED);

    	stepButton.setOnAction(e -> onStep());
    	resetButton.setOnAction(e -> onReset());

    	bar.getChildren().addAll(
    	    title,
    	    clockLabel,
    	    schedBox,
    	    spacer,
    	    stepButton,
    	    resetButton
    	);

    	return bar;
       
    }

    private VBox buildMemoryPanel() {
        VBox panel = new VBox(8);
        panel.setPrefWidth(280);
        panel.setPadding(new Insets(12, 12, 0, 0));

        memoryList = new ListView<>();
        memoryList.setStyle("-fx-background-color: " + PANEL_BG + "; " +
                            "-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                            "-fx-border-color: " + BORDER + "; -fx-border-radius: 4;");
        VBox.setVgrow(memoryList, Priority.ALWAYS);

        memoryList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String bg, fg;
                if      (item.contains("EMPTY")) { bg = "#fafafa"; fg = TEXT_DIM;  }
                else if (item.contains("P1_"))   { bg = "#e8f4fd"; fg = P1_COLOR;  }
                else if (item.contains("P2_"))   { bg = "#fdf0eb"; fg = P2_COLOR;  }
                else if (item.contains("P3_"))   { bg = "#f0eeff"; fg = P3_COLOR;  }
                else                             { bg = PANEL_BG;  fg = TEXT_MAIN; }
                setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                         "; -fx-padding: 2 6;");
            }
        });

        panel.getChildren().addAll(sectionHeader("MAIN MEMORY  (40 words)"), memoryList);
        return panel;
    }

    private VBox buildCenterPanel() {
        VBox panel = new VBox(12);
        HBox.setHgrow(panel, Priority.ALWAYS);
        panel.setPadding(new Insets(12, 12, 0, 0));

        // ── CPU Box ───────────────────────────────────────────────
        VBox cpuBox = new VBox(8);
        cpuBox.setStyle("-fx-background-color: " + PANEL_BG + "; " +
                        "-fx-border-color: " + ACCENT_BLUE + "; -fx-border-width: 1; " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 14;");

        Label cpuTag = new Label("CPU — EXECUTING THIS CYCLE");
        cpuTag.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                        "-fx-text-fill: " + ACCENT_BLUE + "; -fx-font-weight: bold;");

        runningProcessLabel = new Label("Idle — waiting for processes");
        runningProcessLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; " +
                                     "-fx-font-weight: bold; -fx-text-fill: " + TEXT_MAIN + ";");
        runningProcessLabel.setWrapText(true);

        Label instrTag = new Label("Instruction:");
        instrTag.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                          "-fx-text-fill: " + TEXT_DIM + ";");

        currentInstructionLabel = new Label("—");
        currentInstructionLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 15px; " +
                                         "-fx-font-weight: bold; -fx-text-fill: " + ACCENT_BLUE + ";");
        currentInstructionLabel.setWrapText(true);

        cpuBox.getChildren().addAll(cpuTag, runningProcessLabel, instrTag, currentInstructionLabel);

        HBox badges = buildBadgesRow();
        VBox procOut = new VBox(6);
        VBox.setVgrow(procOut, Priority.ALWAYS);
        processOutputArea = new TextArea();
        processOutputArea.setEditable(false);
        processOutputArea.setWrapText(true);
        processOutputArea.setStyle("-fx-control-inner-background: #f9fff9; " +
                                   "-fx-text-fill: #00703c; " +
                                   "-fx-font-family: 'Courier New'; -fx-font-size: 13px; " +
                                   "-fx-border-color: " + ACCENT_GREEN + "; " +
                                   "-fx-border-radius: 4;");
        VBox.setVgrow(processOutputArea, Priority.ALWAYS);
        procOut.getChildren().addAll(
            sectionHeader("PROCESS OUTPUT  (what programs print)"),
            processOutputArea
        );

        VBox sysLog = new VBox(6);
        VBox.setVgrow(sysLog, Priority.ALWAYS);
        systemLogArea = new TextArea();
        systemLogArea.setEditable(false);
        systemLogArea.setWrapText(true);
        systemLogArea.setStyle("-fx-control-inner-background: #fafafa; " +
                               "-fx-text-fill: #636e72; " +
                               "-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                               "-fx-border-color: " + BORDER + "; " +
                               "-fx-border-radius: 4;");
        VBox.setVgrow(systemLogArea, Priority.ALWAYS);
        sysLog.getChildren().addAll(
            sectionHeader("SYSTEM LOG  (OS / Scheduler events)"),
            systemLogArea
        );

        panel.getChildren().addAll(cpuBox, badges, procOut, sysLog);
        return panel;
    }

    private HBox buildBadgesRow() {
        HBox box = new HBox(10);
        box.setStyle("-fx-background-color: " + PANEL_BG + "; " +
                     "-fx-border-color: " + BORDER + "; -fx-border-width: 1; " +
                     "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12;");
        box.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("PROCESS STATES:");
        lbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                     "-fx-text-fill: " + TEXT_DIM + ";");

        p1Badge = makeBadge("P1", TEXT_DIM, "NOT ARRIVED");
        p2Badge = makeBadge("P2", TEXT_DIM, "NOT ARRIVED");
        p3Badge = makeBadge("P3", TEXT_DIM, "NOT ARRIVED");

        box.getChildren().addAll(lbl, p1Badge, p2Badge, p3Badge);
        return box;
    }

    @SuppressWarnings("unchecked")
    private VBox buildRightPanel() {
        VBox panel = new VBox(10);
        panel.setPrefWidth(230);
        panel.setPadding(new Insets(12, 0, 0, 0));

        if (os.scheduler.algorithm.equals(Scheduler.MLFQ)) {
            mlfqQueueLists = new ListView[4];
            for (int i = 0; i < 4; i++) {
                int quantum = (int) Math.pow(2, i);
                Label qLabel = sectionHeader("QUEUE " + i + "  (quantum=" + quantum + ")");
                mlfqQueueLists[i] = new ListView<>();
                mlfqQueueLists[i].setPrefHeight(95);
                styleQueue(mlfqQueueLists[i], ACCENT_PURPLE, "#f5f0ff");
                panel.getChildren().addAll(qLabel, mlfqQueueLists[i]);
            }
        } else {
            readyQueueList = new ListView<>();
            readyQueueList.setPrefHeight(170);
            styleQueue(readyQueueList, ACCENT_GREEN, "#f0faf7");
            panel.getChildren().addAll(
                sectionHeader("READY QUEUE  (" + os.scheduler.algorithm + ")"),
                readyQueueList
            );
        }

        blockedQueueList = new ListView<>();
        blockedQueueList.setPrefHeight(150);
        styleQueue(blockedQueueList, ACCENT_RED, "#fff5f5");

        diskList = new ListView<>();
        diskList.setPrefHeight(110);
        styleQueue(diskList, ACCENT_GOLD, "#fff8f5");

        panel.getChildren().addAll(
            sectionHeader("BLOCKED QUEUE"),      blockedQueueList,
            sectionHeader("DISK (swapped out)"), diskList
        );
        return panel;
    }

    private void onStep() {
        systemLogArea.appendText("\n=== CLOCK " + os.scheduler.getClock() + " ===\n");
        boolean cont = os.step();
        updateGUI();
        if (!cont) onFinished();
    }

    private void onReset() {
    	os = new OS();
    	os.setSchedulerType(schedulerSelector.getValue());
    	root.setRight(buildRightPanel()); 
        os.onBeforeExecute = () -> {
            runningProcessLabel.setText("Process " + os.lastRunningPID);
            runningProcessLabel.setStyle(
                "-fx-font-family: 'Courier New'; -fx-font-size: 16px; " +
                "-fx-font-weight: bold; -fx-text-fill: " +
                getProcessColor(os.lastRunningPID) + ";");
            currentInstructionLabel.setText(os.lastExecutedInstruction);
        };
        stepButton.setDisable(false);
        stepButton.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                            "-fx-font-weight: bold; -fx-background-color: transparent; " +
                            "-fx-text-fill: " + ACCENT_BLUE + "; -fx-border-color: " + ACCENT_BLUE + "; " +
                            "-fx-border-width: 1; -fx-border-radius: 4; " +
                            "-fx-background-radius: 4; -fx-padding: 6 14; -fx-cursor: hand;");
        processOutputArea.clear();
        systemLogArea.clear();
        resetBadge(p1Badge, "P1");
        resetBadge(p2Badge, "P2");
        resetBadge(p3Badge, "P3");
        updateGUI();
    }

    // add this helper method
    private void resetBadge(Label badge, String pid) {
        badge.setText(pid + "   NOT ARRIVED");
        badge.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                       "-fx-font-weight: bold; -fx-text-fill: " + TEXT_DIM + "; " +
                       "-fx-border-color: " + TEXT_DIM + "; -fx-border-width: 1; " +
                       "-fx-border-radius: 4; -fx-padding: 4 10;");
    }

    private void onFinished() {
        stepButton.setDisable(true);
        stepButton.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                            "-fx-background-color: " + TEXT_DIM + "; " +
                            "-fx-text-fill: white; -fx-border-radius: 4; " +
                            "-fx-background-radius: 4; -fx-padding: 6 14;");
        runningProcessLabel.setText("✓  All processes finished");
        runningProcessLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; " +
                                     "-fx-font-weight: bold; -fx-text-fill: " + ACCENT_GREEN + ";");
        currentInstructionLabel.setText("—");
        systemLogArea.appendText("\n✓ Simulation complete.\n");
    }

    private void updateGUI() {
        // Clock
        clockLabel.setText("CLOCK  " + os.displayClock);

        // CPU — show process and instruction executed this cycle
        if (os.lastRunningPID != -1) {
            runningProcessLabel.setText("Process " + os.lastRunningPID);
            runningProcessLabel.setStyle(
                "-fx-font-family: 'Courier New'; -fx-font-size: 16px; " +
                "-fx-font-weight: bold; -fx-text-fill: " +
                getProcessColor(os.lastRunningPID) + ";");
            currentInstructionLabel.setText(os.lastExecutedInstruction);
        } else {
            runningProcessLabel.setText("Idle — no instruction yet");
            runningProcessLabel.setStyle(
                "-fx-font-family: 'Courier New'; -fx-font-size: 16px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + TEXT_DIM + ";");
            currentInstructionLabel.setText("—");
        }

        // Memory
        memoryList.getItems().clear();
        for (int i = 0; i < 40; i++) {
            MemoryWord w = os.memory.getSlot(i);
            memoryList.getItems().add(
                String.format("[%02d]  %-18s  %s", i, w.key, w.value));
        }
        if (os.scheduler.algorithm.equals(Scheduler.MLFQ)) {
            for (int i = 0; i < 4; i++) {
                mlfqQueueLists[i].getItems().clear();
                for (Process p : os.scheduler.mlfqQueues[i]) {
                    String label = "Process " + p.processID + "   PC=" + p.programCounter;
                    if (p.isOnDisk) label += "   (on disk)";
                    mlfqQueueLists[i].getItems().add(label);
                }
            }
        } else {
            readyQueueList.getItems().clear();
            for (Process p : os.scheduler.readyQueue) {
                String label = "Process " + p.processID + "   PC=" + p.programCounter;
                if (p.isOnDisk) label += "   (on disk)";
                readyQueueList.getItems().add(label);
            }
        }
        blockedQueueList.getItems().clear();
        for (Process p : os.scheduler.blockedQueue) {
            String label = "Process " + p.processID + "   PC=" + p.programCounter;
            if (p.isOnDisk) label += "   (on disk)";
            blockedQueueList.getItems().add(label);
        }
        diskList.getItems().clear();
        for (Process p : os.allProcesses) {
            if (p.isOnDisk) {
                diskList.getItems().add(
                    "Process " + p.processID +
                    "  PC=" + p.programCounter + " swapped out");
            }
        }
        for (Process p : os.allProcesses) {
            Label badge = (p.processID == 1) ? p1Badge
                        : (p.processID == 2) ? p2Badge : p3Badge;
            String color;
            String state;
            if (p.processID == os.lastRunningPID && !p.state.equals(Process.FINISHED)) {
                color = ACCENT_BLUE;
                state = "RUNNING";
            } else {
                state = p.state.toUpperCase();  
                switch (p.state) {
                    case Process.READY:    color = ACCENT_GREEN;  break;
                    case Process.RUNNING:  color = ACCENT_BLUE;   break;
                    case Process.BLOCKED:  color = ACCENT_RED;    break;
                    case Process.FINISHED: color = TEXT_DIM;      break;
                    default:               color = TEXT_DIM;      break;
                }
            }
            badge.setText("P" + p.processID + "   " + state);
            badge.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                           "-fx-font-weight: bold; -fx-text-fill: " + color + "; " +
                           "-fx-border-color: " + color + "; -fx-border-width: 1; " +
                           "-fx-border-radius: 4; -fx-padding: 4 10;");
        }
    }
    private final StringBuilder lineBuffer = new StringBuilder();
    
    private void redirectStdOut() {
        PrintStream interceptor = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                char c = (char) b;
                lineBuffer.append(c);
                if (c == '\n') {
                    String line = lineBuffer.toString();
                    lineBuffer.setLength(0);
                    Platform.runLater(() -> routeLine(line));
                }
            }
        }, true);
        System.setOut(interceptor);
    }
    private void routeLine(String line) {
        String t = line.trim();
        if (t.isEmpty()) return;
        boolean isSystemLine =
            t.startsWith("[")            ||
            t.startsWith("===")          ||
            t.startsWith("┌")            ||
            t.startsWith("└")            ||
            t.startsWith("  Clock")      ||
            t.startsWith("  Running")    ||
            t.startsWith("  Ready")      ||
            t.startsWith("  Blocked")    ||
            t.startsWith("  Queue")      ||
            t.startsWith("Free slots")   ||
            t.startsWith("Please enter") ||
            t.startsWith("========");

        if (isSystemLine) {
            systemLogArea.appendText(line);
            systemLogArea.positionCaret(systemLogArea.getLength());
        } else {
            processOutputArea.appendText(line);
            processOutputArea.positionCaret(processOutputArea.getLength());
        }
    }

    private Process findProcess(int pid) {
        for (Process p : os.allProcesses) {
            if (p.processID == pid) return p;
        }
        return null;
    }
    private String getStateColor(Process p) {
        if (p.isOnDisk) return ACCENT_GOLD;
        switch (p.state) {
            case Process.READY:    return ACCENT_GREEN;
            case Process.RUNNING:  return ACCENT_BLUE;
            case Process.BLOCKED:  return ACCENT_RED;
            case Process.FINISHED: return TEXT_DIM;
            default:               return TEXT_DIM;
        }
    }
    private String getProcessColor(int pid) {
        switch (pid) {
            case 1: return P1_COLOR;
            case 2: return P2_COLOR;
            case 3: return P3_COLOR;
            default: return TEXT_MAIN;
        }
    }
    private Label sectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                   "-fx-font-weight: bold; -fx-text-fill: " + TEXT_DIM + "; " +
                   "-fx-padding: 4 0 2 0;");
        return l;
    }
    private Button makeButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                   "-fx-font-weight: bold; -fx-background-color: transparent; " +
                   "-fx-text-fill: " + color + "; -fx-border-color: " + color + "; " +
                   "-fx-border-width: 1; -fx-border-radius: 4; " +
                   "-fx-background-radius: 4; -fx-padding: 6 14; -fx-cursor: hand;");
        return b;
    }
    private Label makeBadge(String pid, String color, String state) {
        Label l = new Label(pid + "   " + state);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                   "-fx-font-weight: bold; -fx-text-fill: " + color + "; " +
                   "-fx-border-color: " + color + "; -fx-border-width: 1; " +
                   "-fx-border-radius: 4; -fx-padding: 4 10;");
        return l;
    }
    private void styleQueue(ListView<String> lv, String color, String bg) {
        lv.setStyle("-fx-background-color: " + bg + "; " +
                    "-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                    "-fx-border-color: " + color + "; -fx-border-width: 1; " +
                    "-fx-border-radius: 4;");
        lv.setCellFactory(v -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-background-color: " + bg + "; " +
                         "-fx-text-fill: " + color + "; -fx-padding: 4 8;");
            }
        });
    }
    private ComboBox<String> buildSchedulerSelector() {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll("RR", "HRRN", "MLFQ");
        cb.setValue("RR");
        cb.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(
                        "-fx-background-color: #f5f0ff; " +
                        "-fx-text-fill: " + ACCENT_PURPLE + ";"
                    );
                }
            }
        });
        cb.setStyle(
            "-fx-font-family: 'Courier New'; " +
            "-fx-font-size: 12px; " +
            "-fx-background-color: #f5f0ff; " +
            "-fx-border-color: " + ACCENT_PURPLE + "; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: " + ACCENT_PURPLE + "; " +
            "-fx-padding: 4 10;"
        );

        return cb;
    }
  
    public static void main(String[] args) { launch(args); }
}