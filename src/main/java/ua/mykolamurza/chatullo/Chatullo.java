package ua.mykolamurza.chatullo;

import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ua.mykolamurza.chatullo.command.*;
import ua.mykolamurza.chatullo.configuration.Config;
import ua.mykolamurza.chatullo.handler.ChatHandler;
import ua.mykolamurza.chatullo.listener.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * Local and global chat system. Pay to write to the server.
 *
 * @author Mykola Murza
 * @version Minecraft 1.20
 */
public final class Chatullo extends JavaPlugin{
    public static Chatullo plugin = null;
    public static boolean papi = false;
    private final Map<UUID, LocalDate> lastLogin = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, Boolean> particles = new HashMap<>();
    private File particlesFile;
    private FileConfiguration particlesConfig;

    @Override
    public void onEnable() {
        plugin = this;

        saveDefaultConfig();
        Config.initialize();
        loadLastLoginData();
        loadParticles();

        getServer().getPluginManager().registerEvents(new PlayerChat(ChatHandler.getInstance()), this);
        getServer().getPluginManager().registerEvents(new PlayerJoin(ChatHandler.getInstance()), this);
        getServer().getPluginManager().registerEvents(new PlayerQuit(ChatHandler.getInstance()), this);
        getServer().getPluginManager().registerEvents(new ItemCraft(plugin), this);
        getServer().getPluginManager().registerEvents(new BlockBreak(), this);


        Objects.requireNonNull(getCommand("chatullo")).setExecutor(new RootCommand(ChatHandler.getInstance()));
        Objects.requireNonNull(getCommand("chatullo")).setTabCompleter(new RootComplete());
        Objects.requireNonNull(getCommand("msg")).setExecutor(new MsgCommand(ChatHandler.getInstance()));
        Objects.requireNonNull(getCommand("msg")).setTabCompleter(new MsgComplete());
        Objects.requireNonNull(getCommand("particles")).setExecutor(new ParticleCommand(ChatHandler.getInstance()));
        Objects.requireNonNull(getCommand("particles")).setTabCompleter(new ParticleComplete());

        if (!setupPAPI()) {
            getLogger().warning("PlaceholderAPI not found, functionality will be missing.");
        }
    }

    private boolean setupPAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return false;
        }
        papi = true;

        return true;
    }

    public Map<UUID, LocalDate> getLastLogin() {
        return lastLogin;
    }

    public void saveLastLoginData() {
        if (dataConfig == null || dataFile == null) {
            return;
        }

        for (Map.Entry<UUID, LocalDate> entry : lastLogin.entrySet()) {
            dataConfig.set(entry.getKey().toString(), entry.getValue().toString());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLastLoginData() {
        dataFile = new File(getDataFolder(), "lastLogin.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            saveResource("lastLogin.yml", false);
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : dataConfig.getKeys(false)) {
            lastLogin.put(UUID.fromString(key), LocalDate.parse(Objects.requireNonNull(dataConfig.getString(key))));
        }
    }

    public Map<UUID, Boolean> getParticles() {
        return particles;
    }

    public void saveParticles() {
        if (particlesConfig == null || particlesFile == null) {
            return;
        }

        for (Map.Entry<UUID, Boolean> entry : particles.entrySet()) {
            particlesConfig.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            particlesConfig.save(particlesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadParticles() {
        particlesFile = new File(getDataFolder(), "particles.yml");
        if (!particlesFile.exists()) {
            particlesFile.getParentFile().mkdirs();
            saveResource("particles.yml", false);
        }

        particlesConfig = YamlConfiguration.loadConfiguration(particlesFile);
        for (String key : particlesConfig.getKeys(false)) {
            particles.put(UUID.fromString(key), particlesConfig.getBoolean(key));
        }
    }

    public static ItemStack getPayItem(int amount) {
        Material payMaterial = Material.valueOf(Config.settings.getString("global-pay.item.material"));
        ItemStack item = new ItemStack(payMaterial);

        String custom_data = Config.settings.getString("global-pay.item.custom-data-tag");
        NamespacedKey key = new NamespacedKey(plugin, custom_data);

        item.editMeta(meta -> {
            meta.displayName(text("Чарівний пил", RED).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(text("Приваблює чарівних воронів!", WHITE).decoration(TextDecoration.ITALIC, false),
                    text("Надає одне спеціальне повідомлення", WHITE).decoration(TextDecoration.ITALIC, false),
                    text("Залишилося використань: " + amount, GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, amount);
        });
        return item;
    }
}
