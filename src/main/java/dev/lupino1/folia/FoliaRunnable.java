package dev.lupino1.folia;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public abstract class FoliaRunnable {

    
    private TaskWrapper task;


    public abstract void run(TaskWrapper task);

    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }


    public void runTaskAtEntity(Entity entity) {
        FoliaManager.runAtEntity(entity, () -> this.run(null));
    }

    public void runTaskAtLocation(Location loc) {
        FoliaManager.runAtLocation(loc, () -> this.run(null));
    }

    public void runTaskGlobal() {
        FoliaManager.runGlobal(() -> this.run(null));
    }

    public void runTaskAsync() {
        FoliaManager.runAsync(() -> this.run(null));
    }


    public void runTaskDelayedGlobal(long ticks) {
        this.task = FoliaManager.runDelayedGlobal(() -> this.run(this.task), ticks);
    }

    public void runTaskDelayedAtEntity(Entity entity, long ticks) {
        this.task = FoliaManager.runDelayedAtEntity(entity, () -> this.run(this.task), ticks);
    }

    public void runTaskDelayedAtLocation(Location loc, long ticks) {
        this.task = FoliaManager.runDelayedAtLocation(loc, () -> this.run(this.task), ticks);
    }

    public void runTaskDelayedAsync(long ticks) {
        this.task = FoliaManager.runDelayedAsync(() -> this.run(this.task), ticks);
    }

    public void runTaskTimerGlobal(long delay, long period) {
        this.task = FoliaManager.runTimerGlobal(t -> {
            this.task = t;
            this.run(t);
        }, delay, period);
    }

    public void runTaskTimerAtEntity(Entity entity, long delay, long period) {
        this.task = FoliaManager.runTimerAtEntity(entity, t -> {
            this.task = t;
            this.run(t);
        }, delay, period);
    }

    public void runTaskTimerAtLocation(Location location, long delay, long period) {
        this.task = FoliaManager.runTimerAtLocation(location, t -> {
            this.task = t;
            this.run(t);
        }, delay, period);
    }

    public void runTaskTimerAsync(long delay,long period){
        this.task = FoliaManager.runAsyncTimer(t -> {
            this.task = t;
            this.run(t);
        },delay,period);

    }
}
