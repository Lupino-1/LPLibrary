package dev.lupino1.config.velocity;

import dev.lupino1.config.ConfigKey;
import dev.lupino1.config.Header;
import dev.lupino1.config.Naming;
import dev.lupino1.config.NamingStrategy;
import dev.lupino1.messages.ColorParser;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * YAML config in memory (SnakeYAML). Velocity counterpart of Paper {@link dev.lupino1.config.YamlConfig}.
 * <p>
 * Reads hit the RAM snapshot — not disk. Prefer {@link VConfigHolder} ({@code reload()} swaps a new instance).
 * Annotations {@link Header}/{@link ConfigKey}/{@link Naming} are shared with Paper.
 * Field comments ({@link dev.lupino1.config.Comment}) are not written back (SnakeYAML).
 */
public abstract class VYamlConfig {

    private transient Path dataDirectory;
    private transient Path file;
    private transient String resourcePath;
    private transient Class<?> pluginClass;
    private transient Logger logger;
    /** In-memory YAML root map; path getters read this. */
    private transient Map<String, Object> root;

    public static <T extends VYamlConfig> T load(
            Path dataDirectory,
            Class<?> pluginClass,
            Logger logger,
            Class<T> type,
            String fileName
    ) {
        return load(dataDirectory, pluginClass, logger, type, fileName, true);
    }

    /**
     * @param updateMissing if {@code true}, writes missing default keys back to disk after load
     */
    public static <T extends VYamlConfig> T load(
            Path dataDirectory,
            Class<?> pluginClass,
            Logger logger,
            Class<T> type,
            String fileName,
            boolean updateMissing
    ) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(pluginClass, "pluginClass");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fileName, "fileName");

        T instance;
        try {
            instance = type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Config " + type.getName() + " needs a public no-args constructor", e);
        }
        instance.bind(dataDirectory, pluginClass, logger, fileName);
        instance.reload(updateMissing);
        return instance;
    }

    public void bind(Path dataDirectory, Class<?> pluginClass, Logger logger, String fileName) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.pluginClass = Objects.requireNonNull(pluginClass, "pluginClass");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.resourcePath = fileName.replace('\\', '/');
        this.file = dataDirectory.resolve(this.resourcePath);
    }

    /**
     * Re-read this instance from disk (mutates fields in place).
     * Prefer {@link VConfigHolder#reload()}.
     */
    public void reload() {
        reload(true);
    }

    /**
     * @param updateMissing if {@code true}, writes missing default keys back to disk after load
     */
    public void reload(boolean updateMissing) {
        ensureBound();
        if (!Files.exists(file)) {
            try {
                Path parent = file.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            } catch (IOException e) {
                logger.error("[Config] Failed to create parent for {}", resourcePath, e);
            }
            saveResourceDefaults();
        }

        this.root = loadRootMap();
        readInto(this, this.root, "");

        if (updateMissing) {
            save();
        }
    }

    /**
     * Writes mapped fields into the in-memory map (preserves unknown keys), then to disk.
     */
    public void save() {
        ensureBound();
        if (root == null) {
            root = new LinkedHashMap<>();
        }
        writeFrom(this, root, "");

        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            options.setIndent(2);
            Yaml yaml = new Yaml(options);

            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writeHeader(writer, getClass());
                yaml.dump(root, writer);
            }
        } catch (IOException e) {
            logger.error("[Config] Failed to save {}", resourcePath, e);
        }
    }

    public Path file() {
        return file;
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    /** Root in-memory YAML (RAM). */
    public VConfigSection raw() {
        ensureRoot();
        return new VConfigSection(root);
    }

    /**
     * Section at {@code path}, or {@code null} if missing / not a section.
     * Empty / null path → root. Reads RAM only.
     */
    public VConfigSection get(String path) {
        ensureRoot();
        if (path == null || path.isEmpty()) {
            return new VConfigSection(root);
        }
        return new VConfigSection(root).getSection(path);
    }

    public boolean contains(String path) {
        ensureRoot();
        return new VConfigSection(root).contains(path);
    }

    public String getString(String path) {
        ensureRoot();
        return new VConfigSection(root).getString(path);
    }

    public String getString(String path, String def) {
        ensureRoot();
        return new VConfigSection(root).getString(path, def);
    }

    /**
     * {@link #getString(String)} then Map {@code %key%} +
     * {@link ColorParser#translateLegacy(String)} ({@code &}/hex/MiniMessage → {@code §}). No PAPI.
     */
    public String getParsedString(String path, Map<String, ?> placeholders) {
        return colorLegacy(applyMap(getString(path), placeholders));
    }

    public String getParsedString(String path, String def, Map<String, ?> placeholders) {
        return colorLegacy(applyMap(getString(path, def), placeholders));
    }

    private static String colorLegacy(String parsed) {
        return parsed == null ? null : ColorParser.translateLegacy(parsed);
    }

    /**
     * Same as {@link #getParsedString(String, Map)} but {@link ColorParser#translateColors(String)}. No PAPI.
     */
    public Component getParsedComponent(String path, Map<String, ?> placeholders) {
        return ColorParser.translateColors(applyMap(getString(path), placeholders));
    }

    public Component getParsedComponent(String path, String def, Map<String, ?> placeholders) {
        return ColorParser.translateColors(applyMap(getString(path, def), placeholders));
    }

    public int getInt(String path) {
        ensureRoot();
        return new VConfigSection(root).getInt(path);
    }

    public int getInt(String path, int def) {
        ensureRoot();
        return new VConfigSection(root).getInt(path, def);
    }

    public long getLong(String path) {
        ensureRoot();
        return new VConfigSection(root).getLong(path);
    }

    public long getLong(String path, long def) {
        ensureRoot();
        return new VConfigSection(root).getLong(path, def);
    }

    public double getDouble(String path) {
        ensureRoot();
        return new VConfigSection(root).getDouble(path);
    }

    public double getDouble(String path, double def) {
        ensureRoot();
        return new VConfigSection(root).getDouble(path, def);
    }

    public boolean getBoolean(String path) {
        ensureRoot();
        return new VConfigSection(root).getBoolean(path);
    }

    public boolean getBoolean(String path, boolean def) {
        ensureRoot();
        return new VConfigSection(root).getBoolean(path, def);
    }

    public List<String> getStringList(String path) {
        ensureRoot();
        return new VConfigSection(root).getStringList(path);
    }

    public Object getObject(String path) {
        ensureRoot();
        return new VConfigSection(root).get(path);
    }

    public Object getObject(String path, Object def) {
        ensureRoot();
        return new VConfigSection(root).get(path, def);
    }

    private void ensureBound() {
        if (dataDirectory == null || file == null || pluginClass == null || logger == null) {
            throw new IllegalStateException("Config not bound — use VYamlConfig.load(...) or bind(...)");
        }
    }

    private void ensureRoot() {
        if (root == null) {
            throw new IllegalStateException("Config YAML not loaded — reload/load first");
        }
    }

    private Map<String, Object> loadRootMap() {
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try (InputStream in = Files.newInputStream(file)) {
            Object loaded = new Yaml().load(in);
            if (loaded instanceof Map<?, ?> map) {
                return VConfigSection.copyMap(map);
            }
            return new LinkedHashMap<>();
        } catch (IOException e) {
            logger.error("[Config] Failed to load {}", resourcePath, e);
            return new LinkedHashMap<>();
        }
    }

    private void saveResourceDefaults() {
        try (InputStream in = pluginClass.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in != null) {
                Files.copy(in, file);
                return;
            }
        } catch (IOException e) {
            logger.warn("[Config] Failed to copy default {}", resourcePath, e);
        }
        save();
    }

    private static void writeHeader(BufferedWriter writer, Class<?> type) throws IOException {
        Header header = type.getAnnotation(Header.class);
        if (header == null || header.value().length == 0) {
            return;
        }
        for (String line : header.value()) {
            writer.write("# ");
            writer.write(line);
            writer.newLine();
        }
        writer.newLine();
    }

    private static void readInto(VYamlConfig target, Map<String, Object> section, String pathPrefix) {
        for (Field field : serializableFields(target.getClass())) {
            String key = keyOf(field);
            String path = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;
            if (!section.containsKey(key)) {
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

    private static void writeFrom(VYamlConfig source, Map<String, Object> section, String pathPrefix) {
        for (Field field : serializableFields(source.getClass())) {
            String key = keyOf(field);
            String path = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;
            try {
                Object value = field.get(source);
                writeValue(section, key, value);
                // @Comment ignored on Velocity (SnakeYAML dump)
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to write config field " + path, e);
            }
        }
    }

    private static Object readValue(Field field, Map<String, Object> section, String key, String fullPath)
            throws ReflectiveOperationException {
        Class<?> type = field.getType();
        Object raw = section.get(key);

        if (VYamlConfig.class.isAssignableFrom(type)) {
            VYamlConfig nested = (VYamlConfig) type.getDeclaredConstructor().newInstance();
            if (raw instanceof Map<?, ?> map) {
                readInto(nested, VConfigSection.castMap(VConfigSection.copyMap(map)), fullPath);
            }
            return nested;
        }

        if (type.isEnum()) {
            if (raw == null) {
                return null;
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object constant = Enum.valueOf((Class<? extends Enum>) type, String.valueOf(raw));
            return constant;
        }

        if (List.class.isAssignableFrom(type)) {
            return readList(field, raw);
        }

        if (Map.class.isAssignableFrom(type)) {
            return readMap(field, raw, fullPath);
        }

        if (type == String.class) {
            return raw == null ? null : String.valueOf(raw);
        }
        if (type == int.class || type == Integer.class) {
            return asInt(raw);
        }
        if (type == long.class || type == Long.class) {
            return asLong(raw);
        }
        if (type == double.class || type == Double.class) {
            return asDouble(raw);
        }
        if (type == float.class || type == Float.class) {
            Double d = asDouble(raw);
            return d == null ? null : d.floatValue();
        }
        if (type == boolean.class || type == Boolean.class) {
            return asBoolean(raw);
        }

        return raw;
    }

    private static List<?> readList(Field field, Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        Type generic = field.getGenericType();
        if (!(generic instanceof ParameterizedType parameterized)) {
            return new ArrayList<>(list);
        }
        Type arg = parameterized.getActualTypeArguments()[0];
        if (!(arg instanceof Class<?> elementType)) {
            return new ArrayList<>(list);
        }
        if (elementType == String.class) {
            List<String> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(item == null ? "" : String.valueOf(item));
            }
            return out;
        }
        if (elementType == Integer.class || elementType == int.class) {
            List<Integer> out = new ArrayList<>(list.size());
            for (Object item : list) {
                Integer n = asInt(item);
                if (n != null) {
                    out.add(n);
                }
            }
            return out;
        }
        if (elementType == Double.class || elementType == double.class) {
            List<Double> out = new ArrayList<>(list.size());
            for (Object item : list) {
                Double n = asDouble(item);
                if (n != null) {
                    out.add(n);
                }
            }
            return out;
        }
        if (elementType == Boolean.class || elementType == boolean.class) {
            List<Boolean> out = new ArrayList<>(list.size());
            for (Object item : list) {
                Boolean b = asBoolean(item);
                if (b != null) {
                    out.add(b);
                }
            }
            return out;
        }
        if (elementType == Long.class || elementType == long.class) {
            List<Long> out = new ArrayList<>(list.size());
            for (Object item : list) {
                Long n = asLong(item);
                if (n != null) {
                    out.add(n);
                }
            }
            return out;
        }
        return new ArrayList<>(list);
    }

    private static Map<String, Object> readMap(Field field, Object raw, String fullPath)
            throws ReflectiveOperationException {
        Class<?> valueType = mapValueType(field);
        if (!(raw instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        if (VYamlConfig.class.isAssignableFrom(valueType)) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String childKey = String.valueOf(entry.getKey());
                VYamlConfig nested = (VYamlConfig) valueType.getDeclaredConstructor().newInstance();
                String childPath = fullPath + "." + childKey;
                if (entry.getValue() instanceof Map<?, ?> childMap) {
                    readInto(nested, VConfigSection.castMap(VConfigSection.copyMap(childMap)), childPath);
                }
                out.put(childKey, nested);
            }
            return out;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
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

    private static void writeValue(Map<String, Object> section, String key, Object value) {
        if (value == null) {
            section.remove(key);
            return;
        }
        if (value instanceof VYamlConfig nested) {
            Map<String, Object> child = section.get(key) instanceof Map<?, ?> existing
                    ? VConfigSection.castMap(existing)
                    : new LinkedHashMap<>();
            if (!(section.get(key) instanceof Map<?, ?>)) {
                section.put(key, child);
            }
            writeFrom(nested, child, key);
            return;
        }
        if (value instanceof Enum<?> enumValue) {
            section.put(key, enumValue.name());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> child = section.get(key) instanceof Map<?, ?> existing
                    ? VConfigSection.castMap(existing)
                    : new LinkedHashMap<>();
            if (!(section.get(key) instanceof Map<?, ?>)) {
                section.put(key, child);
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                writeValue(child, String.valueOf(entry.getKey()), entry.getValue());
            }
            return;
        }
        if (value instanceof List<?> list) {
            section.put(key, new ArrayList<>(list));
            return;
        }
        section.put(key, value);
    }

    private static String keyOf(Field field) {
        ConfigKey annotation = field.getAnnotation(ConfigKey.class);
        if (annotation != null && !annotation.value().isBlank()) {
            return annotation.value();
        }
        return namingStrategy(field.getDeclaringClass()).apply(field.getName());
    }

    /** {@link Naming} on the type or a superclass (up to {@link VYamlConfig}). Else {@link NamingStrategy#IDENTITY}. */
    private static NamingStrategy namingStrategy(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != VYamlConfig.class && current != Object.class) {
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
        while (current != null && current != VYamlConfig.class && current != Object.class) {
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

    private static String applyMap(String raw, Map<String, ?> placeholders) {
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

    private static Integer asInt(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return (int) Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long asLong(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return (long) Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double asDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean asBoolean(Object raw) {
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }
}
