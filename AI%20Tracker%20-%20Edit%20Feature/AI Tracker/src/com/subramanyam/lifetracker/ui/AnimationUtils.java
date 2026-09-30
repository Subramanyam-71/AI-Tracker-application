package com.subramanyam.lifetracker.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Stage 8: small, shared helpers for the app's few subtle animations.
 *
 * Kept deliberately simple — just two Swing Timers that step a value
 * gradually instead of jumping instantly. No animation library needed.
 */
public final class AnimationUtils {

    private AnimationUtils() {
        // utility class — never instantiated
    }

    /** Smoothly moves a progress bar's value to the target instead of jumping straight to it. */
    public static void animateProgressBar(JProgressBar bar, int targetValue) {
        int start = bar.getValue();
        if (start == targetValue) {
            return;
        }
        int direction = targetValue > start ? 1 : -1;
        Timer timer = new Timer(10, null);
        timer.addActionListener(e -> {
            int next = bar.getValue() + direction;
            bar.setValue(next);
            if (next == targetValue) {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }

    /** Smoothly fades a component's background from one color to another over ~150ms. */
    public static void animateBackground(JComponent component, Color from, Color to) {
        int totalSteps = 12;
        int[] currentStep = {0};
        Timer timer = new Timer(12, null);
        timer.addActionListener(e -> {
            currentStep[0]++;
            float ratio = (float) currentStep[0] / totalSteps;
            component.setBackground(blend(from, to, ratio));
            if (currentStep[0] >= totalSteps) {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }

    /** A quick, subtle "pop" — grows a button's text briefly then settles back. Used for completing a task. */
    public static void pulse(AbstractButton button) {
        Font baseFont = button.getFont();
        float baseSize = baseFont.getSize2D();
        int totalSteps = 10;
        int[] currentStep = {0};
        Timer timer = new Timer(15, null);
        timer.addActionListener(e -> {
            currentStep[0]++;
            float progress = currentStep[0] / (float) totalSteps;
            // Peaks at 1.25x size halfway through, returns to 1x by the end.
            float scale = 1f + 0.25f * (float) Math.sin(Math.PI * progress);
            button.setFont(baseFont.deriveFont(baseSize * scale));
            if (currentStep[0] >= totalSteps) {
                button.setFont(baseFont);
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }

    private static Color blend(Color from, Color to, float ratio) {
        int red = interpolate(from.getRed(), to.getRed(), ratio);
        int green = interpolate(from.getGreen(), to.getGreen(), ratio);
        int blue = interpolate(from.getBlue(), to.getBlue(), ratio);
        return new Color(red, green, blue);
    }

    private static int interpolate(int start, int end, float ratio) {
        int value = Math.round(start + (end - start) * ratio);
        return Math.max(0, Math.min(255, value));
    }
}