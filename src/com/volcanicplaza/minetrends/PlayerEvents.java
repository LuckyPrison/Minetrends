package com.volcanicplaza.minetrends;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.ulfric.lib.api.player.PlayerDisconnectEvent;

public class PlayerEvents implements Listener {

	@EventHandler
	public static void onPlayerJoin(PlayerJoinEvent e) {
		Minetrends.playerJoins.put(e.getPlayer().getName(), System.currentTimeMillis());
	}

	@EventHandler
	public static void onPlayerQuit(PlayerDisconnectEvent e) {
		Minetrends.playerJoins.remove(e.getPlayer().getName());
	}

}
