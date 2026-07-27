package dev.lupino1.gui;

import dev.lupino1.folia.FoliaManager;
import dev.lupino1.messages.ColorParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Gui {

    private final JavaPlugin plugin;
    private final int rows;
    /** Adventure title source (placeholders as literal text). Used when {@link #titleTemplate} is null. */
    private volatile Component title;
    /** MiniMessage/legacy string source. When set, preferred over {@link #title} for resolving. */
    private volatile String titleTemplate;
    private final Map<Integer, ItemStack> items = new ConcurrentHashMap<>();
    private final Map<Integer, GuiAction<InventoryClickEvent>> actions = new ConcurrentHashMap<>();

    private volatile GuiAction<Player> openAction;
    private volatile GuiAction<InventoryCloseEvent> closeAction;
    private volatile GuiAction<InventoryClickEvent> defaultAction = event -> event.setCancelled(true);
    private volatile GuiAction<InventoryClickEvent> topClickAction;
    private volatile GuiAction<InventoryClickEvent> bottomClickAction;
    private volatile boolean cancelDrag = true;

    public Gui(JavaPlugin plugin, int rows, Component title) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("rows must be 1-6");
        }
        this.plugin = plugin;
        this.rows = rows;
        this.title = title == null ? Component.empty() : title;
        this.titleTemplate = null;
    }

    public Gui(JavaPlugin plugin, int rows, String title) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("rows must be 1-6");
        }
        this.plugin = plugin;
        this.rows = rows;
        this.titleTemplate = title == null ? "" : title;
        this.title = ColorParser.translateColors(this.titleTemplate);
    }

    public int getRows() {
        return rows;
    }

    public int getSize() {
        return rows * 9;
    }

    /**
     * Unresolved title source (Component constructor / {@link #setTitle(Component)}).
     */
    public Component getTitle() {
        return title;
    }

    public String getTitleTemplate() {
        return titleTemplate;
    }

    /**
     * Adventure title. Placeholders as literal text still work:
     * {@code %page%}, {@code %max%}, {@code %max_pages%}, {@code %page_index%}.
     */
    public Gui setTitle(Component title) {
        this.title = title == null ? Component.empty() : title;
        this.titleTemplate = null;
        return this;
    }

    /**
     * String title (MiniMessage/legacy via {@link ColorParser}). Same placeholders as Component.
     */
    public Gui setTitle(String titleTemplate) {
        this.titleTemplate = titleTemplate == null ? "" : titleTemplate;
        this.title = ColorParser.translateColors(this.titleTemplate);
        return this;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Gui setOpenAction(GuiAction<Player> action) {
        this.openAction = action;
        return this;
    }

    public GuiAction<Player> getOpenAction() {
        return openAction;
    }

    public Gui setCloseAction(GuiAction<InventoryCloseEvent> action) {
        this.closeAction = action;
        return this;
    }

    public GuiAction<InventoryCloseEvent> getCloseAction() {
        return closeAction;
    }

    /**
     * Runs for every click while this GUI is open (top + player inv), before top/bottom/slot actions.
     * Default: {@code event.setCancelled(true)}. Pass {@code null} to clear (e.g. storage menus).
     */
    public Gui setDefaultAction(GuiAction<InventoryClickEvent> action) {
        this.defaultAction = action;
        return this;
    }

    public GuiAction<InventoryClickEvent> getDefaultAction() {
        return defaultAction;
    }

    public Gui setTopClickAction(GuiAction<InventoryClickEvent> action) {
        this.topClickAction = action;
        return this;
    }

    public GuiAction<InventoryClickEvent> getTopClickAction() {
        return topClickAction;
    }

    public Gui setBottomClickAction(GuiAction<InventoryClickEvent> action) {
        this.bottomClickAction = action;
        return this;
    }

    public GuiAction<InventoryClickEvent> getBottomClickAction() {
        return bottomClickAction;
    }

    /**
     * When {@code true}, cancels drags that touch the top (GUI) inventory.
     * Default: {@code true}. Set {@code false} for storage / enderchest-style menus.
     */
    public Gui setCancelDrag(boolean cancelDrag) {
        this.cancelDrag = cancelDrag;
        return this;
    }

    public boolean isCancelDrag() {
        return cancelDrag;
    }

    public Gui setItem(int slot, ItemStack item) {
        return setItem(slot, item, null);
    }

    public Gui setItem(int slot, ItemStack item, GuiAction<InventoryClickEvent> action) {
        checkSlot(slot);
        if (item == null) {
            items.remove(slot);
        } else {
            items.put(slot, item.clone());
        }
        if (action == null) {
            actions.remove(slot);
        } else {
            actions.put(slot, action);
        }
        return this;
    }

    public Gui setItem(int slot, GuiButton button) {
        if (button == null) {
            return setItem(slot, null, null);
        }
        return setItem(slot, button.getItem(), button.getAction());
    }

    /**
     * Multi-slot via tokens: {@code List.of("0-8", "12", "14")} (each entry slot or range).
     * Also accepts one string {@code "0-8,12,14"} / {@code "[0-8,12]"}.
     */
    public Gui setItem(String slots, ItemStack item) {
        return setItem(slots, item, null);
    }

    public Gui setItem(String slots, ItemStack item, GuiAction<InventoryClickEvent> action) {
        return setItem(SlotSelector.parse(slots), item, action);
    }

    public Gui setItem(String slots, GuiButton button) {
        return setItem(SlotSelector.parse(slots), button);
    }

    public Gui setItem(Collection<String> slotTokens, ItemStack item) {
        return setItem(slotTokens, item, null);
    }

    public Gui setItem(Collection<String> slotTokens, ItemStack item, GuiAction<InventoryClickEvent> action) {
        return setItem(SlotSelector.parse(slotTokens), item, action);
    }

    public Gui setItem(Collection<String> slotTokens, GuiButton button) {
        return setItem(SlotSelector.parse(slotTokens), button);
    }

    public Gui setItem(Iterable<Integer> slots, ItemStack item) {
        return setItem(slots, item, null);
    }

    public Gui setItem(Iterable<Integer> slots, ItemStack item, GuiAction<InventoryClickEvent> action) {
        if (slots != null) {
            for (Integer slot : slots) {
                if (slot != null) {
                    setItem(slot, item, action);
                }
            }
        }
        return this;
    }

    public Gui setItem(Iterable<Integer> slots, GuiButton button) {
        if (slots != null) {
            for (Integer slot : slots) {
                if (slot != null) {
                    setItem(slot, button);
                }
            }
        }
        return this;
    }

    public Gui setItem(ItemStack item, GuiAction<InventoryClickEvent> action, int... slots) {
        if (slots != null) {
            for (int slot : slots) {
                setItem(slot, item, action);
            }
        }
        return this;
    }

    public Gui removeItem(int slot) {
        return setItem(slot, null, null);
    }

    public Gui removeItem(String slots) {
        return removeItem(SlotSelector.parse(slots));
    }

    public Gui removeItem(Collection<String> slotTokens) {
        return removeItem(SlotSelector.parse(slotTokens));
    }

    public Gui removeItem(Iterable<Integer> slots) {
        if (slots != null) {
            for (Integer slot : slots) {
                if (slot != null) {
                    removeItem(slot);
                }
            }
        }
        return this;
    }

    public ItemStack getItem(int slot) {
        checkSlot(slot);
        ItemStack item = items.get(slot);
        return item == null ? null : item.clone();
    }

    public GuiAction<InventoryClickEvent> getAction(int slot) {
        checkSlot(slot);
        return actions.get(slot);
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        GuiManager.plugin();
        FoliaManager.runAtEntity(player, () -> openNow(player));
    }

    public void close(Player player) {
        if (player == null) {
            return;
        }
        FoliaManager.runAtEntity(player, () -> {
            if (GuiManager.getGui(player) == this && player.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder) {
                player.closeInventory();
            }
            if (GuiManager.getGui(player) == this) {
                GuiManager.clear(player);
            }
        });
    }

    /**
     * Re-renders the open inventory from current items/actions (e.g. after {@link #setItem}).
     */
    public void update(Player player) {
        if (player == null) {
            return;
        }
        FoliaManager.runAtEntity(player, () -> updateNow(player));
    }

    protected void updateNow(Player player) {
        GuiHolder holder = holderOf(player);
        if (holder == null) {
            return;
        }
        populate(holder.getInventory(), player, holder);
    }

    protected GuiHolder holderOf(Player player) {
        if (player == null || !player.isOnline()) {
            return null;
        }
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder holder)) {
            return null;
        }
        if (holder.getGui() != this || !GuiManager.isValidSession(player, holder)) {
            return null;
        }
        return holder;
    }

    void handleDefault(InventoryClickEvent event) {
        GuiAction<InventoryClickEvent> action = defaultAction;
        if (action != null) {
            action.execute(event);
        }
    }

    void handleTopClick(InventoryClickEvent event) {
        GuiAction<InventoryClickEvent> action = topClickAction;
        if (action != null) {
            action.execute(event);
        }
    }

    void handleBottomClick(InventoryClickEvent event) {
        GuiAction<InventoryClickEvent> action = bottomClickAction;
        if (action != null) {
            action.execute(event);
        }
    }

    void handleSlotClick(InventoryClickEvent event, int slot) {
        GuiAction<InventoryClickEvent> action = actions.get(slot);
        if (action != null) {
            action.execute(event);
            return;
        }
        handleContentClick(event, slot);
    }

    /**
     * Hook for subclasses (e.g. page items). Fixed slot actions are handled first.
     */
    protected void handleContentClick(InventoryClickEvent event, int slot) {
    }

    void handleOpen(Player player) {
        GuiAction<Player> action = openAction;
        if (action != null) {
            action.execute(player);
        }
    }

    void handleClose(InventoryCloseEvent event) {
        GuiAction<InventoryCloseEvent> action = closeAction;
        if (action != null) {
            action.execute(event);
        }
    }

    private void openNow(Player player) {
        if (!player.isOnline()) {
            return;
        }

        UUID sessionId = UUID.randomUUID();
        GuiHolder holder = new GuiHolder(this, sessionId);
        Inventory inventory = Bukkit.createInventory(holder, getSize(), resolveTitle(player, holder));
        holder.setInventory(inventory);

        populate(inventory, player, holder);

        GuiManager.bind(player, sessionId, this);
        player.openInventory(inventory);
        applyTitle(player, holder);
        handleOpen(player);
    }

    /**
     * Fills the inventory for this open session. Subclasses may override.
     */
    protected void populate(Inventory inventory, Player player, GuiHolder holder) {
        inventory.clear();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().clone());
        }
        applyTitle(player, holder);
    }

    protected Component resolveTitle(Player player, GuiHolder holder) {
        int page = titlePageNumber(player, holder);
        int max = titleMaxPages(player, holder);
        int index = titlePageIndex(player, holder);

        String template = titleTemplate;
        if (template != null) {
            return ColorParser.translateColors(replaceTitlePlaceholders(template, page, max, index));
        }
        return replaceTitlePlaceholders(title == null ? Component.empty() : title, page, max, index);
    }

    protected int titlePageNumber(Player player, GuiHolder holder) {
        return 1;
    }

    protected int titleMaxPages(Player player, GuiHolder holder) {
        return 1;
    }

    protected int titlePageIndex(Player player, GuiHolder holder) {
        return 0;
    }

    protected static String replaceTitlePlaceholders(String template, int page, int max, int index) {
        return template
                .replace("%page%", String.valueOf(page))
                .replace("%max%", String.valueOf(max))
                .replace("%max_pages%", String.valueOf(max))
                .replace("%page_index%", String.valueOf(index));
    }

    protected static Component replaceTitlePlaceholders(Component component, int page, int max, int index) {
        return component
                .replaceText(config -> config.matchLiteral("%page%").replacement(String.valueOf(page)))
                .replaceText(config -> config.matchLiteral("%max%").replacement(String.valueOf(max)))
                .replaceText(config -> config.matchLiteral("%max_pages%").replacement(String.valueOf(max)))
                .replaceText(config -> config.matchLiteral("%page_index%").replacement(String.valueOf(index)));
    }

    protected void applyTitle(Player player, GuiHolder holder) {
        if (player == null || !player.isOnline() || holder == null) {
            return;
        }
        Component resolved = resolveTitle(player, holder);
        if (player.getOpenInventory().getTopInventory().equals(holder.getInventory())) {
            player.getOpenInventory().setTitle(ColorParser.toLegacy(resolved));
        }
    }

    protected boolean isFixedSlot(int slot) {
        return items.containsKey(slot);
    }

    protected Map<Integer, ItemStack> fixedItems() {
        return items;
    }

    protected Map<Integer, GuiAction<InventoryClickEvent>> fixedActions() {
        return actions;
    }

    protected void checkSlot(int slot) {
        if (slot < 0 || slot >= getSize()) {
            throw new IllegalArgumentException("slot out of range: " + slot + " (size " + getSize() + ")");
        }
    }
}
