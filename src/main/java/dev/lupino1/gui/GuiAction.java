package dev.lupino1.gui;

@FunctionalInterface
public interface GuiAction<T> {

    /**
     * Called on the player/entity thread. Do not store or use {@code event} asynchronously.
     */
    void execute(T event);
}
