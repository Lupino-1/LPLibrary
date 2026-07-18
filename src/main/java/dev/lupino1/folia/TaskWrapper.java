package dev.lupino1.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

public class TaskWrapper {
    private ScheduledTask foliaTask;
    private BukkitTask bukkitTask;

    public TaskWrapper(ScheduledTask foliaTask) { this.foliaTask = foliaTask; }
    public TaskWrapper(BukkitTask bukkitTask) { this.bukkitTask = bukkitTask; }

    public void cancel() {
        if (foliaTask != null) foliaTask.cancel();
        if (bukkitTask != null) bukkitTask.cancel();
    }

    public boolean isCancelled() {
        if (foliaTask != null) return foliaTask.isCancelled();
        if (bukkitTask != null) return bukkitTask.isCancelled();
        return true;
    }
}