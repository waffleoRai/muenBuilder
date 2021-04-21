package waffleoRai_mubuilder;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import waffleoRai_Utils.FileBuffer;

public class MuenProgramFiles {
	
	
	/*----- Various Ini Keys -----*/
	
	public static final String INIKEY_LAST_PROJ = "LAST_PROJECT_PATH";
	public static final String INIKEY_UNIFONT_NAME = "UNICODE_FONT";
	
	public static final String INIKEY_LAST_SRC = "LAST_ASSETSRC_PATH";
	public static final String INIKEY_LAST_BUILDDIR = "LAST_BUILDDIR_PATH";
	
	/*----- Font -----*/
	
	public static final String ENCODING = "UTF8";
	private static final String[] TRYFONTS = {"Arial Unicode MS", "MS PGothic", "MS Gothic", 
			"AppleGothic", "Takao PGothic",
			"Hiragino Maru Gothic Pro", "Hiragino Kaku Gothic Pro"};
	
	/*----- Paths -----*/
	
	public static final String INI_FILE_NAME = "muenbop.ini";
	
	private static String ini_path;
	
	public static String getIniPath(){
		if(ini_path != null) return ini_path;
		
		String osname = System.getProperty("os.name");
		osname = osname.toLowerCase();
		String username = System.getProperty("user.name");
		
		if(osname.startsWith("win")){
			//Assumed windows
			String dir = "C:\\Users\\" + username;
			dir += "\\AppData\\Local\\waffleorai\\muenbuilder";
			dir += "\\" + INI_FILE_NAME;
			ini_path = dir;
			return dir;
		}
		else{
			//Assumed Unix like
			String dir = System.getProperty("user.home");
			char sep = File.separatorChar;
			dir += sep + "appdata" + sep + "local" + sep + "waffleorai" + sep + "muenbuilder";
			dir += sep + INI_FILE_NAME;
			ini_path = dir;
			return dir;
		}
	}

	/*----- Init Values -----*/
	
	private static Map<String, String> init_values;
	private static String my_unifont;
	
	public static boolean readIni() throws IOException{
		init_values = new TreeMap<String, String>();
		
		String inipath = getIniPath();
		if(!FileBuffer.fileExists(inipath)) return false;
		
		BufferedReader br = new BufferedReader(new FileReader(inipath));
		String line = null;
		while((line = br.readLine()) != null)
		{
			if(line.isEmpty()) continue;
			if(line.startsWith("#")) continue;
			String[] fields = line.split("=");
			if(fields.length < 2) continue;
			init_values.put(fields[0], fields[1]);
		}
		br.close();

		return true;
	}
	
	public static String getIniValue(String key){
		if(init_values == null) return null;
		return init_values.get(key);
	}
	
	public static void setIniValue(String key, String value){
		if(init_values == null) init_values = new HashMap<String, String>();
		init_values.put(key, value);
		//System.err.println("Ini Value Set: key = " + key + " | val = " + value);
	}
	
	public static void createIniFile(String installDir) throws IOException{
		String inipath = getIniPath();
		String inidir = inipath.substring(0, inipath.lastIndexOf(File.separator));
		
		if(!FileBuffer.directoryExists(inidir)) Files.createDirectories(Paths.get(inidir));
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(inipath));
		bw.write(INIKEY_LAST_PROJ + "=" + System.getProperty("user.home") + "\n");
		bw.close();
	}
	
	public static void saveIniFile() throws IOException{
		String inipath = getIniPath();
		List<String> keyset = new ArrayList<String>(init_values.size()+1);
		keyset.addAll(init_values.keySet());
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(inipath));
		for(String k : keyset)
		{
			String v = init_values.get(k);
			bw.write(k + "=" + v + "\n");
		}
		bw.close();
	}
	
	public static Font getUnicodeFont(int style, int size){
		if(my_unifont != null) return new Font(my_unifont, style, size);
		
		//Try the key...
		String fontkey = getIniValue(INIKEY_UNIFONT_NAME);
		
		if(fontkey != null) my_unifont = fontkey;
		else
		{
			//See what's on this system
			String[] flist = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
			for(String name : TRYFONTS)
			{
				if(my_unifont != null) break;
				for(String f : flist)
				{
					if(f.equalsIgnoreCase(name))
					{
						my_unifont = name;
						System.err.println("Unicode font detected: " + my_unifont);
						break;
					}
				}
			}
			setIniValue(INIKEY_UNIFONT_NAME, my_unifont);
		}
		
		return new Font(my_unifont, style, size);
	}
	
}
