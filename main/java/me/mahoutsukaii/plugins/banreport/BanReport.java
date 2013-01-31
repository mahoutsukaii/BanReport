package me.mahoutsukaii.plugins.banreport;
/* hahahahahhaha */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class BanReport extends JavaPlugin {

	static boolean dev = false; //turn off for production.

	public static String maindir = "plugins/BanReport/";

	public static final Logger log = Logger.getLogger("Minecraft");

	private final BanReportPlayerListener playerListener = new BanReportPlayerListener(this);

	public ArrayList<String> bannedNubs = new ArrayList<String>();
	public ArrayList<String> bannedIPs = new ArrayList<String>();

	//messages:
	public FileConfiguration properties = new YamlConfiguration(); 

	public String broadcastBan;
	public String broadcastKick;
	public String broadcastTempBan;
	public String broadcastUnban;
	public String broadcastWarn;
	public String userKick;
	public String userBan;
	public String userTempBan;
	public String userIPBan;
	public String userWarn;
	public boolean useMySQL;
	public int maxWarnings;

	//    ArrayList<String> bannedPlayers = new ArrayList<String>();
	MySQLDatabase db;

	public void getStrings()
	{
		this.broadcastBan = properties.getString("broadcast.Ban", "&6Player &e%victim%&6 was banned by &e%admin%&6! Reason: &e%reason%" );
		this.broadcastKick = properties.getString("broadcast.Kick","&6Player &e%victim%&6 was kicked by &e%admin%&6! Reason: &e%reason%" );
		this.broadcastTempBan = properties.getString("broadcast.TempBan", "&6Player &e%victim%&6 was temp-banned by &e%admin%&6 for &e%time% &6Reason: &e%reason%");
		this.broadcastUnban = properties.getString("broadcast.Unban","&e%victim%&6 was unbanned by &e%admin%&6!");
		this.broadcastWarn = properties.getString("broadcast.warn","&e%victim%&6 was warned by &e%admin%&6. Reason: &e%reason%");

		this.userKick = properties.getString("user.Kick","&6You were kicked by &e%admin%. &6Reason: &e%reason%");
		this.userBan = properties.getString("user.Ban","&6You were banned by &e%admin%. &6Reason: &e%reason%");
		this.userTempBan = properties.getString("user.TempBan","&6You were temp-banned by %admin% for %time% Reason: &e%reason%");
		this.userIPBan = properties.getString("user.IPBan","&6Your IP is banned.");
		this.useMySQL = properties.getBoolean("use-mysql", true);
		this.userWarn = properties.getString("user.warn","&6You were warned by &e%admin%&6 Reason &e%reason%");
		this.maxWarnings = properties.getInt("max-warnings", 3);
		// For the lulz
		String mysqlDatabase = properties.getString("mysql-database", "jdbc:mysql://localhost:3306/minecraft");
		String mysqlUser = properties.getString("mysql-user", "root");
		String mysqlPassword = properties.getString("mysql-password", "root");

		// end lulz
		properties.set("use-mysql", useMySQL);
		properties.set("max-warnings", maxWarnings);
		properties.set("mysql-database", mysqlDatabase);
		properties.set("mysql-user", mysqlUser);
		properties.set("mysql-password", mysqlPassword);
		properties.set("broadcast.ban", broadcastBan);
		properties.set("broadcast.Kick", broadcastKick);
		properties.set("broadcast.TempBan", broadcastTempBan);
		properties.set("broadcast.Unban", broadcastUnban);
		properties.set("broadcast.warn", broadcastWarn);
		properties.set("user.Kick", userKick);
		properties.set("user.ban", userBan);
		properties.set("user.TempBan", userTempBan);
		properties.set("user.IPban", userIPBan);
		properties.set("user.warn",	 userWarn);

		try {
			properties.save(new File("plugins/BanReport/config.yml"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




	public void onDisable() {
		bannedNubs.clear();
		System.out.println(this + " is now disabled!");
	}


	public void onEnable() {

		new File(maindir).mkdir();
		try {
			properties.load(new File("plugins/BanReport/config.yml"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getStrings();
		db = new MySQLDatabase(this);
		db.initialise();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(playerListener, this);
		/*
		pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Highest, this);
		 */

		System.out.println(this + " is now enabled!");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		String commandName = command.getName().toLowerCase();
		String[] trimmedArgs = args;

		if(commandName.equals("baninfo"))
		{ 
			return banInfo(sender, trimmedArgs);
		}
		if(commandName.equals("addinfo"))
		{ 
			return addInfo(sender, trimmedArgs);
		}
		if(commandName.equals("removeinfo"))
		{ 
			return removeInfo(sender, trimmedArgs);
		}

		if(commandName.equals("bantp"))
		{
			return banTp(sender, trimmedArgs);
		}

		if(commandName.equals("ban"))
		{
			return banPlayer(sender, trimmedArgs);
		}

		if(commandName.equals("unban"))
		{
			return unbanPlayer(sender, trimmedArgs);
		}
		if(commandName.equals("kick"))
		{
			return kickPlayer(sender, trimmedArgs);
		}
		if(commandName.equals("banip"))
		{
			return banIP(sender, trimmedArgs);
		}

		if(commandName.equals("tempban"))
		{
			return tempBan(sender, trimmedArgs);
		}

		if(commandName.equals("banexport"))
		{
			if(args.length < 1)
				return exportBans(sender);

		}

		if(commandName.equals("banimport"))
		{
			return importBans(sender);
		}

		if(commandName.equals("warn"))
		{
			return warnPlayer(sender, trimmedArgs);
		}

		if(commandName.equals("unwarn"))
		{
			return unwarnPlayer(sender, trimmedArgs);
		}
		if(commandName.equals("warnings"))
		{
			return viewWarnings(sender, trimmedArgs);
		}
		return false;


	}



	private boolean viewWarnings(CommandSender sender, String[] args)
	{
		String player;
		if(args.length < 1)
		{
			if(!getPermission(sender, "banreport.warnings.own"))
			{
				sender.sendMessage(ChatColor.RED + "You do not have permission.");
				return true;
			}

			player = sender.getName();
		}
		else
		{
			if(!getPermission(sender, "banreport.warnings.viewall"))
			{
				sender.sendMessage(ChatColor.RED + "You do not have permission.");
				return true;
			}

			player = expandName(args[0]);
		}

		db.viewWarnings(sender, player);
		return true;
	}

	private boolean warnPlayer(CommandSender sender, String[] args)
	{
		boolean silent = false;
		if(!getPermission(sender, "banreport.warn"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		//we has permission!

		if(args.length < 2)
		{
			return false;
		}
		if(args[1].equals("-s"))
			silent = true;

		//we have the player and reason!

		String victim = expandName(args[0]);
		String admin = sender.getName();
		String reason;

		if(silent)
		{
			if(args.length < 3)
				return false;
			reason = combineSplit(2,args, " ");

		}
		else reason = combineSplit(1, args, " ");

		db.warnPlayer(victim, admin, reason);
		//broadcast
		Player onlineVictim = null;
		onlineVictim = getServer().getPlayer(victim);
		if(onlineVictim != null)
		{
			String warnMsg = this.userWarn;
			warnMsg = warnMsg.replace("%reason%", reason);
			warnMsg = warnMsg.replace("%admin%", admin);

			onlineVictim.sendMessage(formatMessage(warnMsg));
		}

		if(!silent)
		{
			String broadcastMsg = this.broadcastWarn;
			broadcastMsg = broadcastMsg.replace("%reason%", reason);
			broadcastMsg = broadcastMsg.replace("%admin%", admin);
			broadcastMsg = broadcastMsg.replace("%victim%", victim);

			getServer().broadcastMessage(formatMessage(broadcastMsg));
		}

		if(db.getWarnings(victim) >= this.maxWarnings)
		{
			if(!db.checkBanList(victim))
			{
				db.addPlayer(victim, "Exceeded Maximum Warnings.", admin, 0);
				if(onlineVictim != null)
					onlineVictim.kickPlayer("Exceeded Maximum Warnings.");
			}
		}
		return true;
	}

	private boolean unwarnPlayer(CommandSender sender, String[] args)
	{
		if(!sender.hasPermission("banreport.warn"))
			return false;
		
		if(args.length < 2)
			return false;
		
		String victim = args[0];
		int warning;
		
		try {
			warning = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(ChatColor.RED + "Invalid argument.");
			return true;
		}
		if(db.getWarnings(args[0]) < warning)
		{
			sender.sendMessage(ChatColor.RED + victim + " has no warning for number " + warning);
			return true;
		}
		
		db.removeWarning(victim, warning);
		sender.sendMessage(ChatColor.GREEN + "Success!");
		return true;
	}
	private boolean banInfo(CommandSender sender, String[] args)
	{
		if(!getPermission(sender, "banreport.baninfo"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		if(args.length < 1)
			return false;

		String victim = args[0];
		if(!db.checkBanList(args[0]))
		{
			sender.sendMessage(ChatColor.RED + victim + " is not banned!");
			return true;
		}
		String banReason = db.getInfo(victim).reason;
		String banAdmin = db.getInfo(victim).admin;

		sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.YELLOW + victim + ChatColor.GREEN + " banned by " + ChatColor.YELLOW + banAdmin);
		sender.sendMessage(ChatColor.RED + banReason);
		HashMap<Integer, BanInfo> banInfo = db.getBanInfo(victim);

		if(banInfo.size() < 1)
		{
			sender.sendMessage(ChatColor.RED + "No additional information found.");
			return true;
		}
		for(int i=0; i < banInfo.keySet().size(); i++ )
		{
			BanInfo info = banInfo.get(i);
			sender.sendMessage(ChatColor.GRAY + "["+(i+1)+"] " + info.getAdmin() + " @ (" + info.getX() + ", " + info.getY() + ", " + info.getZ() + ") -> "
					+ ChatColor.GOLD + info.getInfo());
		}

		return true;
	}


	private boolean addInfo(CommandSender sender, String[] args)
	{

		if(!getPermission(sender, "banreport.baninfo"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		if(args.length < 1)
			return false;
		String victim = args[0];
		String reason;

		if(args.length < 2)
			reason = "<Coords>";
		else reason = combineSplit(1, args, " ");

		db.addInfo(sender, victim, reason);
		sender.sendMessage(ChatColor.GREEN + "Info sent!");
		return true;

	}

	private boolean removeInfo(CommandSender sender, String[] args)
	{
		if(!getPermission(sender, "banreport.baninfo"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		if(args.length < 2)
			return false;

		int id = Integer.parseInt(args[1]) -1;
		String victim = args[0];
		if(id < 0 | id > db.getBanInfo(victim).size())
		{
			sender.sendMessage(ChatColor.RED + "Invalid ID.");
			return true;
		}

		db.removeInfo(db.getBanInfo(victim).get(id).getID());
		//System.out.print(db.getBanInfo(victim).get(id).getID());
		sender.sendMessage(ChatColor.GREEN + "Info removed.");
		return true;
	}

	public Boolean banTp(CommandSender sender, String[] args)
	{
		if(!(sender instanceof Player))
		{
			sender.sendMessage("no console tp.");
			return true;
		}
		if(!getPermission(sender, "banreport.bantp"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		if(args.length < 2)
			return false;

		Player player = (Player)sender;
		int id = Integer.parseInt(args[1]) -1;
		String victim = args[0];
		if(id < 0 | id > db.getBanInfo(victim).size())
		{
			sender.sendMessage(ChatColor.RED + "Invalid ID.");
			return true;
		}
		BanInfo banInfo = db.getBanInfo(victim).get(id);

		player.teleport(new Location(player.getWorld(), banInfo.getX(), banInfo.getY(), banInfo.getZ()));
		sender.sendMessage(ChatColor.GREEN + "Teleport.");
		return true;
	}


	public Boolean banPlayer(CommandSender sender, String[] args)
	{

		if(!getPermission(sender, "banreport.ban"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		if(args.length < 1) //not enough arguments.
		{

			return false;

		}

		String victim = expandName(args[0]);

		if (db.checkBanList(victim))
		{
			sender.sendMessage(ChatColor.RED + "Player " + ChatColor.YELLOW + victim + ChatColor.RED + " is already banned.");
			sender.sendMessage(ChatColor.RED + "Sending baninfo...");
			addInfo(sender, args);
			return true;
		}

		String reason;
		Boolean silent = false;
		if(args.length > 1)
			if(args[1].equals("-s"))
			{
				silent = true;
			}

		if(!silent)
		{
			if(args.length < 2)
				reason = "undefined.";
			else
				reason = combineSplit(1, args, " ");
		}
		else
		{
			if(args.length < 3)
				reason = "undefined.";
			else
				reason = combineSplit(2, args, " ");
		}

		db.addPlayer(victim, reason, sender.getName(), 0);

		sender.sendMessage(ChatColor.GREEN + "Successfully banned " + victim + "!");
		Player actualVictim = this.getServer().getPlayer(victim);
		if(actualVictim != null)
		{
			String message = this.userBan;
			message = message.replace("%victim%", victim);
			message = message.replace("%admin%", sender.getName());
			message = message.replace("%reason%", reason);
			String IP = actualVictim.getAddress().getAddress().getHostAddress();
			db.addPermList(victim, reason, sender.getName(), IP);
			actualVictim.kickPlayer(formatMessage(message));

		}
		else
			db.addPermList(victim, reason, sender.getName(), "0.0.0.0");
		String message = this.broadcastBan;
		message = message.replace("%victim%", victim);
		message = message.replace("%admin%", sender.getName());
		message = message.replace("%reason%", reason);
		if(!silent)
		{


			this.getServer().broadcastMessage(formatMessage(message));
		}
		log.log(Level.INFO,"[BanReport] " + victim + " was banned by " + sender.getName() + " Reason: " + reason);


		return true;

	}

	public Boolean tempBan(CommandSender sender, String[] args)
	{
		if(!getPermission(sender, "banreport.ban"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		if(args.length < 2) //not enough arguments.
		{

			return false;

		}

		String victim = expandName(args[0]);

		if (db.checkBanList(victim))
		{
			sender.sendMessage(ChatColor.RED + "Player " + ChatColor.YELLOW + victim + ChatColor.RED + " is already banned.");
			return true;
		}

		String reason;
		long time;
		char unit;
		String unparsedTime;
		Boolean silent = false;
		if(args[1].equals("-s"))
		{
			silent = true;
		}

		if(!silent)
		{
			if(args.length < 3)
				reason = "undefined.";
			else
				reason = combineSplit(2, args, " ");

			unparsedTime = args[1];
			time = Long.parseLong(unparsedTime.replaceAll("[\\D+]", ""));
			unit = unparsedTime.charAt(unparsedTime.length() -1);

		}
		else
		{
			if(args.length < 4)
				reason = "undefined.";
			else
				reason = combineSplit(3, args, " ");

			unparsedTime = args[2];
			time = Long.parseLong(unparsedTime.replaceAll("[\\D]", ""));
			unit = unparsedTime.charAt(unparsedTime.length() -1);
		}

		//lets get the time .....  D:

		long temptime = new Date().getTime();



		if(unit == 's')
			temptime = temptime + time * 1000;		
		if(unit == 'm')
			temptime = temptime + time * 1000 * 60;
		if(unit == 'h')
			temptime = temptime + time * 1000 * 60 * 60;
		if(unit == 'd')
			temptime = temptime + time * 1000 * 60 * 60 * 24;

		if(temptime <= new Date().getTime())
		{
			//sender.sendMessage("" + temptime);
			//sender.sendMessage("" + unit);
			sender.sendMessage(ChatColor.RED + "invalid time.");
			return true;
		}


		//minutes = time * 1000 * 60
		//hours = time * 1000 * 60 * 60
		//days = time * 1000 * 60 * 60 * 24





		db.addPlayer(victim, reason, sender.getName(), temptime);

		sender.sendMessage(ChatColor.GREEN + "Successfully tempbanned " + victim + "!");
		Player actualVictim = this.getServer().getPlayer(victim);

		if(actualVictim != null)
		{
			String message = this.userTempBan;
			message = message.replace("%victim%", victim);
			message = message.replace("%admin%", sender.getName());
			message = message.replace("%reason%", reason);
			message = message.replace("%time%", getTimeDifference(temptime));
			actualVictim.kickPlayer(formatMessage(message));
		}

		String message = this.broadcastTempBan;
		message = message.replace("%victim%", victim);
		message = message.replace("%admin%", sender.getName());
		message = message.replace("%reason%", reason);
		message = message.replace("%time%", getTimeDifference(temptime));

		if(!silent)
		{
			this.getServer().broadcastMessage(formatMessage(message));
		}
		log.log(Level.INFO,"[BanReport] " + victim + " was temp banned by " + sender.getName() + " for " + time + " reason: " + reason);


		return true;
	}

	public Boolean unbanPlayer(CommandSender sender, String[] args)
	{
		if(!getPermission(sender, "banreport.unban"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		if(args.length < 1) //not enough arguments.
		{

			return false;

		}

		String victim = expandName(args[0]);

		if (!db.checkBanList(victim))
		{
			sender.sendMessage(ChatColor.RED + "Player " + ChatColor.YELLOW + victim + ChatColor.RED + " is not banned.");
			return true;
		}

		Boolean silent = false;
		if(args.length > 1)
			if(args[1].equals("-s"))
			{
				silent = true;
			}


		db.removePlayer(victim);

		sender.sendMessage(ChatColor.GREEN + "Successfully unbanned " + victim + "!");

		String message = this.broadcastUnban;
		message = message.replace("%victim%", victim);
		message = message.replace("%admin%", sender.getName());

		if(!silent)
		{
			this.getServer().broadcastMessage(formatMessage(message));
		}
		log.log(Level.INFO,"[BanReport] " + victim + " was unbanned by " + sender.getName());

		return true;


	}

	public Boolean kickPlayer(CommandSender sender, String[] args)
	{
		if(!getPermission(sender, "banreport.kick"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}
		if(args.length < 1) //not enough arguments.
		{
			return false;
		}

		String victim = expandName(args[0]);


		String reason;
		Boolean silent = false;
		if(args.length > 1)
			if(args[1].equals("-s"))
			{
				silent = true;
			}

		if(!silent)
		{
			if(args.length < 2)
				reason = "undefined.";
			else
				reason = combineSplit(1, args, " ");
		}
		else
		{
			if(args.length < 3)
				reason = "undefined.";
			else
				reason = combineSplit(2, args, " ");
		}



		Player actualVictim = this.getServer().getPlayer(victim);

		if(actualVictim == null)
		{
			sender.sendMessage(ChatColor.RED + "Player " + ChatColor.YELLOW + victim + ChatColor.RED + " is not online.");
			return true;
		}
		String message = this.userKick;
		message = message.replace("%victim%", victim);
		message = message.replace("%admin%", sender.getName());
		message = message.replace("%reason%", reason);
		actualVictim.kickPlayer(formatMessage(message));

		message = this.broadcastKick;
		message = message.replace("%victim%", victim);
		message = message.replace("%admin%", sender.getName());
		message = message.replace("%reason%", reason);

		if(!silent)
		{
			this.getServer().broadcastMessage(formatMessage(message));
		}
		log.log(Level.INFO, "[BanReport] " + victim + " was kicked by " + sender.getName() + " Reason: " + reason);

		return true;

	}

	public Boolean banIP(CommandSender sender, String[] args)
	{
		if(!getPermission(sender, "banreport.banip"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		if(args.length < 1) //not enough arguments.
		{

			return false;

		}

		String victim = args[0];
		Player actualvictim = this.getServer().getPlayer(expandName(victim));
		if(actualvictim != null)
		{
			victim = actualvictim.getAddress().getAddress().getHostAddress().toString();
		}


		if (db.checkIP(victim))
		{
			sender.sendMessage(ChatColor.RED + "This IP is already blocked.");
			return true;
		}

		db.addIP(victim, sender.getName());



		sender.sendMessage(ChatColor.GREEN + "Successfully blocked " + victim + "!");
		log.log(Level.INFO, "[BanReport] " + victim + " blocked by " + sender.getName());
		kickIPs(victim);
		return true;

	}

	public String combineSplit(int startIndex, String[] string, String seperator) {
		StringBuilder builder = new StringBuilder();

		for (int i = startIndex; i < string.length; i++) {
			builder.append(string[i]);
			builder.append(seperator);
		}

		builder.deleteCharAt(builder.length() - seperator.length()); // remove
		return builder.toString();
	}

	public String expandName(String name) {
	/*	int m = 0;
		String Result = "";
		for (int n = 0; n < getServer().getOnlinePlayers().length; n++) {
			String str = getServer().getOnlinePlayers()[n].getName();
			if (str.matches("(?i).*" + Name + ".*")) {
				m++;
				Result = str;
				if(m==2) {
					return null;
				}
			}
			if (str.equalsIgnoreCase(Name))
				return str;
		}
		if (m == 1)
			return Result;
		if (m > 1) {
			return null;
		}
		if (m < 1) {
			return Name;
		}
		*/
		String result = name;
			Player p = getServer().getPlayer(name);

			if(p!=null)
				result = p.getName();
		
	
		return result;
	}

	public void kickIPs(String IP)
	{
		Player player = null;
		for(int n = 0; n < getServer().getOnlinePlayers().length; n++)
		{
			player = getServer().getOnlinePlayers()[n];
			if(player.getAddress().getAddress().getHostAddress().toString().equals(IP))
			{
				db.addPlayer(player.getName().toLowerCase(), "IP Ban.", "autoban", 0);
				log.log(Level.INFO, "[BanReport] " + player.getName() + " was autobanned by IP." );

				String message = this.userIPBan;
				player.kickPlayer(formatMessage(message));

			}
		}
	}

	protected boolean exportBans(CommandSender sender)
	{
		if(!getPermission(sender, "banreport.banio"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		try {
			BufferedWriter banlist = new BufferedWriter(new FileWriter("banned-players.txt",true));
			db.initialise();
			for(String i : bannedNubs)
			{
				banlist.newLine();
				banlist.write(i);
			}
			banlist.close();

			BufferedWriter banlistIP = new BufferedWriter(new FileWriter("banned-ips.txt",true));
			for(String i : bannedIPs)
			{
				banlistIP.newLine();
				banlistIP.write(i);
			}
			banlistIP.close();
			sender.sendMessage(ChatColor.GREEN + "Banlist exported.");
			log.log(Level.INFO, sender.getName() + " exported the banlist.");
			return true;

		} catch (IOException e) {
			log.log(Level.SEVERE, "[BanReport] could not export ban list.");
			sender.sendMessage(ChatColor.RED + "Could not export ban list.");
		}


		return true;
	}

	protected boolean importBans(CommandSender sender)
	{

		if(!getPermission(sender, "banreport.banio"))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission.");
			return true;
		}

		try {
			db.initialise();
			BufferedReader banlist = new BufferedReader(new FileReader("banned-players.txt"));
			String strLine;

			while ((strLine = banlist.readLine()) != null)   {
				// add it to the database :(
				if(!bannedNubs.contains(strLine))
					db.addPlayer(strLine, "undefined","bl: " + sender.getName(), 0);
			}
			BufferedReader banlistIP = new BufferedReader(new FileReader("banned-ips.txt"));
			strLine = "";

			while ((strLine = banlistIP.readLine()) != null)   {
				// add it to the database :(
				if(!bannedIPs.contains(strLine))
					db.removePlayer(strLine); //remove them from the database as an name so we can add it later
				db.addIP(strLine, "bl: " + sender.getName());
			}

			//db.initialise(this);		
			banlist.close();

			sender.sendMessage(ChatColor.GREEN + "Banlist imported.");
			log.log(Level.INFO, sender.getName() + " imported the banlist to the database.");
			return true;

		} catch (IOException e) {
			log.log(Level.SEVERE, "[BanReport] could not import ban list.");
			sender.sendMessage(ChatColor.RED + "Could not import ban list.");
		}


		return true;
	}

	protected void createDefaultConfiguration(String name) {
		File actual = new File(getDataFolder(), name);
		if (!actual.exists()) {

			InputStream input =
					this.getClass().getResourceAsStream("/defaults/" + name);
			if (input != null) {
				FileOutputStream output = null;

				try {
					output = new FileOutputStream(actual);
					byte[] buf = new byte[8192];
					int length = 0;
					while ((length = input.read(buf)) > 0) {
						output.write(buf, 0, length);
					}

					System.out.println(getDescription().getName()
							+ ": Default configuration file written: " + name);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (input != null)
							input.close();
					} catch (IOException e) {}

					try {
						if (output != null)
							output.close();
					} catch (IOException e) {}
				}
			}
		}
	}

	public String getTimeDifference(long tempTime)
	{

		long difference = tempTime - new Date().getTime();
		long timespace;
		String timespaceUnit = " day(s).";
		long timespacedays = difference / (1000*60*60*24);
		long timespacehours = difference / (1000*60*60);
		long timespaceminutes = difference / (1000*60);
		long timespaceseconds = difference / (1000);

		timespace = timespacedays;
		if(timespace < 1)
		{
			timespace = timespacehours;
			timespaceUnit = " hour(s).";
		}
		if(timespace < 1)
		{
			timespace = timespaceminutes;
			timespaceUnit = " minute(s).";
		}
		if(timespace < 1)
		{
			timespace = timespaceseconds;
			timespaceUnit = " second(s).";
		}

		return timespace + timespaceUnit;
	}

	public String formatMessage(String str){
		//	String funnyChar = new Character((char) 167).toString();
		str = str.replace("&", "§");
		return str;
	}


	public boolean getPermission(CommandSender sender, String node)
	{
		if(!(sender instanceof Player))
			return true;
		Player player = (Player)sender;
		if(player.hasPermission(node))
		{
			return true;
		}


		return false;
	}

}