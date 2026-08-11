package dev.lupino1.config;

/**
 * How Java camelCase field names map to YAML keys when {@link ConfigKey} is absent.
 */
public enum NamingStrategy {

    /** Field name as-is ({@code maxBlocks} → {@code maxBlocks}). */
    IDENTITY {
        @Override
        public String apply(String fieldName) {
            return fieldName;
        }
    },

    /** {@code maxBlocks} → {@code max-blocks}. */
    KEBAB {
        @Override
        public String apply(String fieldName) {
            return camelTo(fieldName, '-');
        }
    },

    /** {@code maxBlocks} → {@code max_blocks}. */
    SNAKE {
        @Override
        public String apply(String fieldName) {
            return camelTo(fieldName, '_');
        }
    };

    public abstract String apply(String fieldName);

    private static String camelTo(String name, char separator) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder out = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                boolean prevLower = i > 0 && Character.isLowerCase(name.charAt(i - 1));
                boolean nextLower = i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1));
                if (i > 0 && (prevLower || nextLower)) {
                    out.append(separator);
                }
                out.append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
