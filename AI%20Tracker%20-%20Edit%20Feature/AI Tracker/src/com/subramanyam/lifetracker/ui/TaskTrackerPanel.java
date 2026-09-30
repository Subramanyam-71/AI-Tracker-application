package com.subramanyam.lifetracker.ui;

import com.subramanyam.lifetracker.model.DailyProgress;
import com.subramanyam.lifetracker.model.Task;
import com.subramanyam.lifetracker.model.TaskStatus;
import com.subramanyam.lifetracker.service.AIProgressService;
import com.subramanyam.lifetracker.service.MotivationService;
import com.subramanyam.lifetracker.service.ProgressService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Stage 3: Daily task tracking.
 *
 * Shows today's 6 tracked items (Gym, AI Project, DSA, Client Editing,
 * GT Jersey, RGB Lights) each as a row with a status button. Clicking
 * a status button cycles it: Not Started -> In Progress -> Completed.
 * There's also a small note box below for a line like "Gym completed".
 *
 * Statuses, custom tasks, and notes are saved automatically after each change.
 */
public class TaskTrackerPanel extends JPanel {

    private static final Color BG_COLOR = new Color(247, 248, 250);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color TEXT_MUTED = new Color(120, 124, 134);

    private static final Color NOT_STARTED_COLOR = new Color(235, 236, 240);
    private static final Color IN_PROGRESS_COLOR = new Color(255, 196, 87);
    private static final Color COMPLETED_COLOR = new Color(64, 191, 118);
    private static final Color INSIGHT_BG_COLOR = new Color(238, 240, 255);
    private static final Color INSIGHT_BORDER_COLOR = new Color(210, 214, 250);

    private final ProgressService progressService;
    private final MotivationService motivationService = new MotivationService();
    private final AIProgressService aiProgressService = new AIProgressService();
    private final JTextField noteField = new JTextField();
    private final JTextField newTaskField = new JTextField();
    private final JPanel taskListPanel = new JPanel();
    private final JTextArea insightArea = new JTextArea();
    private DailyProgress todayProgress;

    public TaskTrackerPanel(ProgressService progressService) {
        this.progressService = progressService;

        setLayout(new BorderLayout(0, 16));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        LocalDate today = LocalDate.now();
        todayProgress = loadTodayProgress(today);

        add(buildHeading(today), BorderLayout.NORTH);
        add(buildTaskListScrollPane(todayProgress), BorderLayout.CENTER);
        add(buildBottomSection(todayProgress), BorderLayout.SOUTH);
    }

    private DailyProgress loadTodayProgress(LocalDate today) {
        return progressService.getProgressFor(today);
    }

    private JPanel buildHeading(LocalDate today) {
        JPanel headingPanel = new JPanel();
        headingPanel.setLayout(new BoxLayout(headingPanel, BoxLayout.Y_AXIS));
        headingPanel.setBackground(BG_COLOR);

        String formatted = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"));
        JLabel heading = new JLabel("Today's Goals — " + formatted);
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));

        JLabel quote = new JLabel("“" + motivationService.getDailyQuote(today) + "”");
        quote.setFont(new Font("SansSerif", Font.ITALIC, 13));
        quote.setForeground(TEXT_MUTED);

        headingPanel.add(heading);
        headingPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headingPanel.add(quote);
        return headingPanel;
    }

    /** Wraps the task list in a scroll pane so adding many tasks doesn't blow up the window. */
    private JScrollPane buildTaskListScrollPane(DailyProgress todayProgress) {
        taskListPanel.setLayout(new BoxLayout(taskListPanel, BoxLayout.Y_AXIS));
        taskListPanel.setBackground(BG_COLOR);

        for (Task task : todayProgress.getTasks()) {
            addTaskRowToList(task);
        }

        JScrollPane scrollPane = new JScrollPane(taskListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    /** Adds one task's row + spacing to the on-screen list (used for both default and newly-added tasks). */
    private void addTaskRowToList(Task task) {
        taskListPanel.add(buildTaskRow(task));
        taskListPanel.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    /** Clears and re-adds every row — the simplest safe way to reflect a task being removed. */
    private void rebuildTaskList() {
        taskListPanel.removeAll();
        for (Task task : todayProgress.getTasks()) {
            addTaskRowToList(task);
        }
        taskListPanel.revalidate();
        taskListPanel.repaint();
    }

    private JPanel buildTaskRow(Task task) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(CARD_COLOR);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        JLabel nameLabel = new JLabel(task.getName());
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JButton statusButton = new JButton();
        statusButton.setFocusPainted(false);
        statusButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        statusButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusButton.setPreferredSize(new Dimension(120, 30));
        applyStatusStyle(statusButton, task.getStatus(), false);

        // Each click cycles the task's status and updates the button to match.
        statusButton.addActionListener(e -> {
            task.setStatus(task.getStatus().next());
            applyStatusStyle(statusButton, task.getStatus(), true);
            if (task.getStatus() == TaskStatus.COMPLETED) {
                AnimationUtils.pulse(statusButton);
            }
            progressService.save();
            refreshInsight();
        });

        // Edit button — lets you correct spelling or rename a task without changing its status.
        JButton editButton = new JButton("Edit");
        editButton.setFocusPainted(false);
        editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        editButton.setToolTipText("Edit this task name");
        editButton.addActionListener(e -> {
            String editedName = JOptionPane.showInputDialog(
                    this,
                    "Edit task name:",
                    task.getName()
            );

            if (editedName != null) {
                editedName = editedName.trim();
                if (!editedName.isEmpty() && !editedName.equals(task.getName())) {
                    task.setName(editedName);
                    rebuildTaskList();
                    progressService.save();
                    refreshInsight();
                }
            }
        });

        // A small "remove" button — mainly there to undo a task added by mistake.
        JButton removeButton = new JButton("\u2715"); // ✕
        removeButton.setFocusPainted(false);
        removeButton.setBorderPainted(false);
        removeButton.setContentAreaFilled(false);
        removeButton.setForeground(new Color(190, 60, 60));
        removeButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeButton.setToolTipText("Remove this task");
        removeButton.addActionListener(e -> {
            todayProgress.getTasks().remove(task);
            rebuildTaskList();
            progressService.save();
            refreshInsight();
        });

        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightSide.setBackground(CARD_COLOR);
        rightSide.add(statusButton);
        rightSide.add(editButton);
        rightSide.add(removeButton);

        row.add(nameLabel, BorderLayout.WEST);
        row.add(rightSide, BorderLayout.EAST);
        return row;
    }

    /** Updates a status button's text and color to match the task's current status. */
    private void applyStatusStyle(JButton button, TaskStatus status, boolean animate) {
        button.setText(status.getLabel());
        Color targetBackground;
        Color foreground;
        switch (status) {
            case COMPLETED -> {
                targetBackground = COMPLETED_COLOR;
                foreground = Color.WHITE;
            }
            case IN_PROGRESS -> {
                targetBackground = IN_PROGRESS_COLOR;
                foreground = Color.WHITE;
            }
            default -> {
                targetBackground = NOT_STARTED_COLOR;
                foreground = Color.DARK_GRAY;
            }
        }
        button.setForeground(foreground);
        if (animate) {
            AnimationUtils.animateBackground(button, button.getBackground(), targetBackground);
        } else {
            button.setBackground(targetBackground);
        }
    }

    private JPanel buildBottomSection(DailyProgress todayProgress) {
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(BG_COLOR);

        bottom.add(buildInsightSection());
        bottom.add(Box.createRigidArea(new Dimension(0, 12)));
        bottom.add(buildAddTaskRow(todayProgress));
        bottom.add(Box.createRigidArea(new Dimension(0, 12)));
        bottom.add(buildNoteSection(todayProgress));
        return bottom;
    }

    /** Stage 7: a small card showing the AI-style analysis of today's progress. */
    private JPanel buildInsightSection() {
        JPanel section = new JPanel(new BorderLayout(10, 0));
        section.setBackground(INSIGHT_BG_COLOR);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INSIGHT_BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel icon = new JLabel("\uD83E\uDD16"); // robot emoji
        icon.setFont(new Font("SansSerif", Font.PLAIN, 20));
        icon.setVerticalAlignment(SwingConstants.TOP);

        insightArea.setEditable(false);
        insightArea.setLineWrap(true);
        insightArea.setWrapStyleWord(true);
        insightArea.setOpaque(false);
        insightArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        refreshInsight();

        section.add(icon, BorderLayout.WEST);
        section.add(insightArea, BorderLayout.CENTER);
        return section;
    }

    /** Recomputes the AI-style message from today's current task statuses. */
    private void refreshInsight() {
        insightArea.setText(aiProgressService.generateAnalysis(todayProgress));
    }

    /** A text field + button for adding a brand-new task to today's list. */
    private JPanel buildAddTaskRow(DailyProgress todayProgress) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(BG_COLOR);

        newTaskField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        newTaskField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JButton addButton = new JButton("+ Add Task");
        addButton.setFocusPainted(false);
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addButton.setFont(new Font("SansSerif", Font.BOLD, 12));

        Runnable addNewTask = () -> {
            String name = newTaskField.getText().trim();
            if (name.isEmpty()) {
                return;
            }
            Task newTask = new Task(name);
            todayProgress.getTasks().add(newTask);
            rebuildTaskList();
            newTaskField.setText("");
            progressService.save();
            refreshInsight();
        };

        addButton.addActionListener(e -> addNewTask.run());
        // Also let pressing Enter in the field add the task, not just clicking the button.
        newTaskField.addActionListener(e -> addNewTask.run());

        row.add(newTaskField, BorderLayout.CENTER);
        row.add(addButton, BorderLayout.EAST);
        return row;
    }

    private JPanel buildNoteSection(DailyProgress todayProgress) {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBackground(BG_COLOR);
        section.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JLabel label = new JLabel("Today's Note");
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(TEXT_MUTED);

        noteField.setText(todayProgress.getNote());
        noteField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        noteField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        // Save the note into the model as soon as the field loses focus —
        // simple enough for now; a "Save" button isn't really needed here.
        noteField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                todayProgress.setNote(noteField.getText());
                progressService.save();
            }
        });

        section.add(label, BorderLayout.NORTH);
        section.add(noteField, BorderLayout.CENTER);
        return section;
    }
}