package dev.lupino1.command;

import java.util.Objects;

/**
 * Message keys for command deny/usage feedback.
 * <p>
 * Built-in defaults: {@code command.no-permission|usage|player-only|console-only}.
 * Set plugin-wide once via {@link #setDefaults(CommandMessageKeys)}; override per root with
 * {@link LPCommand#commandMessageKeys(CommandMessageKeys)} or per-node 2-arg methods.
 */
public final class CommandMessageKeys {

    public static final CommandMessageKeys DEFAULT = new CommandMessageKeys(
            "command.no-permission",
            "command.usage",
            "command.player-only",
            "command.console-only"
    );

    private static volatile CommandMessageKeys globalDefaults = DEFAULT;

    private final String noPermission;
    private final String usage;
    private final String playerOnly;
    private final String consoleOnly;

    public CommandMessageKeys(
            String noPermission,
            String usage,
            String playerOnly,
            String consoleOnly
    ) {
        this.noPermission = requireKey(noPermission, "command.no-permission");
        this.usage = requireKey(usage, "command.usage");
        this.playerOnly = requireKey(playerOnly, "command.player-only");
        this.consoleOnly = requireKey(consoleOnly, "command.console-only");
    }

    /** Current plugin-wide defaults (used by new {@link LPCommand} roots). */
    public static CommandMessageKeys defaults() {
        return globalDefaults;
    }

    /** Set once in {@code onEnable}; all later {@code LPCommand.create} pick this up. */
    public static void setDefaults(CommandMessageKeys keys) {
        globalDefaults = keys == null ? DEFAULT : keys;
    }

    /** Restore built-in {@link #DEFAULT}. */
    public static void resetDefaults() {
        globalDefaults = DEFAULT;
    }

    /** {@code prefix + ".no-permission"} etc. */
    public static CommandMessageKeys prefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        String base = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
        return new CommandMessageKeys(
                base + ".no-permission",
                base + ".usage",
                base + ".player-only",
                base + ".console-only"
        );
    }

    public String noPermission() {
        return noPermission;
    }

    public String usage() {
        return usage;
    }

    public String playerOnly() {
        return playerOnly;
    }

    public String consoleOnly() {
        return consoleOnly;
    }

    public CommandMessageKeys noPermission(String key) {
        return new CommandMessageKeys(key, usage, playerOnly, consoleOnly);
    }

    public CommandMessageKeys usage(String key) {
        return new CommandMessageKeys(noPermission, key, playerOnly, consoleOnly);
    }

    public CommandMessageKeys playerOnly(String key) {
        return new CommandMessageKeys(noPermission, usage, key, consoleOnly);
    }

    public CommandMessageKeys consoleOnly(String key) {
        return new CommandMessageKeys(noPermission, usage, playerOnly, key);
    }

    private static String requireKey(String key, String fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        return key;
    }
}
