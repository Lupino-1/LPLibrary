package dev.lupino1.command;

import dev.lupino1.messages.ColorParser;
import dev.lupino1.messages.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fluent command tree. Register at runtime (no {@code plugin.yml}).
 * Prefer {@link LPSubCommand} classes for non-trivial trees; lambdas OK for tiny hubs.
 *
 * <pre>{@code
 * LPCommand.create(plugin, "shop")
 *     .permission("shop.use")
 *     .sub(new BuyCommand(), new SellCommand(), new AdminCommand())
 *     .execute(ctx -> ctx.reply("usage"))
 *     .register();
 * }</pre>
 */
public final class LPCommand {

    private static final String FALLBACK_NO_PERMISSION = "<red>No permission.";
    private static final String FALLBACK_PLAYER_ONLY = "<red>Only players can use this command.";
    private static final String FALLBACK_CONSOLE_ONLY = "<red>Only the console can use this command.";

    private final JavaPlugin plugin;
    private final String name;
    private final boolean root;

    private String permission;
    private String noPermissionMessageKey;
    private SenderType senderType = SenderType.ANY;
    private String playerOnlyMessageKey;
    private String consoleOnlyMessageKey;
    private String description = "";
    private String usage = "";
    private String usageMessageKey;
    private List<String> aliases = List.of();
    private MessageManager messages;
    private CommandMessageKeys messageKeys = CommandMessageKeys.defaults();
    private CommandHandler handler;
    private TabHandler tabHandler;
    private final Map<String, LPCommand> children = new LinkedHashMap<>();

    private LPCommand(JavaPlugin plugin, String name, boolean root) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.name = Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT);
        this.root = root;
    }

    public static LPCommand create(JavaPlugin plugin, String name) {
        return new LPCommand(plugin, name, true);
    }

    public String name() {
        return name;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public LPCommand permission(String permission) {
        this.permission = permission;
        return this;
    }

    /** Optional {@link MessageManager} key for the no-permission message on this node. */
    public LPCommand permission(String permission, String noPermissionMessageKey) {
        this.permission = permission;
        this.noPermissionMessageKey = blankToNull(noPermissionMessageKey);
        return this;
    }

    public LPCommand playerOnly() {
        this.senderType = SenderType.PLAYER;
        return this;
    }

    /** Optional {@link MessageManager} key for the player-only message on this node. */
    public LPCommand playerOnly(String messageKey) {
        this.senderType = SenderType.PLAYER;
        this.playerOnlyMessageKey = blankToNull(messageKey);
        return this;
    }

    public LPCommand consoleOnly() {
        this.senderType = SenderType.CONSOLE;
        return this;
    }

    /** Optional {@link MessageManager} key for the console-only message on this node. */
    public LPCommand consoleOnly(String messageKey) {
        this.senderType = SenderType.CONSOLE;
        this.consoleOnlyMessageKey = blankToNull(messageKey);
        return this;
    }

    public LPCommand senderType(SenderType type) {
        this.senderType = type == null ? SenderType.ANY : type;
        return this;
    }

    public LPCommand description(String description) {
        this.description = description == null ? "" : description;
        return this;
    }

    public LPCommand usage(String usage) {
        this.usage = usage == null ? "" : usage;
        return this;
    }

    /** Optional {@link MessageManager} key for the usage message on this node. */
    public LPCommand usage(String usage, String usageMessageKey) {
        this.usage = usage == null ? "" : usage;
        this.usageMessageKey = blankToNull(usageMessageKey);
        return this;
    }

    /** Root only. */
    public LPCommand aliases(String... aliases) {
        requireRoot("aliases");
        if (aliases == null || aliases.length == 0) {
            this.aliases = List.of();
        } else {
            List<String> list = new ArrayList<>(aliases.length);
            for (String alias : aliases) {
                if (alias != null && !alias.isBlank()) {
                    list.add(alias.toLowerCase(Locale.ROOT));
                }
            }
            this.aliases = List.copyOf(list);
        }
        return this;
    }

    /** Root only — optional MessageManager for default deny/usage keys. */
    public LPCommand messages(MessageManager messages) {
        requireRoot("messages");
        this.messages = messages;
        propagateMessages(this, messages);
        return this;
    }

    /**
     * Root only — change default message keys for the whole tree.
     * Per-node overrides: {@link #permission(String, String)}, {@link #usage(String, String)}, etc.
     */
    public LPCommand commandMessageKeys(CommandMessageKeys keys) {
        requireRoot("commandMessageKeys");
        this.messageKeys = keys == null ? CommandMessageKeys.defaults() : keys;
        propagateMessageKeys(this, this.messageKeys);
        return this;
    }

    public LPCommand execute(CommandHandler handler) {
        this.handler = handler;
        return this;
    }

    public LPCommand tabComplete(TabHandler tabHandler) {
        this.tabHandler = tabHandler;
        return this;
    }

    public LPCommand sub(String name, Consumer<LPCommand> configure) {
        Objects.requireNonNull(configure, "configure");
        LPCommand child = new LPCommand(plugin, name, false);
        inheritTreeConfig(child);
        configure.accept(child);
        return sub(child);
    }

    public LPCommand sub(LPSubCommand subCommand) {
        Objects.requireNonNull(subCommand, "subCommand");
        LPCommand child = new LPCommand(plugin, subCommand.name(), false);
        inheritTreeConfig(child);
        subCommand.install(child);
        return sub(child);
    }

    public LPCommand sub(LPSubCommand... subCommands) {
        Objects.requireNonNull(subCommands, "subCommands");
        for (LPSubCommand subCommand : subCommands) {
            sub(subCommand);
        }
        return this;
    }

    public LPCommand sub(LPCommand child) {
        Objects.requireNonNull(child, "child");
        if (child.root) {
            throw new IllegalArgumentException("Cannot nest a root command");
        }
        children.put(child.name, child);
        return this;
    }

    public void register() {
        requireRoot("register");
        CommandRegistrar.register(this);
    }

    List<String> aliases() {
        return aliases;
    }

    String description() {
        return description;
    }

    String usage() {
        return usage;
    }

    String permission() {
        return permission;
    }

    MessageManager messages() {
        return messages;
    }

    boolean execute(CommandSender sender, String label, String[] rawArgs) {
        Match match = match(rawArgs);
        LPCommand node = match.node;
        String[] remaining = match.remaining;

        for (LPCommand step : match.path) {
            if (!step.checkSender(sender, messages)) {
                return true;
            }
            if (!step.checkPermission(sender, messages)) {
                return true;
            }
        }

        if (node.handler != null) {
            try {
                node.handler.execute(new CommandContext(plugin, sender, label, remaining, rawArgs, messages));
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "[Commands] /" + label + " failed", e);
                sender.sendMessage(ColorParser.translateColors("<red>Command error. Check console."));
            }
            return true;
        }

        if (!node.children.isEmpty() && remaining.length > 0) {
            sendUnknown(sender, remaining[0]);
            return true;
        }

        sendUsage(sender, node);
        return true;
    }

    List<String> tabComplete(CommandSender sender, String label, String[] rawArgs) {
        if (!canUse(sender)) {
            return List.of();
        }
        String[] args = rawArgs == null ? new String[0] : rawArgs;
        LPCommand node = this;
        int index = 0;

        while (index < args.length) {
            String token = args[index];
            boolean last = index == args.length - 1;
            LPCommand child = node.findChild(token);

            if (last) {
                List<String> fromSubs = node.suggestChildren(sender, token);
                if (node.tabHandler != null) {
                    String[] remaining = Arrays.copyOfRange(args, index, args.length);
                    CommandContext ctx = new CommandContext(plugin, sender, label, remaining, args, messages);
                    List<String> custom = node.tabHandler.complete(ctx);
                    if (custom != null && !custom.isEmpty()) {
                        String prefix = token == null ? "" : token.toLowerCase(Locale.ROOT);
                        List<String> filtered = new ArrayList<>();
                        for (String s : custom) {
                            if (s != null && (prefix.isEmpty() || s.toLowerCase(Locale.ROOT).startsWith(prefix))) {
                                filtered.add(s);
                            }
                        }
                        if (!fromSubs.isEmpty()) {
                            filtered.addAll(0, fromSubs);
                        }
                        return filtered;
                    }
                }
                return fromSubs;
            }

            if (child == null) {
                return List.of();
            }
            if (!child.canUse(sender)) {
                return List.of();
            }
            node = child;
            index++;
        }

        // No args yet — suggest subcommands / tab handler with empty remaining
        List<String> fromSubs = node.suggestChildren(sender, "");
        if (node.tabHandler != null) {
            CommandContext ctx = new CommandContext(plugin, sender, label, new String[0], args, messages);
            List<String> custom = node.tabHandler.complete(ctx);
            if (custom != null && !custom.isEmpty()) {
                List<String> out = new ArrayList<>(fromSubs);
                out.addAll(custom);
                return out;
            }
        }
        return fromSubs;
    }

    private Match match(String[] rawArgs) {
        String[] args = rawArgs == null ? new String[0] : rawArgs;
        List<LPCommand> path = new ArrayList<>();
        LPCommand node = this;
        path.add(node);
        int index = 0;
        while (index < args.length) {
            LPCommand child = node.findChild(args[index]);
            if (child == null) {
                break;
            }
            node = child;
            path.add(node);
            index++;
        }
        String[] remaining = index >= args.length
                ? new String[0]
                : Arrays.copyOfRange(args, index, args.length);
        return new Match(node, remaining, path);
    }

    private LPCommand findChild(String token) {
        if (token == null) {
            return null;
        }
        return children.get(token.toLowerCase(Locale.ROOT));
    }

    private List<String> suggestChildren(CommandSender sender, String prefix) {
        if (children.isEmpty()) {
            return List.of();
        }
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (LPCommand child : children.values()) {
            if (!child.canUse(sender)) {
                continue;
            }
            if (p.isEmpty() || child.name.startsWith(p)) {
                out.add(child.name);
            }
        }
        return out;
    }

    private boolean canUse(CommandSender sender) {
        if (senderType == SenderType.PLAYER && !(sender instanceof Player)) {
            return false;
        }
        if (senderType == SenderType.CONSOLE && !(sender instanceof ConsoleCommandSender)) {
            return false;
        }
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }

    private boolean checkSender(CommandSender sender, MessageManager messages) {
        if (senderType == SenderType.PLAYER && !(sender instanceof Player)) {
            sendKeyed(sender, messages, resolvePlayerOnlyKey(), FALLBACK_PLAYER_ONLY, Map.of());
            return false;
        }
        if (senderType == SenderType.CONSOLE && !(sender instanceof ConsoleCommandSender)) {
            sendKeyed(sender, messages, resolveConsoleOnlyKey(), FALLBACK_CONSOLE_ONLY, Map.of());
            return false;
        }
        return true;
    }

    private boolean checkPermission(CommandSender sender, MessageManager messages) {
        if (permission == null || permission.isEmpty() || sender.hasPermission(permission)) {
            return true;
        }
        Map<String, String> ph = Map.of("permission", permission);
        sendKeyed(sender, messages, resolveNoPermissionKey(), FALLBACK_NO_PERMISSION, ph);
        return false;
    }

    private void sendUsage(CommandSender sender, LPCommand node) {
        String usageText = node.usage.isEmpty() ? "/" + node.name : node.usage;
        Map<String, String> ph = Map.of("usage", usageText);
        if (sendKeyed(sender, messages, node.resolveUsageKey(), null, ph)) {
            return;
        }
        if (!node.usage.isEmpty()) {
            sender.sendMessage(ColorParser.translateColors("<gray>Usage: <white>" + node.usage));
        } else if (!node.children.isEmpty()) {
            sender.sendMessage(ColorParser.translateColors(
                    "<gray>Subcommands: <white>" + String.join(", ", node.children.keySet())));
        }
    }

    private void sendUnknown(CommandSender sender, String token) {
        sender.sendMessage(ColorParser.translateColors("<red>Unknown subcommand: <white>" + token));
    }

    private static boolean sendKeyed(
            CommandSender sender,
            MessageManager messages,
            String key,
            String fallback,
            Map<String, String> placeholders
    ) {
        if (messages != null && messages.getRaw(key) != null) {
            messages.send(sender, key, placeholders);
            return true;
        }
        if (fallback != null) {
            sender.sendMessage(ColorParser.translateColors(fallback));
            return true;
        }
        return false;
    }

    private String resolveNoPermissionKey() {
        return noPermissionMessageKey != null ? noPermissionMessageKey : messageKeys.noPermission();
    }

    private String resolveUsageKey() {
        return usageMessageKey != null ? usageMessageKey : messageKeys.usage();
    }

    private String resolvePlayerOnlyKey() {
        return playerOnlyMessageKey != null ? playerOnlyMessageKey : messageKeys.playerOnly();
    }

    private String resolveConsoleOnlyKey() {
        return consoleOnlyMessageKey != null ? consoleOnlyMessageKey : messageKeys.consoleOnly();
    }

    private void inheritTreeConfig(LPCommand child) {
        child.messages = this.messages;
        child.messageKeys = this.messageKeys;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void requireRoot(String method) {
        if (!root) {
            throw new IllegalStateException(method + "() is only valid on the root command");
        }
    }

    private static void propagateMessages(LPCommand node, MessageManager messages) {
        node.messages = messages;
        for (LPCommand child : node.children.values()) {
            propagateMessages(child, messages);
        }
    }

    private static void propagateMessageKeys(LPCommand node, CommandMessageKeys keys) {
        node.messageKeys = keys;
        for (LPCommand child : node.children.values()) {
            propagateMessageKeys(child, keys);
        }
    }

    private record Match(LPCommand node, String[] remaining, List<LPCommand> path) {
    }
}
