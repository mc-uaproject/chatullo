package ua.mykolamurza.chatullo;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ua.mykolamurza.chatullo.command.MsgCommand;
import ua.mykolamurza.chatullo.command.MsgComplete;
import ua.mykolamurza.chatullo.command.RootCommand;
import ua.mykolamurza.chatullo.command.RootComplete;
import ua.mykolamurza.chatullo.configuration.Config;
import ua.mykolamurza.chatullo.handler.ChatHandler;
import ua.mykolamurza.chatullo.listener.BlockBreak;
import ua.mykolamurza.chatullo.listener.PlayerChat;
import ua.mykolamurza.chatullo.listener.PlayerJoin;
import ua.mykolamurza.chatullo.listener.PlayerQuit;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Local and global chat system. Pay to write to the server.
 *
 * @author Mykola Murza
 * @version Minecraft 1.20
 */
public final class Chatullo extends JavaPlugin {
    public static Chatullo plugin = null;
    public static boolean papi = false;
    private final Map<UUID, LocalDate> lastLogin = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        plugin = this;

        saveDefaultConfig();
        Config.initialize();
        loadLastLoginData();

        getServer().getPluginManager().registerEvents(new PlayerChat(ChatHandler.getInstance()), this);
        getServer().getPluginManager().registerEvents(new PlayerJoin(ChatHandler.getInstance()), this);
        getServer().getPluginManager().registerEvents(new PlayerQuit(ChatHandler.getInstance()), this);
        getServer().getPluginManager().registerEvents(new BlockBreak(), this);

        Objects.requireNonNull(getCommand("chatullo")).setExecutor(new RootCommand(ChatHandler.getInstance()));
        Objects.requireNonNull(getCommand("chatullo")).setTabCompleter(new RootComplete());
        Objects.requireNonNull(getCommand("msg")).setExecutor(new MsgCommand(ChatHandler.getInstance()));
        Objects.requireNonNull(getCommand("msg")).setTabCompleter(new MsgComplete());

        if (!setupPAPI()) {
            getLogger().warning("PlaceholderAPI not found, functionality will be missing.");
        }

        if (!setupNBT()) {
            getLogger().warning("NBTAPI not found, functionality will be missing.");
        }

    }

    private boolean setupPAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return false;
        }
        papi = true;

        return true;
    }

    private boolean setupNBT() {
        return Bukkit.getPluginManager().getPlugin("NBTAPI") != null;
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
}
