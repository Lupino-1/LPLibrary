package dev.lupino1.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FoliaManager {

    private static Plugin plugin;
    private static boolean isFolia;

    public static void init(Plugin pl){

        plugin = pl;

        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

    }
    public static void runAtEntity(Entity entity, Runnable runnable) {
        if (isFolia) {
            entity.getScheduler().run(plugin, task -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runAtLocation(Location loc, Runnable runnable) {
        if (isFolia) {
            Bukkit.getRegionScheduler().execute(plugin, loc, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runGlobal(Runnable runnable) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }
    public static void runAsync(Runnable runnable) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public static TaskWrapper runDelayedGlobal(Runnable runnable, long ticks) {
        if (isFolia) {
            return new TaskWrapper( Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), ticks));
        } else {
            return new TaskWrapper(  Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks));
        }
    }

    public static TaskWrapper runDelayedAtEntity(Entity entity, Runnable runnable, long ticks) {
        if (isFolia) {
            return new TaskWrapper( entity.getScheduler().runDelayed(plugin, task -> runnable.run(), null, ticks));
        } else {
            return new TaskWrapper( Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks));

        }

    }

    public static TaskWrapper runDelayedAtLocation(Location loc, Runnable runnable, long ticks) {
        if (isFolia) {

            return new TaskWrapper( Bukkit.getRegionScheduler().runDelayed(plugin, loc, task -> runnable.run(), ticks));
        } else {
            return new TaskWrapper( Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks));

        }
    }
    public static TaskWrapper runDelayedAsync(Runnable runnable, long ticks) {
        if (isFolia) {
            return new TaskWrapper(Bukkit.getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), ticks * 50, TimeUnit.MILLISECONDS));
        } else {
            return new TaskWrapper(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, ticks));


        }
    }

    public static TaskWrapper runTimerGlobal(Consumer<TaskWrapper> action, long delay, long period) {
        if (isFolia) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,
                    t -> action.accept(new TaskWrapper(t)), delay, period);
            return new TaskWrapper(task);
        } else {
            TaskWrapper[] wrapper = new TaskWrapper[1];
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (wrapper[0] != null) action.accept(wrapper[0]);
            }, delay, period);
            wrapper[0] = new TaskWrapper(bukkitTask);
            return wrapper[0];
        }
    }

    public static TaskWrapper runTimerAtEntity(Entity entity, Consumer<TaskWrapper> action, long delay, long period) {
        if (isFolia) {
            ScheduledTask task = entity.getScheduler().runAtFixedRate(plugin,
                    t -> action.accept(new TaskWrapper(t)), null, delay, period);
            return new TaskWrapper(task);
        } else {
            TaskWrapper[] wrapper = new TaskWrapper[1];
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (wrapper[0] != null) action.accept(wrapper[0]);
            }, delay, period);
            wrapper[0] = new TaskWrapper(bukkitTask);
            return wrapper[0];
        }
    }

    public static TaskWrapper runTimerAtLocation(Location loc, Consumer<TaskWrapper> action, long delay, long period) {
        if (isFolia) {
            ScheduledTask task = Bukkit.getRegionScheduler().runAtFixedRate(plugin, loc,
                    t -> action.accept(new TaskWrapper(t)), delay, period);
            return new TaskWrapper(task);
        } else {
            TaskWrapper[] wrapper = new TaskWrapper[1];
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (wrapper[0] != null) action.accept(wrapper[0]);
            }, delay, period);
            wrapper[0] = new TaskWrapper(bukkitTask);
            return wrapper[0];
        }
    }

    public static TaskWrapper runAsyncTimer(Consumer<TaskWrapper> action, long delayTicks, long periodTicks) {
        if (isFolia) {
            ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin,
                    t -> action.accept(new TaskWrapper(t)),
                    delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
            return new TaskWrapper(task);
        } else {
            TaskWrapper[] wrapper = new TaskWrapper[1];
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (wrapper[0] != null) action.accept(wrapper[0]);
            }, delayTicks, periodTicks);
            wrapper[0] = new TaskWrapper(bukkitTask);
            return wrapper[0];
        }
    }
}