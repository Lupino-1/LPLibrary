# LPLibrary — project memory

Agent: read this at session start. Update when we decide something important (API, plans, conventions). Keep short.
**Auto-save:** after every planning decision or finished feature in this project, update this file before ending the turn — do not wait for the user to ask.

## Stack / target
- Paper/Folia library (`paper-api`), package `dev.lupino1`
- Init: `LPLibrary.init(JavaPlugin)` → `FoliaManager` + `GuiManager`

## Done
- **Folia**: `FoliaManager`, `FoliaRunnable`, `TaskWrapper`
- **Messages**: `MessageManager`, `ColorParser`
    - init: `new MessageManager(plugin [, fileName [, saveDefaults]])`
    - `saveDefaults=true` → `plugin.saveResource(path, false)` (path always `/`)
    - maps: volatile + immutable `Map.copyOf`; optional future: single snapshot object for multi-map consistency
    - placeholders: `Map<String, ?>` — `String` (před MiniMessage) i `Component` (Adventure replaceText po parse)
- **Project memory**: `MEMORY.md` + `.cursor/rules/project-memory.mdc` (alwaysApply, auto-update)
- **GUI** (`dev.lupino1.gui`):
    - `Gui`, `PaginatedGui`, `GuiButton`, `GuiAction<T>`, `GuiHolder`, `GuiManager`, `GuiListener`
    - `new Gui` / `new PaginatedGui` → `setItem` → `open`
    - session: player PDC + holder; page per session on `GuiHolder`
    - actions: `defaultAction` (default cancel), open/close/top/bottom (null), slot
    - `PaginatedGui`: fixed `setItem` (nepočítá se) vs `addPageItem` (počítá se)
    - content slots = size − fixed; `getPage`/`getMaxPages`/`next`/`previous`/`update`
    - title: String i Component; placeholdery `%page%` `%max%` `%max_pages%` `%page_index%` (Component přes Adventure replaceText)
    - `update(player)` na Gui i PaginatedGui (po setItem/remove v action)
    - `ItemBuilder`; GUI item = ItemStack + slot/page action

## Planned / ideas (basic kit do všech pluginů)
- **Command library** (priorita) — anotace/builder, subcmd, tab complete, sender checks, messages napojení
- optional later: config helper, cooldown/map cache, player PDC util
- GUI = ne do každého pluginu (ok v lib stejně)

## Status
- Core do všech: Folia + Messages
- GUI v1+paginated: good enough když potřeba; chest-only hard limit


## Ref — TriumphGUI (https://github.com/TriumphTeam/triumph-gui)
Inspo, ne copy-paste. Oni: holder = BaseGui, GuiItem+NBT UUID, InteractionModifier enum, builders, Paginated/Scrolling/Storage.
My: Folia `runAtEntity`, player PDC session, `GuiAction<T>`, default cancel + cancelDrag.

## Conventions
- Czech terse replies preferred by owner
- Resource paths for jar/disk: forward slashes only
- Implement planned modules only when user asks
