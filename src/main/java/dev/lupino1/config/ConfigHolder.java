package dev.lupino1.config;

import dev.lupino1.folia.FoliaManager;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Folia-safe config: {@link #reload()} loads a <b>new</b> snapshot and swaps a {@code volatile} ref.
 * Path / field reads hit RAM only — no disk, no thread wait.
 * Disk I/O: prefer {@link #reloadAsync()} / {@link #saveAsync()} / {@link #editAsync(Consumer)}.
 * {@link #editAsync} mutates on the caller thread (Bukkit objects OK), save is async.
 *
 * <pre>{@code
 * ConfigHolder<ConfigDocument> config =
 *     ConfigHolder.create(this, ConfigDocument.class, "config.yml");
 *
 * ConfigurationSection db = config.get("database");
 * String url = config.getString("database.url");
 *
 * config.reloadAsync(); // disk off region thread
 * config.editAsync(doc -> doc.raw().set("spawn", player.getLocation()));
 * }</pre>
 */
public final class ConfigHolder<T extends YamlConfig> {

    private final JavaPlugin plugin;
    private final Class<T> type;
    private final String fileName;
    private final boolean updateMissing;
    private volatile T current;

    private ConfigHolder(JavaPlugin plugin, Class<T> type, String fileName, boolean updateMissing) {
        this.plugin = plugin;
        this.type = type;
        this.fileName = fileName;
        this.updateMissing = updateMissing;

    }

    public static <T extends YamlConfig> ConfigHolder<T> create(
            JavaPlugin plugin,
            Class<T> type,
            String fileName
    ) {

        return create(plugin, type, fileName, true);
    }

    public static <T extends YamlConfig> ConfigHolder<T> create(
            JavaPlugin plugin,
            Class<T> type,
            String fileName,
            boolean updateMissing
    ) {
        ConfigHolder<T> holder = new ConfigHolder<>(
                Objects.requireNonNull(plugin, "plugin"),
                Objects.requireNonNull(type, "type"),
                Objects.requireNonNull(fileName, "fileName"),
                updateMissing
        );
        holder.reload();
        return holder;
    }

    /** Typed snapshot. Never cache across reload if you need fresh values. */
    public T get() {
        return current;
    }

    /**
     * Section at path from current snapshot (RAM). Empty path → root. {@code null} if missing.
     */
    public ConfigurationSection get(String path) {
        return require().get(path);
    }

    public boolean contains(String path) {
        return require().contains(path);
    }

    public String getString(String path) {
        return require().getString(path);
    }

    public String getString(String path, String def) {
        return require().getString(path, def);
    }

    /** {@link YamlConfig#getParsedString(String, Player)} on current snapshot. */
    public String getParsedString(String path, Player player) {
        return require().getParsedString(path, player);
    }

    public String getParsedString(String path, Player player, Map<String, ?> placeholders) {
        return require().getParsedString(path, player, placeholders);
    }

    public String getParsedString(String path, String def, Player player) {
        return require().getParsedString(path, def, player);
    }

    public String getParsedString(String path, String def, Player player, Map<String, ?> placeholders) {
        return require().getParsedString(path, def, player, placeholders);
    }

    public Component getParsedComponent(String path, Player player) {
        return require().getParsedComponent(path, player);
    }

    public Component getParsedComponent(String path, Player player, Map<String, ?> placeholders) {
        return require().getParsedComponent(path, player, placeholders);
    }

    public Component getParsedComponent(String path, String def, Player player) {
        return require().getParsedComponent(path, def, player);
    }

    public Component getParsedComponent(String path, String def, Player player, Map<String, ?> placeholders) {
        return require().getParsedComponent(path, def, player, placeholders);
    }

    public int getInt(String path) {
        return require().getInt(path);
    }

    public int getInt(String path, int def) {
        return require().getInt(path, def);
    }

    public long getLong(String path) {
        return require().getLong(path);
    }

    public long getLong(String path, long def) {
        return require().getLong(path, def);
    }

    public double getDouble(String path) {
        return require().getDouble(path);
    }

    public double getDouble(String path, double def) {
        return require().getDouble(path, def);
    }

    public boolean getBoolean(String path) {
        return require().getBoolean(path);
    }

    public boolean getBoolean(String path, boolean def) {
        return require().getBoolean(path, def);
    }

    public List<String> getStringList(String path) {
        return require().getStringList(path);
    }

    public Object getObject(String path) {
        return require().getObject(path);
    }

    public Object getObject(String path, Object def) {
        return require().getObject(path, def);
    }

    /**
     * Load a new instance from disk and publish it. In-flight readers may still see the previous snapshot.
     * Sync — prefer {@link #reloadAsync()} off the region thread.
     */
    public synchronized T reload() {
        T next = YamlConfig.load(plugin, type, fileName, updateMissing);
        current = next;
        return next;
    }

    /** {@link #reload()} on Folia async scheduler. Requires {@code LPLibrary.init}. */
    public CompletableFuture<T> reloadAsync() {
        CompletableFuture<T> future = new CompletableFuture<>();
        FoliaManager.runAsync(() -> {
            try {
                future.complete(reload());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /** Save the current snapshot to disk. Sync — prefer {@link #saveAsync()}. */
    public synchronized void save() {
        T snapshot = current;
        if (snapshot != null) {
            snapshot.save();
        }
    }

    /** {@link #save()} on Folia async scheduler. Requires {@code LPLibrary.init}. */
    public CompletableFuture<Void> saveAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        FoliaManager.runAsync(() -> {
            try {
                save();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Mutate current snapshot then save to disk (sync). Prefer {@link #editAsync(Consumer)}
     * so disk I/O does not block the caller.
     */
    public synchronized void edit(Consumer<T> editor) {
        Objects.requireNonNull(editor, "editor");
        T snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Config not loaded");
        }
        editor.accept(snapshot);
        snapshot.save();
    }

    /**
     * Editor runs on the <b>calling</b> thread (so {@code Location}, {@code ItemStack}, etc. serialize safely),
     * then persists that snapshot on the Folia async scheduler. Requires {@code LPLibrary.init}.
     * Keep the editor fast — no disk/sleep; Bukkit only if the caller is already on a safe thread.
     */
    public CompletableFuture<Void> editAsync(Consumer<T> editor) {
        Objects.requireNonNull(editor, "editor");
        final T snapshot;
        synchronized (this) {
            snapshot = current;
            if (snapshot == null) {
                throw new IllegalStateException("Config not loaded");
            }
            editor.accept(snapshot);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        FoliaManager.runAsync(() -> {
            try {
                synchronized (this) {
                    snapshot.save();
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private T require() {
        T snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Config not loaded");
        }
        return snapshot;
    }
}
