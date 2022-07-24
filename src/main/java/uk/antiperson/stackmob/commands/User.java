package uk.antiperson.stackmob.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import uk.antiperson.stackmob.utils.Utilities;

public record User(CommandSender sender) {

    public void sendRawMessage(String message) {
        sender.sendMessage(Component.text(message));
    }

    public void sendInfo(String message) {
        sendMessage(MessageType.INFO, message);
    }

    public void sendError(String message) {
        sendMessage(MessageType.ERROR, message);
    }

    public void sendSuccess(String message) {
        sendMessage(MessageType.SUCCESS, message);
    }

    private void sendMessage(MessageType type, String string) {
        sendMessage(type, Component.text(string));
    }

    private void sendMessage(MessageType type, Component component) {
        component = switch (type) {
            case INFO -> component.color(NamedTextColor.YELLOW);
            case ERROR -> component.color(NamedTextColor.RED);
            case SUCCESS -> component.color(NamedTextColor.GREEN);
        };
        sender.sendMessage(Utilities.PREFIX.append(component));
    }

    enum MessageType {
        INFO,
        ERROR,
        SUCCESS
    }
}
