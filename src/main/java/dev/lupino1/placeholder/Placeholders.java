package dev.lupino1.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * Soft PlaceholderAPI bridge. Without PAPI on the server this is a no-op.
 * <p>
 * Flow: Map {@code %key%} replace → PAPI ({@link Player} only) → caller MiniMessage / raw use.
 */
public final class Placeholders {

    private Placeholders() {
    }

    /** {@code true} when PlaceholderAPI plugin is enabled on this server. */
    public static boolean isAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Map string placeholders then PAPI. {@link Component} map values are skipped
     * (messages apply them after MiniMessage).
     */
    public static String apply(String raw, Player player, Map<String, ?> placeholders) {
        if (raw == null) {
            return null;
        }
        return applyPapi(player, applyMap(raw, placeholders));
    }

    /** Map {@code %key%} only (string values). */
    public static String applyMap(String raw, Map<String, ?> placeholders) {
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

    /** PAPI when available and {@code player != null}; otherwise returns {@code text}. */
    public static String applyPapi(Player player, String text) {
        if (text == null || player == null || !isAvailable()) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
