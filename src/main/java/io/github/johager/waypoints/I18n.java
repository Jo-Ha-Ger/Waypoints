package io.github.johager.waypoints;

import java.util.HashMap;
import java.util.Map;

/**
 * All texts in English and German. Placeholders: {0}, {1}, ...
 * Alle Texte auf Englisch und Deutsch. Platzhalter: {0}, {1}, ...
 */
public final class I18n {

    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> DE = new HashMap<>();

    private I18n() {
    }

    private static void m(String key, String en, String de) {
        EN.put(key, en);
        DE.put(key, de);
    }

    static {
        // General
        m("prefix", "[Waypoints] ", "[Wegpunkte] ");
        m("only-players", "This command can only be used by players.", "Dieser Befehl ist nur für Spieler.");
        m("unknown", "Unknown subcommand. Help: /{0} help", "Unbekannter Unterbefehl. Hilfe mit /{0} help");
        m("usage.set", "Usage: /{0} set <name>", "Benutzung: /{0} set <Name>");
        m("usage.nav", "Usage: /{0} nav <name>", "Benutzung: /{0} nav <Name>");
        m("usage.del", "Usage: /{0} del <name>", "Benutzung: /{0} del <Name>");

        // Dimensions
        m("dim.NORMAL", "Overworld", "Oberwelt");
        m("dim.NETHER", "Nether", "Nether");
        m("dim.THE_END", "The End", "End");
        m("dim.CUSTOM", "Custom world", "Sonderwelt");

        // Names
        m("name.empty", "The name must not be empty.", "Der Name darf nicht leer sein.");
        m("name.too-long", "The name can be at most 32 characters long.", "Der Name darf höchstens 32 Zeichen lang sein.");
        m("name.invalid", "The name contains invalid characters.", "Der Name enthält ungültige Zeichen.");

        // Waypoints
        m("wp.exists", "\"{0}\" already exists. Delete it first with /wp del {0}",
                "\"{0}\" gibt es schon. Erst löschen mit /wp del {0}");
        m("wp.limit", "You have reached the limit of {0} waypoints.",
                "Du hast das Maximum von {0} Wegpunkten erreicht.");
        m("wp.saved", "Waypoint \"{0}\" saved: {1} ({2})", "Wegpunkt \"{0}\" gespeichert: {1} ({2})");
        m("wp.not-found", "No waypoint named \"{0}\" found.", "Kein Wegpunkt namens \"{0}\" gefunden.");
        m("wp.deleted", "Waypoint \"{0}\" deleted.", "Wegpunkt \"{0}\" gelöscht.");

        // Name input via chat
        m("ask.line1", "Type a name for this location in chat.", "Schreib jetzt den Namen für diesen Ort in den Chat.");
        m("ask.line2", "The location is already remembered. Type \"cancel\" to abort.",
                "Der Ort wurde schon gemerkt. Zum Abbrechen: \"cancel\" schreiben.");
        m("ask.cancelled", "Saving cancelled.", "Speichern abgebrochen.");

        // Navigation
        m("nav.started", "Navigating to \"{0}\". Look at the bar at the top of your screen.",
                "Navigation zu \"{0}\" gestartet. Die Anzeige ist oben am Bildschirm.");
        m("nav.stopped", "Navigation stopped.", "Navigation beendet.");
        m("nav.none", "No navigation is running.", "Es läuft gerade keine Navigation.");
        m("nav.arrived", "You have reached \"{0}\"!", "Ziel \"{0}\" erreicht!");
        m("nav.other-dim", "  is in: {0}  ", "  liegt in: {0}  ");
        m("nav.converted", " (converted)", " (umgerechnet)");
        m("nav.continues", "Navigation to \"{0}\" continues. Stop it with /wp stop",
                "Navigation zu \"{0}\" läuft weiter. Beenden mit /wp stop");

        // Death point
        m("death.name", "Last death", "Letzter Tod");
        m("death.saved", "Your death point has been saved. ", "Dein Todespunkt wurde gespeichert. ");
        m("death.button", "[Navigate there]", "[Hin navigieren]");
        m("death.button-hover", "Start navigation to your death point", "Navigation zum Todespunkt starten");
        m("death.on", "Death point is now ON. Where you die will be saved automatically.",
                "Todespunkt ist jetzt AN. Deine Todesstelle wird automatisch gespeichert.");
        m("death.off", "Death point is now OFF. Your saved death point was removed.",
                "Todespunkt ist jetzt AUS. Dein gespeicherter Todespunkt wurde entfernt.");
        m("death.server-off", "The death point is disabled on this server.",
                "Der Todespunkt ist auf diesem Server deaktiviert.");

        // Language
        m("lang.set", "Language set to English.", "Sprache auf Deutsch gestellt.");
        m("lang.auto", "Language now follows your game language.", "Die Sprache richtet sich jetzt nach deiner Spielsprache.");
        m("lang.usage", "Usage: /{0} language <en|de|auto>", "Benutzung: /{0} language <de|en|auto>");

        // Sharing
        m("share.usage", "Usage: /{0} share <player> <waypoint>", "Benutzung: /{0} share <Spieler> <Wegpunkt>");
        m("share.no-permission", "You don't have permission to share waypoints.",
                "Du hast keine Berechtigung, Wegpunkte zu teilen.");
        m("share.offline", "Player \"{0}\" is not online.", "Spieler \"{0}\" ist nicht online.");
        m("share.self", "You can't share a waypoint with yourself.", "Du kannst keinen Wegpunkt mit dir selbst teilen.");
        m("share.inbox-full", "{0}'s inbox is full.", "Der Posteingang von {0} ist voll.");
        m("share.sent", "Waypoint \"{0}\" sent to {1}.", "Wegpunkt \"{0}\" an {1} geschickt.");
        m("share.received", "{0} shared the waypoint \"{1}\" with you: {2} ({3}) ",
                "{0} hat den Wegpunkt \"{1}\" mit dir geteilt: {2} ({3}) ");
        m("share.inbox-hint", "Can't click? Open your inbox with /wp inbox",
                "Klicken geht nicht? Öffne deinen Posteingang mit /wp inbox");
        m("share.accept-btn", "[Accept]", "[Annehmen]");
        m("share.accept-hover", "Save this waypoint to your list", "Diesen Wegpunkt in deine Liste übernehmen");
        m("share.decline-btn", "[Decline]", "[Ablehnen]");
        m("share.decline-hover", "Discard", "Verwerfen");
        m("share.accepted", "\"{0}\" was added to your waypoints.", "\"{0}\" wurde zu deinen Wegpunkten hinzugefügt.");
        m("share.declined", "Shared waypoint \"{0}\" discarded.", "Geteilter Wegpunkt \"{0}\" verworfen.");
        m("share.not-found", "This shared waypoint no longer exists.", "Diesen geteilten Wegpunkt gibt es nicht mehr.");

        // Show in public chat
        m("show.usage", "Usage: /{0} show <waypoint>", "Benutzung: /{0} show <Wegpunkt>");
        m("show.broadcast", "{0} shows the waypoint \"{1}\": {2} ({3}) ",
                "{0} zeigt den Wegpunkt \"{1}\": {2} ({3}) ");
        m("show.save-btn", "[Save]", "[Speichern]");
        m("show.save-hover", "Save to your waypoints", "In deine Wegpunkte übernehmen");
        m("show.cooldown", "Please wait a few seconds before showing another waypoint.",
                "Bitte warte ein paar Sekunden, bevor du den nächsten Wegpunkt zeigst.");
        m("show.expired", "This waypoint is no longer available.", "Dieser Wegpunkt ist nicht mehr verfügbar.");

        // Server waypoints
        m("server.usage", "Usage: /{0} server set <name> | /{0} server del <name>",
                "Benutzung: /{0} server set <Name> | /{0} server del <Name>");
        m("server.no-permission", "Only admins can manage server waypoints.",
                "Nur Admins können Server-Wegpunkte verwalten.");
        m("server.exists", "Server waypoint \"{0}\" already exists.", "Den Server-Wegpunkt \"{0}\" gibt es schon.");
        m("server.saved", "Server waypoint \"{0}\" saved: {1} ({2})", "Server-Wegpunkt \"{0}\" gespeichert: {1} ({2})");
        m("server.deleted", "Server waypoint \"{0}\" deleted.", "Server-Wegpunkt \"{0}\" gelöscht.");
        m("server.not-found", "No server waypoint named \"{0}\" found.", "Kein Server-Wegpunkt namens \"{0}\" gefunden.");

        // List
        m("list.empty", "You don't have any waypoints yet. Save one with /{0} set <name>",
                "Du hast noch keine Wegpunkte. Speichern mit /{0} set <Name>");
        m("list.server-header", "Server waypoints:", "Server-Wegpunkte:");
        m("list.header", "Your waypoints ({0}):", "Deine Wegpunkte ({0}):");
        m("btn.go", "[Go]", "[Hin]");
        m("btn.go-hover", "Start navigation to {0}", "Navigation zu {0} starten");
        m("btn.stop", "[Stop]", "[Stopp]");
        m("btn.stop-hover", "Stop navigation", "Navigation beenden");
        m("btn.del-hover", "Delete (press Enter to confirm)", "Löschen (mit Enter bestätigen)");

        // Help
        m("help.header", "Commands:", "Befehle:");
        m("help.menu", "Open the menu", "Menü öffnen");
        m("help.set", "Save your current location", "Aktuellen Ort speichern");
        m("help.nav", "Start navigation", "Navigation starten");
        m("help.stop", "Stop navigation", "Navigation beenden");
        m("help.list", "List all waypoints in chat", "Alle Wegpunkte im Chat");
        m("help.del", "Delete a waypoint", "Wegpunkt löschen");
        m("help.share", "Send a waypoint to a player", "Wegpunkt an einen Spieler schicken");
        m("help.show", "Show a waypoint to everyone in chat", "Wegpunkt allen im Chat zeigen");
        m("help.inbox", "Waypoints shared with you", "Mit dir geteilte Wegpunkte");
        m("help.death", "Turn the death point on/off", "Todespunkt an/aus");
        m("help.lang", "Change language (en/de/auto)", "Sprache ändern (de/en/auto)");
        m("help.server", "Manage server waypoints (admin)", "Server-Wegpunkte verwalten (Admin)");

        // Menu
        m("menu.title", "Waypoints", "Wegpunkte");
        m("menu.prev", "◀ Previous page", "◀ Vorherige Seite");
        m("menu.next", "Next page ▶", "Nächste Seite ▶");
        m("menu.save", "➕ Save current location", "➕ Aktuellen Ort speichern");
        m("menu.save-lore1", "Saves the spot where you", "Speichert die Stelle, an der du");
        m("menu.save-lore2", "are standing right now. You", "gerade stehst. Den Namen gibst");
        m("menu.save-lore3", "type the name in chat afterwards.", "du danach im Chat ein.");
        m("menu.stop", "✖ Stop navigation", "✖ Navigation beenden");
        m("menu.current", "Current target: {0}", "Aktuelles Ziel: {0}");
        m("menu.help", "Help", "Hilfe");
        m("menu.count", "Waypoints: {0} / {1}", "Wegpunkte: {0} / {1}");
        m("menu.page", "Page {0} / {1}", "Seite {0} / {1}");
        m("menu.help1", "Click a waypoint to", "Klick auf einen Wegpunkt:");
        m("menu.help2", "start or stop navigation", "Navigation starten oder beenden");
        m("menu.help3", "Commands: /wp help", "Befehle: /wp help");
        m("menu.delete-on", "Delete mode: ON", "Löschmodus: AN");
        m("menu.delete-on1", "Click a waypoint twice", "Klicke einen Wegpunkt zweimal,");
        m("menu.delete-on2", "to delete it.", "um ihn zu löschen.");
        m("menu.delete-on3", "Click here to finish.", "Hier klicken zum Beenden.");
        m("menu.delete-off", "Delete mode: OFF", "Löschmodus: AUS");
        m("menu.delete-off1", "Click here to be able", "Hier klicken, um Wegpunkte");
        m("menu.delete-off2", "to delete waypoints.", "löschen zu können.");
        m("menu.lang", "Language: {0}", "Sprache: {0}");
        m("menu.lang-auto", "(automatic)", "(automatisch)");
        m("menu.lang-lore", "Click to switch: English / Deutsch / auto",
                "Klick zum Wechseln: English / Deutsch / auto");
        m("menu.death-on", "Death point: ON", "Todespunkt: AN");
        m("menu.death-off", "Death point: OFF", "Todespunkt: AUS");
        m("menu.death-lore", "Saves where you die automatically.", "Speichert automatisch, wo du stirbst.");
        m("menu.toggle", "Click to toggle.", "Klick zum Umschalten.");
        m("menu.inbox", "Inbox ({0})", "Posteingang ({0})");
        m("menu.inbox-lore", "Waypoints others shared with you", "Wegpunkte, die andere mit dir geteilt haben");
        m("item.active", "▶ Active target", "▶ Aktives Ziel");
        m("item.server", "⚑ Server waypoint", "⚑ Server-Wegpunkt");
        m("item.dimension", "Dimension: {0}", "Dimension: {0}");
        m("item.distance", "Distance: {0}", "Entfernung: {0}");
        m("item.saved", "Saved: {0}", "Gespeichert: {0}");
        m("item.confirm", "Click again = DELETE", "Nochmal klicken = LÖSCHEN");
        m("item.select-delete", "Click: select for deletion", "Klick: zum Löschen auswählen");
        m("item.click-stop", "Click: stop navigation", "Klick: Navigation beenden");
        m("item.click-go", "Click: navigate here", "Klick: hierhin navigieren");

        // Inbox menu
        m("inbox.title", "Inbox", "Posteingang");
        m("inbox.empty", "Your inbox is empty.", "Dein Posteingang ist leer.");
        m("inbox.from", "From: {0}", "Von: {0}");
        m("inbox.click", "Click: accept", "Klick: annehmen");
        m("inbox.right", "Right-click: decline", "Rechtsklick: ablehnen");
        m("inbox.back", "◀ Back", "◀ Zurück");
        m("inbox.decline-all", "Decline all", "Alle ablehnen");
        m("inbox.declined-all", "All shared waypoints discarded.", "Alle geteilten Wegpunkte verworfen.");
    }

    public static String t(Lang lang, String key, Object... args) {
        String text = (lang == Lang.DE ? DE : EN).get(key);
        if (text == null) {
            text = EN.getOrDefault(key, key);
        }
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }

    public static String dimension(Lang lang, String dimension) {
        String key = "dim." + dimension;
        return EN.containsKey(key) ? t(lang, key) : t(lang, "dim.CUSTOM");
    }

    static boolean has(String key) {
        return EN.containsKey(key);
    }

    /** Für Tests: gibt es für jeden Schlüssel beide Sprachen? */
    static boolean complete() {
        return EN.keySet().equals(DE.keySet());
    }
}
