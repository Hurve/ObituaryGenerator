package wales.z.plugin.ObituaryGenerator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;


import java.util.concurrent.CompletableFuture;


public class Main extends JavaPlugin implements Listener {
	

	private Boolean discordOn = false;
	
	@Override
	public void onEnable() {
		getLogger().info("Plugin Enabled");
		saveDefaultConfig();
		
        getServer().getPluginManager().registerEvents(this, this);
        
        if (getConfig().getString("gemini-api-key").equals("ENTER_HERE")) {
        	getLogger().info("You have not specified a Gemini API Key. This plugin will not function!");
        }
        if (getConfig().getString("discord-api-key").equals("ENTER_HERE")) {
        	getLogger().info("You have not specified a Discord API Key. Discord obituary messages will not be sent.");
        } else if (getConfig().getString("discord-channel-id").equals("ENTER_HERE")) {
        	getLogger().info("You have not specified a Discord Channel ID. Your messages will not send successfully.");
        } else if (!(getConfig().getString("discord-channel-id").equals("ENTER_HERE") && getConfig().getString("discord-api-key").equals("ENTER_HERE"))){
        	discordOn = true;
        }
        
        
        
	}
	

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent pde) {
		
		String deathMessage = pde.getDeathMessage();
		String playerName = pde.getEntity().getName();
		
		
		StringBuilder inventoryString = new StringBuilder();

		for (ItemStack item : pde.getEntity().getInventory()) {
		    if (item != null && item.getType() != Material.AIR) {
		        inventoryString.append(item.getAmount())
		                       .append("x ")
		                       .append(item.getType().toString())
		                       .append("\n");
		    }
		}

		String items = inventoryString.toString();
		
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
			try {
				String obit = Requestor.requestObit("Create a 100 word obituary for a minecraft player named " + playerName + " whose death message was " + deathMessage + " and who had " + items + " in their inventory. DO NOT make anything up. Use the exact player name, don't try to correct it. Use the information provided. Do not output the exact item name, but make it human readable and grammatically correct. Only cover the relevant items. Make the message funny.", getConfig().getString("gemini-api-key"));
				return obit;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			
        });
        
        future.thenAccept(response -> {
        	Bukkit.getScheduler().runTask(this, () -> {
        		if (getConfig().getBoolean("minecraft-chat-messages")) {
        			Bukkit.broadcastMessage(response);
        		}
            	if (discordOn) {
            		String errorReply = Requestor.sendMessage(response, getConfig().getString("discord-channel-id"), getConfig().getString("discord-api-key"));
            		if (!(errorReply == null)) {
            			getLogger().info(errorReply);
            		}
            	}
        	});
        });

	}
	
}
