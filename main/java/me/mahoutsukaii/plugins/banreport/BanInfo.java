package me.mahoutsukaii.plugins.banreport;

public class BanInfo {

	private String player;
	private String admin;
	private String information;
	private int x;
	private int y;
	private int z;
	private int id;
	
	public BanInfo( String player, String admin, String information, int x, int y, int z, int id)
	{
		this.player = player;
		this.admin = admin;
		this.x = x;
		this.y = y;
		this.z = z;
		this.information = information;
		this.id = id;
	}
	
	public String getPlayer()
	{
		return player;
	}
	
	public String getAdmin()
	{
		return admin;
	}
	
	public String getInfo()
	{
		return information;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getZ()
	{
		return z;
	}
	
	public int getID()
	{
		return id;
	}
	
	
}
