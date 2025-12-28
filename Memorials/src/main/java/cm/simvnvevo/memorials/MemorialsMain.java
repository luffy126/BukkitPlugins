package cm.simvnvevo.memorials;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MemorialsMain extends JavaPlugin {



    @Override
    public void onEnable() {
        Bukkit.getServer().getConsoleSender().sendMessage("Activando plugin Memorials...");
    }

    @Override
    public void onDisable(){
        Bukkit.getServer().getConsoleSender().sendMessage("Apagando Memorials...");
    }
}
