package dev.lupino1.command;

import dev.lupino1.messages.ColorParser;
import dev.lupino1.messages.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CommandContext {

    private final JavaPlugin plugin;
    private final CommandSender sender;
    private final String label;
    private final String[] args;
    private final String[] rawArgs;
    private final MessageManager messages;

    CommandContext(
            JavaPlugin plugin,
            CommandSender sender,
            String label,
            String[] args,
            String[] rawArgs,
            MessageManager messages
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.sender = Objects.requireNonNull(sender, "sender");
        this.label = label == null ? "" : label;
        this.args = args == null ? new String[0] : args;
        this.rawArgs = rawArgs == null ? new String[0] : rawArgs;
        this.messages = messages;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public CommandSender sender() {
        return sender;
    }

    public Player player() {
        if (!(sender instanceof Player player)) {
            throw new IllegalStateException("Sender is not a player");
        }
        return player;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public boolean isConsole() {
        return sender instanceof ConsoleCommandSender;
    }

    public String label() {
        return label;
    }

    /** Args remaining at the matched node. */
    public String[] args() {
        return args.clone();
    }

    public int argsLength() {
        return args.length;
    }

    public String arg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }

    public String arg(int index, String def) {
        String value = arg(index);
        return value == null ? def : value;
    }

    public Integer argInt(int index) {
        String raw = arg(index);
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int argInt(int index, int def) {
        Integer value = argInt(index);
        return value == null ? def : value;
    }

    /** Full args as typed after the root label. */
    public String[] rawArgs() {
        return rawArgs.clone();
    }

    public MessageManager messages() {
        return messages;
    }

    public void reply(Component message) {
        if (message != null) {
            sender.sendMessage(message);
        }
    }

    public void reply(String miniMessage) {
        if (miniMessage != null) {
            sender.sendMessage(ColorParser.translateColors(miniMessage));
        }
    }

    public void reply(List<String> miniMessages) {
        if (miniMessages == null) {
            return;
        }
        for (String line : miniMessages) {
            reply(line);
        }
    }

    public void send(String messageKey) {
        send(messageKey, null, true);
    }

    public void send(String messageKey, Map<String, ?> placeholders) {
        send(messageKey, placeholders, true);
    }

    public void send(String messageKey, Map<String, ?> placeholders, boolean prefix) {
        if (messages == null) {
            reply("<red>Missing MessageManager (key: " + messageKey + ")");
            return;
        }
        messages.send(sender, messageKey, placeholders, prefix);
    }

    public void sendList(String messageKey) {
        sendList(messageKey, null);
    }

    public void sendList(String messageKey, Map<String, ?> placeholders) {
        if (messages == null) {
            reply("<red>Missing MessageManager (key: " + messageKey + ")");
            return;
        }
        messages.sendList(sender, messageKey, placeholders);
    }
}
