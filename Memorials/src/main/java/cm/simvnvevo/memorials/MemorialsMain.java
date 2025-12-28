package cm.simvnvevo.memorials;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MemorialsMain extends JavaPlugin implements Listener {

    public static void spawnEstructura(Location location, String nombreEstructura) {
        try {
            File archivoSchem = new File("plugins/WorldEdit/schematics/" + nombreEstructura + ".schem");

            if (!archivoSchem.exists()) {
                Bukkit.getLogger().severe("No existe el schem: " + archivoSchem.getAbsolutePath());
                return;
            }

            var format = ClipboardFormats.findByFile(archivoSchem);
            if (format == null) {
                Bukkit.getLogger().severe("Formato schem no reconocido");
                return;
            }

            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(archivoSchem))) {
                clipboard = reader.read();
            }

            Bukkit.getLogger().info("Pegando estructura en " + location);

            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSession(BukkitAdapter.adapt(location.getWorld()))) {

                ClipboardHolder holder = new ClipboardHolder(clipboard);

                Operations.complete(
                        holder.createPaste(editSession)
                                .to(BlockVector3.at(
                                        location.getBlockX(),
                                        location.getBlockY(),
                                        location.getBlockZ()
                                ))
                                .ignoreAirBlocks(false)
                                .build()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getConsoleSender().sendMessage("Activando plugin Memorials...");
    }

    @Override
    public void onDisable(){
        Bukkit.getServer().getConsoleSender().sendMessage("Apagando Memorials...");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Location loc = event.getEntity().getLocation();
        spawnEstructura(loc, "structure");
    }
}
