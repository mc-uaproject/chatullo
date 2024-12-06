package ua.mykolamurza.chatullo.listener;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import ua.mykolamurza.chatullo.Chatullo;

import java.util.Random;

public class BlockBreak implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Material block = event.getBlock().getType();
        if ((block == Material.REDSTONE_ORE || block == Material.DEEPSLATE_REDSTONE_ORE) && event.getExpToDrop() > 0) {
            if (random.nextInt(100) < 5) {
                giveItems(event.getPlayer(), random.nextInt(3) + 1);
                event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1, 1);
            }
        }
    }

    private void giveItems(Player player, int itemCount) {
        ItemStack item = Chatullo.getPayItem(itemCount);
        player.getWorld().dropItemNaturally(player.getLocation(), item);
    }

}
