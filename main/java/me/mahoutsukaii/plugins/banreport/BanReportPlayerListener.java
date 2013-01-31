package me.mahoutsukaii.plugins.banreport;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.entity.Player;
import java.util.Date;
import java.util.logging.Level;


public class BanReportPlayerListener implements Listener {

	BanReport plugin;

	public BanReportPlayerListener(BanReport instance)
	{
		this.plugin = instance;
	}
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerLogin(PlayerLoginEvent event)
	{
		Player player = event.getPlayer();
		if(!plugin.db.checkBanList(player.getName()))
		{
			return;
		}
		String kickMsg;

		EditBan playerInfo = plugin.db.getInfo(player.getName());


		kickMsg = plugin.userBan;
		kickMsg = kickMsg.replace("%admin%", playerInfo.admin);
		kickMsg = kickMsg.replace("%reason%", playerInfo.reason);
		if(playerInfo.temptime != 0)
		{
			long difference = playerInfo.temptime - new Date().getTime();
			if (difference <= 0)
			{
				//		System.out.print( "" + playerInfo.temptime + " as opposed to " + new Date().getTime());
				plugin.db.removePlayer(player.getName());
				BanReport.log.log(Level.INFO, "[BanReport] " + player.getName() + " has been released from temp ban!");
				return;
			}
			else
			{
				String time = plugin.getTimeDifference(difference + new Date().getTime());

				kickMsg = plugin.userTempBan;
				kickMsg = kickMsg.replace("%admin%", playerInfo.admin);
				kickMsg = kickMsg.replace("%reason%", playerInfo.reason);
				kickMsg = kickMsg.replace("%time%", time);

			}
		}

		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, plugin.formatMessage(kickMsg));
	}


	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		String IP = player.getAddress().getAddress().getHostAddress().toString();
		if(!plugin.db.checkIP(IP) & !plugin.db.checkBanList(player.getName())) return;
		if(plugin.db.checkIP(IP))
		{
			event.setJoinMessage(null);
			if(!plugin.db.checkBanList(player.getName()))
			{
				BanReport.log.log(Level.INFO, "[BanReport] " + player.getName() + " was autobanned for their IP." );
				plugin.db.addPlayer(player.getName().toLowerCase(), "IP Ban.", "autoban", 0);
			}

		}

		if(plugin.db.checkBanList(player.getName()))
		{
			event.setJoinMessage(null);
			EditBan playerInfo = plugin.db.getInfo(player.getName());

			//Timestamp  time = new Timestamp(new Date().getTime());
			//	plugin.log.log(Level.INFO, "[HERERE] " + playerInfo.admin);
			String kickMsg;
			kickMsg = plugin.userBan;
			kickMsg = kickMsg.replace("%admin%", playerInfo.admin);
			kickMsg = kickMsg.replace("%reason%", playerInfo.reason);

			player.kickPlayer(plugin.formatMessage(kickMsg));
		}
	}

}
