package ua.mykolamurza.chatullo.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mykolamurza.chatullo.Chatullo;
import ua.mykolamurza.chatullo.configuration.Config;
import ua.mykolamurza.chatullo.handler.ChatHandler;
import ua.mykolamurza.chatullo.handler.PrivateMessageHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public class MsgCommand implements CommandExecutor {

    private final ChatHandler chatHandler;
    private final Chatullo chatullo;
    private Cache<UUID, Long> cooldown = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();


    public MsgCommand(ChatHandler chatHandler) {
        this.chatHandler = chatHandler;
        this.chatullo = Chatullo.plugin;
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

            if (cooldown.asMap().containsKey(sender.getUniqueId())) {
                long timeLeft = cooldown.asMap().get(sender.getUniqueId()) - System.currentTimeMillis();
                sender.sendMessage(text("Ви зможете надіслати повідомлення, через " + TimeUnit.MILLISECONDS.toSeconds(timeLeft) + " секунд", RED));
                return true;
            }

            List<Character> extraAllowedChars = Config.settings.getCharacterList("allowed-chars");

            StringBuilder extraCharsBuilder = new StringBuilder();
            for (Character ch : extraAllowedChars) {
                if ("\\^$.|?*+()[]{}".indexOf(ch) != -1) {
                    extraCharsBuilder.append("\\");
                }
                extraCharsBuilder.append(ch);
            }

            String combinedPattern = "[\\p{IsCyrillic}\\p{IsLatin}\\d\\p{Punct}\\s" + extraCharsBuilder + "]+";

            Pattern pattern = Pattern.compile(combinedPattern);
            if (!pattern.matcher(String.join(" ", strings).replaceFirst(playerName, "")).matches()) {
                sender.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.banned")));
                return true;
            }

            if (chatHandler.payWithItem(sender)) {
                sender.sendMessage(chatHandler.formatMessage(Config.settings.getString("magic-bee-send")));
                cooldown.put(sender.getUniqueId(), System.currentTimeMillis() + 10000);
                spawnBee(player, sender, String.join(" ", strings).replaceFirst(playerName, ""));
            }
        } else {
            commandSender.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.console")));
        }
        return true;
    }

    public void spawnBee(Player recipient, Player sender, String message) {
        double radius = 20.0;
        double xOffset;
        double zOffset;
        Location spawnLocation;

        for (int i = 0; i < 20; i++) {
            xOffset = (Math.random() - 0.5) * radius * 2;
            zOffset = (Math.random() - 0.5) * radius * 2;
            spawnLocation = recipient.getLocation().add(xOffset, 0, zOffset);
            Bee bee = (Bee) recipient.getWorld().spawnEntity(spawnLocation, EntityType.BEE);
            if (!bee.getPathfinder().findPath(recipient).canReachFinalPoint()) {
                bee.remove();
            }else {
                bee.setInvulnerable(true);
                bee.setCollidable(false);
                bee.customName(text("Мандрівна бджола (Натисни, щоб прочитати)", GOLD).decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true));
                bee.setCustomNameVisible(true);

                chatullo.getServer().getPluginManager().registerEvents(new PrivateMessageHandler(chatHandler, message, sender, recipient, bee), chatullo);
                break;
            }
        }

    }

}
