package com.volcanicplaza.minetrends.command;

import org.bukkit.ChatColor;

import com.ulfric.lib.api.command.Command;
import com.ulfric.lib.api.command.SimpleSubCommand;
import com.volcanicplaza.minetrends.Minetrends;

class SubcommandReload extends SimpleSubCommand {

	protected SubcommandReload(Command command)
	{
		super(command, "reload", "rel");

		this.withNode("minetrends.reload");
	}

	@Override
	public void run()
	{
		Minetrends.refreshConfig();

		this.getSender().sendMessage(ChatColor.AQUA + "Minetrends configuration reloaded!");
	}

}