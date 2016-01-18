package com.volcanicplaza.minetrends;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.ulfric.lib.api.module.Plugin;
import com.volcanicplaza.minetrends.command.CommandMinetrends;

public class Minetrends extends Plugin {
	
	public static JavaPlugin plugin = null;
	public static String hostname = null;
	
	//Contains public and private.
	public static String key = null;
	
	public static String publicKey = null;
	public static String privateKey = null;
	
	public static int time = 0;
	public static BukkitTask runnable;
	
	//Backend Systems API Version
	public static double apiVersion = 1;
	
	//Player Join Times
	public static Map<String, Long> playerJoins = new HashMap<>();
	
	@Override
	public void load(){
		plugin = this;
		this.addCommand("minetrends", new CommandMinetrends(this.getModuleVersion()));
	}
	
	@Override
	public void enable(){
		plugin.getConfig().options().copyDefaults(true);
		saveConfig();
		plugin.reloadConfig();
		
		hostname = "http://api.minetrends.com";
		//hostname = "http://192.168.1.33";
		
		refreshConfig();
		
		publicKey = Encryption.getServerKey();
		privateKey = Encryption.getPrivateKey();
		
		//If any players are currently online, add them to the playerJoins HashMap.
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (!(Minetrends.playerJoins.containsKey(player.getName()))){
				Minetrends.playerJoins.put(player.getName(), System.currentTimeMillis());
			}
		}
		
		//Start TPS monitor.
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new TPSChecker(), 100L, 1L);
		
		//Register Event Listeners
		Bukkit.getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
		
		Bukkit.getLogger().info(getDescription().getName() + " v" + getDescription().getVersion() + " has been enabled!");
	}
	
	@Override
	public void disable(){
		Bukkit.getLogger().info(getDescription().getName() + " v" + getDescription().getVersion() + " has been disabled!");
		Minetrends.runnable = null;
	}
	
	public static int getFrequency(){
		Bukkit.getLogger().info("<Minetrends> Authenticating with Minetrends...");
		URL url = null;
		HttpURLConnection conn = null;
		int responseInt = 0;
		
		String urlParameters = "key=" + Minetrends.publicKey;
		  try {
			  url = new URL(Minetrends.hostname + "/api/getfrequency");
			  
		  } catch (Exception ex){
			  ex.printStackTrace();
		  }
		
		try {
			conn = (HttpURLConnection) url.openConnection();
		try {
			conn.setRequestMethod("POST"); //use post method
			conn.setDoOutput(true); //we will send stuff
			conn.setDoInput(true); //we want feedback
			conn.setUseCaches(false); //no caches
			conn.setConnectTimeout(4000); //set timeout
			conn.setInstanceFollowRedirects(true);
			conn.setAllowUserInteraction(false);
			conn.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
			conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		}
			catch (ProtocolException e) {
			e.printStackTrace();
		}
		
		// Open a stream which can write to the URL
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		
		// Open a stream which can read the server response
		InputStream in = null;
		BufferedReader rd = null;
		try {
			in = conn.getInputStream();
			rd = new BufferedReader(new InputStreamReader(in));
			String response = rd.readLine();
			
			if (response != null){
				//Check if the response is a int.
				if (response.equalsIgnoreCase("invalid-key")){
					responseInt = -1;
				} else {
					try {
				        responseInt = Integer.parseInt(response); 
				    } catch(NumberFormatException e) { 
				    	responseInt = 0;
				    }
				}
			} else {
				responseInt = 0;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally { //in this case, we are ensured to close the input stream
			if (in != null) in.close();
			if (rd != null) rd.close();
		}
		} catch (IOException e) {
		}finally {  //in this case, we are ensured to close the connection itself
			if (conn != null)
			conn.disconnect();
		}
		return responseInt;
		
	}
	
	public static void refreshConfig(){
		if (Minetrends.runnable != null) {
			Minetrends.runnable.cancel();
		}
		
		plugin.reloadConfig();
		key = plugin.getConfig().getString("key");
		
		privateKey = Encryption.getPrivateKey();
		publicKey = Encryption.getServerKey();
		
		int time = getFrequency();
		if (time == 0){
			//Received no data.
			Bukkit.getLogger().warning("***************************************************************");
			Bukkit.getLogger().warning("<Minetrends> Could not connect to the Minetrends servers.");
			Bukkit.getLogger().warning("***************************************************************");
		} else if (time == -1){
			//Invalid key
			Bukkit.getLogger().warning("***************************************************************");
			Bukkit.getLogger().warning("<Minetrends> You have specified an Invalid Server Key!");
			Bukkit.getLogger().warning("***************************************************************");
		} else {
			Bukkit.getLogger().info("<Minetrends> Sucessfully authenticated with Minetrends.");
			Minetrends.runnable = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new SendRunnable(), 20L, (20 * time));
			
		}
	}
	
	public static String getData(){
		ObjectMapper mapper = new ObjectMapper();
		
		List<Player> playersObj = new ArrayList<>();
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			playersObj.add(player);
		}
		
		Map<String,Object> servers = new HashMap<>();
		
		Map<String,Object> data = new HashMap<>();
		Map<String,Object> playersList = new HashMap<>();
		
		for (Player plr : playersObj){
			Map<String,String> player = new HashMap<>();
			
			//Player's IP Address
			player.put("ADDRESS", Encryption.encryptString(plr.getAddress().toString()));
			
			//Player UUID. New in Minecraft v1.7
			player.put("UUID", Encryption.encryptString(plr.getUniqueId().toString()));
			
			//Player's XP Level
			player.put("XPLEVELS", Encryption.encryptString(String.valueOf(plr.getLevel())));
			
			//How long the player has been playing this login session.
			long plrSessionTime = 0;
			plrSessionTime = System.currentTimeMillis() - Minetrends.playerJoins.get(plr.getName());
			player.put("sessionTime", Encryption.encryptString((plrSessionTime / 1000) + ""));
			
			//Player's Minecraft Language
			player.put("appLanguage", Encryption.encryptString(getLanguage(plr)));
			
			//Add to the main data array
			playersList.put(Encryption.encryptString(plr.getName()), player);
		}
		
		data.put("players", playersList);
		
		data.put("BUKKITVERSION", Encryption.encryptString(plugin.getServer().getBukkitVersion().toString()));
		
		data.put("TIMEZONE", Encryption.encryptString(TimeZone.getDefault().getDisplayName()));
		data.put("TIMEZONEID", Encryption.encryptString(TimeZone.getDefault().getID()));
		data.put("TIMELOCAL", Encryption.encryptString(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())));
		
		//Memory Usage
		data.put("memoryFree", "" + Encryption.encryptString(Runtime.getRuntime().freeMemory() + ""));
		data.put("memoryTotal", "" + Encryption.encryptString(Runtime.getRuntime().totalMemory() + ""));
		data.put("memoryMax", Encryption.encryptString(Runtime.getRuntime().maxMemory() + ""));
		
		//Java Virtual Machine Uptime
		long JVMStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
		long currentTime = new Date().getTime();
		long upTime = (currentTime - JVMStartTime) / 1000;
		data.put("uptime", "" + Encryption.encryptString(upTime + ""));
		
		//Total Number of Entities
		int totalEntities = 0;
		for (World world : Bukkit.getWorlds()) {
			totalEntities = totalEntities + world.getEntities().size();
		}
		data.put("totalEntities", Encryption.encryptString(totalEntities + ""));
		
		//TPS Monitor
		data.put("TPS", Encryption.encryptString(new DecimalFormat("#.####").format(TPSChecker.getTPS()) + ""));
		
		//Diskspace Usage
		File hd = new File("/");
		data.put("diskspaceFree", Encryption.encryptString(hd.getUsableSpace() + ""));
		data.put("diskspaceTotal", Encryption.encryptString(hd.getTotalSpace() + ""));
		
		//Installed plugin version.
		data.put("pluginVersion", plugin.getDescription().getVersion());
		
		if (Minetrends.privateKey == null){
			data.put("secure", false);
		} else {
			data.put("secure", true);
		}
		
		servers.put(Minetrends.publicKey, data);
		
		try {
			//String result = mapper.writeValueAsString(data);
			String result = mapper.writeValueAsString(servers);
			return result;
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static String getLanguage(Player p){
		Object ep;
		Field f;
		String language = null;
		try {
			ep = getMethod("getHandle", p.getClass()).invoke(p, (Object[]) null);
			f = ep.getClass().getDeclaredField("locale");
			f.setAccessible(true);
			language = (String) f.get(ep);
		} catch (Exception e) {
			//Error when trying to retrieve language.
			language = "N/A";
		}
		return language;
	}
	
	private static Method getMethod(String name, Class<?> clazz) {
		for (Method m : clazz.getDeclaredMethods()) {
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}

}
