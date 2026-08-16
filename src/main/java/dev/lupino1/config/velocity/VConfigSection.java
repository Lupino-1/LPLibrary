package dev.lupino1.config.velocity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Nested YAML map view (Velocity stand-in for Bukkit {@code ConfigurationSection}).
 * Path uses {@code .} separators. Mutating {@link #set} updates the backing map in RAM.
 */
public final class VConfigSection {

    private final Map<String, Object> root;
    private final String prefix;

    VConfigSection(Map<String, Object> root) {
        this(root, "");
    }

    private VConfigSection(Map<String, Object> root, String prefix) {
        this.root = Objects.requireNonNull(root, "root");
        this.prefix = prefix == null ? "" : prefix;
    }

    /** Backing map for this section (not a copy). */
    public Map<String, Object> asMap() {
        return root;
    }

    public String getCurrentPath() {
        return prefix;
    }

    public Set<String> getKeys(boolean deep) {
        if (!deep) {
            return Collections.unmodifiableSet(root.keySet());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        flattenKeys("", root, out);
        return Collections.unmodifiableSet(out.keySet());
    }

    public boolean contains(String path) {
        return get(path) != null || isSection(path);
    }

    public Object get(String path) {
        if (path == null || path.isEmpty()) {
            return root;
        }
        return resolve(path);
    }

    public Object get(String path, Object def) {
        Object value = get(path);
        return value != null ? value : def;
    }

    public VConfigSection getSection(String path) {
        if (path == null || path.isEmpty()) {
            return this;
        }
        Object value = resolve(path);
        if (value instanceof Map<?, ?> map) {
            return new VConfigSection(castMap(map), join(prefix, path));
        }
        return null;
    }

    public boolean isSection(String path) {
        return resolve(path) instanceof Map<?, ?>;
    }

    public void set(String path, Object value) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map<?, ?>)) {
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(parts[i], created);
                current = created;
            } else {
                current = castMap(next);
            }
        }
        if (value == null) {
            current.remove(parts[parts.length - 1]);
        } else {
            current.put(parts[parts.length - 1], value);
        }
    }

    public String getString(String path) {
        Object value = get(path);
        return value == null ? null : String.valueOf(value);
    }

    public String getString(String path, String def) {
        String value = getString(path);
        return value != null ? value : def;
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int def) {
        Number number = asNumber(get(path));
        return number != null ? number.intValue() : def;
    }

    public long getLong(String path) {
        return getLong(path, 0L);
    }

    public long getLong(String path, long def) {
        Number number = asNumber(get(path));
        return number != null ? number.longValue() : def;
    }

    public double getDouble(String path) {
        return getDouble(path, 0D);
    }

    public double getDouble(String path, double def) {
        Number number = asNumber(get(path));
        return number != null ? number.doubleValue() : def;
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean def) {
        Object value = get(path);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return def;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public List<String> getStringList(String path) {
        Object value = get(path);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object item : list) {
            out.add(item == null ? "" : String.valueOf(item));
        }
        return out;
    }

    public List<?> getList(String path) {
        Object value = get(path);
        if (value instanceof List<?> list) {
            return list;
        }
        return null;
    }

    private Object resolve(String path) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Number asNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> castMap(Object map) {
        return (Map<String, Object>) map;
    }

    static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                out.put(String.valueOf(entry.getKey()), copyMap(nested));
            } else if (value instanceof List<?> list) {
                out.put(String.valueOf(entry.getKey()), new ArrayList<>(list));
            } else {
                out.put(String.valueOf(entry.getKey()), value);
            }
        }
        return out;
    }

    private static void flattenKeys(String prefix, Map<?, ?> map, Map<String, Object> out) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            out.put(key, value);
            if (value instanceof Map<?, ?> nested) {
                flattenKeys(key, nested, out);
            }
        }
    }

    private static String join(String prefix, String path) {
        if (prefix == null || prefix.isEmpty()) {
            return path;
        }
        return prefix + "." + path;
    }
}
