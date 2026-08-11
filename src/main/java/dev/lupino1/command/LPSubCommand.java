package dev.lupino1.command;

import java.util.List;

/**
 * One subcommand = one class. Wire into a tree via {@link LPCommand#sub(LPSubCommand)}.
 *
 * <pre>{@code
 * public final class BuyCommand extends LPSubCommand {
 *     @Override public String name() { return "buy"; }
 *     @Override public String permission() { return "shop.buy"; }
 *     @Override public void execute(CommandContext ctx) { ctx.reply("buy"); }
 * }
 * }</pre>
 *
 * Nested children: override {@link #nest(LPCommand)}.
 */
public abstract class LPSubCommand {

    public abstract String name();

    public String permission() {
        return null;
    }

    public SenderType senderType() {
        return SenderType.ANY;
    }

    public String usage() {
        return "";
    }

    public String description() {
        return "";
    }

    public void execute(CommandContext ctx) {
        // leaf / default — override
    }

    public List<String> tabComplete(CommandContext ctx) {
        return List.of();
    }

    /** Register nested {@link LPSubCommand}s on this node. */
    public void nest(LPCommand command) {
        // optional
    }

    final void install(LPCommand command) {
        String perm = permission();
        if (perm != null && !perm.isEmpty()) {
            command.permission(perm);
        }
        command.senderType(senderType());
        String usage = usage();
        if (usage != null && !usage.isEmpty()) {
            command.usage(usage);
        }
        String description = description();
        if (description != null && !description.isEmpty()) {
            command.description(description);
        }
        command.execute(this::execute);
        command.tabComplete(ctx -> {
            List<String> suggestions = tabComplete(ctx);
            return suggestions == null ? List.of() : suggestions;
        });
        nest(command);
    }
}
