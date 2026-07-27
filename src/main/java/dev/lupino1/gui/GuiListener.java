package dev.lupino1.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

final class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof GuiHolder holder)) {
            return;
        }

        if (!GuiManager.isValidSession(player, holder)) {
            event.setCancelled(true);
            return;
        }

        Gui gui = holder.getGui();
        gui.handleDefault(event);

        Inventory clicked = event.getClickedInventory();

        if (clicked == null || clicked.equals(top)) {
            gui.handleTopClick(event);
            if (clicked != null) {
                int slot = event.getSlot();
                if (slot >= 0 && slot < gui.getSize()) {
                    gui.handleSlotClick(event, slot);
                }
            }
            return;
        }

        gui.handleBottomClick(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof GuiHolder holder)) {
            return;
        }

        if (!GuiManager.isValidSession(player, holder)) {
            event.setCancelled(true);
            return;
        }

        if (!holder.getGui().isCancelDrag()) {
            return;
        }

        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) {
            return;
        }
        if (!GuiManager.isValidSession(player, holder)) {
            return;
        }
        holder.getGui().handleClose(event);
        GuiManager.clear(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        GuiManager.clear(event.getPlayer());
    }
}
