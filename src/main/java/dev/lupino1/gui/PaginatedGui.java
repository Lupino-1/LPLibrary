package dev.lupino1.gui;

import dev.lupino1.folia.FoliaManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Paginated GUI.
 * <ul>
 *   <li>{@link #setItem(int, ItemStack)} — fixed slots (nav, border…), not counted as page content</li>
 *   <li>{@link #addPageItem(ItemStack, GuiAction)} — counted items placed into remaining slots</li>
 * </ul>
 * Content slots = all slots minus fixed ones. Page is per-player (session).
 */
public class PaginatedGui extends Gui {

    private final List<GuiButton> pageItems = new CopyOnWriteArrayList<>();

    public PaginatedGui(JavaPlugin plugin, int rows, Component title) {
        super(plugin, rows, title);
    }

    public PaginatedGui(JavaPlugin plugin, int rows, String title) {
        super(plugin, rows, title);
    }

    public PaginatedGui addPageItem(ItemStack item) {
        return addPageItem(item, null);
    }

    public PaginatedGui addPageItem(ItemStack item, GuiAction<InventoryClickEvent> action) {
        if (item == null) {
            return this;
        }
        pageItems.add(new GuiButton(item, action));
        return this;
    }

    public PaginatedGui addPageItem(GuiButton button) {
        if (button != null && button.getItem() != null) {
            pageItems.add(button);
        }
        return this;
    }

    public PaginatedGui addPageItems(Collection<GuiButton> buttons) {
        if (buttons != null) {
            for (GuiButton button : buttons) {
                addPageItem(button);
            }
        }
        return this;
    }

    public PaginatedGui clearPageItems() {
        pageItems.clear();
        return this;
    }

    public List<GuiButton> getPageItems() {
        return List.copyOf(pageItems);
    }

    public int getPageItemCount() {
        return pageItems.size();
    }

    /**
     * Slots free for page content (inventory size minus fixed {@link #setItem} slots).
     */
    public List<Integer> getContentSlots() {
        List<Integer> slots = new ArrayList<>();
        int size = getSize();
        for (int i = 0; i < size; i++) {
            if (!isFixedSlot(i)) {
                slots.add(i);
            }
        }
        return slots;
    }

    public int getContentSlotCount() {
        return getContentSlots().size();
    }

    /**
     * Max page index + 1. Always at least 1.
     */
    public int getMaxPages() {
        int perPage = getContentSlotCount();
        if (perPage <= 0) {
            return 1;
        }
        if (pageItems.isEmpty()) {
            return 1;
        }
        return (pageItems.size() + perPage - 1) / perPage;
    }

    /**
     * Current page for this player (0-based). {@code 0} if GUI not open for them.
     */
    public int getPage(Player player) {
        GuiHolder holder = holderOf(player);
        return holder == null ? 0 : holder.getPage();
    }

    /**
     * Human page number (1-based) for display.
     */
    public int getPageNumber(Player player) {
        return getPage(player) + 1;
    }

    public boolean hasNext(Player player) {
        return getPage(player) < getMaxPages() - 1;
    }

    public boolean hasPrevious(Player player) {
        return getPage(player) > 0;
    }

    public void next(Player player) {
        if (player == null) {
            return;
        }
        FoliaManager.runAtEntity(player, () -> {
            GuiHolder holder = holderOf(player);
            if (holder == null) {
                return;
            }
            int page = holder.getPage();
            if (page < getMaxPages() - 1) {
                holder.setPage(page + 1);
                populate(holder.getInventory(), player, holder);
            }
        });
    }

    public void previous(Player player) {
        if (player == null) {
            return;
        }
        FoliaManager.runAtEntity(player, () -> {
            GuiHolder holder = holderOf(player);
            if (holder == null) {
                return;
            }
            int page = holder.getPage();
            if (page > 0) {
                holder.setPage(page - 1);
                populate(holder.getInventory(), player, holder);
            }
        });
    }

    public void setPage(Player player, int page) {
        if (player == null) {
            return;
        }
        FoliaManager.runAtEntity(player, () -> {
            GuiHolder holder = holderOf(player);
            if (holder == null) {
                return;
            }
            int max = getMaxPages();
            holder.setPage(Math.max(0, Math.min(page, max - 1)));
            populate(holder.getInventory(), player, holder);
        });
    }

    /**
     * Re-render open inventory (e.g. after changing page items).
     */
    @Override
    protected void updateNow(Player player) {
        GuiHolder holder = holderOf(player);
        if (holder == null) {
            return;
        }
        int max = getMaxPages();
        if (holder.getPage() >= max) {
            holder.setPage(max - 1);
        }
        populate(holder.getInventory(), player, holder);
    }

    @Override
    protected void populate(Inventory inventory, Player player, GuiHolder holder) {
        inventory.clear();

        for (var entry : fixedItems().entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().clone());
        }

        List<Integer> contentSlots = getContentSlots();
        if (contentSlots.isEmpty()) {
            applyTitle(player, holder);
            return;
        }

        int page = holder.getPage();
        int max = getMaxPages();
        if (page >= max) {
            page = max - 1;
            holder.setPage(page);
        }

        int start = page * contentSlots.size();
        for (int i = 0; i < contentSlots.size(); i++) {
            int itemIndex = start + i;
            if (itemIndex >= pageItems.size()) {
                break;
            }
            GuiButton button = pageItems.get(itemIndex);
            ItemStack stack = button.getItem();
            if (stack != null) {
                inventory.setItem(contentSlots.get(i), stack);
            }
        }

        applyTitle(player, holder);
    }

    @Override
    protected int titlePageNumber(Player player, GuiHolder holder) {
        return (holder == null ? 0 : holder.getPage()) + 1;
    }

    @Override
    protected int titleMaxPages(Player player, GuiHolder holder) {
        return getMaxPages();
    }

    @Override
    protected int titlePageIndex(Player player, GuiHolder holder) {
        return holder == null ? 0 : holder.getPage();
    }

    @Override
    protected void handleContentClick(InventoryClickEvent event, int slot) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GuiHolder holder)) {
            return;
        }
        if (holder.getGui() != this) {
            return;
        }

        List<Integer> contentSlots = getContentSlots();
        int indexInPage = contentSlots.indexOf(slot);
        if (indexInPage < 0) {
            return;
        }

        int itemIndex = holder.getPage() * contentSlots.size() + indexInPage;
        if (itemIndex < 0 || itemIndex >= pageItems.size()) {
            return;
        }

        GuiAction<InventoryClickEvent> action = pageItems.get(itemIndex).getAction();
        if (action != null) {
            action.execute(event);
        }
    }
}
