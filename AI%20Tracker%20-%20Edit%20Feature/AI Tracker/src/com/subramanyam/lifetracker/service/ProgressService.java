package com.subramanyam.lifetracker.service;

import com.subramanyam.lifetracker.model.DailyProgress;
import com.subramanyam.lifetracker.model.Task;
import com.subramanyam.lifetracker.model.TaskStatus;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of DailyProgress for every date the user has touched.
 *
 * Stage 3 note: this is just an in-memory HashMap, so all data is lost
 * when the app closes. Stage 5 will swap this out to load from / save
 * to a file, without needing to change any of the UI code that uses it.
 */
public class ProgressService {

    private static final Path SAVE_FILE = Path.of(
            System.getProperty("user.home"), ".life-progress-tracker", "progress-data.ser");

    // The 6 fixed items from the app's task list (Personal Growth + Money/Goals).
    private static final String[] DEFAULT_TASK_NAMES = {
            "Gym", "AI Platform / Project", "DSA",
            "Client Editing", "GT Jersey", "RGB Lights"
    };

    private final Map<LocalDate, DailyProgress> progressByDate = new HashMap<>();

    public ProgressService() {
        load();
    }

    /** Returns the DailyProgress for a date, creating a fresh one (all tasks Not Started) if needed. */
    public DailyProgress getProgressFor(LocalDate date) {
        return progressByDate.computeIfAbsent(date, this::createFreshDay);
    }

    /** Returns a day only when the user has already opened or changed it. */
    public DailyProgress getExistingProgress(LocalDate date) {
        return progressByDate.get(date);
    }

    /** Saves every tracked date, task status, custom task, and note to disk. */
    public void save() {
        try {
            Files.createDirectories(SAVE_FILE.getParent());
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(SAVE_FILE))) {
                output.writeObject(progressByDate);
            }
        } catch (IOException exception) {
            System.err.println("Could not save Life Progress data: " + exception.getMessage());
        }
    }

    /** Loads previously saved progress. A missing or invalid save starts a fresh tracker safely. */
    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(SAVE_FILE)) {
            return;
        }

        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(SAVE_FILE))) {
            Object savedData = input.readObject();
            if (savedData instanceof Map<?, ?>) {
                progressByDate.putAll((Map<LocalDate, DailyProgress>) savedData);
            }
        } catch (IOException | ClassNotFoundException | ClassCastException exception) {
            System.err.println("Could not load Life Progress data: " + exception.getMessage());
        }
    }

    /** Counts tasks of one status for days the user has tracked in a month. */
    public int getMonthlyTaskCount(YearMonth month, TaskStatus status) {
        return getMonthlyTasks(month).stream()
                .mapToInt(task -> task.getStatus() == status ? 1 : 0)
                .sum();
    }

    public int getMonthlyTaskTotal(YearMonth month) {
        return getMonthlyTasks(month).size();
    }

    public int getMonthlyTaskCountForGoal(YearMonth month, String goalName, TaskStatus status) {
        return (int) getMonthlyTasks(month).stream()
                .filter(task -> task.getName().equals(goalName) && task.getStatus() == status)
                .count();
    }

    public int getMonthlyTaskTotalForGoal(YearMonth month, String goalName) {
        return (int) getMonthlyTasks(month).stream()
                .filter(task -> task.getName().equals(goalName))
                .count();
    }

    /** Only names that actually have at least one real tracked task this month — no forced empty entries. */
    public Set<String> getMonthlyGoalNames(YearMonth month) {
        Set<String> names = new LinkedHashSet<>();
        getMonthlyTasks(month).forEach(task -> names.add(task.getName()));
        return names;
    }

    private List<Task> getMonthlyTasks(YearMonth month) {
        return progressByDate.entrySet().stream()
                .filter(entry -> YearMonth.from(entry.getKey()).equals(month))
                .flatMap(entry -> entry.getValue().getTasks().stream())
                .toList();
    }

    private DailyProgress createFreshDay(LocalDate date) {
        List<Task> tasks = new ArrayList<>();
        for (String name : DEFAULT_TASK_NAMES) {
            tasks.add(new Task(name));
        }
        return new DailyProgress(date, tasks);
    }
}