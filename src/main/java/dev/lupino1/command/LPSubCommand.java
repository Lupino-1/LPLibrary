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

    /** Optional {@link MessageManager} key override for no-permission on this sub. */
    public String permissionMessageKey() {
        return null;
    }

    public SenderType senderType() {
        return SenderType.ANY;
    }

    /** Optional {@link MessageManager} key when {@link #senderType()} is {@link SenderType#PLAYER}. */
    public String playerOnlyMessageKey() {
        return null;
    }

    /** Optional {@link MessageManager} key when {@link #senderType()} is {@link SenderType#CONSOLE}. */
    public String consoleOnlyMessageKey() {
        return null;
    }

    public String usage() {
        return "";
    }

    /** Optional {@link MessageManager} key override for usage on this sub. */
    public String usageMessageKey() {
        return null;
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
        String permKey = permissionMessageKey();
        if (perm != null && !perm.isEmpty()) {
            if (permKey != null && !permKey.isEmpty()) {
                command.permission(perm, permKey);
            } else {
                command.permission(perm);
            }
        }

        SenderType type = senderType();
        String playerKey = playerOnlyMessageKey();
        String consoleKey = consoleOnlyMessageKey();
        if (type == SenderType.PLAYER) {
            if (playerKey != null && !playerKey.isEmpty()) {
                command.playerOnly(playerKey);
            } else {
                command.playerOnly();
            }
        } else if (type == SenderType.CONSOLE) {
            if (consoleKey != null && !consoleKey.isEmpty()) {
                command.consoleOnly(consoleKey);
            } else {
                command.consoleOnly();
            }
        } else {
            command.senderType(type);
        }

        String usage = usage();
        String usageKey = usageMessageKey();
        if (usage != null && !usage.isEmpty()) {
            if (usageKey != null && !usageKey.isEmpty()) {
                command.usage(usage, usageKey);
            } else {
                command.usage(usage);
            }
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
