package dev.lupino1.messages.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.lupino1.messages.ColorParser;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Velocity counterpart of {@link dev.lupino1.messages.MessageManager}.
 * Same DX minus PAPI / Bukkit (map {@code %key%} + MiniMessage only).
 */
public class VMessageManager {

    private final ProxyServer server;
    private final Path messagesFile;
    private final String resourcePath;
    private final boolean saveDefaults;
    private final Class<?> pluginClass;
    private final Logger logger;

    private volatile Map<String, String> strings = Map.of();
    private volatile Map<String, List<String>> lists = Map.of();
    private volatile String prefixRaw = "";

    public VMessageManager(ProxyServer server, Path dataDirectory, Class<?> pluginClass, Logger logger) {
        this(server, dataDirectory, pluginClass, logger, "messages.yml", true);
    }

    public VMessageManager(ProxyServer server, Path dataDirectory, Class<?> pluginClass, Logger logger, String fileName) {
        this(server, dataDirectory, pluginClass, logger, fileName, true);
    }

    /**
     * @param fileName     path in the jar and under the plugin data folder
     * @param saveDefaults if {@code true}, copies the jar resource to disk when missing
     */
    public VMessageManager(
            ProxyServer server,
            Path dataDirectory,
            Class<?> pluginClass,
            Logger logger,
            String fileName,
            boolean saveDefaults
    ) {
        this.server = server;
        this.pluginClass = pluginClass;
        this.logger = logger;
        this.resourcePath = fileName.replace('\\', '/');
        this.messagesFile = dataDirectory.resolve(this.resourcePath);
        this.saveDefaults = saveDefaults;
        ensureFile();
        reload();
    }

    public void reload() {
        if (!Files.exists(messagesFile)) {
            ensureFile();
        }

        Map<String, String> newStrings = new HashMap<>();
        Map<String, List<String>> newLists = new HashMap<>();

        try {
            if (!Files.exists(messagesFile)) {
                this.strings = Map.of();
                this.lists = Map.of();
                this.prefixRaw = "";
                return;
            }
            Object root;
            try (InputStream in = Files.newInputStream(messagesFile)) {
                root = new Yaml().load(in);
            }
            flatten("", root, newStrings, newLists);
        } catch (IOException e) {
            logger.warn("[Messages] Failed to load {}", resourcePath, e);
        }

        this.strings = Map.copyOf(newStrings);
        this.lists = Map.copyOf(newLists);
        this.prefixRaw = newStrings.getOrDefault("prefix", "");
    }

    public CompletableFuture<Void> reloadAsync() {
        return CompletableFuture.runAsync(() -> {
            reload();
        });
    }

    public Component parse(String input) {
        return parse(input, null);
    }

    public Component parse(String input, Map<String, ?> placeholders) {
        return applyPlaceholders(input, placeholders);
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
     * Flow: raw (+ optional prefix) → Map {@code %key%} → MiniMessage → Component map replacements.
     */
    public Component get(String key, Map<String, ?> placeholders, boolean prefix) {
        Map<String, String> strings = this.strings;
        String prefixRaw = this.prefixRaw;

        String raw = strings.get(key);
        if (raw == null) {
            logger.warn("[Messages] Missing key: {}", key);
            return Component.text(key);
        }

        String source = (prefix && !prefixRaw.isEmpty()) ? prefixRaw + raw : raw;
        return applyPlaceholders(source, placeholders);
    }

    public List<Component> getList(String key) {
        return getList(key, null);
    }

    public List<Component> getList(String key, Map<String, ?> placeholders) {
        Map<String, List<String>> lists = this.lists;
        Map<String, String> strings = this.strings;

        List<String> rawList = lists.get(key);
        if (rawList == null) {
            String single = strings.get(key);
            if (single != null) {
                return List.of(applyPlaceholders(single, placeholders));
            }
            logger.warn("[Messages] Missing list key: {}", key);
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

    public void send(Audience audience, String key) {
        send(audience, key, null, true);
    }

    public void send(Audience audience, String key, Map<String, ?> placeholders) {
        send(audience, key, placeholders, true);
    }

    public void send(Audience audience, String key, boolean prefix) {
        send(audience, key, null, prefix);
    }

    public void send(Audience audience, String key, Map<String, ?> placeholders, boolean prefix) {
        if (!canSend(audience)) {
            return;
        }
        if (lists.containsKey(key) && !strings.containsKey(key)) {
            sendList(audience, key, placeholders);
            return;
        }
        audience.sendMessage(get(key, placeholders, prefix));
    }

    /** YAML list key — one chat line each. No prefix (same as {@link #getList}). */
    public void sendList(Audience audience, String key) {
        sendList(audience, key, null);
    }

    public void sendList(Audience audience, String key, Map<String, ?> placeholders) {
        if (!canSend(audience)) {
            return;
        }
        for (Component line : getList(key, placeholders)) {
            audience.sendMessage(line);
        }
    }

    public void sendParsed(Audience audience, String message) {
        sendParsed(audience, message, null);
    }

    public void sendParsed(Audience audience, String message, Map<String, ?> placeholders) {
        if (!canSend(audience) || message == null) {
            return;
        }
        audience.sendMessage(applyPlaceholders(message, placeholders));
    }

    public void sendParsed(Audience audience, List<String> messages) {
        sendParsed(audience, messages, null);
    }

    public void sendParsed(Audience audience, List<String> messages, Map<String, ?> placeholders) {
        if (!canSend(audience) || messages == null || messages.isEmpty()) {
            return;
        }
        for (String line : messages) {
            if (line == null) {
                continue;
            }
            audience.sendMessage(applyPlaceholders(line, placeholders));
        }
    }

    public void actionBar(Player player, String key) {
        actionBar(player, key, null, true);
    }

    public void actionBar(Player player, String key, Map<String, ?> placeholders) {
        actionBar(player, key, placeholders, true);
    }

    public void actionBar(Player player, String key, Map<String, ?> placeholders, boolean prefix) {
        if (!canSend(player)) {
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
        Component message = get(key, placeholders, prefix);
        for (Player player : server.getAllPlayers()) {
            player.sendMessage(message);
        }
        server.getConsoleCommandSource().sendMessage(message);
    }

    private void ensureFile() {
        if (!saveDefaults || Files.exists(messagesFile)) {
            return;
        }

        try {
            Path parent = messagesFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (InputStream in = pluginClass.getClassLoader().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    logger.warn("[Messages] Embedded resource not found: {}", resourcePath);
                    return;
                }
                Files.copy(in, messagesFile);
            }
        } catch (IOException e) {
            logger.warn("[Messages] Failed to copy default {}", resourcePath, e);
        }
    }

    private static boolean canSend(Audience audience) {
        if (audience == null) {
            return false;
        }
        if (audience instanceof Player player) {
            return player.isActive();
        }
        return true;
    }

    /**
     * String placeholders → MiniMessage. Component placeholders → Adventure {@code replaceText} after parse.
     * No PAPI (not on Velocity).
     */
    private static Component applyPlaceholders(String raw, Map<String, ?> placeholders) {
        if (raw == null) {
            return Component.empty();
        }

        String withStrings = applyMap(raw, placeholders);
        Component result = ColorParser.translateColors(withStrings);

        if (placeholders == null || placeholders.isEmpty()) {
            return result;
        }
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

    private static String applyMap(String raw, Map<String, ?> placeholders) {
        if (raw == null || placeholders == null || placeholders.isEmpty()) {
            return raw;
        }
        String out = raw;
        for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Component) {
                continue;
            }
            String text = value == null ? "" : String.valueOf(value);
            out = out.replace("%" + entry.getKey() + "%", text);
        }
        return out;
    }

    private static void flatten(
            String prefix,
            Object node,
            Map<String, String> strings,
            Map<String, List<String>> lists
    ) {
        if (node == null) {
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = prefix.isEmpty()
                        ? String.valueOf(entry.getKey())
                        : prefix + "." + entry.getKey();
                flatten(key, entry.getValue(), strings, lists);
            }
            return;
        }
        if (node instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(item == null ? "" : String.valueOf(item));
            }
            lists.put(prefix, List.copyOf(out));
            return;
        }
        strings.put(prefix, String.valueOf(node));
    }
}
