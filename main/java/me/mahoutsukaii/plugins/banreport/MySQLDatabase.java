/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.mahoutsukaii.plugins.banreport;

/**
 *
 * @author Alec
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.Date;
import java.util.HashMap;
//import java.util.ArrayList;


import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;



public class MySQLDatabase{

	BanReport plugin;
	String mysqlTable = "banlist";
	public MySQLDatabase(BanReport instance)
	{
		plugin = instance;
	}
	public Connection getSQLConnection()  {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(new File("plugins/BanReport/config.yml"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvalidConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(plugin.useMySQL)
		{
			String mysqlDatabase = config.getString("mysql-database","jdbc:mysql://localhost:3306/minecraft");
			String mysqlUser = config.getString("mysql-user", "root");
			String mysqlPassword = config.getString("mysql-password","root");
			mysqlTable = config.getString("mysql-table","banlist");
			try {

				return DriverManager.getConnection(mysqlDatabase + "?autoReconnect=true&user=" + mysqlUser + "&password=" + mysqlPassword);
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "Unable to retreive connection", ex);
			}
		}
		else
		{
			try {
				try {
					Class.forName("org.sqlite.JDBC");
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return  DriverManager.getConnection("jdbc:sqlite:plugins/BanReport/banlist.db");
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "Unable to retreive connection", ex);
			}

		}

		return null;
	}

	public void initialise(){
		Connection conn = getSQLConnection();

		plugin.bannedNubs.clear();
		plugin.bannedIPs.clear();
		if(!plugin.useMySQL)
		{
			makeSQLiteTables();
		}

		if (conn == null) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Could not establish SQL connection. Disabling BanReport");
			plugin.getServer().getPluginManager().disablePlugin(plugin);
			return;
		} else {


			PreparedStatement ps = null;
			ResultSet rs = null;
			try {

				ps = conn.prepareStatement("SELECT * FROM " + mysqlTable);
				rs = ps.executeQuery();
				while (rs.next()){
					plugin.bannedNubs.add(rs.getString("name").toLowerCase());
				}
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute MySQL statement: ", ex);
			} finally {
				try {
					if (ps != null)
						ps.close();
					if (rs != null)
						rs.close();
					if (conn != null)
						conn.close();
				} catch (SQLException ex) {
					BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close MySQL connection: ", ex);
				}
			}	

			try {
				conn.close();
				initialiseIPs();
				BanReport.log.log(Level.INFO, "[BanReport] SQL connection initialised." );
			} catch (SQLException e) {
				e.printStackTrace();
				plugin.getServer().getPluginManager().disablePlugin(plugin);
			}
		}
	}

	public void initialiseIPs()
	{
		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT * FROM " + mysqlTable + " WHERE IP != ''");
			rs = ps.executeQuery();
			while (rs.next()){
				plugin.bannedIPs.add(rs.getString("IP"));
			}
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}

	}

	public int getWarnings(String player)
	{
		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		int warnings = 0;
		try {
			ps = conn.prepareStatement("SELECT * FROM warnings WHERE name = ?");
			ps.setString(1, player);
			rs = ps.executeQuery();

			while (rs.next()){
				warnings++;
			}

		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}

		return warnings;
	}
	public void viewWarnings(CommandSender sender, String player)
	{

		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		int i = 0;
		try {
			ps = conn.prepareStatement("SELECT * FROM warnings WHERE name = ?");
			ps.setString(1, player);
			rs = ps.executeQuery();
			sender.sendMessage(ChatColor.GREEN + "Warnings for " + player + ":");
			while (rs.next()){
				i++;
				sender.sendMessage(ChatColor.GRAY + "["+i+"]" + ChatColor.GREEN + rs.getString("warning") + " issued by " + ChatColor.YELLOW + rs.getString("admin"));
			}

		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
	}
	public void warnPlayer(String player, String admin, String warning)
	{
		String mysqlTable = "warnings";
		Connection conn = null;
		PreparedStatement ps = null;
	
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("INSERT INTO " + mysqlTable + " (name,warning,admin) VALUES(?,?,?)");
			ps.setString(1, player);
			ps.setString(2, warning);
			ps.setString(3, admin);
			ps.executeUpdate();
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
	}

	public int findIdFromWarning(String player, int index)
	{
		int id = 0;
		int i = 0;
		String mysqlTable = "warnings";
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("SELECT * FROM " + mysqlTable + " WHERE name= ? ");
			ps.setString(1, player);
			rs = ps.executeQuery();
			while(rs.next())
			{
				System.out.print(rs.getString("reason"));
				System.out.print(rs.getInt("id"));
				i++;
				if(index == i)
				id = rs.getInt("id");
			}
		rs.close();
		ps.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return id;
	}
	public void removeWarning(String player, int index)
	{
		String mysqlTable = "warnings";
		Connection conn = null;
		PreparedStatement ps = null;

		int id = findIdFromWarning(player, index);


		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("DELETE FROM "+mysqlTable + "WHERE id=?");
			ps.setInt(1, id);
			ps.executeUpdate();
			ps.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean checkBanList(String player)
	{

		if(plugin.bannedNubs.contains(player.toLowerCase()))
			return true;


		return false;
		/*
		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
		try {
			ps = conn.prepareStatement("SELECT * FROM " + mysqlTable + " WHERE name = ?");
			ps.setString(1, player);
			rs = ps.executeQuery();
			while (rs.next()){
				return true;
			}
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
		return false;
		 */
	}


	public EditBan getInfo(String player)
	{
		EditBan editban = null;
		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = conn.prepareStatement("SELECT * FROM " + mysqlTable + " WHERE name = ?");
			ps.setString(1, player);
			rs = ps.executeQuery();
			while (rs.next()){
				if(rs.getLong("temptime") != 0)
					editban = new EditBan(rs.getString("name").toLowerCase(), rs.getString("reason"), rs.getString("admin"), rs.getTimestamp("temptime"));
				else
					editban =  new EditBan(rs.getString("name"), rs.getString("reason"), rs.getString("admin"),new Timestamp(0));
			}
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
		return editban;
	}

	public void addPermList(String victim, String reason, String admin, String IP)
	{

		PreparedStatement ps = null;
		try{
			ps = getSQLConnection().prepareStatement("INSERT INTO banhistory (name, reason, admin, date, IP) values (?,?,?,?,?)");
			ps.setString(1, victim);
			ps.setString(2, reason);
			ps.setString(3, admin);
			ps.setTimestamp(4, new Timestamp(new Date().getTime()));
			ps.setString(5, IP);
			ps.executeUpdate();
			ps.close();

		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	public void addPlayer(String player, String reason, String admin, long tempTime)
	{

		plugin.bannedNubs.add(player.toLowerCase());
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("INSERT INTO " + mysqlTable + " (name,reason,admin,time,temptime) VALUES(?,?,?,?,?)");
			Timestamp temptime = new Timestamp(tempTime);
			if(tempTime != 0)
				ps.setTimestamp(5, temptime);
			else
				ps.setLong(5, tempTime);
			ps.setString(1, player);
			ps.setString(2, reason);
			ps.setString(3, admin);
			Timestamp  time = new Timestamp(new Date().getTime());
			ps.setTimestamp(4, time);
			ps.executeUpdate();
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
	}

	public void addIP(String IP, String admin)
	{
		if(IP.length() > 15)
			IP = IP.substring(0,14);
		plugin.bannedIPs.add(IP);
		plugin.bannedNubs.add(IP);
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("INSERT INTO " + mysqlTable + " (name,reason,admin,time,temptime,IP) VALUES(?,?,?,?,?,?)");
			ps.setLong(5, 0);
			ps.setString(1, IP);
			ps.setString(2, "IP BAN");
			ps.setString(3, admin);
			Timestamp  time = new Timestamp(new Date().getTime());
			ps.setTimestamp(4, time);
			ps.setString(6, IP);
			ps.executeUpdate();
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
	}

	public boolean removePlayer(String player)
	{



		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("DELETE FROM " + mysqlTable + " WHERE name = ?");
			ps.setString(1, player);
			ps.executeUpdate();
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
			return false;
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
		plugin.bannedNubs.remove(player.toLowerCase());
		if(plugin.bannedIPs.contains(player.toLowerCase()))
			plugin.bannedIPs.remove(player.toLowerCase());


		return true;

	}




	public boolean checkIP(String IP)
	{
		if(plugin.bannedIPs.contains(IP))
			return true;

		return false;
		/*
		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
		try {
			ps = conn.prepareStatement("SELECT * FROM " + mysqlTable + " WHERE IP = ?");
			ps.setString(1, IP);
			rs = ps.executeQuery();
			while (rs.next()){
				return true;
			}
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
		return false;
		 */
	}
	public void addInfo(CommandSender sender, String victim, String information)
	{
		int x = 0;
		int y = 0;
		int z = 0;
		if(sender instanceof Player)
		{
			Player player = (Player)sender;
			x = player.getLocation().getBlockX();
			y = player.getLocation().getBlockY();
			z = player.getLocation().getBlockZ();
		}
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("INSERT INTO baninfo (player,information,admin,x,y,z) VALUES(?,?,?,?,?,?)");
			ps.setString(1, victim);
			ps.setString(2, information);
			ps.setString(3, sender.getName());
			ps.setInt(4, x);
			ps.setInt(5, y);
			ps.setInt(6, z);
			ps.executeUpdate();
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}

	}

	public HashMap<Integer, BanInfo> getBanInfo(String player)
	{
		HashMap<Integer, BanInfo> hashMap = new HashMap<Integer, BanInfo>();
		int i=0;
		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT * FROM baninfo WHERE player = ?");
			ps.setString(1, player);
			rs = ps.executeQuery();

			while (rs.next()){

				hashMap.put(i, new BanInfo(player, rs.getString("admin"), rs.getString("information"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("id")));
				i++;
			}

		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
		return hashMap;
	}

	public void removeInfo(int id)
	{
		String mysqlTable = "baninfo";

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("DELETE FROM " + mysqlTable + " WHERE id = ?");
			ps.setInt(1, id);
			ps.executeUpdate();
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
	}


	public void updateSQLite()
	{
		String query = "CREATE TABLE `warnings` ("
				+ "`id` integer primary key autoincrement,"
				+ "`name` varchar(30) NOT NULL,"
				+ "`warning` text NOT NULL,"
				+ "`admin` varchar(30) NOT NULL)";
		String query2 = "CREATE TABLE `baninfo` ("
				+"`id` integer primary key autoincrement,"
				+"`player` varchar(32) NOT NULL,"
				+"`information` text NOT NULL,"
				+"`admin` varchar(32) NOT NULL,"
				+"`x` int(4) NOT NULL,"
				+"`y` int(4) NOT NULL,"
				+"`z` int(4) NOT NULL)";
		String query3 = "CREATE TABLE `banhistory` ( `id` integer primary key autoincrement,"
				+ "`name` varchar(32) NOT NULL,"
				+ "`reason` text NOT NULL," 
				+"`admin` varchar(32) NOT NULL," 
				+"`date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," 
				+"`ip` varchar(15) NOT NULL)";

		PreparedStatement ps = null;
		Connection conn = getSQLConnection();
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement(query);
			ps.executeUpdate();
			ps = conn.prepareStatement(query2);
			ps.executeUpdate();
			ps = conn.prepareStatement(query3);
			ps.executeUpdate();
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
	}
	public void makeSQLiteTables()
	{
		String query = 	"CREATE TABLE `banlist` ("
				+		"  `name` varchar(32) NOT NULL,"
				+		"  `reason` text NOT NULL,"
				+	 	" `admin` varchar(32) NOT NULL,"
				+  "`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,"
				+  "`temptime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',"
				+  "`IP` varchar(15) DEFAULT NULL,"
				+ " PRIMARY KEY (`name`)"
				+	"); ";


		if(!tableExists())
		{
			PreparedStatement ps = null;
			Connection conn = getSQLConnection();
			try {
				conn = getSQLConnection();
				ps = conn.prepareStatement(query);
				ps.executeUpdate();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
			} finally {
				try {
					if (ps != null)
						ps.close();
					if (conn != null)
						conn.close();
				} catch (SQLException ex) {
					BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
				}
			}


		}
		if(!otherTablesExist())
		{
			updateSQLite();
		}

	}
	private boolean tableExists() {
		ResultSet rs = null;
		try {
			Connection conn = getSQLConnection();
			DatabaseMetaData dbm = conn.getMetaData();
			rs = dbm.getTables(null, null, "banlist", null);
			if (!rs.next()) {
				if (rs != null) {
					rs.close();
				}

				if (conn != null)
					conn.close();
				return false;
			}
			if (rs != null) {
				rs.close();
			}
			if (conn != null)
				conn.close();
			return true;
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
			return false;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
	}
	private boolean otherTablesExist() {
		ResultSet rs = null;
		try {
			Connection conn = getSQLConnection();
			DatabaseMetaData dbm = conn.getMetaData();
			rs = dbm.getTables(null, null, "banhistory", null);
			if (!rs.next()) {
				if (rs != null) {
					rs.close();
				}

				if (conn != null)
					conn.close();
				return false;
			}
			if (rs != null) {
				rs.close();
			}
			if (conn != null)
				conn.close();
			return true;
		} catch (SQLException ex) {
			BanReport.log.log(Level.SEVERE, "[BanReport] Couldn't execute SQL statement: ", ex);
			return false;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}
	}
}
