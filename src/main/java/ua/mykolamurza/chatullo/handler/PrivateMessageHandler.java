package ua.mykolamurza.chatullo.handler;

import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ua.mykolamurza.chatullo.Chatullo;
import ua.mykolamurza.chatullo.configuration.Config;

import java.util.Objects;

public class PrivateMessageHandler implements Listener {
    private final ChatHandler chatHandler;
    String message;
    Player sender;
    Player recipient;
    BukkitTask task;
    Bee bee;
    BukkitTask beeDespawnTask;

    public PrivateMessageHandler(ChatHandler chatHandler, String message, Player sender, Player recipient, Bee bee) {
        this.chatHandler = chatHandler;
        this.message = message;
        this.sender = sender;
        this.recipient = recipient;
        this.bee = bee;

        this.task = new BukkitRunnable(){
            @Override
            public void run() {
                if (!bee.isValid() || !recipient.isOnline()) {
                    bee.remove();
                    return;
                }
                bee.getPathfinder().stopPathfinding();
                bee.setFlower(null);

                Pathfinder.PathResult result = bee.getPathfinder().findPath(recipient);

                if (Objects.requireNonNull(result).canReachFinalPoint()) {
                    bee.getPathfinder().moveTo(result, 1);
                }else {
                    bee.teleport(new Location(recipient.getWorld(), recipient.getLocation().getX(), recipient.getLocation().getY(), recipient.getLocation().getZ()));
                }

                bee.getWorld().spawnParticle(Particle.END_ROD, bee.getLocation(), 10, 0.1,0.1,0.1, 0.02);
            }
        }.runTaskTimer(Chatullo.plugin, 0L, 5);

        beeDespawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                recipient.sendMessage(chatHandler.formatMessage(Config.messages.getString("magic-bee-receive")));
                recipient.sendMessage(chatHandler.formatMessage(MessageType.PRIVATE_TO, sender, message));
                sender.sendMessage(chatHandler.formatMessage(MessageType.PRIVATE_FROM, sender, message));
                selfDestruct();
            }
        }.runTaskLater(Chatullo.plugin, 400);


    }
    @EventHandler
    public void playerClick(PlayerInteractEntityEvent event) {
        if (event.getPlayer().equals(recipient) && event.getRightClicked().equals(bee)) {
            recipient.sendMessage(chatHandler.formatMessage(Config.messages.getString("magic-bee-receive")));
            recipient.sendMessage(chatHandler.formatMessage(MessageType.PRIVATE_TO, sender, message));
            sender.sendMessage(chatHandler.formatMessage(MessageType.PRIVATE_FROM, sender, message));
            selfDestruct();
        }
    }
    private void selfDestruct() {
        if (beeDespawnTask != null) {
            beeDespawnTask.cancel();
            beeDespawnTask = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (bee != null && bee.isValid()) {
            bee.remove();
            bee = null;
        }
        HandlerList.unregisterAll(this);

        message = null;
        sender = null;
        recipient = null;

    }

}
