package dev.lupino1.command;

import java.util.List;

@FunctionalInterface
public interface TabHandler {

    /**
     * @return suggestions for the current argument token (may be empty)
     */
    List<String> complete(CommandContext ctx);
}
