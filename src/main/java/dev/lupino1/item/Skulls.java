package dev.lupino1.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class Skulls {

    private Skulls() {
    }

    static @NotNull ItemStack fromUrl(@NotNull String url) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) {
            return item;
        }

        try {
            UUID id = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
            String name = id.toString().replace("-", "").substring(0, 16);
            PlayerProfile profile = Bukkit.createProfile(id, name);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(url).toURL());
            profile.setTextures(textures);
            meta.setPlayerProfile(profile);
            item.setItemMeta(meta);
        } catch (Exception ignored) {
            // plain PLAYER_HEAD on bad URL
        }
        return item;
    }
}
