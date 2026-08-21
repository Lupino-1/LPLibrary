package dev.lupino1.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

/**
 * Parses config-friendly item strings: material name or http(s) texture URL → skull.
 */
public final class ItemParser {

    private ItemParser() {
    }

    /**
     * Material name ({@code DIAMOND}, {@code player_head}, …) or texture URL → {@link ItemStack}.
     * Invalid input → {@link Material#STONE}.
     */
    public static @NotNull ItemStack parse(@NotNull String input) {
        return parse(input, Material.STONE);
    }

    public static @NotNull ItemStack parse(@NotNull String input, @NotNull Material fallback) {
        Objects.requireNonNull(fallback, "fallback");
        String cleaned = Objects.requireNonNull(input, "input").trim();
        if (cleaned.isEmpty()) {
            return new ItemStack(fallback);
        }

        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return Skulls.fromUrl(cleaned);
        }

        try {
            return new ItemStack(Material.valueOf(cleaned.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return new ItemStack(fallback);
        }
    }
}
