package dev.lupino1.item;

import dev.lupino1.messages.ColorParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ItemBuilder {

    private final ItemStack item;

    private ItemBuilder(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item").clone();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(new ItemStack(material, amount));
    }

    public static ItemBuilder of(ItemStack item) {
        return new ItemBuilder(item);
    }

    /**
     * Material name ({@code DIAMOND}) or http(s) texture URL → skull.
     * Invalid → {@link Material#STONE}.
     */
    public static ItemBuilder of(String input) {
        return new ItemBuilder(ItemParser.parse(input));
    }

    public static ItemBuilder of(String input, Material fallback) {
        return new ItemBuilder(ItemParser.parse(input, fallback));
    }

    /**
     * Player head with skin from a Minecraft texture URL
     * (e.g. {@code http://textures.minecraft.net/texture/...}).
     */
    public static ItemBuilder ofSkull(String textureUrl) {
        return new ItemBuilder(Skulls.fromUrl(Objects.requireNonNull(textureUrl, "textureUrl")));
    }

    /**
     * Sets this stack to {@link Material#PLAYER_HEAD} with the given texture URL.
     */
    public ItemBuilder skullUrl(String textureUrl) {
        ItemStack skull = Skulls.fromUrl(Objects.requireNonNull(textureUrl, "textureUrl"));
        item.setType(Material.PLAYER_HEAD);
        item.setItemMeta(skull.getItemMeta());
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder name(Component name) {
        return meta(meta -> meta.displayName(noItalic(name)));
    }

    public ItemBuilder name(String name) {
        return name(ColorParser.translateColors(name));
    }

    public ItemBuilder lore(Component... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<Component> lines) {
        if (lines == null || lines.isEmpty()) {
            return meta(meta -> meta.lore(List.of()));
        }
        List<Component> normalized = new ArrayList<>(lines.size());
        for (Component line : lines) {
            normalized.add(noItalic(line));
        }
        return meta(meta -> meta.lore(List.copyOf(normalized)));
    }

    public ItemBuilder lore(String... lines) {
        List<Component> components = new ArrayList<>(lines.length);
        for (String line : lines) {
            components.add(ColorParser.translateColors(line));
        }
        return lore(components);
    }

    public ItemBuilder appendLore(String... lines) {
        return meta(meta -> {
            List<Component> lore = meta.lore();
            if (lore == null) {
                lore = new ArrayList<>();
            } else {
                lore = new ArrayList<>(lore);
            }
            for (String line : lines) {
                lore.add(noItalic(ColorParser.translateColors(line)));
            }
            meta.lore(lore);
        });
    }

    private static Component noItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    public ItemBuilder glow(boolean glow) {
        return meta(meta -> {
            if (glow) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeEnchant(Enchantment.DURABILITY);
            }
        });
    }

    public ItemBuilder flags(ItemFlag... flags) {
        return meta(meta -> meta.addItemFlags(flags));
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        return meta(meta -> meta.setUnbreakable(unbreakable));
    }

    public ItemBuilder customModelData(int data) {
        return meta(meta -> meta.setCustomModelData(data));
    }

    public ItemBuilder meta(Consumer<ItemMeta> consumer) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            consumer.accept(meta);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return item.clone();
    }
}
