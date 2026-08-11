package dev.lupino1.command;

@FunctionalInterface
public interface CommandHandler {

    void execute(CommandContext ctx);
}
