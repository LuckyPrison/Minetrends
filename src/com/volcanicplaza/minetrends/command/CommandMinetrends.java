package com.volcanicplaza.minetrends.command;

import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.ulfric.lib.api.command.SimpleCommand;
import com.ulfric.lib.api.java.Strings;

public class CommandMinetrends extends SimpleCommand {

	public CommandMinetrends(String version)
	{
		this.withSubcommand(new SubcommandReload(this));
		this.withSubcommand(new SubcommandKey(this));

		this.version = Optional.ofNullable(version).orElse("1.0.0");
	}

	private final String version;

	@Override
	public void run()
	{
		CommandSender sender = this.getSender();

		sender.sendMessage(ChatColor.AQUA + "-=-=-=-=-=- Minetrends -=-=-=-=-=-");
		sender.sendMessage(ChatColor.AQUA + "/minetrends" + ChatColor.GRAY + " Shows this help page.");
		sender.sendMessage(ChatColor.AQUA + "/minetrends key <server_key>" + ChatColor.GRAY + " Add your server key.");
		sender.sendMessage(ChatColor.AQUA + "/minetrends reload" + ChatColor.GRAY + " Reload the Minetrends configuration file.");
		sender.sendMessage(ChatColor.AQUA + Strings.format("-=-=-=-=-=-[  v{0}  ]-=-=-=-=-=-", this.version));
	}

}