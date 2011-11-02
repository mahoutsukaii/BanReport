package jadon.mahoutsukaii.plugins.banreport;


import java.sql.Timestamp;

public class EditBan {

	public String name;
	public String reason;
	public String admin;
	public long temptime;
	public String additional;
	
	EditBan(String name, String reason, String admin, Timestamp temptime, String additional){
		
		this.temptime = temptime.getTime();
		this.name = name;
		this.reason = reason;
		this.admin = admin;
		this.additional = additional;
	}
		
}
