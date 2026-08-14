package dev.lupino1.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

final class CommandRegistrar {

    private static final Map<String, RegisteredCommand> REGISTERED = new ConcurrentHashMap<>();

    private CommandRegistrar() {
    }

    static void register(LPCommand root) {
        JavaPlugin plugin = root.plugin();
        String name = root.name();
        String key = plugin.getName().toLowerCase(Locale.ROOT) + ":" + name;

        RegisteredCommand previous = REGISTERED.remove(key);
        if (previous != null) {
            unregister(previous);
        }

        RegisteredCommand command = new RegisteredCommand(root);
        CommandMap map = Bukkit.getCommandMap();
        map.register(plugin.getName().toLowerCase(Locale.ROOT), command);
        REGISTERED.put(key, command);
        syncCommands(plugin);
    }

    private static void unregister(RegisteredCommand command) {
        try {
            CommandMap map = Bukkit.getCommandMap();
            command.unregister(map);
            Method known = map.getClass().getMethod("getKnownCommands");
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) known.invoke(map);
            knownCommands.entrySet().removeIf(e -> e.getValue() == command);
        } catch (ReflectiveOperationException e) {
            command.getPlugin().getLogger().log(Level.WARNING, "[Commands] Failed to unregister " + command.getName(), e);
        }
    }

    private static void syncCommands(JavaPlugin plugin) {
        try {
            Method sync = Bukkit.getServer().getClass().getMethod("syncCommands");
            sync.invoke(Bukkit.getServer());
        } catch (NoSuchMethodException ignored) {
            // non-Paper
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(Level.WARNING, "[Commands] syncCommands failed", e);
        }
    }

    static final class RegisteredCommand extends Command implements PluginIdentifiableCommand {

        private final LPCommand root;
        private final JavaPlugin plugin;

        RegisteredCommand(LPCommand root) {
            super(root.name());
            this.root = root;
            this.plugin = root.plugin();
            setDescription(root.description());
            setUsage(root.usage().isEmpty() ? "/" + root.name() : root.usage());
            setAliases(new ArrayList<>(root.aliases()));
            String perm = root.permission();
            if (perm != null && !perm.isEmpty()) {
                // Hides root from / tab when missing; sub-perms still checked in LPCommand tree.
                setPermission(perm);
            }
        }

        @Override
        public @NotNull Plugin getPlugin() {
            return plugin;
        }

        /**
         * Same deny text as the command tree ({@link MessageManager} / defaults).
         * Avoids Bukkit's vanilla permission message.
         */
        @Override
        public boolean testPermission(@NotNull CommandSender target) {
            if (testPermissionSilent(target)) {
                return true;
            }
            root.checkPermission(target, root.messages());
            return false;
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            return root.execute(sender, commandLabel, args);
        }

        @Override
        public @NotNull List<String> tabComplete(
                @NotNull CommandSender sender,
                @NotNull String alias,
                @NotNull String[] args
        ) throws IllegalArgumentException {
            List<String> result = root.tabComplete(sender, alias, args);
            return result == null ? Collections.emptyList() : result;
        }
    }
}
