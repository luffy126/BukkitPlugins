package cm.simvnvevo;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class PluginMain extends JavaPlugin implements Listener {

    private File jugadoresMuertos;
    private FileConfiguration jugadoresMuertosConfig;
    private HashMap<UUID, Integer> muertesJugadores = new HashMap<>();

    // cargar guardar y escribir

    public void cargarArchivo() {
        jugadoresMuertos = new File(getDataFolder(), "jugadores_muertos.yml");

        if(!jugadoresMuertos.exists()){
            jugadoresMuertos.getParentFile().mkdirs();
            saveResource("jugadores_muertos.yml", false);
        }

        jugadoresMuertosConfig = YamlConfiguration.loadConfiguration(jugadoresMuertos);

        if (jugadoresMuertosConfig.getConfigurationSection("jugadores") == null) {
            jugadoresMuertosConfig.createSection("jugadores");
            guardarArchivo();
        }

        for (String uuidStr : jugadoresMuertosConfig.getConfigurationSection("jugadores").getKeys(false)) {
            UUID IDJugador = UUID.fromString(uuidStr);
            int contadorJugador = jugadoresMuertosConfig.getInt("jugadores." + uuidStr, 0);
            muertesJugadores.put(IDJugador, contadorJugador);
        }

    }

    private void guardarArchivo() {
        try {
            jugadoresMuertosConfig.save(jugadoresMuertos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void guardarJugador(UUID ID) {

        String key = ID.toString();
        String path = "jugadores." + ID.toString();

        // primera vez entrando?
        if (!jugadoresMuertosConfig.contains(path)) {
            jugadoresMuertosConfig.set(path, 0);
            guardarArchivo();
            muertesJugadores.put(ID, 0);
        }

        // ya entró antes, se carga su contador
        else {
            int contador = jugadoresMuertosConfig.getInt(path, 0);
            muertesJugadores.put(ID, contador);
        }
    }

    private int getContadorMuertes(UUID key) {
        return muertesJugadores.getOrDefault(key, 0);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player jugador = event.getPlayer();
        UUID IDJugadorJoin = jugador.getUniqueId();

        guardarJugador(IDJugadorJoin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player jugador = event.getEntity();
        UUID IDJugador = jugador.getUniqueId();
        int contadorJugador = getContadorMuertes(IDJugador);
        String path = "jugadores." + IDJugador.toString();

        contadorJugador++;
        muertesJugadores.put(IDJugador, contadorJugador);
        jugadoresMuertosConfig.set(path, contadorJugador);
        guardarArchivo();
    }

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage("Activando Plugin ContadorMuertes...");
        cargarArchivo();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("Gracias por usar el plugin!");
    }

}
