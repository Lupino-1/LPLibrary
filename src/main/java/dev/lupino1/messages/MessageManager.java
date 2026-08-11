package dev.lupino1.messages;

import dev.lupino1.folia.FoliaManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class MessageManager {

    private final JavaPlugin plugin;
    private final File messagesFile;
    private final String resourcePath;
    private final boolean saveDefaults;

    private volatile Map<String, String> strings = Map.of();
    private volatile Map<String, List<String>> lists = Map.of();
    private volatile String prefixRaw = "";

    public MessageManager(JavaPlugin plugin) {
        this(plugin, "messages.yml", true);
    }

    public MessageManager(JavaPlugin plugin, String fileName) {
        this(plugin, fileName, true);
    }

    /**
     * @param fileName     path in the jar and under the plugin data folder (same as {@link JavaPlugin#saveResource})
     * @param saveDefaults if {@code true}, copies the jar resource to disk when missing
     */
    public MessageManager(JavaPlugin plugin, String fileName, boolean saveDefaults) {
        this.plugin = plugin;
        this.resourcePath = fileName.replace('\\', '/');
        this.messagesFile = new File(plugin.getDataFolder(), this.resourcePath);
        this.saveDefaults = saveDefaults;
        ensureFile();
        reload();
    }

    public void reload() {
        if (!messagesFile.exists()) {
            ensureFile();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);

        Map<String, String> newStrings = new HashMap<>();
        Map<String, List<String>> newLists = new HashMap<>();

        for (String key : config.getKeys(true)) {
            if (config.isConfigurationSection(key)) {
                continue;
            }
            if (config.isList(key)) {
                newLists.put(key, List.copyOf(config.getStringList(key)));
            } else if (config.isString(key)) {
                String value = config.getString(key);
                if (value != null) {
                    newStrings.put(key, value);
                }
            }
        }

        this.strings = Map.copyOf(newStrings);
        this.lists = Map.copyOf(newLists);
        this.prefixRaw = newStrings.getOrDefault("prefix", "");
    }

    public CompletableFuture<Void> reloadAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        FoliaManager.runAsync(() -> {
            try {
                reload();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public Component parse(String input) {
        return ColorParser.translateColors(input);
    }

    public Component get(String key) {
        return get(key, null, true);
    }

    public Component get(String key, boolean prefix) {
        return get(key, null, prefix);
    }

    public Component get(String key, Map<String, ?> placeholders) {
        return get(key, placeholders, true);
    }

    /**
     * @param placeholders values may be {@link String} or {@link Component}
     */
    public Component get(String key, Map<String, ?> placeholders, boolean prefix) {
        Map<String, String> strings = this.strings;
        String prefixRaw = this.prefixRaw;

        String raw = strings.get(key);
        if (raw == null) {
            plugin.getLogger().warning("[Messages] Missing key: " + key);
            return Component.text(key);
        }

        // Concat before MiniMessage — Component.append after prefix <reset> ate message colors
        String source = (prefix && !prefixRaw.isEmpty()) ? prefixRaw + raw : raw;
        return applyPlaceholders(source, placeholders);
    }

    public List<Component> getList(String key) {
        return getList(key, null);
    }

    /**
     * @param placeholders values may be {@link String} or {@link Component}
     */
    public List<Component> getList(String key, Map<String, ?> placeholders) {
        Map<String, List<String>> lists = this.lists;
        Map<String, String> strings = this.strings;

        List<String> rawList = lists.get(key);
        if (rawList == null) {
            String single = strings.get(key);
            if (single != null) {
                return List.of(applyPlaceholders(single, placeholders));
            }
            plugin.getLogger().warning("[Messages] Missing list key: " + key);
            return List.of(Component.text(key));
        }

        List<Component> result = new ArrayList<>(rawList.size());
        for (String line : rawList) {
            result.add(applyPlaceholders(line, placeholders));
        }
        return result;
    }

    public String getRaw(String key) {
        return strings.get(key);
    }

    public String getRaw(String key, String def) {
        return strings.getOrDefault(key, def);
    }

    public List<String> getRawList(String key) {
        List<String> rawList = lists.get(key);
        return rawList == null ? Collections.emptyList() : rawList;
    }

    public String getPrefixRaw() {
        return prefixRaw;
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, null, true);
    }

    public void send(CommandSender sender, String key, Map<String, ?> placeholders) {
        send(sender, key, placeholders, true);
    }

    public void send(CommandSender sender, String key, boolean prefix) {
        send(sender, key, null, prefix);
    }

    public void send(CommandSender sender, String key, Map<String, ?> placeholders, boolean prefix) {
        if (sender == null) {
            return;
        }
        if (sender instanceof Player player && !player.isOnline()) {
            return;
        }
        sender.sendMessage(get(key, placeholders, prefix));
    }

    public void sendParsed(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(ColorParser.translateColors(message));
    }

    public void actionBar(Player player, String key) {
        actionBar(player, key, null, true);
    }

    public void actionBar(Player player, String key, Map<String, ?> placeholders) {
        actionBar(player, key, placeholders, true);
    }

    public void actionBar(Player player, String key, Map<String, ?> placeholders, boolean prefix) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.sendActionBar(get(key, placeholders, prefix));
    }

    public void broadcast(String key) {
        broadcast(key, null, true);
    }

    public void broadcast(String key, Map<String, ?> placeholders) {
        broadcast(key, placeholders, true);
    }

    public void broadcast(String key, Map<String, ?> placeholders, boolean prefix) {
        Bukkit.broadcast(get(key, placeholders, prefix));
    }

    private void ensureFile() {
        if (!saveDefaults || messagesFile.exists()) {
            return;
        }

        try {
            plugin.saveResource(resourcePath, false);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "[Messages] Embedded resource not found: " + resourcePath, e);
        }
    }

    /**
     * String placeholders → before MiniMessage. Component placeholders → Adventure {@code replaceText} after parse.
     */
    private static Component applyPlaceholders(String raw, Map<String, ?> placeholders) {
        if (raw == null) {
            return Component.empty();
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return ColorParser.translateColors(raw);
        }

        String withStrings = raw;
        for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Component) {
                continue;
            }
            String text = value == null ? "" : String.valueOf(value);
            withStrings = withStrings.replace("%" + entry.getKey() + "%", text);
        }

        Component result = ColorParser.translateColors(withStrings);
        for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof Component component)) {
                continue;
            }
            String token = "%" + entry.getKey() + "%";
            result = result.replaceText(config -> config.matchLiteral(token).replacement(component));
        }
        return result;
    }
}
