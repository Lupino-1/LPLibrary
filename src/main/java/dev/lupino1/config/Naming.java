package dev.lupino1.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level YAML key naming for mapped fields.
 * {@link ConfigKey} on a field always wins. Default without this annotation = {@link NamingStrategy#IDENTITY}.
 *
 * <pre>{@code
 * @Naming(NamingStrategy.KEBAB)
 * public class PluginSettings extends YamlConfig {
 *     private int maxBlocks; // max-blocks
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Naming {
    NamingStrategy value();
}
