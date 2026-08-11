package dev.lupino1;

import dev.lupino1.folia.FoliaManager;
import dev.lupino1.gui.GuiManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LPLibrary {

    private LPLibrary() {
    }

    public static void init(JavaPlugin plugin) {
        FoliaManager.init(plugin);
        GuiManager.init(plugin);
    }
}
