package com.volcanicplaza.minetrends.command;

import org.bukkit.ChatColor;

import com.ulfric.lib.api.command.Command;
import com.ulfric.lib.api.command.SimpleSubCommand;
import com.ulfric.lib.api.command.arg.ArgStrategy;
import com.volcanicplaza.minetrends.Minetrends;

class SubcommandKey extends SimpleSubCommand {

	protected SubcommandKey(Command command)
	{
		super(command, "key", "setkey");

		this.withArgument("key", ArgStrategy.ENTERED_STRING, "minetrends.key_needed");

		this.withNode("minetrends.key");
	}

	@Override
	public void run()
	{
		Minetrends.plugin.getConfig().set("key", this.getObject("key"));
		Minetrends.plugin.saveConfig();
		Minetrends.refreshConfig();

		this.getSender().sendMessage(ChatColor.AQUA + "Your server key has been successfully added!");
	}

}