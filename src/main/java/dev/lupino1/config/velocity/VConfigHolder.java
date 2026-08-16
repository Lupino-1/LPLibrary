package dev.lupino1.config.velocity;

import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Snapshot-swapping config holder (Velocity). Same DX as Paper {@link dev.lupino1.config.ConfigHolder}.
 * Path / field reads hit RAM only. Disk I/O: prefer {@link #reloadAsync()} / {@link #saveAsync()} / {@link #editAsync}.
 *
 * <pre>{@code
 * VConfigHolder<VConfigDocument> config =
 *     VConfigHolder.create(dataDir, getClass(), logger, VConfigDocument.class, "config.yml");
 *
 * VConfigSection db = config.get("database");
 * String url = config.getString("database.url");
 * }</pre>
 */
public final class VConfigHolder<T extends VYamlConfig> {

    private final Path dataDirectory;
    private final Class<?> pluginClass;
    private final Logger logger;
    private final Class<T> type;
    private final String fileName;
    private final boolean updateMissing;
    private volatile T current;

    private VConfigHolder(
            Path dataDirectory,
            Class<?> pluginClass,
            Logger logger,
            Class<T> type,
            String fileName,
            boolean updateMissing
    ) {
        this.dataDirectory = dataDirectory;
        this.pluginClass = pluginClass;
        this.logger = logger;
        this.type = type;
        this.fileName = fileName;
        this.updateMissing = updateMissing;
    }

    public static <T extends VYamlConfig> VConfigHolder<T> create(
            Path dataDirectory,
            Class<?> pluginClass,
            Logger logger,
            Class<T> type,
            String fileName
    ) {
        return create(dataDirectory, pluginClass, logger, type, fileName, true);
    }

    public static <T extends VYamlConfig> VConfigHolder<T> create(
            Path dataDirectory,
            Class<?> pluginClass,
            Logger logger,
            Class<T> type,
            String fileName,
            boolean updateMissing
    ) {
        VConfigHolder<T> holder = new VConfigHolder<>(
                Objects.requireNonNull(dataDirectory, "dataDirectory"),
                Objects.requireNonNull(pluginClass, "pluginClass"),
                Objects.requireNonNull(logger, "logger"),
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
    public VConfigSection get(String path) {
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

    public String getParsedString(String path, Map<String, ?> placeholders) {
        return require().getParsedString(path, placeholders);
    }

    public String getParsedString(String path, String def, Map<String, ?> placeholders) {
        return require().getParsedString(path, def, placeholders);
    }

    public Component getParsedComponent(String path, Map<String, ?> placeholders) {
        return require().getParsedComponent(path, placeholders);
    }

    public Component getParsedComponent(String path, String def, Map<String, ?> placeholders) {
        return require().getParsedComponent(path, def, placeholders);
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
     */
    public synchronized T reload() {
        T next = VYamlConfig.load(dataDirectory, pluginClass, logger, type, fileName, updateMissing);
        current = next;
        return next;
    }

    public CompletableFuture<T> reloadAsync() {
        return CompletableFuture.supplyAsync(this::reload);
    }

    /** Save the current snapshot to disk. Sync — prefer {@link #saveAsync()}. */
    public synchronized void save() {
        T snapshot = current;
        if (snapshot != null) {
            snapshot.save();
        }
    }

    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(this::save);
    }

    /**
     * Mutate current snapshot then save to disk (sync). Prefer {@link #editAsync(Consumer)}.
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
     * Editor runs on the calling thread, then persists that snapshot async.
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
        return CompletableFuture.runAsync(() -> {
            synchronized (this) {
                snapshot.save();
            }
        });
    }

    private T require() {
        T snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Config not loaded");
        }
        return snapshot;
    }
}
