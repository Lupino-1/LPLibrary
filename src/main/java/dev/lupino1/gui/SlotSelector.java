package dev.lupino1.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses slot tokens: single {@code 12}, range {@code 0-8} (inclusive),
 * or comma lists {@code 0-8,12,14} / {@code [0-8,12]}.
 */
public final class SlotSelector {

    private SlotSelector() {
    }

    /**
     * Each entry is a slot, range, or comma-list (same rules as {@link #parse(String)}).
     */
    public static List<Integer> parse(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        Set<Integer> slots = new LinkedHashSet<>();
        for (String token : tokens) {
            slots.addAll(parse(token));
        }
        return List.copyOf(slots);
    }

    public static List<Integer> parse(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        String raw = input.trim();
        if (raw.startsWith("[") && raw.endsWith("]")) {
            raw = raw.substring(1, raw.length() - 1).trim();
        }
        if (raw.isEmpty()) {
            return List.of();
        }

        Set<Integer> slots = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            addToken(slots, part.trim());
        }
        return List.copyOf(slots);
    }

    private static void addToken(Set<Integer> slots, String token) {
        if (token.isEmpty()) {
            return;
        }
        int dash = token.indexOf('-');
        if (dash > 0 && dash < token.length() - 1
                && isDigits(token.substring(0, dash).trim())
                && isDigits(token.substring(dash + 1).trim())) {
            int from = Integer.parseInt(token.substring(0, dash).trim());
            int to = Integer.parseInt(token.substring(dash + 1).trim());
            if (from > to) {
                int tmp = from;
                from = to;
                to = tmp;
            }
            for (int i = from; i <= to; i++) {
                slots.add(i);
            }
            return;
        }
        slots.add(parseInt(token, token));
    }

    private static boolean isDigits(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static int parseInt(String value, String token) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid slot token: '" + token + "'");
        }
    }
}
