package ua.mykolamurza.chatullo.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ua.mykolamurza.chatullo.configuration.Config;
import ua.mykolamurza.chatullo.handler.ChatHandler;

import java.util.List;
import java.util.regex.Pattern;

public class PlayerChat implements Listener {

    private final ChatHandler chatHandler;

    public PlayerChat(ChatHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        TextComponent message = (TextComponent) event.message();

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
        if (!pattern.matcher(message.content()).matches()) {
            player.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.banned")));
            return;
        }

        if (message.content().startsWith("!")) {
            writeToTheGlobalChat(event, player, message);
        } else {
            writeToTheLocalChat(event, player, message);
        }
    }

    private void writeToTheGlobalChat(AsyncChatEvent event, Player player, TextComponent message) {
        if (!player.hasPermission("chatullo.global") && !player.isOp()) {
            player.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.permission")));
            return;
        }

        if (chatHandler.payWithItem(player)) {
            chatHandler.writeToGlobalChat(event, player, message.content().substring(1));
        }
    }

    private void writeToTheLocalChat(AsyncChatEvent event, Player player, TextComponent message) {
        if (!player.hasPermission("chatullo.local") && !player.isOp()) {
            player.sendMessage(chatHandler.formatMessage(Config.messages.getString("error.permission")));
            return;
        }

        chatHandler.writeToLocalChat(event, player, message.content());
    }
}
