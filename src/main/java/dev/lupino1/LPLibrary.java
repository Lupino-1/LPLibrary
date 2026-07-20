package dev.lupino1;

import dev.lupino1.folia.FoliaManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LPLibrary {

    public static void init(JavaPlugin plugin) {
        FoliaManager.init(plugin);
    }
}
