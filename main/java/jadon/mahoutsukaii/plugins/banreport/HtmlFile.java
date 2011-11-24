package jadon.mahoutsukaii.plugins.banreport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import org.bukkit.ChatColor;

public class HtmlFile {

	private String header;
	private String footer;
	private String body;
	private static String lineStart = "<tr><td>";
	private static String start = "<td>";
	private static String end = "</td>";
	private static String LineEnd = "</td></tr>";
	private BanReport plugin;
	private int i;
	
	public HtmlFile(BanReport instance)
	{

		try {
			header = convertStreamToString(getClass().getResourceAsStream("/defaults/banlist.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		footer = "<script type='text/javascript'>";
		body = "";
		plugin = instance;
		i=0;
	}
	
	public String convertStreamToString(InputStream is) throws IOException {
	    if (is != null) {
	        Writer writer = new StringWriter();

	        char[] buffer = new char[1024];
	        try {
	            Reader reader = new BufferedReader(
	                    new InputStreamReader(is, "UTF-8"));
	            int n;
	            while ((n = reader.read(buffer)) != -1) {
	                writer.write(buffer, 0, n);
	            }
	        } finally {
	            is.close();
	        }
	        return writer.toString();
	    } else {        
	        return "";
	    }
	}
	
	public void add(String player,String reason, String admin, long time, long tempTime, String additional)
	{
		body = body + lineStart + player + end + start + reason + end + start + admin + end + start + new Date(time) + end;
		if(tempTime == 0)
			body = body + start + "Permanent." + end;
		else
			body = body + start + new Date(tempTime) + end;
		
		body = body + start + format(additional, reason) + LineEnd;
	
	}
	public String format(String additional, String reason)
	{

		String script = "<a href='#' onmouseover='menuOn("+i+")' onmouseout='menuOff("+i+")'><font class='menuAnchor' face='Arial' color='#000000'>Additional</font></a>";
		footer = footer +"addMenu(250,"+(i*24+20)+");";
		
		String add =reason.replace("'", "&#39;") + "/r" + additional;
    	String[] formatted = add.split("/r");
    	for(int z=0;z<formatted.length;z++)
    	{
    		footer = footer + "addMenuItem('"+formatted[z]+"','','');";
    	}
    	i++;
		return script;
	}
	protected void save() {
		
		footer = footer + "drawMenus();</script></table></html>";
		body = header + body + footer;
		
		String name = "banlist.html";
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(name));
			out.write(body);
			out.close();
			} 
			catch (IOException e) 
			{ 
			System.out.println("Exception ");

			}
		System.out.println("[BanReport] banlist.html saved!");
	}

}
