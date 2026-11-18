package io.github.johager.waypoints;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /wp                          open the menu
 * /wp set name                 save current location
 * /wp nav name                 start navigation
 * /wp stop                     stop navigation
 * /wp list                     list waypoints in chat
 * /wp del name                 delete a waypoint
 * /wp share player name        send a waypoint to a player
 * /wp show name                show a waypoint to everyone in chat
 * /wp inbox                    waypoints shared with you
 * /wp accept id | decline id   (used by the clickable chat buttons)
 * /wp death                    death point on/off
 * /wp language en|de|auto      language
 * /wp server set|del name      server waypoints (admin)
 * /wp help
 */
public final class WaypointCommand implements CommandExecutor, TabCompleter {

    private final WaypointsPlugin plugin;

    public WaypointCommand(WaypointsPlugin plugin) {
        this.plugin = plugin;
    }

    private static String rest(String[] args, int from) {
        return args.length > from ? String.join(" ", Arrays.copyOfRange(args, from, args.length)).trim() : "";
    }

    private static int parseId(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.error(sender, "only-players");
            return true;
        }
        PlayerData data = plugin.data(player);
        String sub = args.length == 0 ? "menu" : args[0].toLowerCase(Locale.ROOT);
        String name = rest(args, 1);

        switch (sub) {
            case "menu", "gui" -> new WaypointMenu(plugin, player, data).open();
            case "set", "add" -> {
                if (name.isEmpty()) {
                    plugin.error(player, "usage.set", label);
                } else {
                    plugin.createWaypoint(player, data, name, player.getLocation());
                }
            }
            case "nav", "go" -> {
                if (name.isEmpty()) {
                    plugin.error(player, "usage.nav", label);
                    return true;
                }
                Ref ref = plugin.resolve(data, name);
                if (ref == null) {
                    plugin.error(player, "wp.not-found", name);
                } else {
                    plugin.navigation().start(player, data, ref);
                }
            }
            case "stop" -> {
                if (data.navigation() == null) {
                    plugin.info(player, "nav.none");
                } else {
                    plugin.navigation().stop(player, data, true);
                }
            }
            case "list" -> sendList(player, data, label);
            case "del", "delete", "remove" -> {
                if (name.isEmpty()) {
                    plugin.error(player, "usage.del", label);
                } else {
                    plugin.deleteWaypoint(player, data, name);
                }
            }
            case "share" -> {
                if (args.length < 3) {
                    plugin.error(player, "share.usage", label);
                } else {
                    plugin.share(player, data, args[1], rest(args, 2));
                }
            }
            case "show" -> {
                if (name.isEmpty()) {
                    plugin.error(player, "show.usage", label);
                } else {
                    plugin.show(player, data, name);
                }
            }
            case "inbox" -> new InboxMenu(plugin, player, data).open();
            case "accept" -> plugin.acceptShared(player, data, parseId(name));
            case "decline" -> plugin.declineShared(player, data, parseId(name));
            case "saveshown" -> plugin.saveShown(player, data, parseId(name));
            case "death" -> plugin.toggleDeathPoint(player, data);
            case "language", "lang" -> {
                if (name.equalsIgnoreCase("auto")) {
                    plugin.setLanguage(player, data, null);
                } else {
                    Lang lang = Lang.parse(name);
                    if (lang == null) {
                        plugin.error(player, "lang.usage", label);
                    } else {
                        plugin.setLanguage(player, data, lang);
                    }
                }
            }
            case "server" -> handleServer(player, args, label);
            case "help", "?" -> sendHelp(player, label);
            default -> plugin.error(player, "unknown", label);
        }
        return true;
    }

    private void handleServer(Player player, String[] args, String label) {
        if (!player.hasPermission(WaypointsPlugin.PERM_ADMIN)) {
            plugin.error(player, "server.no-permission");
            return;
        }
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        String name = rest(args, 2);
        if (name.isEmpty()) {
            plugin.error(player, "server.usage", label);
            return;
        }
        switch (action) {
            case "set", "add" -> plugin.createServerWaypoint(player, name);
            case "del", "delete", "remove" -> plugin.deleteServerWaypoint(player, name);
            default -> plugin.error(player, "server.usage", label);
        }
    }

    private void sendList(Player player, PlayerData data, String label) {
        Lang lang = plugin.lang(player);
        List<Ref> server = new ArrayList<>();
        List<Ref> own = new ArrayList<>();
        for (Ref ref : plugin.allRefs(data)) {
            (ref.kind() == Ref.Kind.SERVER ? server : own).add(ref);
        }
        if (server.isEmpty() && own.isEmpty()) {
            plugin.info(player, "list.empty", label);
            return;
        }
        if (!server.isEmpty()) {
            plugin.info(player, "list.server-header");
            for (Ref ref : server) {
                player.sendMessage(line(player, data, lang, ref, player.hasPermission(WaypointsPlugin.PERM_ADMIN)));
            }
        }
        if (!own.isEmpty()) {
            plugin.info(player, "list.header", own.size());
            for (Ref ref : own) {
                player.sendMessage(line(player, data, lang, ref, true));
            }
        }
    }

    private Component line(Player player, PlayerData data, Lang lang, Ref ref, boolean deletable) {
        Waypoint wp = ref.waypoint();
        boolean active = data.isNavigatingTo(ref.id());
        String shown = plugin.displayName(player, ref);
        NamedTextColor nameColor = ref.kind() == Ref.Kind.SERVER ? NamedTextColor.AQUA : NamedTextColor.GOLD;
        Component line = Text.of(active ? " ▶ " : " • ", active ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)
                .append(Text.of(shown, nameColor))
                .append(Text.of("  " + NavMath.coords(wp.x(), wp.y(), wp.z()), NamedTextColor.WHITE))
                .append(Text.of("  " + I18n.dimension(lang, wp.dimension()) + "  ", NamedTextColor.GRAY));
        if (active) {
            line = line.append(Text.button(I18n.t(lang, "btn.stop"), NamedTextColor.YELLOW,
                    "/waypoint stop", I18n.t(lang, "btn.stop-hover")));
        } else {
            line = line.append(Text.button(I18n.t(lang, "btn.go"), NamedTextColor.GREEN,
                    "/waypoint nav " + ref.id(), I18n.t(lang, "btn.go-hover", shown)));
        }
        if (deletable) {
            String delCommand = ref.kind() == Ref.Kind.SERVER
                    ? "/waypoint server del " + wp.name()
                    : "/waypoint del " + ref.id();
            line = line.append(Text.of(" ", NamedTextColor.GRAY))
                    .append(Text.suggestButton("[✖]", NamedTextColor.RED, delCommand, I18n.t(lang, "btn.del-hover")));
        }
        return line;
    }

    private void sendHelp(Player player, String label) {
        Lang lang = plugin.lang(player);
        String l = "/" + label;
        plugin.info(player, "help.header");
        help(player, l, I18n.t(lang, "help.menu"));
        help(player, l + " set <name>", I18n.t(lang, "help.set"));
        help(player, l + " nav <name>", I18n.t(lang, "help.nav"));
        help(player, l + " stop", I18n.t(lang, "help.stop"));
        help(player, l + " list", I18n.t(lang, "help.list"));
        help(player, l + " del <name>", I18n.t(lang, "help.del"));
        if (player.hasPermission(WaypointsPlugin.PERM_SHARE)) {
            help(player, l + " share <player> <name>", I18n.t(lang, "help.share"));
            help(player, l + " show <name>", I18n.t(lang, "help.show"));
        }
        help(player, l + " inbox", I18n.t(lang, "help.inbox"));
        if (plugin.deathWaypointEnabled()) {
            help(player, l + " death", I18n.t(lang, "help.death"));
        }
        help(player, l + " language <en|de|auto>", I18n.t(lang, "help.lang"));
        if (player.hasPermission(WaypointsPlugin.PERM_ADMIN)) {
            help(player, l + " server <set|del> <name>", I18n.t(lang, "help.server"));
        }
    }

    private static void help(Player player, String cmd, String text) {
        player.sendMessage(Text.of(" " + cmd, NamedTextColor.GOLD).append(Text.of(" – " + text, NamedTextColor.GRAY)));
    }

    // ------------------------------------------------------------------ Tab completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (!(sender instanceof Player player)) {
            return result;
        }
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("set", "nav", "stop", "list", "del", "inbox", "language", "help"));
            if (player.hasPermission(WaypointsPlugin.PERM_SHARE)) {
                subs.add("share");
                subs.add("show");
            }
            if (plugin.deathWaypointEnabled()) {
                subs.add("death");
            }
            if (player.hasPermission(WaypointsPlugin.PERM_ADMIN)) {
                subs.add("server");
            }
            return filter(subs, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        PlayerData data = plugin.store().get(player.getUniqueId());
        if (data == null) {
            return result;
        }
        switch (sub) {
            case "language", "lang" -> {
                if (args.length == 2) {
                    return filter(List.of("en", "de", "auto"), args[1]);
                }
            }
            case "nav", "go", "show" -> {
                return names(namesFor(data, true, true), args, 1);
            }
            case "del", "delete", "remove" -> {
                return names(namesFor(data, true, false), args, 1);
            }
            case "share" -> {
                if (args.length == 2) {
                    List<String> players = new ArrayList<>();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!p.getUniqueId().equals(player.getUniqueId())) {
                            players.add(p.getName());
                        }
                    }
                    return filter(players, args[1]);
                }
                return names(namesFor(data, true, true), args, 2);
            }
            case "server" -> {
                if (!player.hasPermission(WaypointsPlugin.PERM_ADMIN)) {
                    return result;
                }
                if (args.length == 2) {
                    return filter(List.of("set", "del"), args[1]);
                }
                if (args[1].equalsIgnoreCase("del")) {
                    List<String> names = new ArrayList<>();
                    for (Waypoint wp : plugin.store().serverWaypoints()) {
                        names.add(wp.name());
                    }
                    return names(names, args, 2);
                }
            }
            default -> {
                return result;
            }
        }
        return result;
    }

    private List<String> namesFor(PlayerData data, boolean own, boolean server) {
        List<String> names = new ArrayList<>();
        if (own) {
            if (data.death() != null) {
                names.add("death");
            }
            for (Waypoint wp : data.waypoints()) {
                names.add(wp.name());
            }
        }
        if (server) {
            for (Waypoint wp : plugin.store().serverWaypoints()) {
                if (!names.contains(wp.name())) {
                    names.add(wp.name());
                }
            }
        }
        return names;
    }

    private static List<String> filter(List<String> options, String typed) {
        List<String> result = new ArrayList<>();
        String start = typed.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(start)) {
                result.add(option);
            }
        }
        return result;
    }

    /** Vervollständigt Namen mit Leerzeichen; {@code from} = Index des ersten Namens-Arguments. */
    private static List<String> names(List<String> names, String[] args, int from) {
        List<String> result = new ArrayList<>();
        if (args.length <= from) {
            return result;
        }
        String typed = String.join(" ", Arrays.copyOfRange(args, from, args.length)).toLowerCase(Locale.ROOT);
        int skipWords = args.length - from - 1;
        for (String n : names) {
            if (n.toLowerCase(Locale.ROOT).startsWith(typed)) {
                String[] words = n.split(" ");
                if (words.length > skipWords) {
                    result.add(String.join(" ", Arrays.copyOfRange(words, skipWords, words.length)));
                }
            }
        }
        return result;
    }
}
