package dev.lupino1.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class GuiButton {

    private final ItemStack item;
    private final GuiAction<InventoryClickEvent> action;

    public GuiButton(ItemStack item, GuiAction<InventoryClickEvent> action) {
        this.item = item == null ? null : item.clone();
        this.action = action;
    }

    public ItemStack getItem() {
        return item == null ? null : item.clone();
    }

    public GuiAction<InventoryClickEvent> getAction() {
        return action;
    }
}
