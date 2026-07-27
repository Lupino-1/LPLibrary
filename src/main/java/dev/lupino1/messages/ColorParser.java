package dev.lupino1.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

public final class ColorParser {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern BUKKIT_HEX_PATTERN = Pattern.compile(
            "(?i)&x&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])"
    );
    private static final Pattern AMPERSAND_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern RAW_HEX_PATTERN = Pattern.compile("(?<!<)#([A-Fa-f0-9]{6})(?![^<]*>)");
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("&([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);

    private ColorParser() {
    }

    public static Component translateColors(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        String formatted = input;
        formatted = BUKKIT_HEX_PATTERN.matcher(formatted).replaceAll("<#$1$2$3$4$5$6>");
        formatted = AMPERSAND_HEX_PATTERN.matcher(formatted).replaceAll("<#$1>");
        formatted = RAW_HEX_PATTERN.matcher(formatted).replaceAll("<#$1>");
        formatted = replaceLegacyCodes(formatted);

        return MINI_MESSAGE.deserialize(formatted);
    }

    public static String translateLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return SECTION_SERIALIZER.serialize(translateColors(input));
    }

    public static String toLegacy(Component component) {
        if (component == null) {
            return "";
        }
        return SECTION_SERIALIZER.serialize(component);
    }

    private static String replaceLegacyCodes(String text) {
        return LEGACY_CODE_PATTERN.matcher(text).replaceAll(match -> switch (match.group(1).toLowerCase().charAt(0)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<b>";
            case 'm' -> "<st>";
            case 'n' -> "<u>";
            case 'o' -> "<i>";
            case 'r' -> "<reset>";
            default -> match.group();
        });
    }
}
