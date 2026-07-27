package dev.lupino1.gui;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiManager {

    private static JavaPlugin plugin;
    private static NamespacedKey sessionKey;
    private static boolean registered;

    /** player UUID → session UUID */
    private static final Map<UUID, UUID> PLAYER_SESSIONS = new ConcurrentHashMap<>();
    /** session UUID → Gui template */
    private static final Map<UUID, Gui> SESSIONS = new ConcurrentHashMap<>();

    private GuiManager() {
    }

    public static void init(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        sessionKey = new NamespacedKey(javaPlugin, "gui_session");
        if (!registered) {
            javaPlugin.getServer().getPluginManager().registerEvents(new GuiListener(), javaPlugin);
            registered = true;
        }
    }

    static Plugin plugin() {
        ensureInit();
        return plugin;
    }

    static NamespacedKey sessionKey() {
        ensureInit();
        return sessionKey;
    }

    static void bind(Player player, UUID sessionId, Gui gui) {
        ensureInit();
        clear(player);
        PLAYER_SESSIONS.put(player.getUniqueId(), sessionId);
        SESSIONS.put(sessionId, gui);
        player.getPersistentDataContainer().set(sessionKey, PersistentDataType.STRING, sessionId.toString());
    }

    static void clear(Player player) {
        if (plugin == null || player == null) {
            return;
        }
        UUID sessionId = PLAYER_SESSIONS.remove(player.getUniqueId());
        if (sessionId != null) {
            SESSIONS.remove(sessionId);
        }
        String raw = player.getPersistentDataContainer().get(sessionKey, PersistentDataType.STRING);
        if (raw != null) {
            try {
                SESSIONS.remove(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
            player.getPersistentDataContainer().remove(sessionKey);
        }
    }

    static UUID getSessionId(Player player) {
        if (plugin == null || player == null) {
            return null;
        }
        UUID fromMap = PLAYER_SESSIONS.get(player.getUniqueId());
        if (fromMap != null) {
            return fromMap;
        }
        String raw = player.getPersistentDataContainer().get(sessionKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Gui getGui(Player player) {
        UUID sessionId = getSessionId(player);
        if (sessionId == null) {
            return null;
        }
        return SESSIONS.get(sessionId);
    }

    static boolean isValidSession(Player player, GuiHolder holder) {
        if (player == null || holder == null) {
            return false;
        }
        UUID sessionId = getSessionId(player);
        return sessionId != null && sessionId.equals(holder.getSessionId());
    }

    private static void ensureInit() {
        if (plugin == null) {
            throw new IllegalStateException("GuiManager not initialized. Call LPLibrary.init(plugin) first.");
        }
    }
}
