package io.github.johager.waypoints;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Das Menü (Truhen-Oberfläche). Funktioniert auch für Bedrock-Spieler über Geyser,
 * da nur einfache Klicks benötigt werden.
 */
public final class WaypointMenu implements InventoryHolder {

    static final int PAGE_SIZE = 45;
    static final int SLOT_PREV = 45;
    static final int SLOT_LANG = 46;
    static final int SLOT_SAVE = 47;
    static final int SLOT_STOP = 48;
    static final int SLOT_INFO = 49;
    static final int SLOT_DELETE_MODE = 50;
    static final int SLOT_DEATH = 51;
    static final int SLOT_INBOX = 52;
    static final int SLOT_NEXT = 53;

    private final WaypointsPlugin plugin;
    private final Player player;
    private final PlayerData data;
    private final Inventory inventory;
    private final String[] slotIds = new String[PAGE_SIZE];

    private int page;
    private boolean deleteMode;
    private String pendingDelete;

    public WaypointMenu(WaypointsPlugin plugin, Player player, PlayerData data) {
        this.plugin = plugin;
        this.player = player;
        this.data = data;
        this.inventory = Bukkit.createInventory(this, 54,
                Text.bold(plugin.t(player, "menu.title"), NamedTextColor.DARK_AQUA));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    private String t(String key, Object... args) {
        return plugin.t(player, key, args);
    }

    void render() {
        inventory.clear();
        List<Ref> all = plugin.allRefs(data);
        int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));

        for (int i = 0; i < PAGE_SIZE; i++) {
            slotIds[i] = null;
            int index = page * PAGE_SIZE + i;
            if (index < all.size()) {
                Ref ref = all.get(index);
                slotIds[i] = ref.id();
                inventory.setItem(i, waypointItem(ref));
            }
        }

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, Text.item(" ", NamedTextColor.GRAY), List.of());
        for (int slot = PAGE_SIZE; slot < 54; slot++) {
            inventory.setItem(slot, filler);
        }

        if (page > 0) {
            inventory.setItem(SLOT_PREV, item(Material.ARROW, Text.item(t("menu.prev"), NamedTextColor.YELLOW), List.of()));
        }
        if (page < pages - 1) {
            inventory.setItem(SLOT_NEXT, item(Material.ARROW, Text.item(t("menu.next"), NamedTextColor.YELLOW), List.of()));
        }

        String langName = plugin.lang(player).displayName()
                + (data.language() == null ? " " + t("menu.lang-auto") : "");
        inventory.setItem(SLOT_LANG, item(Material.BOOK,
                Text.item(t("menu.lang", langName), NamedTextColor.AQUA), List.of(
                        Text.item(t("menu.lang-lore"), NamedTextColor.GRAY))));

        inventory.setItem(SLOT_SAVE, item(Material.NAME_TAG,
                Text.item(t("menu.save"), NamedTextColor.GREEN), List.of(
                        Text.item(t("menu.save-lore1"), NamedTextColor.GRAY),
                        Text.item(t("menu.save-lore2"), NamedTextColor.GRAY),
                        Text.item(t("menu.save-lore3"), NamedTextColor.GRAY))));

        Ref active = plugin.lookup(data, data.navigation());
        if (active != null) {
            inventory.setItem(SLOT_STOP, item(Material.BARRIER,
                    Text.item(t("menu.stop"), NamedTextColor.RED), List.of(
                            Text.item(t("menu.current", plugin.displayName(player, active)), NamedTextColor.GRAY))));
        }

        inventory.setItem(SLOT_INFO, item(Material.OAK_SIGN,
                Text.item(t("menu.help"), NamedTextColor.AQUA), List.of(
                        Text.item(t("menu.count", data.count(), plugin.maxWaypoints()), NamedTextColor.WHITE),
                        Text.item(t("menu.page", page + 1, pages), NamedTextColor.WHITE),
                        Text.item(" ", NamedTextColor.GRAY),
                        Text.item(t("menu.help1"), NamedTextColor.GRAY),
                        Text.item(t("menu.help2"), NamedTextColor.GRAY),
                        Text.item(" ", NamedTextColor.GRAY),
                        Text.item(t("menu.help3"), NamedTextColor.GRAY))));

        if (deleteMode) {
            inventory.setItem(SLOT_DELETE_MODE, item(Material.TNT,
                    Text.item(t("menu.delete-on"), NamedTextColor.RED), List.of(
                            Text.item(t("menu.delete-on1"), NamedTextColor.GRAY),
                            Text.item(t("menu.delete-on2"), NamedTextColor.GRAY),
                            Text.item(t("menu.delete-on3"), NamedTextColor.YELLOW))));
        } else {
            inventory.setItem(SLOT_DELETE_MODE, item(Material.LAVA_BUCKET,
                    Text.item(t("menu.delete-off"), NamedTextColor.GOLD), List.of(
                            Text.item(t("menu.delete-off1"), NamedTextColor.GRAY),
                            Text.item(t("menu.delete-off2"), NamedTextColor.GRAY))));
        }

        if (plugin.deathWaypointEnabled()) {
            boolean on = data.deathEnabled();
            inventory.setItem(SLOT_DEATH, item(on ? Material.SKELETON_SKULL : Material.BONE,
                    Text.item(t(on ? "menu.death-on" : "menu.death-off"), on ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                    List.of(Text.item(t("menu.death-lore"), NamedTextColor.GRAY),
                            Text.item(t("menu.toggle"), NamedTextColor.YELLOW))));
        }

        int inboxSize = data.inbox().size();
        inventory.setItem(SLOT_INBOX, item(inboxSize > 0 ? Material.CHEST : Material.ENDER_CHEST,
                Text.item(t("menu.inbox", inboxSize), inboxSize > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                List.of(Text.item(t("menu.inbox-lore"), NamedTextColor.GRAY))));
    }

    private ItemStack waypointItem(Ref ref) {
        Waypoint wp = ref.waypoint();
        Lang lang = plugin.lang(player);
        boolean active = data.isNavigatingTo(ref.id());
        boolean confirm = deleteMode && pendingDelete != null && pendingDelete.equalsIgnoreCase(ref.id());

        Material material;
        if (confirm) {
            material = Material.TNT;
        } else if (ref.kind() == Ref.Kind.DEATH) {
            material = Material.SKELETON_SKULL;
        } else if (ref.kind() == Ref.Kind.SERVER) {
            material = Material.LODESTONE;
        } else {
            material = switch (wp.dimension()) {
                case "NETHER" -> Material.NETHERRACK;
                case "THE_END" -> Material.END_STONE;
                case "NORMAL" -> Material.GRASS_BLOCK;
                default -> Material.COMPASS;
            };
        }

        List<Component> lore = new ArrayList<>();
        if (active) {
            lore.add(Text.item(t("item.active"), NamedTextColor.GREEN));
        }
        if (ref.kind() == Ref.Kind.SERVER) {
            lore.add(Text.item(t("item.server"), NamedTextColor.AQUA));
        }
        lore.add(Text.item(NavMath.coords(wp.x(), wp.y(), wp.z()), NamedTextColor.WHITE));
        lore.add(Text.item(t("item.dimension", I18n.dimension(lang, wp.dimension())), NamedTextColor.GRAY));

        Location loc = player.getLocation();
        NavMath.Target target = NavMath.resolve(player.getWorld().getName(),
                player.getWorld().getEnvironment().name(), wp);
        if (target != null) {
            double dx = target.x() - loc.getX();
            double dz = target.z() - loc.getZ();
            String dist = NavMath.distance(Math.sqrt(dx * dx + dz * dz), lang)
                    + (target.converted() ? I18n.t(lang, "nav.converted") : "");
            lore.add(Text.item(t("item.distance", dist), NamedTextColor.GRAY));
        }
        if (wp.created() > 0) {
            String date = new SimpleDateFormat(lang.datePattern()).format(new Date(wp.created()));
            lore.add(Text.item(t("item.saved", date), NamedTextColor.DARK_GRAY));
        }
        lore.add(Text.item(" ", NamedTextColor.GRAY));
        if (confirm) {
            lore.add(Text.item(t("item.confirm"), NamedTextColor.RED));
        } else if (deleteMode) {
            lore.add(Text.item(t("item.select-delete"), NamedTextColor.RED));
        } else if (active) {
            lore.add(Text.item(t("item.click-stop"), NamedTextColor.YELLOW));
        } else {
            lore.add(Text.item(t("item.click-go"), NamedTextColor.YELLOW));
        }

        NamedTextColor nameColor = confirm ? NamedTextColor.RED : active ? NamedTextColor.GREEN
                : ref.kind() == Ref.Kind.SERVER ? NamedTextColor.AQUA : NamedTextColor.GOLD;
        ItemStack stack = item(material, Text.item(plugin.displayName(player, ref), nameColor), lore);
        if (active) {
            ItemMeta meta = stack.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Verarbeitet einen Klick in das obere Inventar. */
    void click(int slot) {
        if (slot < PAGE_SIZE) {
            String id = slotIds[slot];
            if (id == null) {
                return;
            }
            Ref ref = plugin.lookup(data, id);
            if (ref == null) {
                render();
                return;
            }
            if (deleteMode) {
                if (ref.kind() == Ref.Kind.SERVER && !player.hasPermission(WaypointsPlugin.PERM_ADMIN)) {
                    plugin.error(player, "server.no-permission");
                } else if (pendingDelete != null && pendingDelete.equalsIgnoreCase(ref.id())) {
                    pendingDelete = null;
                    if (ref.kind() == Ref.Kind.SERVER) {
                        plugin.deleteServerWaypoint(player, ref.waypoint().name());
                    } else {
                        plugin.deleteWaypoint(player, data, ref.id());
                    }
                } else {
                    pendingDelete = ref.id();
                }
                render();
                return;
            }
            if (data.isNavigatingTo(ref.id())) {
                plugin.navigation().stop(player, data, true);
                render();
            } else {
                player.closeInventory();
                plugin.navigation().start(player, data, ref);
            }
            return;
        }

        pendingDelete = null;
        switch (slot) {
            case SLOT_PREV -> {
                page--;
                render();
            }
            case SLOT_NEXT -> {
                page++;
                render();
            }
            case SLOT_LANG -> {
                // Reihenfolge: automatisch → English → Deutsch → automatisch
                Lang next = data.language() == null ? Lang.EN : data.language() == Lang.EN ? Lang.DE : null;
                plugin.setLanguage(player, data, next);
                reopen();
            }
            case SLOT_DELETE_MODE -> {
                deleteMode = !deleteMode;
                render();
            }
            case SLOT_DEATH -> {
                if (plugin.deathWaypointEnabled()) {
                    plugin.toggleDeathPoint(player, data);
                    render();
                }
            }
            case SLOT_INBOX -> {
                InboxMenu inbox = new InboxMenu(plugin, player, data);
                Runnable task = inbox::open;
                Bukkit.getScheduler().runTask(plugin, task);
            }
            case SLOT_STOP -> {
                if (data.navigation() != null) {
                    plugin.navigation().stop(player, data, true);
                    render();
                }
            }
            case SLOT_SAVE -> {
                player.closeInventory();
                plugin.askForName(player);
            }
            default -> {
                if (deleteMode) {
                    render();
                }
            }
        }
    }

    /** Neu öffnen, damit auch der Titel in der neuen Sprache erscheint. */
    private void reopen() {
        WaypointMenu menu = new WaypointMenu(plugin, player, data);
        menu.page = page;
        // Nicht direkt im Klick-Event öffnen, sondern einen Tick später
        Runnable task = menu::open;
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
