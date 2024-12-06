package ua.mykolamurza.chatullo.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import ua.mykolamurza.chatullo.Chatullo;
import ua.mykolamurza.chatullo.configuration.Config;

import java.util.Objects;

public class ItemCraft implements Listener {
    final ShapelessRecipe recipe;
    final String custom_data = Config.settings.getString("global-pay.item.custom-data-tag");
    final NamespacedKey key = new NamespacedKey(Chatullo.plugin, custom_data);

    public ItemCraft(Plugin plugin) {
        recipe = new ShapelessRecipe(new NamespacedKey(plugin, "powder-recipe"), Chatullo.getPayItem(1));
        recipe.setCategory(CraftingBookCategory.MISC);
        Material material = Material.valueOf(Config.settings.getString("global-pay.item.material"));
        recipe.addIngredient(material);
        recipe.addIngredient(material);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onItemPrepareCraft(PrepareItemCraftEvent event){
        if (event.getInventory().getResult() == null) return;

        boolean isFound = false;
        int amount = 0;
        for (int i = 1; i <= 9; i++) {
            ItemStack itemStack = event.getInventory().getItem(i);
            if (itemStack == null) continue;
            if (itemStack.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
                amount += Objects.requireNonNull(itemStack.getPersistentDataContainer().get(key, PersistentDataType.INTEGER));
                isFound = true;
            }
        }
        if (event.getInventory().getResult().getPersistentDataContainer().has(key, PersistentDataType.INTEGER) && isFound) {
            event.getInventory().setResult(Chatullo.getPayItem(amount));
        }else if (!event.getInventory().getResult().getType().equals(Material.valueOf(Config.settings.getString("global-pay.item.material")))
                && isFound) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
        }else if (event.getInventory().getResult().getType().equals(Material.valueOf(Config.settings.getString("global-pay.item.material")))
                && !isFound) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler
    public void onItemSmith(SmithItemEvent event) {
        if (event.getInventory().containsAtLeast(Chatullo.getPayItem(1), 1) && event.getWhoClicked() instanceof Player player) {
            player.sendMessage(Component.text("Ваші руки не можуть випустити цей предмет!", NamedTextColor.GOLD));
            event.setCancelled(true);
        }
    }
}
