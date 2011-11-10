package jadon.mahoutsukaii.plugins.banreport;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.entity.Player;
import java.util.Date;
import java.util.logging.Level;


public class BanReportPlayerListener extends PlayerListener {
	
	BanReport plugin;
	
	public BanReportPlayerListener(BanReport instance)
	{
		this.plugin = instance;
	}
	
	public void onPlayerLogin(PlayerLoginEvent event)
	{
		Player player = event.getPlayer();
	//	System.out.print(plugin.bannedNubs);
		if(plugin.db.checkBanList(player.getName().toLowerCase()))
		{
			String kickMsg;

		EditBan playerInfo = plugin.db.getInfo(player.getName().toLowerCase());
		
		
		kickMsg = plugin.userBan;
   	 	kickMsg = kickMsg.replace("%admin%", playerInfo.admin);
   	 	kickMsg = kickMsg.replace("%reason%", playerInfo.reason);
	//	plugin.log.log(Level.INFO, "[HERERE] " + playerInfo.temptime);
	//	plugin.log.log(Level.INFO, "[HERERE] " + new Date().getTime());
		if(playerInfo.temptime != 0)
		{
			long difference = playerInfo.temptime - new Date().getTime();
			if (difference <= 0)
			{
	//		System.out.print( "" + playerInfo.temptime + " as opposed to " + new Date().getTime());
			plugin.db.removePlayer(player.getName().toLowerCase());
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

		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMsg);
		}
		
	}
	
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		String IP = player.getAddress().getAddress().getHostAddress().toString();
		
		if(plugin.db.checkIP(IP))
		{
			event.setJoinMessage(null);
			if(!plugin.db.checkBanList(player.getName().toLowerCase()))
			{
				BanReport.log.log(Level.INFO, "[BanReport] " + player.getName() + " was autobanned for their IP." );
				plugin.db.addPlayer(player.getName().toLowerCase(), "IP Ban.", "autoban", 0);
			}
			
		}
		
		if(plugin.db.checkBanList(player.getName().toLowerCase()))
		{
			event.setJoinMessage(null);
			EditBan playerInfo = plugin.db.getInfo(player.getName().toLowerCase());
			
			//Timestamp  time = new Timestamp(new Date().getTime());
		//	plugin.log.log(Level.INFO, "[HERERE] " + playerInfo.admin);
			String kickMsg;
			kickMsg = plugin.userBan;
	   	 	kickMsg = kickMsg.replace("%admin%", playerInfo.admin);
	   	 	kickMsg = kickMsg.replace("%reason%", playerInfo.reason);
			
			player.kickPlayer(kickMsg);
		}
	}

}
