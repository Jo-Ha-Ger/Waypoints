package io.github.johager.waypoints;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Posteingang mit Wegpunkten, die andere geteilt haben. Klick = annehmen,
 * Rechtsklick = ablehnen. Bedrock-Spieler nehmen per Antippen an und können
 * mit "Alle ablehnen" aufräumen.
 */
public final class InboxMenu implements InventoryHolder {

    static final int SIZE = 27;
    static final int SLOT_BACK = 18;
    static final int SLOT_DECLINE_ALL = 26;

    private final WaypointsPlugin plugin;
    private final Player player;
    private final PlayerData data;
    private final Inventory inventory;
    private final int[] slotIds = new int[PlayerData.INBOX_LIMIT];

    public InboxMenu(WaypointsPlugin plugin, Player player, PlayerData data) {
        this.plugin = plugin;
        this.player = player;
        this.data = data;
        this.inventory = Bukkit.createInventory(this, SIZE,
                Text.bold(plugin.t(player, "inbox.title"), NamedTextColor.DARK_AQUA));
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
        Lang lang = plugin.lang(player);
        List<Shared> inbox = data.inbox();
        for (int i = 0; i < PlayerData.INBOX_LIMIT; i++) {
            slotIds[i] = -1;
            if (i < inbox.size()) {
                Shared shared = inbox.get(i);
                slotIds[i] = shared.id();
                Waypoint wp = shared.waypoint();
                List<Component> lore = new ArrayList<>();
                lore.add(Text.item(t("inbox.from", shared.from()), NamedTextColor.AQUA));
                lore.add(Text.item(NavMath.coords(wp.x(), wp.y(), wp.z()), NamedTextColor.WHITE));
                lore.add(Text.item(t("item.dimension", I18n.dimension(lang, wp.dimension())), NamedTextColor.GRAY));
                lore.add(Text.item(" ", NamedTextColor.GRAY));
                lore.add(Text.item(t("inbox.click"), NamedTextColor.GREEN));
                lore.add(Text.item(t("inbox.right"), NamedTextColor.RED));
                inventory.setItem(i, item(Material.PAPER, Text.item(wp.name(), NamedTextColor.GOLD), lore));
            }
        }
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, Text.item(" ", NamedTextColor.GRAY), List.of());
        for (int slot = PlayerData.INBOX_LIMIT; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(SLOT_BACK, item(Material.ARROW, Text.item(t("inbox.back"), NamedTextColor.YELLOW), List.of()));
        if (inbox.isEmpty()) {
            inventory.setItem(22, item(Material.ENDER_CHEST, Text.item(t("inbox.empty"), NamedTextColor.GRAY), List.of()));
        } else {
            inventory.setItem(SLOT_DECLINE_ALL, item(Material.BARRIER,
                    Text.item(t("inbox.decline-all"), NamedTextColor.RED), List.of()));
        }
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    void click(int slot, boolean rightClick) {
        if (slot < PlayerData.INBOX_LIMIT) {
            int id = slotIds[slot];
            if (id < 0) {
                return;
            }
            if (rightClick) {
                plugin.declineShared(player, data, id);
            } else {
                plugin.acceptShared(player, data, id);
            }
            render();
            return;
        }
        if (slot == SLOT_BACK) {
            WaypointMenu menu = new WaypointMenu(plugin, player, data);
            Runnable task = menu::open;
            Bukkit.getScheduler().runTask(plugin, task);
        } else if (slot == SLOT_DECLINE_ALL && !data.inbox().isEmpty()) {
            data.clearInbox();
            plugin.store().save(data);
            plugin.info(player, "inbox.declined-all");
            render();
        }
    }
}
