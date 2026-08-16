package dev.lupino1.config.velocity;

/**
 * Path-only config (no mapped fields). Use with {@link VConfigHolder}.
 *
 * <pre>{@code
 * VConfigHolder<VConfigDocument> config =
 *     VConfigHolder.create(dataDir, getClass(), logger, VConfigDocument.class, "config.yml");
 * String url = config.getString("database.url");
 * VConfigSection db = config.get("database");
 * }</pre>
 */
public class VConfigDocument extends VYamlConfig {
}
