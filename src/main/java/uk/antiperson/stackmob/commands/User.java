package uk.antiperson.stackmob.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import uk.antiperson.stackmob.utils.Utilities;

public record User(CommandSender sender) {

    public void sendRawMessage(String message) {
        sender.sendMessage(message);
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

    private void sendMessage(MessageType type, String rawMessage) {
        StringBuilder message = new StringBuilder(Utilities.PREFIX);
        switch (type) {
            case INFO -> message.append(ChatColor.YELLOW);
            case ERROR -> message.append(ChatColor.RED);
            case SUCCESS -> message.append(ChatColor.GREEN);
        }
        message.append(rawMessage);
        sender.sendMessage(message.toString());
    }

    enum MessageType {
        INFO,
        ERROR,
        SUCCESS
    }
}
