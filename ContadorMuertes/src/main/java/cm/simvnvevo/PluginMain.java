package cm.simvnvevo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PluginMain extends JavaPlugin implements Listener {

    private static final int TOP_N = 10;

    private File jugadoresMuertos;
    private FileConfiguration jugadoresMuertosConfig;
    private final HashMap<UUID, Integer> muertesJugadores = new HashMap<>();

    // ----------------- archivo -----------------

    public void cargarArchivo() {
        jugadoresMuertos = new File(getDataFolder(), "jugadores_muertos.yml");

        if (!jugadoresMuertos.exists()) {
            jugadoresMuertos.getParentFile().mkdirs();
            saveResource("jugadores_muertos.yml", false);
        }

        jugadoresMuertosConfig = YamlConfiguration.loadConfiguration(jugadoresMuertos);

        if (jugadoresMuertosConfig.getConfigurationSection("jugadores") == null) {
            jugadoresMuertosConfig.createSection("jugadores");
            guardarArchivo();
        }

        muertesJugadores.clear();
        for (String uuidStr : jugadoresMuertosConfig.getConfigurationSection("jugadores").getKeys(false)) {
            try {
                UUID id = UUID.fromString(uuidStr);
                int muertes = jugadoresMuertosConfig.getInt("jugadores." + uuidStr, 0);
                muertesJugadores.put(id, muertes);
            } catch (IllegalArgumentException ignored) {
                // UUID invalido
            }
        }
    }

    private void guardarArchivo() {
        try {
            jugadoresMuertosConfig.save(jugadoresMuertos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ----------------- jugadores -----------------

    private void guardarJugador(UUID id, String nombreActual) {
        String pathMuertes = "jugadores." + id.toString();
        String pathNombre = "nombres." + id.toString(); // opcional para mostrar offline con nombre

        if (!jugadoresMuertosConfig.contains(pathMuertes)) {
            jugadoresMuertosConfig.set(pathMuertes, 0);
            muertesJugadores.put(id, 0);
            jugadoresMuertosConfig.set(pathNombre, nombreActual);
            guardarArchivo();
        } else {
            int muertes = jugadoresMuertosConfig.getInt(pathMuertes, 0);
            muertesJugadores.put(id, muertes);

            jugadoresMuertosConfig.set(pathNombre, nombreActual);
            guardarArchivo();
        }
    }

    private int getContadorMuertes(UUID id) {
        return muertesJugadores.getOrDefault(id, 0);
    }

    // ----------------- scoreboard top muertes -----------------

    private void actualizarScoreboardTopGlobal() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard sb = manager.getNewScoreboard();
        Objective obj = sb.registerNewObjective("topmuertes", "dummy",
                ChatColor.RED + "" + ChatColor.BOLD + "Top muertes");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<Map.Entry<UUID, Integer>> top = muertesJugadores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_N)
                .collect(Collectors.toList());

        int score = Math.min(15, top.size() + 2);

        obj.getScore(ChatColor.GRAY + "----------------").setScore(score--);

        if (top.isEmpty()) {
            obj.getScore(ChatColor.YELLOW + "Sin datos").setScore(score--);
        } else {
            for (int i = 0; i < top.size() && score > 0; i++) {
                UUID uuid = top.get(i).getKey();
                int muertes = top.get(i).getValue();

                String nombre = jugadoresMuertosConfig.getString("nombres." + uuid.toString(), null);
                if (nombre == null) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    nombre = op.getName();
                }
                if (nombre == null) nombre = uuid.toString().substring(0, 8);

                String linea = ChatColor.WHITE + "" + (i + 1) + ". "
                        + ChatColor.AQUA + nombre
                        + ChatColor.GRAY + " (" + muertes + ")";

                // Evitar entradas demasiado largas (limite practico)
                if (linea.length() > 40) {
                    linea = linea.substring(0, 40);
                }

                obj.getScore(linea).setScore(score--);
            }
        }

        obj.getScore(ChatColor.GRAY + "----------------" + ChatColor.RESET).setScore(score--);

        // Aplicar a todos los jugadores online (mismo scoreboard global)
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(sb);
        }
    }

    // ----------------- eventos -----------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player jugador = event.getPlayer();
        UUID id = jugador.getUniqueId();

        guardarJugador(id, jugador.getName());
        actualizarScoreboardTopGlobal();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player jugador = event.getEntity();
        UUID id = jugador.getUniqueId();
        int contador = getContadorMuertes(id) + 1;

        muertesJugadores.put(id, contador);

        String pathMuertes = "jugadores." + id.toString();
        jugadoresMuertosConfig.set(pathMuertes, contador);

        jugadoresMuertosConfig.set("nombres." + id.toString(), jugador.getName());

        guardarArchivo();
        actualizarScoreboardTopGlobal();
    }

    // ----------------- ciclo de vida -----------------

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage("Activando Plugin ContadorMuertes...");
        cargarArchivo();
        getServer().getPluginManager().registerEvents(this, this);
        actualizarScoreboardTopGlobal();
    }

    @Override
    public void onDisable() {
        for (Map.Entry<UUID, Integer> e : muertesJugadores.entrySet()) {
            jugadoresMuertosConfig.set("jugadores." + e.getKey().toString(), e.getValue());
        }
        guardarArchivo();
        Bukkit.getConsoleSender().sendMessage("Gracias por usar el plugin! :D");
    }
}
