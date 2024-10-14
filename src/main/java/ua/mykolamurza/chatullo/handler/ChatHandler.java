package ua.mykolamurza.chatullo.handler;

import de.tr7zw.nbtapi.NBTItem;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ua.mykolamurza.chatullo.Chatullo;
import ua.mykolamurza.chatullo.configuration.Config;
import ua.mykolamurza.chatullo.mention.AsciiTree;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * @author Mykola Murza
 * @author justADeni
 */
public class ChatHandler {
    private static ChatHandler instance = null;
    private final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private AsciiTree tree = null;

    private ChatHandler() {
    }

    public static ChatHandler getInstance() {
        if (instance == null) {
            instance = new ChatHandler();
        }
        return instance;
    }

    private static int square(int input) {
        return input * input;
    }

    public void updateTree() {
        tree = new AsciiTree(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
    }

    public void writeToGlobalChat(AsyncChatEvent event, Player player, String message) {
        event.viewers().forEach(recipient ->
                recipient.sendMessage(formatMessage(MessageType.GLOBAL, player, formatMentions(player, recipient, message))));
    }

    public void writeToLocalChat(AsyncChatEvent event, Player player, String message) {
        event.viewers().stream()
                .filter(audience -> audience instanceof Player && isPlayerHearLocalChat(player, (Player) audience)
                                    || audience instanceof ConsoleCommandSender)
                .forEach(recipient ->
                        recipient.sendMessage(formatMessage(MessageType.LOCAL, player, formatMentions(player, recipient, message))));
    }

    public TextComponent formatMessage(MessageType type, Player player, String message) {
        String formatted = switch (type) {
            case GLOBAL -> Config.settings.getString("global-format");
            case LOCAL -> Config.settings.getString("local-format");
            case PRIVATE_FROM -> Config.settings.getString("private-format-from");
            case PRIVATE_TO -> Config.settings.getString("private-format-to");
            case OTHER -> message;
        };

        Disguise disguise = DisguiseAPI.getDisguise(player);
        String playerName;
        if (disguise != null) {
            if (disguise instanceof PlayerDisguise playerDisguise) {
                playerName = playerDisguise.getName();
            } else {
                playerName = player.getName();
            }
        } else {
            playerName = player.getName();
        }

        if (player.hasMetadata("dreamy_mist")) {
            playerName = "%$@!#&*)";
        }

        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            PotionEffect invisibilityEffect = Objects.requireNonNull(player.getPotionEffect(PotionEffectType.INVISIBILITY));
            int level = invisibilityEffect.getAmplifier();
            playerName = generateObfuscatedName(player.getName(), level);
        }

        if (Chatullo.papi) {
            return LEGACY.deserialize(PlaceholderAPI.setPlaceholders(player,
                    formatted.replace("%player%", playerName)).replace("%message%", message));
        } else {
            return LEGACY.deserialize(
                    formatted.replace("%player%", playerName).replace("%message%", message));
        }
    }


    private String generateObfuscatedName(String playerName, int level) {
        Random random = new Random();
        StringBuilder obfuscated = new StringBuilder();
        int length = Math.max(3, playerName.length() - (level + 1));

        for (int i = 0; i < length; i++) {
            char symbol = (char) (random.nextInt(93) + 33);
            obfuscated.append(symbol);
        }
        return obfuscated.toString();
    }

    public boolean payWithItem(Player player) {
        if (!player.hasPermission("chatullo.bypass") && !player.isOp()) {
            if (true) {
//            if (Config.settings.getBoolean("global-pay.item.enabled")) {
                PlayerInventory inventory = player.getInventory();
                if (!inventory.contains(Material.valueOf(Config.settings.getString("global-pay.item.material")))) {
                    player.sendMessage(formatMessage(Config.messages.getString("error.item")));
                    return false;
                }

                ItemStack[] items = inventory.getContents();
                for (ItemStack itemStack : items) {
                    if (itemStack == null) {
                        continue;
                    }
                    if (itemStack.getType() == Material.valueOf(Config.settings.getString("global-pay.item.material"))) {
                        String nbt = Config.settings.getString("global-pay.item.nbt");
                        NBTItem nbtItem = new NBTItem(itemStack);
                        if (nbtItem.getString("nbt") != null && nbtItem.getString("nbt").equals(nbt)) {
                            itemStack.setAmount(itemStack.getAmount() - 1);
                            return true;
                        }
                    }
                }
                player.sendMessage(formatMessage(Config.messages.getString("error.item")));
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }


    public TextComponent formatMessage(String message) {
        return LEGACY.deserialize(message);
    }

    private boolean isPlayerHearLocalChat(Player player, Player viewer) {
        return viewer.getWorld().equals(player.getWorld()) &&
               viewer.getLocation().distanceSquared(player.getLocation()) <= square(Config.settings.getInt("radius"));
    }

    private String formatMentions(Player player, Audience recipient, String message) {
        if (recipient instanceof ConsoleCommandSender) {
            return message;
        }

        if (recipient == player) {
            return message;
        }

        if (!Config.settings.getBoolean("mentions.enabled")) {
            return message;
        }

        String formatted = message;
        int additional = 0;
        List<Integer> foundIndexes = tree.findMultiple(formatted);
        if (!foundIndexes.isEmpty()) {
            for (int index : foundIndexes) {
                int start = (index >> 16) + additional;
                int end = start + (short) (index);
                String word = formatted.substring(start, end);

                if (word.equals(player.getName())) {
                    continue;
                }

                if (!word.equals(((Player) recipient).getName())) {
                    continue;
                }

                if (Config.settings.getBoolean("mentions.highlight.enabled")) {
                    String replaced = Config.settings.getString("mentions.highlight.format").replace("%player%", word);
                    String intermediate = formatted.substring(0, start) + replaced + formatted.substring(end);

                    // We have to account for the fact that formatting shifts indexes around
                    additional += intermediate.length() - formatted.length();
                    formatted = intermediate;
                }

                if (Config.settings.getBoolean("mentions.sound.enabled")) {
                    float volume = (float) Config.settings.getDouble("mentions.sound.volume");
                    float pitch = (float) Config.settings.getDouble("mentions.sound.pitch");
                    String name = Config.settings.getString("mentions.sound.name");
                    Sound sound = Sound.sound(Key.key(name), Sound.Source.BLOCK, volume, pitch);
                    recipient.playSound(sound);
                }

                if (Config.settings.getBoolean("mentions.actionbar.enabled")) {
                    recipient.sendActionBar(LEGACY.deserialize(Config.messages.getString("actionbar")));
                }
            }
        }

        return formatted;
    }
}