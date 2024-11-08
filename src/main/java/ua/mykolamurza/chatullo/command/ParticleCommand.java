package ua.mykolamurza.chatullo.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mykolamurza.chatullo.Chatullo;
import ua.mykolamurza.chatullo.configuration.Config;
import ua.mykolamurza.chatullo.handler.ChatHandler;

public class ParticleCommand implements CommandExecutor {

    private final ChatHandler chatHandler;

    public ParticleCommand(ChatHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 1 && args[0].equalsIgnoreCase("on")) {
                Chatullo.plugin.getParticles().put(player.getUniqueId(), true);
                Chatullo.plugin.saveParticles();
                player.sendMessage(chatHandler.formatMessage(Config.messages.getString("particles-on")));
                return true;
            }else if (args.length == 1 && args[0].equalsIgnoreCase("off")) {
                Chatullo.plugin.getParticles().put(player.getUniqueId(), false);
                Chatullo.plugin.saveParticles();
                player.sendMessage(chatHandler.formatMessage(Config.messages.getString("particles-off")));
                return true;
            }
            player.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.args")));
        }
        return false;
    }
}
