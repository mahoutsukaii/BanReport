/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jadon.mahoutsukaii.plugins.banreport;

/**
 *
 * @author Alec
 */
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.Date;
//import java.util.ArrayList;


import org.bukkit.util.config.Configuration;

@SuppressWarnings("deprecation")

public class MySQLDatabase{

	BanReport plugin;

	public static Connection getSQLConnection()  {
		Configuration Config = new Configuration(new File("plugins/BanReport/config.yml"));
		Config.load();
		if(BanReport.useMySQL)
		{
			String mysqlDatabase = Config.getString("mysql-database","jdbc:mysql://localhost:3306/minecraft");
			String mysqlUser = Config.getString("mysql-user","root");
			String mysqlPassword = Config.getString("mysql-password","root");
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

	public void initialise(BanReport plugin){
		this.plugin = plugin;
		Connection conn = getSQLConnection();
		String mysqlTable = plugin.getConfiguration().getString("mysql-table","banlist");
		plugin.bannedNubs.clear();
		plugin.bannedIPs.clear();
		if(!BanReport.useMySQL)
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
		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
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
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				BanReport.log.log(Level.SEVERE, "[BanReport] Failed to close SQL connection: ", ex);
			}
		}

	}
	public boolean checkBanList(String player)
	{
		
		if(plugin.bannedNubs.contains(player))
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

	public String getBanInfo(String player, boolean local)
	{
		String reason;
		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
		try {
			ps = conn.prepareStatement("SELECT * FROM " + mysqlTable + " WHERE name = ?");
			ps.setString(1, player);
			rs = ps.executeQuery();
			while (rs.next()){
				if(local == false)
					reason = rs.getString("reason") + "/r" + rs.getString("additional");
				else
					reason = rs.getString("additional");
				return reason;
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
		return null;
	}
	
	public EditBan getInfo(String player)
	{
		
		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
		try {
			ps = conn.prepareStatement("SELECT * FROM " + mysqlTable + " WHERE name = ?");
			ps.setString(1, player);
			rs = ps.executeQuery();
			while (rs.next()){
				if(rs.getLong("temptime") != 0)
				return new EditBan(rs.getString("name"), rs.getString("reason"), rs.getString("admin"), rs.getTimestamp("temptime"), rs.getString("additional"));
				else
					return  new EditBan(rs.getString("name"), rs.getString("reason"), rs.getString("admin"),new Timestamp(0), rs.getString("additional"));
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
		return null;
	}
	
	public void addPlayer(String player, String reason, String admin, long tempTime)
	{
		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
		plugin.bannedNubs.add(player);
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("INSERT INTO " + mysqlTable + " (name,reason,admin,time,temptime,additional) VALUES(?,?,?,?,?,?)");
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
			ps.setString(6, "");
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
		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
		plugin.bannedIPs.add(IP);
		plugin.bannedNubs.add(IP);
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("INSERT INTO " + mysqlTable + " (name,reason,admin,time,temptime,IP,additional) VALUES(?,?,?,?,?,?,?)");
			ps.setLong(5, 0);
			ps.setString(1, IP);
			ps.setString(2, "IP BAN");
			ps.setString(3, admin);
			Timestamp  time = new Timestamp(new Date().getTime());
			ps.setTimestamp(4, time);
			ps.setString(6, IP);
			ps.setString(7, "");
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

		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
		

		
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
		plugin.bannedNubs.remove(player);
		if(plugin.bannedIPs.contains(player))
			plugin.bannedIPs.remove(player);

		
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
	public void addInfo(String player, String info, boolean override)
	{
		
		//EditBan currentStuffs = getInfo(player);
		
		//System.out.print(currentStuffs.time);
		

		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
		String info2 = getBanInfo(player, true);
		
		if(info2 == "")
			info2 = info;
		else
		info2 = info2 + "/r" + info;
		
		if(override == true)
			info2 = info;
		
		//info2 = "herp"; //lol testin
		

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("UPDATE " +mysqlTable+ " SET additional = ? WHERE name = ?"); //need something like EditBan..
		//	ps.setString(1, currentStuffs.reason);
		//	ps.setString(1, currentStuffs.admin);
		//	ps.setTimestamp(2, currentStuffs.time);
		//	ps.setTimestamp(3, currentStuffs.tempTime); //yeah fuck this
			ps.setString(1, info2);
			ps.setString(2, player);
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
				+  "`additional` varchar(4096) NOT NULL,"
				+ " PRIMARY KEY (`name`)"
				+	") ";
		
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
		
	}
	private boolean tableExists() {
    	ResultSet rs = null;
    	try {
    		Connection conn = getSQLConnection();
    		DatabaseMetaData dbm = conn.getMetaData();
    		rs = dbm.getTables(null, null, "banlist", null);
    		if (!rs.next()) {
    			return false;
    		}
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
