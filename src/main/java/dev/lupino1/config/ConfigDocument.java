package dev.lupino1.config;

/**
 * Path-only config (no mapped fields). Use with {@link ConfigHolder}.
 *
 * <pre>{@code
 * ConfigHolder<ConfigDocument> config = ConfigHolder.create(plugin, ConfigDocument.class, "config.yml");
 * String url = config.getString("database.url");
 * ConfigurationSection db = config.get("database");
 * }</pre>
 */
public class ConfigDocument extends YamlConfig {
}
