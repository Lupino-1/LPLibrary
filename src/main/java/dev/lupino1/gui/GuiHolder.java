package dev.lupino1.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class GuiHolder implements InventoryHolder {

    private final Gui gui;
    private final UUID sessionId;
    private int page;

    GuiHolder(Gui gui, UUID sessionId) {
        this.gui = gui;
        this.sessionId = sessionId;
    }

    public Gui getGui() {
        return gui;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int getPage() {
        return page;
    }

    void setPage(int page) {
        this.page = Math.max(0, page);
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
