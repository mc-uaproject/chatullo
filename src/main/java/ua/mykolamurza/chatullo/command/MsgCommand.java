package ua.mykolamurza.chatullo.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mykolamurza.chatullo.configuration.Config;
import ua.mykolamurza.chatullo.handler.ChatHandler;
import ua.mykolamurza.chatullo.handler.MessageType;

public class MsgCommand implements CommandExecutor {

    private final ChatHandler chatHandler;

    public MsgCommand(ChatHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!commandSender.hasPermission("chatullo.msg") && !commandSender.isOp()) {
            commandSender.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.permission")));
            return true;
        }
        if (commandSender instanceof Player sender) {
            if (strings.length < 2) {
                commandSender.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.msg")));
                return true;
            }

            String playerName = strings[0];
            if (playerName == null) {
                sender.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.player")));
                return true;
            }

            Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                sender.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.player")));
                return true;
            }

            if (sender.getName().equals(playerName)) {
                sender.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.self")));
                return true;
            }

            if (chatHandler.payWithItem(sender)) {
                player.sendMessage(chatHandler.formatMessage(MessageType.PRIVATE_TO, sender, String.join(" ", strings).replaceFirst(playerName, "")));
                sender.sendMessage(chatHandler.formatMessage(MessageType.PRIVATE_FROM, player, String.join(" ", strings).replaceFirst(playerName, "")));
            } else {
                commandSender.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.item")));
            }
            return true;
        } else {
            commandSender.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.console")));
            return true;
        }
    }

}
