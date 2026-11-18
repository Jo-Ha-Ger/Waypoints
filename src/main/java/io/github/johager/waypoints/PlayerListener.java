package io.github.johager.waypoints;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.InventoryHolder;

public final class PlayerListener implements Listener {

    private final WaypointsPlugin plugin;

    public PlayerListener(WaypointsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.store().load(player.getUniqueId(), player.getName());
        Ref active = plugin.lookup(data, data.navigation());
        if (active != null) {
            plugin.info(player, "nav.continues", plugin.displayName(player, active));
        }
        if (!data.inbox().isEmpty()) {
            plugin.info(player, "share.inbox-hint");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.store().get(player.getUniqueId());
        if (data != null) {
            plugin.navigation().hide(player, data);
        }
        plugin.clearPendingName(player.getUniqueId());
        plugin.store().unload(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        plugin.saveDeathPoint(player, player.getLocation());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.deathWaypointEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        PlayerData data = plugin.store().get(player.getUniqueId());
        if (data == null || !data.deathEnabled() || data.death() == null) {
            return;
        }
        Lang lang = plugin.lang(player);
        player.sendMessage(Text.info(lang, I18n.t(lang, "death.saved"))
                .append(Text.button(I18n.t(lang, "death.button"), NamedTextColor.GREEN,
                        "/waypoint nav " + PlayerData.DEATH_ID, I18n.t(lang, "death.button-hover"))));
    }

    /** Namenseingabe für „Aktuellen Ort speichern“ abfangen, bevor sie im Chat landet. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        WaypointsPlugin.PendingName pending = plugin.takePendingName(player.getUniqueId());
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Runnable task = () -> {
            if (!player.isOnline()) {
                return;
            }
            if (text.equalsIgnoreCase("cancel") || text.equalsIgnoreCase("abbrechen")
                    || text.equalsIgnoreCase("abbruch")) {
                plugin.info(player, "ask.cancelled");
                return;
            }
            PlayerData data = plugin.store().get(player.getUniqueId());
            if (data != null) {
                plugin.createWaypoint(player, data, text, pending.location());
            }
        };
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMenuClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder(false);
        if (!(holder instanceof WaypointMenu) && !(holder instanceof InboxMenu)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        if (holder instanceof WaypointMenu menu) {
            menu.click(slot);
        } else if (holder instanceof InboxMenu inbox) {
            inbox.click(slot, event.isRightClick());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMenuDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder(false);
        if (holder instanceof WaypointMenu || holder instanceof InboxMenu) {
            event.setCancelled(true);
        }
    }
}
