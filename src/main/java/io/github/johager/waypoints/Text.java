package io.github.johager.waypoints;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/** Kleine Helfer für farbige Texte. */
public final class Text {

    private Text() {
    }

    public static Component of(String text, TextColor color) {
        return Component.text(text, color);
    }

    public static Component bold(String text, TextColor color) {
        Component c = Component.text(text, color);
        return c.decoration(TextDecoration.BOLD, true);
    }

    /** Für Item-Namen und Beschreibungen: ohne die automatische Kursivschrift. */
    public static Component item(String text, TextColor color) {
        Component c = Component.text(text, color);
        return c.decoration(TextDecoration.ITALIC, false);
    }

    public static Component prefix(Lang lang) {
        return Component.text(I18n.t(lang, "prefix"), NamedTextColor.DARK_AQUA);
    }

    public static Component info(Lang lang, String text) {
        return prefix(lang).append(Component.text(text, NamedTextColor.GRAY));
    }

    public static Component success(Lang lang, String text) {
        return prefix(lang).append(Component.text(text, NamedTextColor.GREEN));
    }

    public static Component error(Lang lang, String text) {
        return prefix(lang).append(Component.text(text, NamedTextColor.RED));
    }

    /** Ein anklickbarer Knopf, der einen Befehl ausführt. */
    public static Component button(String label, TextColor color, String command, String hover) {
        Component c = Component.text(label, color);
        c = c.decoration(TextDecoration.BOLD, true);
        c = c.clickEvent(ClickEvent.runCommand(command));
        return c.hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)));
    }

    /** Ein anklickbarer Knopf, der den Befehl nur in die Chatzeile schreibt (zum Bestätigen). */
    public static Component suggestButton(String label, TextColor color, String command, String hover) {
        Component c = Component.text(label, color);
        c = c.decoration(TextDecoration.BOLD, true);
        c = c.clickEvent(ClickEvent.suggestCommand(command));
        return c.hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)));
    }
}
