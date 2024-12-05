package ua.mykolamurza.chatullo.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;
import ua.mykolamurza.chatullo.Chatullo;
import ua.mykolamurza.chatullo.configuration.Config;
import ua.mykolamurza.chatullo.handler.ChatHandler;
import ua.mykolamurza.chatullo.handler.MessageType;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class PlayerJoin implements Listener {
    private final ChatHandler chatHandler;

    public PlayerJoin(ChatHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (Config.settings.getBoolean("join")) {
            String message = Config.messages.getString("join");
            if (message == null || message.isBlank()) {
                event.joinMessage(null);
            } else {
                event.joinMessage(chatHandler.formatMessage(MessageType.OTHER, event.getPlayer(), message));
            }
        } else {
            event.joinMessage(null);
        }

        if (Config.settings.getBoolean("mentions.enabled")) {
            ChatHandler.getInstance().updateTree();
        }
    }

    @EventHandler
    public void onPlayerJoinDaily(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        LocalDate today = LocalDate.now();

        Map<UUID, LocalDate> lastLogin = Chatullo.plugin.getLastLogin();
        LocalDate lastLoginDate = lastLogin.getOrDefault(playerUUID, LocalDate.MIN);
        if (!lastLoginDate.equals(today)) {
            int itemCount = getPermissionItemCount(player);
            if (itemCount > 0) {
                giveItems(player, itemCount);
                lastLogin.put(playerUUID, today);
                Chatullo.plugin.saveLastLoginData();
                player.sendMessage(Component.text("Дякуємо за підтримку сервера!", NamedTextColor.AQUA));
                player.sendMessage(Component.text("Ви отримали щоденний подарунок!", NamedTextColor.AQUA));
            }
        }
    }
    @EventHandler
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Chatullo.plugin.getParticles().computeIfAbsent(player.getUniqueId(), func -> true);
        Chatullo.plugin.saveParticles();
    }

    @EventHandler
    public void onRedstonePlacement(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        Material material = Material.valueOf(Config.settings.getString("global-pay.item.material"));
        if (item.getType() != material) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        String custom_data = Config.settings.getString("global-pay.item.custom-data-tag");
        NamespacedKey key = new NamespacedKey(Chatullo.plugin, custom_data);
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            event.getPlayer().sendMessage(Component.text("Ваші руки не можуть випустити цей предмет!",
                    NamedTextColor.GOLD));
            event.setCancelled(true);
        }
    }

    private int getPermissionItemCount(Player player) {
        for (String permission : player.getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission).toList()) {
            if (permission.startsWith("chatullo.free.")) {
                try {
                    return Integer.parseInt(permission.split("\\.")[2]);
                } catch (NumberFormatException e) {
                    Chatullo.plugin.getLogger().warning("Invalid permission format: " + permission);
                }
            }
        }
        return 0;
    }

    private void giveItems(Player player, int itemCount) {
        ItemStack item = Chatullo.getPayItem(itemCount);

        if (player.getInventory().firstEmpty() == -1) {
            player.getLocation().getBlock().getWorld().dropItem(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
    }

}
