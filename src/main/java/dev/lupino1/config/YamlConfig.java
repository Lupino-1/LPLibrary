package dev.lupino1.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import dev.lupino1.placeholder.Placeholders;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/**
 * YAML config in memory (Bukkit {@link YamlConfiguration}).
 * <p>
 * Reads hit the RAM snapshot — not disk. Prefer {@link ConfigHolder} under Folia
 * ({@code reload()} swaps a new instance; never mutate a shared instance in place).
 *
 * <pre>{@code
 * ConfigHolder<ConfigDocument> config = ConfigHolder.create(plugin, ConfigDocument.class, "config.yml");
 * ConfigurationSection db = config.get("database");
 * String url = db.getString("url");
 * // or: config.getString("database.url");
 * }</pre>
 */
public abstract class YamlConfig {

    private transient JavaPlugin plugin;
    private transient File file;
    private transient String resourcePath;
    /** In-memory YAML; path getters read this. */
    private transient FileConfiguration yaml;

    public static <T extends YamlConfig> T load(JavaPlugin plugin, Class<T> type, String fileName) {
        return load(plugin, type, fileName, true);
    }

    /**
     * @param updateMissing if {@code true}, writes missing default keys/comments back to disk after load
     */
    public static <T extends YamlConfig> T load(
            JavaPlugin plugin,
            Class<T> type,
            String fileName,
            boolean updateMissing
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fileName, "fileName");

        T instance;
        try {
            instance = type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Config " + type.getName() + " needs a public no-args constructor", e);
        }
        instance.bind(plugin, fileName);
        instance.reload(updateMissing);
        return instance;
    }

    public void bind(JavaPlugin plugin, String fileName) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.resourcePath = fileName.replace('\\', '/');
        this.file = new File(plugin.getDataFolder(), this.resourcePath);
    }

    /**
     * Re-read this instance from disk (mutates fields in place — <b>not</b> Folia-safe for shared refs).
     * Prefer {@link ConfigHolder#reload()}.
     */
    public void reload() {
        reload(true);
    }

    /**
     * @param updateMissing if {@code true}, writes missing default keys/comments back to disk after load
     * @see #reload()
     */
    public void reload(boolean updateMissing) {
        ensureBound();
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            saveResourceDefaults();
        }

        this.yaml = YamlConfiguration.loadConfiguration(file);
        readInto(this, this.yaml, "");

        if (updateMissing) {
            save();
        }
    }

    /**
     * Writes mapped fields into the in-memory YAML (preserves unknown keys), then to disk.
     */
    public void save() {
        ensureBound();
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        applyHeader(yaml, getClass());
        writeFrom(this, yaml, "");

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[Config] Failed to save " + resourcePath, e);
        }
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public File file() {
        return file;
    }

    /** Root in-memory YAML (RAM). */
    public FileConfiguration raw() {
        ensureYaml();
        return yaml;

    }

    /**
     * Section at {@code path}, or {@code null} if missing / not a section.
     * Empty / null path → root. Reads RAM only.
     */
    public ConfigurationSection get(String path) {
        ensureYaml();
        if (path == null || path.isEmpty()) {
            return yaml;
        }
        return yaml.getConfigurationSection(path);
    }

    public boolean contains(String path) {
        ensureYaml();
        return yaml.contains(path);
    }

    public String getString(String path) {
        ensureYaml();
        return yaml.getString(path);
    }

    public String getString(String path, String def) {
        ensureYaml();
        return yaml.getString(path, def);
    }

    /**
     * {@link #getString(String)} then Map {@code %key%} + PlaceholderAPI (soft).
     * Returns raw string — no MiniMessage.
     */
    public String getParsedString(String path, Player player) {
        return getParsedString(path, player, null);
    }

    public String getParsedString(String path, Player player, Map<String, ?> placeholders) {
        return Placeholders.apply(getString(path), player, placeholders);
    }

    public String getParsedString(String path, String def, Player player) {
        return getParsedString(path, def, player, null);
    }

    public String getParsedString(String path, String def, Player player, Map<String, ?> placeholders) {
        return Placeholders.apply(getString(path, def), player, placeholders);
    }

    public int getInt(String path) {
        ensureYaml();
        return yaml.getInt(path);
    }

    public int getInt(String path, int def) {
        ensureYaml();
        return yaml.getInt(path, def);
    }

    public long getLong(String path) {
        ensureYaml();
        return yaml.getLong(path);
    }

    public long getLong(String path, long def) {
        ensureYaml();
        return yaml.getLong(path, def);
    }

    public double getDouble(String path) {
        ensureYaml();
        return yaml.getDouble(path);
    }

    public double getDouble(String path, double def) {
        ensureYaml();
        return yaml.getDouble(path, def);
    }

    public boolean getBoolean(String path) {
        ensureYaml();
        return yaml.getBoolean(path);
    }

    public boolean getBoolean(String path, boolean def) {
        ensureYaml();
        return yaml.getBoolean(path, def);
    }

    public List<String> getStringList(String path) {
        ensureYaml();
        return yaml.getStringList(path);
    }

    public Object getObject(String path) {
        ensureYaml();
        return yaml.get(path);
    }

    public Object getObject(String path, Object def) {
        ensureYaml();
        return yaml.get(path, def);
    }

    private void ensureBound() {
        if (plugin == null || file == null) {
            throw new IllegalStateException("Config not bound — use YamlConfig.load(...) or bind(...)");
        }
    }

    private void ensureYaml() {
        if (yaml == null) {
            throw new IllegalStateException("Config YAML not loaded — reload/load first");
        }
    }

    private void saveResourceDefaults() {
        try {
            if (plugin.getResource(resourcePath) != null) {
                plugin.saveResource(resourcePath, false);
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // no embedded resource
        }
        save();
    }

    private static void applyHeader(FileConfiguration yaml, Class<?> type) {
        Header header = type.getAnnotation(Header.class);
        if (header == null || header.value().length == 0) {
            return;
        }
        yaml.options().setHeader(List.of(header.value()));
    }

    private static void readInto(YamlConfig target, ConfigurationSection section, String pathPrefix) {
        for (Field field : serializableFields(target.getClass())) {
            String key = keyOf(field);
            String path = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;
            if (!sectionContains(section, key)) {
                continue;
            }
            try {
                Object previous = field.get(target);
                Object value = readValue(field, section, key, path);
                value = mergeMaps(previous, value);
                if (value != null || !field.getType().isPrimitive()) {
                    field.set(target, value);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to read config field " + path, e);
            }
        }
    }

    private static void writeFrom(YamlConfig source, FileConfiguration yaml, String pathPrefix) {
        for (Field field : serializableFields(source.getClass())) {
            String key = keyOf(field);
            String path = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;
            try {
                Object value = field.get(source);
                writeValue(yaml, path, value);
                Comment comment = field.getAnnotation(Comment.class);
                if (comment != null && comment.value().length > 0) {
                    yaml.setComments(path, List.of(comment.value()));
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to write config field " + path, e);
            }
        }
    }

    private static Object readValue(Field field, ConfigurationSection section, String key, String fullPath)
            throws ReflectiveOperationException {
        Class<?> type = field.getType();

        if (YamlConfig.class.isAssignableFrom(type)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            YamlConfig nested = (YamlConfig) type.getDeclaredConstructor().newInstance();
            if (child != null) {
                readInto(nested, child, fullPath);
            }
            return nested;
        }

        if (type.isEnum()) {
            String raw = section.getString(key);
            if (raw == null) {
                return null;
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object constant = Enum.valueOf((Class<? extends Enum>) type, raw);
            return constant;
        }

        if (ConfigurationSerializable.class.isAssignableFrom(type)) {
            return section.get(key);
        }

        if (List.class.isAssignableFrom(type)) {
            return readList(field, section, key);
        }

        if (Map.class.isAssignableFrom(type)) {
            return readMap(field, section, key, fullPath);
        }

        if (type == String.class) {
            return section.getString(key);
        }
        if (type == int.class || type == Integer.class) {
            return section.getInt(key);
        }
        if (type == long.class || type == Long.class) {
            return section.getLong(key);
        }
        if (type == double.class || type == Double.class) {
            return section.getDouble(key);
        }
        if (type == float.class || type == Float.class) {
            return (float) section.getDouble(key);
        }
        if (type == boolean.class || type == Boolean.class) {
            return section.getBoolean(key);
        }

        return section.get(key);
    }

    private static List<?> readList(Field field, ConfigurationSection section, String key) {
        Type generic = field.getGenericType();
        if (!(generic instanceof ParameterizedType parameterized)) {
            return section.getList(key);
        }
        Type arg = parameterized.getActualTypeArguments()[0];
        if (!(arg instanceof Class<?> elementType)) {
            return section.getList(key);
        }
        if (elementType == String.class) {
            return section.getStringList(key);
        }
        if (elementType == Integer.class || elementType == int.class) {
            return section.getIntegerList(key);
        }
        if (elementType == Double.class || elementType == double.class) {
            return section.getDoubleList(key);
        }
        if (elementType == Boolean.class || elementType == boolean.class) {
            return section.getBooleanList(key);
        }
        if (elementType == Long.class || elementType == long.class) {
            return section.getLongList(key);
        }
        return section.getList(key);
    }

    private static Map<String, Object> readMap(Field field, ConfigurationSection section, String key, String fullPath)
            throws ReflectiveOperationException {
        Class<?> valueType = mapValueType(field);
        ConfigurationSection mapSection = section.getConfigurationSection(key);
        if (mapSection == null) {
            Object raw = section.get(key);
            if (raw instanceof Map<?, ?> map) {
                Map<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null) {
                        out.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                return out;
            }
            return new LinkedHashMap<>();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        if (YamlConfig.class.isAssignableFrom(valueType)) {
            for (String childKey : mapSection.getKeys(false)) {
                ConfigurationSection child = mapSection.getConfigurationSection(childKey);
                YamlConfig nested = (YamlConfig) valueType.getDeclaredConstructor().newInstance();
                String childPath = fullPath + "." + childKey;
                if (child != null) {
                    readInto(nested, child, childPath);
                }
                out.put(childKey, nested);
            }
            return out;
        }

        for (String childKey : mapSection.getKeys(false)) {
            out.put(childKey, mapSection.get(childKey));
        }
        return out;
    }

    private static Class<?> mapValueType(Field field) {
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType parameterized) {
            Type[] args = parameterized.getActualTypeArguments();
            if (args.length == 2 && args[1] instanceof Class<?> valueClass) {
                return valueClass;
            }
        }
        return Object.class;
    }

    /**
     * Keep ctor / field defaults for map keys missing in YAML; YAML values win on overlap.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object mergeMaps(Object previous, Object loaded) {
        if (!(previous instanceof Map<?, ?> prev) || !(loaded instanceof Map<?, ?> load)) {
            return loaded;
        }
        if (prev.isEmpty()) {
            return loaded;
        }
        Map out = new LinkedHashMap(prev);
        out.putAll(load);
        return out;
    }

    private static void writeValue(FileConfiguration yaml, String path, Object value) {
        if (value == null) {
            yaml.set(path, null);
            return;
        }
        if (value instanceof YamlConfig nested) {
            writeFrom(nested, yaml, path);
            return;
        }
        if (value instanceof Enum<?> enumValue) {
            yaml.set(path, enumValue.name());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                writeValue(yaml, path + "." + entry.getKey(), entry.getValue());
            }
            return;
        }
        if (value instanceof List<?> list) {
            yaml.set(path, new ArrayList<>(list));
            return;
        }
        yaml.set(path, value);
    }

    private static boolean sectionContains(ConfigurationSection section, String key) {
        return section.getKeys(false).contains(key);
    }

    private static String keyOf(Field field) {
        ConfigKey annotation = field.getAnnotation(ConfigKey.class);
        if (annotation != null && !annotation.value().isBlank()) {
            return annotation.value();
        }
        return namingStrategy(field.getDeclaringClass()).apply(field.getName());
    }

    /** {@link Naming} on the type or a superclass (up to {@link YamlConfig}). Else {@link NamingStrategy#IDENTITY}. */
    private static NamingStrategy namingStrategy(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != YamlConfig.class && current != Object.class) {
            Naming naming = current.getAnnotation(Naming.class);
            if (naming != null) {
                return naming.value();
            }
            current = current.getSuperclass();
        }
        return NamingStrategy.IDENTITY;
    }

    private static List<Field> serializableFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != YamlConfig.class && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) {
                    continue;
                }
                field.setAccessible(true);
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }
}
