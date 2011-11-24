package jadon.mahoutsukaii.plugins.banreport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.tehkode.permissions.bukkit.PermissionsEx;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijikokun.bukkit.Permissions.Permissions;


@SuppressWarnings("deprecation")

public class BanReport extends JavaPlugin {
	
	static boolean dev = false; //turn off for production.
	
	public static String maindir = "plugins/BanReport/";
	
    public static final Logger log = Logger.getLogger("Minecraft");
    
	private final BanReportPlayerListener playerListener = new BanReportPlayerListener(this);
	
	public ArrayList<String> bannedNubs = new ArrayList<String>();
	public ArrayList<String> bannedIPs = new ArrayList<String>();
	
	//messages:
	public Configuration properties = new Configuration(new File("plugins/BanReport/config.yml"));
	
	
	public String broadcastBan;
	public String broadcastKick;
	public String broadcastTempBan;
	public String broadcastUnban;
	public String userKick;
	public String userBan;
	public String userTempBan;
	public String userIPBan;
	
	public boolean useMySQL;
	
	private Plugin permissionsEx;
	private Plugin groupManager;
	private Plugin permissions;
//    ArrayList<String> bannedPlayers = new ArrayList<String>();
    MySQLDatabase db;
    
    public void getStrings()
    {
    	this.broadcastBan = properties.getNode("broadcast").getString("Ban");
    	this.broadcastKick = properties.getNode("broadcast").getString("Kick");
    	this.broadcastTempBan = properties.getNode("broadcast").getString("TempBan");
    	this.broadcastUnban = properties.getNode("broadcast").getString("Unban");
    	
    	this.userKick = properties.getNode("user").getString("Kick");
    	this.userBan = properties.getNode("user").getString("Ban");
    	this.userTempBan = properties.getNode("user").getString("TempBan");
    	this.userIPBan = properties.getNode("user").getString("IPBan");
    	this.useMySQL = properties.getBoolean("use-mysql", true);

    }
    

    
    
    public void onDisable() {
		bannedNubs.clear();
        System.out.println(this + " is now disabled!");
    }


	public void onEnable() {
		permissionsEx = getServer().getPluginManager().getPlugin("PermissionsEx");
		groupManager = getServer().getPluginManager().getPlugin("GroupManager");
		permissions = getServer().getPluginManager().getPlugin("Permissions");
		
		new File(maindir).mkdir();

		createDefaultConfiguration("config.yml");
		properties.load();
		getStrings();
        db = new MySQLDatabase(this);
        db.initialise();
        
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Highest, this);

        
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
    	   if(trimmedArgs.length < 1)
    	   return exportBans(sender);
    	   else
    		   return exportHTML(sender);
       }
       
       if(commandName.equals("banimport"))
       {
    	   return importBans(sender);
       }
        return false;
        
        
    }
    
    private boolean exportHTML(CommandSender sender)
    {
    	
    	db.createHTMLFile();

    	return true;
    }
    private boolean banInfo(CommandSender sender, String[] args)
    {
    	

        if(!getPermission(sender, "banreport.baninfo"))
        {
        	sender.sendMessage(ChatColor.RED + "You do not have permission.");
        	return true;
        }
        
        if(args.length < 1) //not enough arguments.
        {
        	
        	return false;
        
    }
        
        String victim = args[0];
        if (!db.checkBanList(victim))
        	{
        	sender.sendMessage(ChatColor.RED + victim + " was not found on the ban list.");
        	return true;
        	}
        
       
       formatMessage(sender, victim);

        
        return true;
    }
    
    private void formatMessage(CommandSender sender, String victim)
    {
    	sender.sendMessage(ChatColor.RED + victim + " banned by " + db.getInfo(victim).admin +".");
    	String unformWithRed =  db.getInfo(victim).reason + "/r" + db.getInfo(victim).additional;
    	String[] formatted = unformWithRed.split("/r");
    	for(int i=0;i<formatted.length;i++)
    	{
    		if(i == 0)
    			sender.sendMessage(ChatColor.RED + formatted[i]);
    		else
    		sender.sendMessage(ChatColor.GRAY + "[" + i + "] " + formatted[i]);
    		}
    	
    }
    
    
    private boolean addInfo(CommandSender sender, String[] args)
    {

        Player player = null;
        if(sender instanceof Player)
        	player=(Player)sender;
        
        if(!getPermission(sender, "banreport.baninfo"))
        {
        	sender.sendMessage(ChatColor.RED + "You do not have permission.");
        	return true;
        }
         
         if(args.length < 1) //not enough arguments.
         {
         	
         	return false;
         
     }
         
         String victim = args[0];
         if (!db.checkBanList(victim))
         	{
         	sender.sendMessage(ChatColor.RED + victim + " was not found on the ban list.");
         	return true;
         	}
         
         String info;
         if(args.length < 2)
        	 info = "<coords>";
         else
          info = combineSplit(1, args, " ");
         if(sender instanceof Player)
         info =player.getName() + " @ (" + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ() + ") -> " + ChatColor.GOLD + info;
         else
        	 info = "CONSOLE @ (0,0,0) -> " + ChatColor.GOLD + info;
         
         db.addInfo(victim, info, false);
         sender.sendMessage(ChatColor.GREEN + "Data updated.");
         return true;
    }
    
    private boolean removeInfo(CommandSender sender, String[] args)
    {

        if(!getPermission(sender, "banreport.baninfo"))
        {
        	sender.sendMessage(ChatColor.RED + "You do not have permission.");
        	return true;
        }
        
        if(args.length < 2) //not enough arguments.
        {
        	
        	return false;
        
    }
        
        String victim = args[0];
        if (!db.checkBanList(victim))
        	{
        	sender.sendMessage(ChatColor.RED + victim + " was not found on the ban list.");
        	return true;
        	}
      

        
        String rawReasons = (String)db.getInfo(victim).additional;
        if(rawReasons == "")
        {
        	sender.sendMessage(ChatColor.RED + "Nothing to remove!");
        	return true;
        }
        
    	String[] formatted = rawReasons.split("/r");
    	
    	int id;

    		id= Integer.parseInt(args[1].toString())-1;


    	if(id >= formatted.length | id < 0)
    	{
    		sender.sendMessage(ChatColor.RED + "Invalid ID.");
    		return true;
    	}
    	
    	formatted[id] = "";
    	
    	rawReasons = "";
    	for(int i=0;i < formatted.length; i++)
    	{
    		if(formatted[i] != "")
    		{
    		if(rawReasons == "")
    			rawReasons = formatted[i];
    		else
    		rawReasons = rawReasons + "/r" + formatted[i];
    		}
    		
    	}
    	
    	db.addInfo(victim, rawReasons, true);
    	
    	sender.sendMessage(ChatColor.GREEN + "Data updated.");
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
        	 actualVictim.kickPlayer(formatMessage(message));

         }
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
    
    public Boolean banTp(CommandSender sender, String[] args)
    {
    	Player player = null;
        if(!getPermission(sender, "banreport.bantp"))
        {
        	sender.sendMessage(ChatColor.RED + "You do not have permission.");
        	return true;
        }
        
        if(sender instanceof Player)
        {
        	player = (Player)sender;
        }
        else return true;
        
         if(args.length < 2) //not enough arguments.
         {
         	
         	return false;
         
     }
         
         String victim = args[0];
         if (!db.checkBanList(victim))
         	{
         	sender.sendMessage(ChatColor.RED + victim + " was not found on the ban list.");
         	return true;
         	}
       

         
         String rawReasons = db.getInfo(victim).additional;
         if(rawReasons == "")
         {
         	sender.sendMessage(ChatColor.RED + "No information.");
         	return true;
         }
         
     	String[] formatted = rawReasons.split("/r");
     	
     	int id;

     		id= Integer.parseInt(args[1].toString())-1;


     	if(id >= formatted.length | id < 0)
     	{
     		sender.sendMessage(ChatColor.RED + "Invalid ID.");
     		return true;
     	}
     	
     	
     	String[] unformattedCoords = formatted[id].split(",", 3);
     	
     	//sender.sendMessage(unformattedCoords[0]);
     	//sender.sendMessage(unformattedCoords[1]);
     	//sender.sendMessage(unformattedCoords[2]);
     	
     	String[] splitX = unformattedCoords[0].split("@",2);
     	String splitY = unformattedCoords[1];
     	String[] splitZ = unformattedCoords[2].split("->",2);
     	
     	int x = Integer.parseInt(splitX[1].replace("(", "").replace(" ", ""));
     	int y = Integer.parseInt(splitY.replace(" ", ""));
     	int z = Integer.parseInt(splitZ[0].replace(")","").replace(" ", ""));

     	
     	Location location = new Location(player.getLocation().getWorld(), x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());
     	
     	sender.sendMessage(ChatColor.GREEN + "Teleporting...");
     // sender.sendMessage("" + x);
     // sender.sendMessage("" + y);
     //	sender.sendMessage("" + z);
     	player.teleport(location);
     	sender.sendMessage(ChatColor.GRAY + formatted[id]);
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
	
	public String expandName(String Name) {
		int m = 0;
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
		return Name;
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

		//PermissionsEx
		if(permissionsEx!=null)
			if( ((PermissionsEx) permissionsEx).getPermissionManager().has(player, node)) return true;

		if(permissions!=null)
			if(((Permissions) permissions).getHandler().has(player, node)) return true;

		if(groupManager!=null)
			if ( ((GroupManager) groupManager).getWorldsHolder().getWorldPermissions(player).has(player, node)) return true;

		return false;
	}

}