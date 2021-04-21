package waffleoRai_poneater;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import waffleoRai_Utils.FileBuffer;
import waffleoRai_mubuilder.MebAsset;
import waffleoRai_mubuilder.MebTypes;
import waffleoRai_mubuilder.MuenProject;

public class Poneater_umineko_pc {

	
	private static Map<String, String> stralias;
	private static Map<String, Integer> numalias;
	private static Map<Integer, Integer> i_var;
	private static Map<Integer, String> s_var;
	private static Map<String, int[]> arrays;
	private static Map<Integer, Effect> effects;
	
	private static MuenProject proj;
	//private static Map<String, Long> strti_map;
	private static Map<String, MebAsset> strta_map;
	private static Map<String, AMatch> backmap;
	
	private static Map<String, Integer> idx_map;
	
	private static Map<String, Loc> loc_tbl; //Locations of assets in vanilla or 07th modded installation
	
	public static final String TBLNAME_BKG = "bkg_tbl_name";
	
	/*
	 * Dummy textbox 
	 * 
	 *
	 */
	
	//TODO parsing note! When dealing with text lines, may be multiple lines - when encountering text, read char by char until reaching end.
	
	public static final int DEFO_QUAKE_AMT = 20;
	
	public static final String TXB_ASSET_NAME = "txb_maindia";
	public static final String TXB_MOD0_NAME = "diatxb_charname";
	public static final String TXB_MOD1_NAME = "diatxb_contents";
	public static final String STT_CHARNAME_TBL_NAME = "stt_charnames";
	public static final int DEFO_FONT_SIZE = 12;
	
	private static String stt_name;
	private static BufferedWriter stt_jp;
	private static BufferedWriter stt_en;
	private static int stt_en_idx;
	private static int stt_jp_idx;
	private static int st_fnt_sz;
	private static Map<String, Integer> char_outfit_map;
	private static boolean namebox_visible;
	private static boolean char_in_left;
	private static boolean char_in_center;
	private static boolean char_in_right;
	private static boolean bad_cmd_flag = false;
	private static boolean strcount_flag = false;
	private static boolean in_for = false;
	//private static boolean txb_on;
	
	private static final String[] adv_idxs = {"???", "KIN", "KLA", "NAT", "JES",
											  "EVA", "HID", "GEO", "RUD",
											  "KIR", "BUT", "ENJ", "ROS", "MAR",
											  "GEN", "SHA", "KAN", "GOH", "KUM",
											  "NAN", "AMA", "OKO", "KAS", "PRO",
											  "KAW", "NA2", "KU2", "BEA", "BER",
											  "LAM", "WAL", "RON", "GAP", "SAK",
											  "EV2", "S45", "S41", "S00", "RG1",
											  "RG2", "RG3", "RG4", "RG5", "RG6",
											  "RG7", "GOA"};
	
	
	private static final String[] ignored_cmds = {"set_achievement", "gosub", "dwave_eng", "mov", "click", "sel", "delay",
			"csp","adv_default_text_speed_restore_window","windoweffect", "setlangstring","skipoff","adv_mode_off","adv_mode_on",
					"locate","print","inc","rmode"};
	
	//locate x,y does something like "set sentence font to x,y" but I don't know if it's used to position text or what?
	
	private static class Effect{
		public int no;
		public int eff;
		public int dur;
		public String img;
	}
	
	public static class Loc{
		boolean isnsa_v;
		String path_v;
		
		boolean isnsa_m;
		String path_m;
	}
	
	public static class AMatch{
		public MebAsset asset;
		public int idx;
		
		public AMatch(MebAsset a, int i) {
			asset = a;
			idx = i;
		}
	}
	
	private static int resolveVarID(String var_s) {
		char c = var_s.charAt(0);
		
		if(c == '%') {
			//Try to parse the name as an int. If that fails, assume it's a numalias
			String vname = var_s.substring(1);
			try {int i = Integer.parseInt(vname); return i;}
			catch(NumberFormatException ex) {
				//Try a numalias
				Integer num = numalias.get(vname);
				if(num == null) return -1;
				return num;
			}
		}
		else if (c == '$') {
			//String variable, but may be referenced by a number variable or numalias
			if(var_s.charAt(1) == '%') {
				int i1 = resolveVarID(var_s.substring(1));
				Integer ival = i_var.get(i1);
				if(ival == null) return -1;
				return ival;
			}
			String vname = var_s.substring(1);
			try {int i = Integer.parseInt(vname); return i;}
			catch(NumberFormatException ex) {
				//Try a numalias
				Integer num = numalias.get(vname);
				if(num == null) return -1;
				return num;
			}
		}
		else if(c == '?') {
			//TODO
		}
		
		return -1;
	}
	
	private static String[] splitLine(String line) {
		List<String> list = new LinkedList<String>();
		StringBuilder sb = new StringBuilder(2048);
		int slen = line.length();
		boolean inquotes = false;
		for(int i = 0; i < slen; i++) {
			char c = line.charAt(i);
			if(!inquotes) {
				switch(c) {
				case '\"': 
					inquotes = true;
					sb.append(c);
					break;
				case ';': 
					i = slen; //Cause break a lazy way
					break;
				case ':': 
					list.add(sb.toString());
					sb = new StringBuilder(2048);
					break;
				default: sb.append(c); break;
				}
			}
			else {
				if(c == '\"') inquotes = false;
				sb.append(c);
			}
		}
		
		if(sb.length() > 0) list.add(sb.toString());
		if(list.isEmpty()) return null;
		String[] args = new String[list.size()];
		int i = 0;
		for(String s : list) {
			args[i++] = s;
		}
		
		return args;
	}
	
	private static String[] splitCommand(String cmd) {
		List<String> list = new LinkedList<String>();
		StringBuilder sb = new StringBuilder(2048);
		int slen = cmd.length();
		boolean inquotes = false;
		boolean spaced = false;
		for(int i = 0; i < slen; i++) {
			char c = cmd.charAt(i);
			if(!inquotes) {
				switch(c) {
				case '\"': 
					inquotes = true;
					break;
				case ';': 
					i = slen; //Cause break a lazy way
					break;
				case ',': 
					list.add(sb.toString());
					sb = new StringBuilder(2048);
					break;
				case ' ': 
					if(!spaced) {
						list.add(sb.toString());
						sb = new StringBuilder(2048);
					}
					spaced = true;
					break;
				default: sb.append(c); break;
				}
			}
			else {
				if(c == '\"') inquotes = false;
				else sb.append(c);
			}
		}
		
		//
		if(sb.length() > 0) list.add(sb.toString());
		if(list.isEmpty()) return null;
		String[] args = new String[list.size()];
		int i = 0;
		for(String s : list) {
			args[i++] = s;
		}
		
		return args;
	}
	
	private static void readDefineSection(String path) throws IOException {
		
		stralias = new HashMap<String,String>();
		numalias = new HashMap<String,Integer>();
		i_var = new HashMap<Integer,Integer>();
		s_var = new HashMap<Integer,String>();
		arrays = new HashMap<String,int[]>();
		effects = new HashMap<Integer,Effect>();
		
		//Read everything between *define and game
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));
		String line = null;
		//FF to *define
		while((line = br.readLine()) != null) {
			if(line.startsWith("*define")) break;
		}
		
		while((line = br.readLine()) != null) {
			if(line.startsWith("game")) break;
			if(line.isEmpty()) continue;
			if(line.startsWith(";")) continue;
			
			//Don't forget that a : denotes multiple commands
			/*
			 * Recognize
			 * 	mov
			 * 	effect
			 * 	stralias
			 * 	numalias
			 * 	dim
			 * 	inc
			 */
			
			//String[] fields = line.split(";");
			//line = fields[0]; //Remove everything after comment sign
			String[] fields = splitLine(line);
			
			for(int i = 0; i < fields.length; i++) {
				String cmd = fields[i].trim();
				String[] args = splitCommand(cmd);
				if(args == null) continue;
				
				cmd = args[0].toLowerCase();
				if(cmd.equals("mov")) {
					String var_s = args[1];
					String val_s = args[2];
					int var_idx = resolveVarID(var_s);
					int val = Integer.parseInt(val_s);
					i_var.put(var_idx, val);
					System.err.println("%" + var_idx + " set to " + val);
				}
				else if(cmd.equals("effect")) {
					//In this script, the first three appear to always be numeric literals.
					//If there's a 4th arg, it's a string in quotes.
					Effect e = new Effect();
					e.no = Integer.parseInt(args[1]);
					e.eff = Integer.parseInt(args[2]);
					e.dur = Integer.parseInt(args[3]);
					if(args.length > 4) e.img = args[4];
					effects.put(e.no, e);
					System.err.println("Effect " + e.no + " : " + e.eff + " | " + e.dur + " millis | " + e.img);
				}
				else if(cmd.equals("stralias")) {
					//stralias alias str
					stralias.put(args[1], args[2]);
					//System.err.println("String Alias: " + args[1] + " = " + "\"" + args[2] + "\"");
				}
				else if(cmd.equals("numalias")) {
					//numalias alias int
					//Variables often used for the int
					String alias = args[1];
					String val_s = args[2];
					int val = 0;
					if(val_s.charAt(0) == '%') {
						int idx = resolveVarID(val_s);
						Integer n = i_var.get(idx);
						if(n != null) val = n;
					}
					else val = Integer.parseInt(val_s);
					numalias.put(alias, val);
					System.err.println("Num Alias: " + alias + " = " + val);
				}
				else if(cmd.equals("dim")) {
					//dim ?arr[int]
					//It can be different, but that's the only one in this script so fuckit
					String arg = args[1].substring(1);
					String[] a = arg.split("\\[");
					String aname = a[0];
					String istr = a[1];
					int asz = Integer.parseInt(istr.replace("]", ""));
					arrays.put(aname, new int[asz]);
					System.err.println("Array decl: " + aname + "[" + asz + "]");
				}
				else if(cmd.equals("inc")) {
					String a1 = args[1];
					int idx = resolveVarID(a1);
					Integer n = i_var.get(idx);
					if(n == null) n = 0;
					n++;
					i_var.put(idx, n);
					//System.err.println("Increment %" + idx);
				}
			}
			
		}
		
		br.close();
	}
	
	private static void organizeAssets(String rootpath) throws IOException {
		proj = MuenProject.newProject("umineko0", "D:\\usr\\bghos\\code\\mubuilds\\umineko0\\build");
		idx_map = new HashMap<String, Integer>();
		//strti_map = new HashMap<String, Long>();
		strta_map = new HashMap<String, MebAsset>();
		loc_tbl = new HashMap<String, Loc>();
		backmap = new HashMap<String, AMatch>();
		
		System.err.println("Reading backgrounds...");
		//-- Backgrounds
		//Table + ryu and alc variants
		//Prepare engine project links
		String tblpath = rootpath + "\\bkg\\common\\bkg_tbl_itbl.tsv";
		int pkg0 = proj.newPackage("imt_common", "./imt_common.assp");
		int pkg1 = proj.newPackage("bkg_ryu", "./bkg/bkg_ryu.assp");
		int pkg2 = proj.newPackage("bkg_alc", "./bkg/bkg_alc.assp");
		int gid0 = proj.newGroup("imt_common", "./groups/imt_common.assh");
		String bkg_tbl_name = "scebkg_imt";
		MebAsset bkg_tbl = proj.newAsset(pkg0, gid0, tblpath);
		bkg_tbl.setTypeID(MebTypes._IMT_DEF);
		bkg_tbl.setLocalName(bkg_tbl_name);
		
		//Scan bkg straliases, make asset record for each and add to table
		String[] strarr = {"airport", "aquarium", "chapel", "city", "forest", "garden",
							"guesthouse", "kawhouse", "kumhouse", "mainbuilding", "nanclinic", "restaurant", "rosehouse",
							"school", "secrethouse", "ship", "subway"};
		BufferedWriter bw = new BufferedWriter(new FileWriter(tblpath));
		bw.write("RYU\tALC\n");
		int tidx = 0;
		for(String s : strarr) {
			String scanstr = "\\background\\" + s;
			List<String> slist = new LinkedList<String>();
			for(String k : stralias.keySet()) {
				String val = stralias.get(k);
				if(val.contains(scanstr)) {
					slist.add(k);
				}
			}
			
			Collections.sort(slist);
			//New group
			String gname = "bkg_" + s;
			int gid1 = proj.newGroup(gname, "./groups/" + gname + ".assh");
			
			for(String aname : slist) {
				String fname = aname.toLowerCase() + ".png";
				
				//Ryukishi version
				String srcpath = rootpath + "\\bkg\\ryu\\" + s + "\\" + aname.toLowerCase() + ".bmp";
				MebAsset a = proj.newAsset(pkg1, gid1, srcpath);
				a.setLocalName(aname + "_ryu");
				a.setTypeID(MebTypes._IMG_PNG);
				bw.write(a.getLocalName() + "\t");
				
				Loc l = new Loc();
				l.isnsa_v = true; l.path_v = "./bmp/background/" + s + "/" + aname.toLowerCase() + ".bmp";
				l.isnsa_m = true; l.path_m = "./bmp/background/" + s + "/" + aname.toLowerCase() + ".bmp";
				loc_tbl.put(a.getLocalName(), l);
				backmap.put(l.path_m, new AMatch(bkg_tbl, tidx));
				
				//Alchemist version
				srcpath = rootpath + "\\bkg\\alc\\" + s + "\\" + fname;
				a = proj.newAsset(pkg2, gid1, srcpath);
				a.setLocalName(aname + "_alc");
				a.setTypeID(MebTypes._IMG_PNG);
				bw.write(a.getLocalName() + "\n");
				
				l = new Loc();
				l.isnsa_v = false; l.path_v = "<NA>";
				l.isnsa_m = false; l.path_m = "./bmp/background/" + s + "/" + fname;
				loc_tbl.put(a.getLocalName(), l);
				backmap.put(l.path_m, new AMatch(bkg_tbl, tidx));
				
				strta_map.put(aname.toLowerCase(), bkg_tbl);
				idx_map.put(aname.toLowerCase(), tidx++);

			}
		}
		//Gonna bump pettan up here too since it's ryu/alc not pch/alc?
		int bkg_pkg_alc = pkg2;
		int bkg_pkg_ryu = pkg1;
		
		bw.close();
		
		System.err.println("Reading effects...");
		//-- Effects
		//Both the specified effects and all other "effects" images
		//Theres also the bkg/efe effects too which differ between alc and ryu
		//Grouped the same way they are packaged. (common, alc, ryu)
		pkg1 = proj.newPackage("efe_ryu", "./efe/efe_ryu.assp");
		pkg2 = proj.newPackage("efe_alc", "./efe/efe_alc.assp");
		int pkg3 = proj.newPackage("efe_common", "./efe/efe_common.assp");
		int gid1 = proj.newGroup("efe_ryu", "./groups/efe_ryu.assh");
		int gid2 = proj.newGroup("efe_alc", "./groups/efe_alc.assh");
		int gid3 = proj.newGroup("efe_common", "./groups/efe_common.assh");
		//Need language table and art style table...
		String tpath0 = rootpath + "\\efe\\efe_tbl_style.tsv";
		String tpath1 = rootpath + "\\efe\\efe_tbl_lan.tsv";
		MebAsset tbl0 = proj.newAsset(pkg0, gid0, tpath0);
		tbl0.setTypeID(MebTypes._IMT_DEF);
		tbl0.setLocalName("efe_style_imt");
		MebAsset tbl1 = proj.newAsset(pkg0, gid0, tpath1);
		tbl1.setTypeID(MebTypes._IMT_DEF);
		tbl1.setLocalName("efe_lan_imt");
		bw = new BufferedWriter(new FileWriter(tpath0));
		bw.write("RYU\tALC\n");
		BufferedWriter bw1 = new BufferedWriter(new FileWriter(tpath1));
		bw1.write("JP\tEN\n");
		
		//Scan for straliases with /background/efe
		//See where they are in the data dir and sort accordingly.
		//Tables should be in imt_common, not efe_common
		int tidx0 = 0;
		int tidx1 = 1;
		Set<String> efealc_found = new HashSet<String>();
		for(String k : stralias.keySet()) {
			String val = stralias.get(k);
			if(val.contains("\\background\\efe")) {
				//Look for one by that name
				String fname = val.substring(val.lastIndexOf('\\')+1);
				if(FileBuffer.fileExists(rootpath + "\\efe\\alc\\" + fname)) {
					efealc_found.add(rootpath + "\\efe\\alc\\" + fname);
					
					//Style
					
					String srcpath = rootpath + "\\efe\\ryu\\" + fname;
					String fname2 = fname;
					boolean bmp = false;
					if(!FileBuffer.fileExists(srcpath)) {
						//Try bmp
						srcpath = srcpath.replace(".png", ".bmp");
						fname2.replace(".png", ".bmp");
					}
					if(FileBuffer.fileExists(srcpath)) {
						MebAsset a = proj.newAsset(pkg1, gid1, rootpath + "\\efe\\ryu\\" + fname2);
						a.setTypeID(MebTypes._IMG_PNG);
						a.setLocalName(k + "_ryu");
						bw.write(a.getLocalName() + "\t");
						
						Loc l = new Loc();
						l.isnsa_v = true; l.path_v = "./bmp/background/efe/" + fname2;
						l.isnsa_m = true; l.path_m = "./bmp/background/efe/" + fname2;
						loc_tbl.put(a.getLocalName(), l);
						backmap.put(l.path_m, new AMatch(tbl0, tidx));
					}
					else bw.write("<NONE>\t");
					
					MebAsset a = proj.newAsset(pkg2, gid2, rootpath + "\\efe\\alc\\" + fname);
					a.setTypeID(MebTypes._IMG_PNG);
					a.setLocalName(k + "_alc");
					bw.write(a.getLocalName() + "\n");
					
					Loc l = new Loc();
					l.isnsa_v = false; l.path_v = "<NA>";
					l.isnsa_m = false; 
					if(val.contains("big\\")) l.path_m = "./big/bmp/background/efe/" + fname;
					else l.path_m = "./bmp/background/efe/" + fname;
					loc_tbl.put(a.getLocalName(), l);
					backmap.put(l.path_m, new AMatch(tbl0, tidx));
					
					//Map table index
					strta_map.put(k.toLowerCase(), tbl0);
					idx_map.put(k.toLowerCase(), tidx0++);
				}
				else if(FileBuffer.fileExists(rootpath + "\\efe\\common\\jp\\" + fname)) {
					//Lang
					MebAsset a = proj.newAsset(pkg3, gid3, rootpath + "\\efe\\common\\jp\\" + fname);
					a.setTypeID(MebTypes._IMG_PNG);
					a.setLocalName(k + "_jp");
					bw1.write(a.getLocalName() + "\t");
					
					Loc l = new Loc();
					l.isnsa_v = true; l.path_v = "./bmp/background/efe/" + fname;
					l.isnsa_m = false; l.path_m = "./bmp/background/efe/" + fname;
					loc_tbl.put(a.getLocalName(), l);
					backmap.put(l.path_m, new AMatch(tbl1, tidx1));
					
					a = proj.newAsset(pkg3, gid3, rootpath + "\\efe\\common\\en\\" + fname);
					a.setTypeID(MebTypes._IMG_PNG);
					a.setLocalName(k + "_en");
					bw1.write(a.getLocalName() + "\n");
					
					l = new Loc();
					l.isnsa_v = true; l.path_v = "./en/bmp/background/efe/" + fname;
					l.isnsa_m = false; l.path_m = "./en/bmp/background/efe/" + fname;
					loc_tbl.put(a.getLocalName(), l);
					backmap.put(l.path_m, new AMatch(tbl1, tidx1));
					
					//Map table index
					strta_map.put(k.toLowerCase(), tbl1);
					idx_map.put(k.toLowerCase(), tidx1++);
				}
				else {
					//Common
					//So these seem to be files that either don't exist or are alc specific effects that were moved to scene\cg_alc?
					System.err.println("Common effect! " + val);
				}
			}
		}
		bw1.close();
		bw.close();
		
		//Now do all the files in the common dir
		DirectoryStream<Path> dstr = Files.newDirectoryStream(Paths.get(rootpath + "\\efe\\common"));
		for(Path p : dstr) {
			if(p.endsWith("jp") || p.endsWith("en")) continue;
			//Since it only goes one level deep, not gonna bother w/ recursiveness...
			if(Files.isDirectory(p)) {
				//Do files inside it.
				DirectoryStream<Path> dstr1 = Files.newDirectoryStream(p);
				for(Path p1 : dstr1) {
					String dname = p.getFileName().toString();
					String fname = p1.getFileName().toString();
					fname = fname.substring(0, fname.lastIndexOf('.'));
					String aname = "efe_" + dname + "_" + fname;
					
					MebAsset a = proj.newAsset(pkg3, gid3, p1.toAbsolutePath().toString());
					a.setTypeID(MebTypes._IMG_PNG);
					a.setLocalName(aname);
					
					Loc l = new Loc();
					if(dname.equals("clock")) {
						l.isnsa_v = true; l.path_v = "./bmp/clock/" + fname;
						l.isnsa_m = false; l.path_m = "./bmp/clock/" + fname;
					}
					else {
						l.isnsa_v = true; l.path_v = "./bmp/efe/" + dname + "/" + fname;
						l.isnsa_m = false; l.path_m = "./bmp/efe/" + dname + "/" + fname;	
					}
					loc_tbl.put(a.getLocalName(), l);
					backmap.put(l.path_m, new AMatch(a, 0));
					
					strta_map.put(aname.toLowerCase(), a);
				}
				
				dstr1.close();
			}
			else {
				//Make asset and map.
				String fname = p.getFileName().toString();
				fname = fname.substring(0, fname.lastIndexOf('.'));
				String aname = "efe_" + fname;
				
				MebAsset a = proj.newAsset(pkg3, gid3, p.toAbsolutePath().toString());
				a.setTypeID(MebTypes._IMG_PNG);
				a.setLocalName(aname);
				
				Loc l = new Loc();
				l.isnsa_v = true; l.path_v = "./bmp/efe/" + fname;
				l.isnsa_m = false; l.path_m = "./bmp/efe/" + fname;
				loc_tbl.put(a.getLocalName(), l);
				backmap.put(l.path_m, new AMatch(a, 0));
				
				strta_map.put(aname.toLowerCase(), a);
			}
		}
		dstr.close();
		
		
		//Now do the basic effects table... (Fade types)
		//This table goes in the common efe package/group
		tpath0 = rootpath + "\\efe\\efe_tbl_fadetrans.tsv";
		tbl0 = proj.newAsset(pkg3, gid3, tpath0);
		tbl0.setTypeID(MebTypes._EFT_DEF);
		tbl0.setLocalName("efe_tbl_fadetrans");
		bw = new BufferedWriter(new FileWriter(tpath0));
		//Not sure what to do here...
		//Duration(ms)	Call (eg. pons_18)	Image(If appl.)
		bw.write("NAME\tDUR_MILLIS\tMCALL\tSRCIMG\n");
		tidx = 0;
		for(Effect eff : effects.values()) {
			String nm = "umineko_pons" + String.format("%02d", eff.no);
			bw.write(nm + "\t");
			bw.write(Integer.toString(eff.dur));
			bw.write("pons_" + String.format("%02d", eff.eff));
			
			strta_map.put("efe_" + String.format("%02d", eff.no), tbl0);
			idx_map.put("efe_" + String.format("%02d", eff.no), tidx++);
			
			//Now match the source image (if appl.)
			if(eff.img == null) bw.write("<NA>\n");
			else {
				String targname = eff.img.substring(eff.img.lastIndexOf('\\') + 1, eff.img.lastIndexOf('.'));
				targname = "efe_" + targname;
				bw.write(targname + "\n");
			}
			
		}
		bw.close();
		
		System.err.println("Reading fonts...");
		//-- Fonts
		//Just all packaged and grouped together.
		//Looks like these fonts include Noto Sans and Sazanami, so pretty standard Japanese compat fonts
		pkg1 = proj.newPackage("fnt", "./fnt.assp");
		gid1 = proj.newGroup("fnt", "./groups/fnt.assh");
		for(int i = 0; i < 3; i++) {
			//otf fonts
			MebAsset a = proj.newAsset(pkg1, gid1, rootpath + "\\fnt\\face" + i + ".otf");
			a.setLocalName("fnt_face" + i);
			a.setTypeID(MebTypes._FNT_OTF);
			
			Loc l = new Loc();
			l.isnsa_v = false; l.path_v = "./fonts/face" + i + ".otf";
			l.isnsa_m = false; l.path_m = "./fonts/face" + i + ".otf";
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(a, 0));
			
			a = proj.newAsset(pkg1, gid1, rootpath + "\\fnt\\adv\\face" + i + ".otf");
			a.setLocalName("fnt_adv_face" + i);
			a.setTypeID(MebTypes._FNT_OTF);
			
			l = new Loc();
			l.isnsa_v = false; l.path_v = "<NA>";
			l.isnsa_m = false; l.path_m = "./fonts/adv/face" + i + ".otf";
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(a, 0));
		}
		for(int i = 4; i < 8; i++) {
			//numbered ttf fonts
			MebAsset a = proj.newAsset(pkg1, gid1, rootpath + "\\fnt\\face" + i + ".ttf");
			a.setLocalName("fnt_face" + i);
			a.setTypeID(MebTypes._FNT_TTF);
			
			Loc l = new Loc();
			l.isnsa_v = false; l.path_v = "./fonts/face" + i + ".ttf";
			l.isnsa_m = false; l.path_m = "./fonts/face" + i + ".ttf";
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(a, 0));
		}
		//misc ttf fonts
		String[] strarr1 = {"facex", "fixedface0", "oldface0"};
		for(String s : strarr1) {
			//numbered ttf fonts
			MebAsset a = proj.newAsset(pkg1, gid1, rootpath + "\\fnt\\" + s + ".ttf");
			a.setLocalName("fnt_" + s);
			a.setTypeID(MebTypes._FNT_TTF);
			
			Loc l = new Loc();
			l.isnsa_v = false; l.path_v = "./fonts/" + s + ".ttf";
			l.isnsa_m = false; l.path_m = "./fonts/" + s + ".ttf";
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(a, 0));
		}
		
		//-- Movies ugh
		//I'll probably repack these as mkvs or something too. Maybe keep video by itself and make audio separate so can have multiple tracks?
		//Kind of a pain in the ass to multi-lingual these. Maybe figure out a way to strip to base?
		//Then break into parts so logo is its own movie and credits are overlays?
		//Maybe also try to get the old anime opening :3 (I mostly just like Katayoku no Tori)
		
		System.err.println("Reading cg images...");
		//-- Scene images (cg)
		//Doesn't look like the overlap is 1:1 between pch and alc.
		//So only alc will be reffed for now, but I'll try to package all
		pkg1 = proj.newPackage("cg_alc", "./scene/cg_alc.assp");
		pkg2 = proj.newPackage("cg_pch", "./scene/cg_pch.assp");
		gid1 = proj.newGroup("scene_cg", "./groups/scene_cg.assh");
		gid2 = proj.newGroup("scene_cgefe", "./groups/scene_cgefe.assh");
		//Do pettan (which gets packaged with backgrounds) TODO map these too........
		MebAsset a = proj.newAsset(bkg_pkg_alc, gid1, rootpath + "\\scene\\cg_alc\\pettan.png");
		a.setTypeID(MebTypes._IMG_PNG);
		a.setLocalName("cg_pettan_07th");
		Loc l = new Loc();
		l.isnsa_v = false; l.path_v = "<NA>";
		l.isnsa_m = false; l.path_m = "./bmp/pettan/bg.png";
		loc_tbl.put(a.getLocalName(), l);
		backmap.put(l.path_m, new AMatch(a, 0));
		
		a = proj.newAsset(bkg_pkg_ryu, gid1, rootpath + "\\scene\\cg_ryu\\pettan.bmp");
		a.setTypeID(MebTypes._IMG_PNG);
		a.setLocalName("cg_pettan_ryu");
		l = new Loc();
		l.isnsa_v = true; l.path_v = "./bmp/pettan/bg.bmp";
		l.isnsa_m = true; l.path_m = "./bmp/pettan/bg.bmp";
		loc_tbl.put(a.getLocalName(), l);
		backmap.put(l.path_m, new AMatch(a, 0));
		
		//Scan for \background\cg images in stralias
		for(String k : stralias.keySet()) {
			String val = stralias.get(k);
			if(val.contains("\\background\\cg")) {
				String fname = val.substring(val.lastIndexOf('\\')+1);
				a = proj.newAsset(pkg1, gid1, rootpath + "\\scene\\cg_alc\\" + fname);
				a.setTypeID(MebTypes._IMG_PNG);
				a.setLocalName(k);
				
				strta_map.put(k.toLowerCase(), a);
				
				l = new Loc();
				l.isnsa_v = false; l.path_v = "<NA>";
				l.isnsa_m = false; l.path_m = "./big/bmp/background/cg/" + fname;
				loc_tbl.put(a.getLocalName(), l);
				backmap.put(l.path_m, new AMatch(a, 0));
			}
		}
		
		//Scan the efe folder
		dstr = Files.newDirectoryStream(Paths.get(rootpath + "\\efe\\alc"));
		for(Path p : dstr) {
			//only import those that weren't imported above in efe
			if(efealc_found.contains(p.toAbsolutePath().toString())) continue;
			
			String fname = p.getFileName().toString();
			a = proj.newAsset(pkg1, gid2, p.toAbsolutePath().toString());
			a.setTypeID(MebTypes._IMG_PNG);
			
			String aname = "cg_efe_" + fname.substring(0, fname.lastIndexOf('.'));
			a.setLocalName(aname);
			
			strta_map.put(aname.toLowerCase(), a);
			
			l = new Loc();
			l.isnsa_v = false; l.path_v = "<NA>";
			l.isnsa_m = false; l.path_m = "./big/bmp/background/efe/" + fname;
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(a, 0));
		}
		dstr.close();
		
		//Scan the pch folder
		dstr = Files.newDirectoryStream(Paths.get(rootpath + "\\scene\\cg_pch"));
		for(Path p : dstr) {
			String fname = p.getFileName().toString();
			a = proj.newAsset(pkg2, gid1, p.toAbsolutePath().toString());
			a.setTypeID(MebTypes._IMG_PNG);
			
			String aname = "cg_pch_" + fname.substring(0, fname.lastIndexOf('.'));
			a.setLocalName(aname);
			
			strta_map.put(aname.toLowerCase(), a);
			
			l = new Loc();
			l.isnsa_v = true; l.path_v = "./big/bmp/background/cg/" + fname;
			l.isnsa_m = true; l.path_m = "./big/bmp/background/cg/" + fname;
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(a, 0));
		}
		dstr.close();
		
		System.err.println("Reading character sprites...");
		//-- Character portraits/sprites
		//Regular
		pkg1 = proj.newPackage("chrprt_ryu", "./scene/chrprt_ryu.assp");
		pkg2 = proj.newPackage("chrprt_alc", "./scene/chrprt_alc.assp");
		pkg3 = proj.newPackage("chrprt_pch", "./scene/chrprt_pch.assp");
		//Prepare table
		tidx = 0;
		tblpath = rootpath + "\\scene\\chrprt_tbl_sprite.tsv";
		bw = new BufferedWriter(new FileWriter(tblpath));
		bw.write("RYU\tALC\tPCH\n");
		tbl0 = proj.newAsset(pkg0, gid0, tblpath);
		tbl0.setTypeID(MebTypes._IMT_DEF);
		tbl0.setLocalName("chrprt_sprite_imt");
		//I guess just scan the directories...
		String[] chrnames = {"ama", "bea", "ber", "but", "enj", "ev2", "eva", "gap",
							 "gen", "geo", "goa", "goh", "hid", "jes", "kan", "kas",
							 "kaw", "kin", "kir", "kla", "ku2", "kum", "lam", "mar",
							 "na2", "nan", "nat", "oko", "pro", 
							 "rg1", "rg2", "rg3", "rg4", "rg5", "rg6", "rg7", "ron",
							 "ros", "rud", "s00", "s41", "s45", "sak", "sha", "wal"};
		for(String s : chrnames) {
			dstr = Files.newDirectoryStream(Paths.get(rootpath + "\\scene\\chrprt_alc\\" + s));
			for(Path p : dstr) {
				if(Files.isDirectory(p)) {
					//Should be inside numbered directory
					DirectoryStream<Path> dstr1 = Files.newDirectoryStream(p);
					String no = p.getFileName().toString();
					String gname = "chrprt_" + s + "_" + no;
					gid1 = proj.newGroup(gname, "./groups/" + gname +".assh");
					for(Path p1 : dstr1) {
						String fname = p1.getFileName().toString();
						String aname = fname.substring(0, fname.lastIndexOf('.')) + "_" + no;
						aname = aname.toLowerCase();
						
						a = proj.newAsset(pkg1, gid1, rootpath + "\\scene\\chrprt_ryu\\" + s + "\\" + no + "\\" + fname);
						a.setTypeID(MebTypes._IMG_PNG);
						a.setLocalName(aname + "_ryu");
						bw.write(a.getLocalName() + "\t");
						
						l = new Loc();
						l.isnsa_v = true; l.path_v = "./bmp/tati/" + s + "/" + no + "/" + fname;
						l.isnsa_m = true; l.path_m = "./bmp/tati/" + s + "/" + no + "/" + fname;
						loc_tbl.put(a.getLocalName(), l);
						backmap.put(l.path_m, new AMatch(tbl0, tidx));
						
						a = proj.newAsset(pkg2, gid1, rootpath + "\\scene\\chrprt_alc\\" + s + "\\" + no + "\\" + fname);
						a.setTypeID(MebTypes._IMG_PNG);
						a.setLocalName(aname + "_alc");
						bw.write(a.getLocalName() + "\t");
						
						l = new Loc();
						l.isnsa_v = false; l.path_v = "<NA>";
						l.isnsa_m = false; l.path_m = "./big/bmp/tati/" + s + "/" + no + "/" + fname;
						loc_tbl.put(a.getLocalName(), l);
						backmap.put(l.path_m, new AMatch(tbl0, tidx));
						
						a = proj.newAsset(pkg3, gid1, rootpath + "\\scene\\chrprt_pch\\" + s + "\\" + no + "\\" + fname);
						a.setTypeID(MebTypes._IMG_PNG);
						a.setLocalName(aname + "_pch");
						bw.write(a.getLocalName() + "\n");
						
						l = new Loc();
						l.isnsa_v = true; l.path_v = "./big/bmp/tati/" + s + "/" + no + "/" + fname;
						l.isnsa_m = true; l.path_m = "./big/bmp/tati/" + s + "/" + no + "/" + fname;
						loc_tbl.put(a.getLocalName(), l);
						backmap.put(l.path_m, new AMatch(tbl0, tidx));
						
						strta_map.put(aname, tbl0);
						idx_map.put(aname, tidx++);
					}
					dstr1.close();
				}
			}
			dstr.close();	
		}
		bw.close();
		
		//r click
		//Prep Tables
		for(int i = 0; i < 4; i++) {
			
			int ch = i+1;
			gid1 = proj.newGroup("chrprt_rclick_ch" + ch, "./groups/chrprt_rclick_ch" + ch + ".assh");
			tblpath = rootpath + "\\scene\\chrprt_tbl_rclick_" + ch + ".tsv";
			tidx = 0;
			bw = new BufferedWriter(new FileWriter(tblpath));
			bw.write("RYU\tALC\tPCH\n");
			tbl0 = proj.newAsset(pkg0, gid0, tblpath);
			tbl0.setTypeID(MebTypes._IMT_DEF);
			tbl0.setLocalName("chrprt_rclick_" + ch);
			
			//Go thru alc directory to get file names
			int j = 1;
			String dirstm = rootpath + "\\scene\\chrprt_alc\\r_click\\";
			switch(ch) {
			case 2: j = 2; dirstm += "ep2"; break;
			case 3: j = 2; dirstm += "ep3"; break;
			case 4: j = 3; dirstm += "ep4"; break;
			}
			
			for(int k = 0; k < j; k++) {
				String dirget = dirstm;
				if(k > 0) dirget += "_" + (k+1);
				dstr = Files.newDirectoryStream(Paths.get(dirget));
				String epdir = null;
				if(ch > 1) epdir = "ep" + ch;
				if(k > 0) epdir += "_" + (k+1);
				for(Path p : dstr) {
					if(Files.isDirectory(p)) continue;
					
					//Get file name 
					String fname = p.getFileName().toString();
					String name = fname.substring(0, fname.lastIndexOf('.'));
					
					// Remember to prefix episode number/group for asset names
					String aname_stm = "ep" + ch + "_" + (k+1) + "_" + name;
					String suffix = null;
					if(epdir != null) suffix = epdir + "/" + fname;
					else suffix = fname;
					
					//Generate assets for all three styles (and put in table)
					String apath = rootpath + "\\scene\\chrprt_ryu\\r_click\\";
					if(epdir != null) apath += epdir + "\\";
					apath += fname;
					
					a = proj.newAsset(pkg1, gid1, apath);
					a.setTypeID(MebTypes._IMG_PNG);
					a.setLocalName(aname_stm + "_ryu");
					bw.write(a.getLocalName() + "\t");
					
					//These ryukishi sprites are in png format by some miracle
					l = new Loc();
					l.isnsa_v = true; l.path_v = "./bmp/r_click/cha_tati/" + suffix;
					l.isnsa_m = true; l.path_m = "./bmp/r_click/cha_tati/" + suffix;
					loc_tbl.put(a.getLocalName(), l);
					backmap.put(l.path_m, new AMatch(tbl0, tidx));
					
					apath = rootpath + "\\scene\\chrprt_alc\\r_click\\";
					if(epdir != null) apath += epdir + "\\";
					apath += fname;
					
					a = proj.newAsset(pkg2, gid1, apath);
					a.setTypeID(MebTypes._IMG_PNG);
					a.setLocalName(aname_stm + "_alc");
					bw.write(a.getLocalName() + "\t");
					
					l = new Loc();
					l.isnsa_v = false; l.path_v = "<NA>";
					l.isnsa_m = false; l.path_m = "./big/bmp/r_click/cha_tati/" + suffix;
					loc_tbl.put(a.getLocalName(), l);
					backmap.put(l.path_m, new AMatch(tbl0, tidx));
					
					apath = rootpath + "\\scene\\chrprt_pch\\r_click\\";
					if(epdir != null) apath += epdir + "\\";
					apath += fname;
					
					a = proj.newAsset(pkg3, gid1, apath);
					a.setTypeID(MebTypes._IMG_PNG);
					a.setLocalName(aname_stm + "_pch");
					bw.write(a.getLocalName() + "\n");
					
					l = new Loc();
					l.isnsa_v = true; l.path_v = "./big/bmp/r_click/cha_tati/" + suffix;
					l.isnsa_m = true; l.path_m = "./big/bmp/r_click/cha_tati/" + suffix;
					loc_tbl.put(a.getLocalName(), l);
					backmap.put(l.path_m, new AMatch(tbl0, tidx));
					
					//Save table index
					strta_map.put(aname_stm.toLowerCase(), tbl0);
					idx_map.put(aname_stm.toLowerCase(), tidx++);
				}
				dstr.close();
			}
			bw.close();
		}
		
		//zoom
		//Prep table...
		tidx = 0;
		tblpath = rootpath + "\\scene\\chrprt_tbl_zoom.tsv";
		bw = new BufferedWriter(new FileWriter(tblpath));
		bw.write("RYU\tALC\tPCH\n");
		tbl0 = proj.newAsset(pkg0, gid0, tblpath);
		tbl0.setTypeID(MebTypes._IMT_DEF);
		tbl0.setLocalName("chrprt_zoom_imt");
		
		gid1 = proj.newGroup("chrprt_zoom", "./groups/chrprt_zoom.assh");
		dstr = Files.newDirectoryStream(Paths.get(rootpath + "\\scene\\chrprt_alc\\zoom"));
		for(Path p : dstr) {
			if(Files.isDirectory(p)) continue;
			
			String fname = p.getFileName().toString();
			String name = fname.substring(0, fname.lastIndexOf('.'));
			
			a = proj.newAsset(pkg1, gid1, rootpath + "\\scene\\chrprt_ryu\\zoom\\" + fname);
			a.setTypeID(MebTypes._IMG_PNG);
			a.setLocalName(name + "_ryu");
			bw.write(a.getLocalName() + "\t");
			
			l = new Loc();
			l.isnsa_v = true; l.path_v = "./bmp/zoom/" + fname;
			l.isnsa_m = true; l.path_m = "./bmp/zoom/" + fname;
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(tbl0, tidx));
			
			a = proj.newAsset(pkg2, gid1, rootpath + "\\scene\\chrprt_alc\\zoom\\" + fname);
			a.setTypeID(MebTypes._IMG_PNG);
			a.setLocalName(name + "_alc");
			bw.write(a.getLocalName() + "\t");
			
			l = new Loc();
			l.isnsa_v = false; l.path_v = "<NA>";
			l.isnsa_m = false; l.path_m = "./big/bmp/zoom/" + fname;
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(tbl0, tidx));
			
			a = proj.newAsset(pkg3, gid1, rootpath + "\\scene\\chrprt_pch\\zoom\\" + fname);
			a.setTypeID(MebTypes._IMG_PNG);
			a.setLocalName(name + "_pch");
			bw.write(a.getLocalName() + "\n");
			
			l = new Loc();
			l.isnsa_v = true; l.path_v = "./big/bmp/zoom/" + fname;
			l.isnsa_m = true; l.path_m = "./big/bmp/zoom/" + fname;
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(tbl0, tidx));
			
			strta_map.put(name.toLowerCase(), tbl0);
			idx_map.put(name.toLowerCase(), tidx++);
		}
		dstr.close();
		bw.close();
		
		System.err.println("Reading BGM...");
		//-- BGM
		//These are assigned arbitrary numbers somewhere in the script, so that's fun.
		pkg1 = proj.newPackage("bgm_q", "./sound/bgm_q.assp");
		
		String tbl_stem_name = rootpath + "\\sound\\bgm\\tbl\\bgmtbl_ep";
		for(int i = 1; i <= 4; i++) {
			gid1 = proj.newGroup("bgm_ch" + i, "./groups/bgm" + i + ".assh");
			BufferedReader br = new BufferedReader(new FileReader(tbl_stem_name + i + ".tsv"));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] fields = line.split("\t");
				int id = Integer.parseInt(fields[0].trim());
				String name = fields[1].trim();
				
				a = proj.newAsset(pkg1, gid1, rootpath + "\\sound\\bgm\\" + name + ".ogg");
				a.setTypeID(MebTypes._SND_OGG);
				a.setLocalName("bgm_" + String.format("%04d", id) + "_" + name.replace(' ', '_'));
				
				strta_map.put("bgm" + id, a);
				
				l = new Loc();
				l.isnsa_v = false; l.path_v = "./BGM/" + name + ".ogg";
				l.isnsa_m = false; l.path_m = "./BGM/" + name + ".ogg";
				loc_tbl.put(a.getLocalName(), l);
				backmap.put(l.path_m, new AMatch(a, 0));
			}
			br.close();
		}
		
		//-- ME
		System.err.println("Reading ME...");
		String checkdir = "D:\\usr\\bghos\\code\\mubuilds\\umineko0\\source\\data";
		pkg1 = proj.newPackage("me_q", "./sound/me_q.assp");
		
		tbl_stem_name = rootpath + "\\sound\\me\\tbl\\metbl.tsv";
		gid1 = proj.newGroup("sound_me", "./groups/me.assh");
		BufferedReader br = new BufferedReader(new FileReader(tbl_stem_name));
		String line = null;
		while((line = br.readLine()) != null) {
			if(line.isEmpty()) continue;
			if(line.startsWith(";")) continue;
			
			String[] fields = line.split("\t");
			int id = Integer.parseInt(fields[0].trim());
			String name = fields[1].trim();
			
			String apth = rootpath + "\\sound\\me\\" + name + ".wav";
			if(!FileBuffer.fileExists(apth)) {
				//Might be .WAV
				Files.move(Paths.get(rootpath + "\\sound\\me\\" + name + ".WAV"), Paths.get(apth));
			}
			a = proj.newAsset(pkg1, gid1, apth);
			a.setTypeID(MebTypes._SND_OGG);
			a.setLocalName("me_" + String.format("%04d", id) + "_" + name.replace(' ', '_'));
			
			strta_map.put("me" + id, a);
			
			//Determine where it is in source.
			String dir = "ME";
			if(!FileBuffer.fileExists(checkdir + "\\" + dir + "\\" + name + ".wav")) dir = "sys_se";
			if(!FileBuffer.fileExists(checkdir + "\\" + dir + "\\" + name + ".wav")) dir = "SE";
			
			l = new Loc();
			l.isnsa_v = false; l.path_v = "./" + dir + "/" + name + ".wav";
			l.isnsa_m = false; l.path_m = "./" + dir + "/" + name + ".wav";
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(a, 0));
		}
		br.close();
		
		//-- SE
		System.err.println("Reading SE...");
		pkg1 = proj.newPackage("se_q", "./sound/se_q.assp");
		tbl_stem_name = rootpath + "\\sound\\se\\tbl\\setbl.tsv";
		gid1 = proj.newGroup("sound_se", "./groups/se.assh");
		br = new BufferedReader(new FileReader(tbl_stem_name));
		line = null;
		while((line = br.readLine()) != null) {
			if(line.isEmpty()) continue;
			if(line.startsWith(";")) continue;
			
			String[] fields = line.split("\t");
			int id = Integer.parseInt(fields[0].trim());
			String name = fields[1].trim();
			
			String apth = rootpath + "\\sound\\se\\" + name + ".wav";
			if(!FileBuffer.fileExists(apth)) {
				//Might be .WAV
				Files.move(Paths.get(rootpath + "\\sound\\se\\" + name + ".WAV"), Paths.get(apth));
			}
			a = proj.newAsset(pkg1, gid1, apth);
			a.setTypeID(MebTypes._SND_OGG);
			a.setLocalName("se_" + String.format("%04d", id) + "_" + name.replace(' ', '_'));
			
			strta_map.put("se" + id, a);
			
			String dir = "SE";
			if(!FileBuffer.fileExists(checkdir + "\\" + dir + "\\" + name + ".wav")) dir = "sys_se";
			if(!FileBuffer.fileExists(checkdir + "\\" + dir + "\\" + name + ".wav")) dir = "ME";
			
			l = new Loc();
			l.isnsa_v = false; l.path_v = "./" + dir + "/" + name + ".wav";
			l.isnsa_m = false; l.path_m = "./" + dir + "/" + name + ".wav";
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(a, 0));
		}
		br.close();
		
		//-- Voices
		System.err.println("Reading voices...");
		//Packaged by ep and character, grouped by ep
		int[] gids = new int[6];
		ArrayList<Map<String, Integer>> vpkgs = new ArrayList<Map<String, Integer>>(5);
		for(int i = 0; i < 4; i++) {
			int ch = i+1;
			gids[i] = proj.newGroup("vox_jp_" + ch, "./groups/vox_jp_" + ch + ".assh");
			
			Map<String, Integer> vmap = new HashMap<String, Integer>();
			vpkgs.add(vmap);
			
			for(String chr : chrnames) {
				if(chr.equals("goa")) continue; //Don't think they have voices
				
				String pkgname = "vox_jp_" + chr + "_" + ch;
				pkg1 = proj.newPackage(pkgname, "./sound/vox/" + ch + "/" + pkgname + ".assp");
				vmap.put(chr, pkg1);
				
				//Make target folder...
				//String targdir = rootpath + "\\sound\\vox\\jp\\" + ch + "\\" + chr;
				//if(!FileBuffer.directoryExists(targdir)) Files.createDirectory(Paths.get(targdir));
			}
			
			//Add one for extras
			String pkgname = "vox_jp_extra_" + ch;
			pkg1 = proj.newPackage(pkgname, "./sound/vox/" + ch + "/" + pkgname + ".assp");
			vmap.put("extra", pkg1);
			//String targdir = rootpath + "\\sound\\vox\\jp\\" + ch + "\\extra";
			//if(!FileBuffer.directoryExists(targdir)) Files.createDirectory(Paths.get(targdir));
		}
		gids[4] = proj.newGroup("vox_jp_9", "./groups/vox_jp_9.assh");
		gids[5] = proj.newGroup("vox_jp_misc", "./groups/vox_jp_misc.assh");
		Map<String, Integer> vmap = new HashMap<String, Integer>();
		vpkgs.add(vmap);
		for(String chr : chrnames) {
			if(chr.equals("goa")) continue;
			
			String pkgname = "vox_jp_" + chr + "_9";
			pkg1 = proj.newPackage(pkgname, "./sound/vox/tea/" + pkgname + ".assp");
			vmap.put(chr, pkg1);
			
			String targdir = rootpath + "\\sound\\vox\\jp\\tea\\" + chr;
			if(!FileBuffer.directoryExists(targdir)) Files.createDirectory(Paths.get(targdir));
		}
		int miscpkg = proj.newPackage("vox_jp_misc", "./sound/vox/vox_jp_misc.assp");
		
		//Tables
		int[] tidxs = new int[6];
		String[] tblpaths = new String[6];
		BufferedWriter[] bwarr = new BufferedWriter[6];
		MebAsset[] tblassets = new MebAsset[6];
		
		int aut_pkg = proj.newPackage("aut_common", "./aut_common.assp");
		int aut_gid = proj.newGroup("aut_common", "./groups/aut_common.assh");
		for(int i = 0; i < 4; i++) {
			int ch = i+1;
			tblpaths[i] = rootpath + "\\sound\\vox\\vox_tbl_" + ch +".tsv";
			
			bwarr[i] = new BufferedWriter(new FileWriter(tblpaths[i]));
			bwarr[i].write("JP\n");
			
			tblassets[i] = proj.newAsset(aut_pkg, aut_gid, tblpaths[i]);
			tblassets[i].setTypeID(MebTypes._AUT_DEF);
			tblassets[i].setLocalName("vox_aut_" + ch);
		}
		tblpaths[4] = rootpath + "\\sound\\vox\\vox_tbl_9.tsv";
		bwarr[4] = new BufferedWriter(new FileWriter(tblpath));
		bwarr[4].write("JP\n");
		tblassets[4] = proj.newAsset(aut_pkg, aut_gid, tblpaths[4]);
		tblassets[4].setTypeID(MebTypes._AUT_DEF);
		tblassets[4].setLocalName("vox_aut_9");
		tblpaths[5] = rootpath + "\\sound\\vox\\vox_tbl_misc.tsv";
		bwarr[5] = new BufferedWriter(new FileWriter(tblpath));
		bwarr[5].write("JP\n");
		tblassets[5] = proj.newAsset(aut_pkg, aut_gid, tblpaths[5]);
		tblassets[5].setTypeID(MebTypes._AUT_DEF);
		tblassets[5].setLocalName("vox_aut_misc");
		
		//Look for stralias values with voice\
		String srcroot = "D:\\usr\\bghos\\code\\mubuilds\\umineko0\\source\\data\\";
		for(String k : stralias.keySet()) {
			String val = stralias.get(k);
			if(val.contains("voice\\")) {
				//Get character from first 3 letters
				//Then the ep # is first char after underscore
				//Tea parties appear to be packaged with 9
				//mx lines go into the extras pack for their episode
				if(k.length() < 5) continue;
				if(!k.contains("_")) continue;
				
				String chr = k.substring(0, 3).toLowerCase();
				if(chr.equals("mix")) {
					//Get episode (after second underscore)
					int us = k.indexOf('_', 4);
					char echar = k.charAt(us+1);
					int ep = (int)(echar - '0');
					int epidx = ep-1;
					
					//Figure out where to get this file from.
					String srcpath = srcroot + val;
					
					//Figure out where to put this file.
					String trgpath = rootpath + "\\sound\\vox\\jp\\";
					if(ep < 5) trgpath += ep + "\\";
					else {
						trgpath += "tea\\";
						epidx = 4;
					}
					trgpath += "extra\\" + k + ".ogg";
					
					//Copy file
					if(!FileBuffer.fileExists(srcpath)) {
						System.err.println("WARNING - voice file not found: " + k);
						System.err.println("\t" + srcpath);
						continue;
					}
					if(!FileBuffer.fileExists(trgpath)) Files.copy(Paths.get(srcpath), Paths.get(trgpath));
					
					//Prep asset
					vmap = vpkgs.get(epidx);
					int pkgid = vmap.get("extra");
					a = proj.newAsset(pkgid, gids[epidx], trgpath);
					a.setTypeID(MebTypes._SND_OGG);
					a.setLocalName("vx_jp_" + k);
					
					//Add to table
					bwarr[epidx].write(a.getLocalName() + "\n");
					
					
					//Loc
					l = new Loc();
					l.isnsa_v = false; l.path_v = "<NA>";
					l.isnsa_m = true; l.path_m = "." + val.replace('\\', '/');
					loc_tbl.put(a.getLocalName(), l);
					backmap.put(l.path_m, new AMatch(tblassets[epidx], tidxs[epidx]));
					
					//Map
					strta_map.put(k.toLowerCase(), tblassets[epidx]);
					idx_map.put(k.toLowerCase(), tidxs[epidx]++);
				}
				else {
					//If not chr, put it aside for now
					if(k.charAt(3) != '_')continue;
					
					//Get episode
					char echar = k.charAt(4);
					int ep = (int)(echar - '0');
					int epidx = ep-1;
					
					//Figure out where to get this file from.
					String srcpath = srcroot + val;
					
					//Figure out where to put this file.
					String trgpath = rootpath + "\\sound\\vox\\jp\\";
					if(ep < 5) trgpath += ep + "\\";
					else {
						trgpath += "tea\\";
						epidx = 4;
					}
					trgpath += chr + "\\" + k + ".ogg";
					
					//Copy file
					if(!FileBuffer.fileExists(srcpath)) {
						System.err.println("WARNING - voice file not found: " + k);
						System.err.println("\t" + srcpath);
						continue;
					}
					if(!FileBuffer.fileExists(trgpath)) Files.copy(Paths.get(srcpath), Paths.get(trgpath));
					
					//Prep asset
					vmap = vpkgs.get(epidx);
					int pkgid = vmap.get(chr);
					a = proj.newAsset(pkgid, gids[epidx], trgpath);
					a.setTypeID(MebTypes._SND_OGG);
					a.setLocalName("vx_jp_" + k);
					
					//Add to table
					bwarr[epidx].write(a.getLocalName() + "\n");
					
					l = new Loc();
					l.isnsa_v = false; l.path_v = "<NA>";
					l.isnsa_m = true; l.path_m = "." + val.replace('\\', '/');
					loc_tbl.put(a.getLocalName(), l);
					backmap.put(l.path_m, new AMatch(tblassets[epidx], tidxs[epidx]));
					
					//Map
					strta_map.put(k.toLowerCase(), tblassets[epidx]);
					idx_map.put(k.toLowerCase(), tidxs[epidx]++);
				}
				
			}
		}
		
		//Do extra & misc voices
		dstr = Files.newDirectoryStream(Paths.get(srcroot + "\\voice\\99"));
		for(Path p : dstr) {
			//Guess from file name what episode it belongs to.
			String fname = p.getFileName().toString();
			int ep = 6; //Misc
			int slen = fname.length();
			//System.err.println("fname = " + fname);
			for(int i = 0; i < slen; i++) {
				char c= fname.charAt(i);
				if(Character.isDigit(c)) {
					ep = (int)(c - '0');
					//System.err.println("ep = " + ep);
					//if(ep == 9) ep = 5;
					if(ep < 1 || ep > 4) ep = 6;
					//System.err.println("ep = " + ep);
					break;
				}
			}
			int epidx = ep-1;
			
			//Get package,group
			int grp = gids[5];
			int pkg = miscpkg;
			if(ep < 5) {
				grp = gids[epidx];
				vmap = vpkgs.get(epidx);
				pkg = vmap.get("extra");
			}
			
			String trgpath = rootpath + "\\sound\\vox\\jp\\";
			if(ep < 5) trgpath += ep + "\\extra\\" + fname;
			else trgpath += "\\misc\\" + fname;
			if(!FileBuffer.fileExists(trgpath)) Files.copy(p, Paths.get(trgpath));
			String name = fname.substring(0, fname.lastIndexOf('.'));
			
			a = proj.newAsset(pkg, grp, trgpath);
			a.setTypeID(MebTypes._SND_OGG);
			a.setLocalName("vx_jp_" + name);
			
			//Add to table
			bwarr[epidx].write(a.getLocalName() + "\n");
			
			l = new Loc();
			l.isnsa_v = false; l.path_v = "<NA>";
			l.isnsa_m = true; l.path_m = "./voice/99/" + fname;
			loc_tbl.put(a.getLocalName(), l);
			backmap.put(l.path_m, new AMatch(tblassets[epidx], tidxs[epidx]));
			
			//Map
			strta_map.put(name.toLowerCase(), tblassets[epidx]);
			idx_map.put(name.toLowerCase(), tidxs[epidx]++);
			
		}
		dstr.close();
		
		//Close writers!
		for(int i = 0; i < 6; i++) bwarr[i].close();
		
		//-- UI elements that I'm actually keeping?
		
	}
	
	private static AMatch resolvePathToAsset(String srcpath) {
		//Also tries to figure out from string variable names
		//Yuck.
	
		if(srcpath.isEmpty()) return null;
		char c0 = srcpath.charAt(0);
		int varidx = -1;
		String arg = srcpath;
		switch(c0) {
		case '$': 
			varidx = resolveVarID(srcpath);
			arg = s_var.get(varidx);
			break;
		case '%':
			varidx = resolveVarID(srcpath);
			Integer iarg = i_var.get(varidx);
			if(iarg == null) return null;
			arg = iarg.toString();
			break;
		case '?': return null;
		}
		
		if(arg == null) return null;
		
		//Check for aliases...
		String a = stralias.get(arg);
		if(a != null) arg = a;
		
		//Paths need to be processed ugh.
		int semi = arg.lastIndexOf(';');
		if(semi >= 0) arg = arg.substring(semi+1);
		arg = arg.replace('\\', '/');
		if(arg.charAt(0) != '/') arg = "./" + arg;
		else arg = "." + arg;
		arg = arg.toLowerCase();
		
		//Check backmap...
		System.err.println("Asset resolution attempt - lookup path: " + arg);
		AMatch match = backmap.get(arg);
		if(match != null) {
			System.err.println("Asset Match Success: " + match.asset.getLocalName());
		}
		else System.err.println("Failed...");
		
		return match;
	}
	
	private static String processTextline(String in) {
		// also need to modify command splitters to keep text lines together?
		//Needs to handle the formatting stuff too!
		
		//Assumes that the ^ characters have been removed beforehand.
		//Assumes that there is no ending \ or @ (which should be handled by processCommand())
		int slen = in.length();
		StringBuilder sb = new StringBuilder(slen << 1);
		int i = 0;
		//System.err.println("in = " + in);
		while(i < slen) {
			char c = in.charAt(i);
			char d;
			
			switch(c) {
			case '#':
				//Either a color or literal escape. Check next character
				if(i+1 >= slen) {
					System.err.println("Warning: # char at end of line. Will be removed...");
					i++;
				}
				else{
					d = in.charAt(i+1);
					if(Character.isAlphabetic(d) || Character.isDigit(d)) {
						//Assumed color.
						sb.append("\\c<#");
						for(int j = 0; j < 6; j++) sb.append(in.charAt(i+1+j));
						sb.append(">");
						i+=7;
					}
					else {
						sb.append(d);
						i+=2;
					}
				}
				break;
			case '^':
				//Other control characters in this group should be handled by outer parser.
				System.err.println("Warning: Found ^ ctrl character when not expected."); i++;
				break;
			case '_':
				//Eat. 
				 i++; break;
			case '@':
				System.err.println("Warning: Found @ ctrl character when not expected.");
				 i++; break;
			case '/':
				System.err.println("Warning: Found / ctrl character when not expected.");
				 i++; break;
			case '\\':
				System.err.println("Warning: Found \\ ctrl character when not expected.");
				 i++; break;
			case '!':
				//Speed/wait command or ! literal. Need to process next few characters.
				if(i+1 >= slen) {
					 i++; sb.append(c);
				}
				else {
					d = in.charAt(i+1);
					//System.err.println("d = " + d + ", i = " + i);
					//System.err.println("in = " + in);
					if(d == 'w' || d == 'd' || d == 's') {
						StringBuilder param = new StringBuilder(64);
						int j = i+2;
						while(j < slen) {
							char e = in.charAt(j);
							if(!Character.isDigit(e)) {
								if(e == 'd') param.append(e);
								break;
							}
							param.append(e);
							j++;
						}
						i = j;
						int iparam = -1;
						try {iparam = Integer.parseInt(param.toString());}
						catch(NumberFormatException ex) {
							//Only works for !sd
							String pstr = param.toString();
							if(!pstr.equals("d") || d != 's') {
								System.err.println("Warning: command !" + d + " does not accept value of " + pstr + " command will be skipped.");
							}
							else {sb.append("\\S"); i++;}
							break;
						}
						
						switch(d) {
						case 'w': 
						case 'd': 
							sb.append("\\W<");
							sb.append(Integer.toString(iparam));
							sb.append(">");
							break;
						case 's': 
							sb.append("\\S<");
							sb.append(Integer.toString(iparam));
							sb.append(">");
							break;
						}
					}
					else {sb.append(c); i++;}
				}
				break;
			case '{':
				//Variable sub. If can find one.
				if(i+1 >= slen) {
					System.err.println("Warning: { char at end of line. Will be removed...");
					i++;
				}
				else {
					d = in.charAt(i+1);
					StringBuilder var = new StringBuilder(64);
					int j = i+1;
					switch(d) {
					case'%':
					case'$':
					case'?':
						//Read phrase until the closing brace
						while(j < slen) {
							char e = in.charAt(j++);
							if(e == '}') break;
							var.append(e);
						}
						i = j;
						int vidx = resolveVarID(var.toString());
						if(vidx < 0) System.err.println("Warning: Variable " + var.toString() + " not recognized.");
						if(d == '%') {
							Integer v = i_var.get(vidx);
							if(v == null) v = 0;
							sb.append(v);
						}
						else if(d == '$') {
							String s = s_var.get(vidx);
							if(s == null) s = "";
							sb.append(s);
						}
						break;
					default:
						System.err.println("Warning: Invalid character following {. Will interpret as literal.");
						sb.append(c);
						i++;
						break;
					}
				}
				break;
			case '~':
				//Unless literal, process as formatting block.
				if(i+1 >= slen) {
					System.err.println("Warning: ~ char at end of line. Will be removed...");
					i++;
				}
				else {
					d = in.charAt(i+1);
					if(d == '~') {
						sb.append('~');
						i+=2;
					}
					else {
						int j = i+1;
						while(j < slen) {
							char e = in.charAt(j++);
							int k = j;
							char f;
							int n;
							boolean b;
							StringBuilder param = new StringBuilder(64);
							if(e == '~') break; //End of block.
							if(Character.isWhitespace(e)) continue;
							switch(e) {
							case 'c': 
								param.append(in.charAt(j++));
								sb.append("\\f<fnt_face");
								sb.append(param.toString());
								sb.append(">");
								break;
							case 'd': 
								sb.append("\\f");
								break;
							case 'r': 
								//Disable italics. Skip for now
								break;
							case 'i': 
								sb.append("\\i");
								break;
							case 't': 
								//Disable bold. Skip for now
								break;
							case 'b': 
								sb.append("\\b");
								break;
							case 'f': 
								sb.append("\\f");
								break;
							case 's': 
								//Set to display case. Ignore for now.
								break;
							case '=': 
								//Set font size.
								//Read until no more numbers
								while(k < slen) {
									f = in.charAt(k++);
									if(!Character.isDigit(f)) break;
									param.append(f);
								}
								j = k-1;
								n = Integer.parseInt(param.toString());
								sb.append("\\s<");
								sb.append(Integer.toString(n));
								sb.append(">");
								st_fnt_sz = n;
								break;
							case '%': 
								while(k < slen) {
									f = in.charAt(k++);
									if(!Character.isDigit(f)) break;
									param.append(f);
								}
								j = k-1;
								n = Integer.parseInt(param.toString());
								double perc = (double)n/100.0;
								n = (int)Math.round(perc * (double)st_fnt_sz);
								
								sb.append("\\s<");
								sb.append(Integer.toString(n));
								sb.append(">");
								st_fnt_sz = n;
								break;
							case '+': 
								while(k < slen) {
									f = in.charAt(k++);
									if(!Character.isDigit(f)) break;
									param.append(f);
								}
								j = k-1;
								n = Integer.parseInt(param.toString());
								sb.append("\\s<");
								sb.append(Integer.toString(st_fnt_sz+n));
								sb.append(">");
								st_fnt_sz += n;
								break;
							case '-': 
								while(k < slen) {
									f = in.charAt(k++);
									if(!Character.isDigit(f)) break;
									param.append(f);
								}
								j = k-1;
								n = Integer.parseInt(param.toString());
								sb.append("\\s<");
								sb.append(Integer.toString(st_fnt_sz-n));
								sb.append(">");
								st_fnt_sz -= n;
								break;
							case 'x': 
								//Parse value out of the way, but otherwise ignore for now.
								b = true;
								while(k < slen) {
									f = in.charAt(k++);
									if(b && (f == '+' || f == '-')) {
										b = false;
										continue;
									}
									if(!Character.isDigit(f)) break;
									param.append(f);
									b = false;
								}
								j = k-1;
								break;
							case 'y': 
								//Parse value out of the way, but otherwise ignore for now.
								b = true;
								while(k < slen) {
									f = in.charAt(k++);
									if(b && (f == '+' || f == '-')) {
										b = false;
										continue;
									}
									if(!Character.isDigit(f)) break;
									param.append(f);
									b = false;
								}
								j = k-1;
								break;
							case 'n': 
								//Set x indent to current x for this page. Ignore for now.
								break;
							case 'u': 
								//Reset x indent. Ignore for now.
								break;
							default:
								System.err.println("Warning: \'" + e + "\' not recognized as format block command");
								break;
							}
						}
						i = j;
					}
				}
				break;
			case '\n': sb.append("\\n"); i++; break;
			default: sb.append(c); i++; break;
			}
			
		}
		
		
		return sb.toString();
	}
	
	private static AMatch resolveImage(String arg) {
		MebAsset a = strta_map.get(arg);
		if(a == null) {
			//Assumed to be raw path
			AMatch ma = resolvePathToAsset(arg);
			if(ma == null || ma.asset == null) {return null;}
			else{return ma;}
		}
		else {
			if(a.isTable()) {
				Integer idx = idx_map.get(arg);
				AMatch ma = new AMatch(a, idx);
				return ma;
			}
			else {return new AMatch(a,-1);}
		}
	}
	
	private static String resolveEffect(String arg) {
		String a2 = arg.toLowerCase();
		int e = -1;
		try {e = Integer.parseInt(a2);}
		catch(NumberFormatException ex) {
			//System.err.println("ERR -- command \"" + fullcmd + "\" -- effect parameter could not be resolved!");
			return null;
		}
		StringBuilder sb = new StringBuilder(128);
		
		String lookup = "efe_" + String.format("%02d", e);
		MebAsset a = strta_map.get(lookup);
		if(a != null) {
			Integer idx = idx_map.get(lookup);
			sb.append("transition=FROMTBL,");
			sb.append(a.getLocalName());
			sb.append("," + idx);
		}
		else {
			//0 and 1 are common. Not sure what they mean, but let's put something in
			if(e == 0) {
				//Instant?
			}
			else if(e == 1) {
				sb.append("transition=CROSSFADE,250");
			}
			else return null;
			//else System.err.println("Warning -- command \"" + fullcmd + "\" -- effect parameter could not be resolved!");
		}
		
		return sb.toString();
	}
	
	private static int resolveIntArg(String arg) {
		
		if(arg.startsWith("%")) {
			int varidx = resolveVarID(arg);
			Integer val = i_var.get(varidx);
			if(val != null) return val;
		}
		else {
			try {return Integer.parseInt(arg);}
			catch(NumberFormatException ex) {
				//Try numalias.
				Integer numa = numalias.get(arg);
				if(numa != null) {
					return numa;
				}
			}
		}
		
		throw new NumberFormatException();
	}
	
	private static String processCommand(String cmd) throws IOException {
		//TODO
		//TODO will also need the command splitter to untangle the strings from the voice lines...
		//	seems each string is NOT prefaced individually with a lang**, so need to be processed differently
		
		//Split into arguments.
		String fullcmd = cmd;
		String[] args = splitCommand(cmd);
		
		cmd = args[0].toLowerCase();
		if(cmd.equals("advchar")) {
			//advchar "CHARNUM"
			//Pretty sure it sets textbox char name. Think these are reverse numaliases for some reason.
			//The charname table will probably be a 3D table I think - set by language, full name on/off, name order
			//Either way, all the sce script needs is the entry index which'll just be the advchar alias
			StringBuilder sb = new StringBuilder(512);
			sb.append("CLEAR_TXB " + TXB_ASSET_NAME + " ALL\n"); //If ur switching character, why keep text?
			
			//Try to parse the argument.
			int idx = -1;
			if(args.length > 1) {
				//I'll be nice and just let it default to -1
				String targ = args[1];
				targ = targ.replace("\"", ""); //Remove quotes, if present
				try {idx = Integer.parseInt(targ);}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" requires integer arg!");
					return null;
				}
			}
			//Show/hide
			
			if(idx < 0) {
				//Hide name module
				namebox_visible = false;
				sb.append("SET_TXB_VIS " + TXB_ASSET_NAME + " " + TXB_MOD0_NAME + " false");
			}
			else {
				//Set text
				if(!namebox_visible) {
					sb.append("SET_TXB_VIS " + TXB_ASSET_NAME + " " + TXB_MOD0_NAME + " true\n");
					namebox_visible = true;
				}
				sb.append("SET_TEXT ");
				sb.append(TXB_ASSET_NAME + " ");
				sb.append(TXB_MOD0_NAME + " ");
				sb.append(STT_CHARNAME_TBL_NAME + " " + idx);
				
				if(idx < adv_idxs.length) sb.append("  //" + adv_idxs[idx]);
				else System.err.println("adv index not recognized: " + idx);
			}
			
			return sb.toString();
		}
		else if(cmd.equals("adv_mode_off") || cmd.equals("adv_mode_on")) {
			//Just copy as a comment.
			return "//" + cmd;
		}
		else if(cmd.equals("bg")) {
			//This looks like a draw background, but not sure what second arg is. Effect maybe? Yes, I think that's it.
			//Draw background arg1 with effect arg2
			//"black" and "white" as arg1 appear to be keywords just meaning blank to black or white
			
			if(args.length < 2) {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
				return null;
			}
			
			StringBuilder sb = new StringBuilder(512);
			
			String a1 = args[1].toLowerCase();
			String comment = null;
			if(a1.equals("black")) sb.append("FILL_2D #000000");
			else if(a1.equals("white")) sb.append("FILL_2D #ffffff");
			else {
				//Find the matching table entry...
				//idxmap should have the table index mapped to stralias in all lower case
				//This may also be an efe, cg, direct file reference, etc. Don't forget to check for those cases.
				sb.append("DRAW_2D ");
				MebAsset a = strta_map.get(a1);
				if(a == null) {
					//Assumed to be raw path
					AMatch ma = resolvePathToAsset(a1);
					if(ma == null || ma.asset == null) {
						//System.err.println("ERR -- command \"" + fullcmd + "\" -- source parameter could not be resolved!");
						//return null;
						sb.append("ASSETNOTFOUND__\"" + args[1] + "\"");
					}
					else{
						sb.append(ma.asset.getLocalName());
						if(ma.asset.isTable()) sb.append(" " + ma.idx);}
				}
				else {
					sb.append(a.getLocalName());
					if(a.isTable()) {
						Integer idx = idx_map.get(a1);
						sb.append(" " + idx);
						comment = a1;
					}
				}
			}
			
			//Layer param...
			sb.append(" LYR_BKG pos=0,0");
			
			//Now transition param, if present...
			if(args.length > 2) {
				String a2 = args[2].toLowerCase();
				int e = -1;
				try {e = Integer.parseInt(a2);}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- effect parameter could not be resolved!");
					return null;
				}
				// check for third param!
				if(args.length > 3) {
					//"umineko_pons" + String.format("%02d", eff.no);
					int eff = Integer.parseInt(args[2]);
					int millis = Integer.parseInt(args[3]);
					sb.append(" transition=CALLBACK,umineko_pons" + String.format("%02d", eff) + "," + millis);
				}
				else {
					String lookup = "efe_" + String.format("%02d", e);
					MebAsset a = strta_map.get(lookup);
					if(a != null) {
						Integer idx = idx_map.get(lookup);
						sb.append(" transition=FROMTBL,");
						sb.append(a.getLocalName());
						sb.append("," + idx);
					}
					else {
						//0 and 1 are common. Not sure what they mean, but let's put something in
						if(e == 0) {
							//Instant?
						}
						else if(e == 1) {
							sb.append(" transition=CROSSFADE,250");
						}
						else System.err.println("Warning -- command \"" + fullcmd + "\" -- effect parameter could not be resolved!");
					}	
				}
			}
			
			if(comment != null) sb.append("  //" + comment);
				
			return sb.toString();
		}
		else if(cmd.equals("bgmplay") || cmd.equals("bgmplay2") || cmd.equals("bgm1") || cmd.equals("bgm1v")) {
			// Play BGM at table index (need to import BGM table)
			//bgmplay idx
			//bgmplay idx vol

			if(args.length < 2){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			
			int idx = -1;
			try {
				idx = Integer.parseInt(args[1]);
			}
			catch(NumberFormatException ex) {
				System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
				return null;
			}
			
			//Match asset.
			MebAsset a = strta_map.get("bgm" + idx);
			if(a == null) {
				System.err.println("ERR -- command \"" + fullcmd + "\" -- source could not be resolved");
				return null;
			}
			StringBuilder sb = new StringBuilder(512);
			sb.append("PLAY_SOUND " + a.getLocalName() + " BGM_CH");
			
			if(args.length > 2) {
				//Vol
				try {
					int vol = Integer.parseInt(args[2]);
					sb.append(" vol="+vol);
				}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
					return null;
				}
			}
			
			return sb.toString();
			
		}
		else if(cmd.equals("bgmstop")) {
			//
			return "STOP_SOUND BGM_CH";
		}
		else if(cmd.equals("bgmvol")) {
			//
			
			if(args.length < 2) {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
				return null;
			}
			
			if(in_for && args[1].startsWith("%")) {
				//Eh just do for value
				return "SET_VOL BGM_CH $FORVAL";
			}
			else {
				int ch = resolveIntArg(args[1]);
				return "SET_VOL BGM_CH " + ch;
			}
		}
		else if(cmd.equals("br")) {
			//I think it's a textbox break.
			return "TXB_NEWLINE";
		}
		else if(cmd.equals("chvol")) {
			//Channel volume
			if(args.length < 3) {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
				return null;
			}
			
			int ch = resolveIntArg(args[1].trim());
			int vol = resolveIntArg(args[2].trim());
			
			return "SET_VOL " + ch + " " + vol;
		}
		else if(cmd.equals("cl")) {
			//Clears sprite from one or all of the char sprite slots.
			if(args.length < 2) {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
				return null;
			}
			
			//First arg is l,c,r, or a
			//Second arg is transition effect
			StringBuilder sb = new StringBuilder(512);
			String effarg = null;
			if(args.length == 3) {
				String a2 = args[2].toLowerCase();
				int e = -1;
				try {e = Integer.parseInt(a2);}
				catch(NumberFormatException ex) {
					System.err.println("Warning -- command \"" + fullcmd + "\" -- effect parameter could not be resolved!");
				}
				
				String lookup = "efe_" + String.format("%02d", e);
				MebAsset a = strta_map.get(lookup);
				if(a == null) {
					if(e == 0) {
						//Instant?
					}
					else if(e == 1) {
						sb.append(" transition=CROSSFADE,250");
					}
					else System.err.println("Warning -- command \"" + fullcmd + "\" -- effect parameter could not be resolved!");
				}
				else {
					Integer idx = idx_map.get(lookup);
					effarg = "transition=FROMTBL," + a.getLocalName() + "," + idx;	
				}
			}
			else if(args.length > 3) {
				String ename = args[args.length-1];
				ename = ename.replace("\"", "").trim();
				ename = ename.replace(".dll", "");
				String[] fields = ename.split("/");
				ename = "umineko_pons";
				for(int i = 0; i < fields.length; i++) {
					ename += "_" + fields[i];
				}
				
				effarg = "";
				effarg += " transition=CALLBACK," + ename;
				for(int i = 2; i < args.length-1; i++) {
					effarg += "," + args[i];
				}
			}
			
			if(args[1].length() != 1) {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid slot argument!");
				return null;
			}
			char c0 = args[1].charAt(0);
			switch(c0) {
			case 'l':
				sb.append("CLEAR_LAYER LYR_CHRPRT_LEFT");
				char_in_left = false;
				break;
			case 'c':
				sb.append("CLEAR_LAYER LYR_CHRPRT_CENTER");
				char_in_center = false;
				break;
			case 'r':
				sb.append("CLEAR_LAYER LYR_CHRPRT_RIGHT");
				char_in_right = false;
				break;
			case 'a':
				sb.append("CLEAR_LAYER LYR_CHRPRT_ALL");
				char_in_left = false;
				char_in_center = false;
				char_in_right = false;
				break;
			default:
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid slot argument!");
				return null;
			}
			
			if(effarg != null) sb.append(" " + effarg);	
			return sb.toString();
		}
		else if(cmd.equals("click")) {
			return "WAIT_FOR USER_CONT";
		}
		else if(cmd.equals("clr_oldnew_bg")) {
			return "CLEAR_LAYER LYR_CG";
		}
		else if(cmd.equals("cross1")) {
			//Takes six args, fades out BGM and ME1-5 I think.
			StringBuilder sb = new StringBuilder(1024);
			
			//Parse the first two args
			int a1 = resolveIntArg(args[1].trim());
			int a2 = resolveIntArg(args[2].trim());
			
			sb.append("FADEOUT_CH BGM_CH " + a1);
			
			//And the five ME channels...
			for(int i = 0; i < 5; i++) {
				int mech = i+1;
				sb.append("\nSET_VOL ME_CH_" + mech + " " + a2 + " " + a1);
			}
			
			return sb.toString();
		}
		else if(cmd.equals("cross2")) {
			StringBuilder sb = new StringBuilder(1024);
			
			//Parse the first two args
			int a1 = resolveIntArg(args[1].trim());
			int a2 = resolveIntArg(args[2].trim());
			
			sb.append("nSET_VOL BGM_CH " + a2 + " " + a1);
			return sb.toString();
		}
		else if(cmd.equals("csp")) {
			//Clear sprite with handle
			if(args[1].equals("-1")) return "CLEAR_LAYER LYR_SPR_ALL";
			return "CLEAR_LAYER LYR_SPR_" + args[1];
		}
		else if(cmd.equals("csp_var")) {
			//Clear sprite with handle
			return "CLEAR_LAYER LYR_SPR_" + args[1];
		}
		else if(cmd.equals("delay")) {
			
			if(args.length < 2) {
				System.err.println("ERR -- command \"" + fullcmd + "\" insufficient args!");
				return null;
			}
			
			int millis = 0;
			try {millis = Integer.parseInt(args[1]);}
			catch(NumberFormatException ex) {
				System.err.println("ERR -- command \"" + fullcmd + "\" arg must be integer");
				return null;
			}
			
			return "DELAY " + millis;
		}
		else if(cmd.equals("dllefe")) {
			StringBuilder sb = new StringBuilder(512);
			sb.append("CALL_METHOD umineko_pons_dllefe");
			int acount = args.length;
			if(acount > 1) {
				for(int i = 1; i < acount; i++) sb.append(" " + args[i]);
			}
			
			return sb.toString();
		}
		else if(cmd.equals("dllefe_off")) {
			return "CALL_METHOD umineko_pons_dllefe_off";
		}
		else if(cmd.equals("dwave_eng")) {
			//Maybe just do nothing here since no English voices anyway. Don't want to dup all the JP files
		}
		else if(cmd.equals("dwave_jp")) {
			//Play vox file if language set to Japanese (since Eng has same voices, may only use this command)
			//I'm actually going to ignore the first param (which seems to be voice len) and only read second which is the file path alias
			//Don't forget command to cut any voices currently playing.
			
			if(args.length < 3) {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
				return null;
			}
			
			StringBuilder sb = new StringBuilder(512);
			//sb.append("STOP_SOUND VOX_CH\n");
			sb.append("PLAY_SOUND ");
			
			String a1 = args[2].toLowerCase();
			MebAsset a = strta_map.get(a1);
			if(a == null) {
				//Resolve from path.
				AMatch m = resolvePathToAsset(a1);
				
				if(m == null || m.asset == null) {
					//I'll just make it an unresolved link for now. Not sure why some of them are missing but still used in script.
					System.err.println("Warning -- command \"" + fullcmd + "\" source could not be resolved!");
					sb.append(" <ASSETUNRESOLVED_" + a1 + "> VOX_CH");
					
					return sb.toString();
				}
				a = m.asset;
				sb.append(a.getLocalName() + " ");
				sb.append(m.idx + " ");	
			}
			else {
				sb.append(a.getLocalName() + " ");
				
				if(a.isTable()) {
					Integer idx = idx_map.get(a1);
					if(idx == null) {
						System.err.println("ERR -- command \"" + fullcmd + "\" source could not be resolved!");
						return null;
					}
					sb.append(idx + " ");
				}	
			}
			
			sb.append("VOX_CH");
			return sb.toString();
		}
		else if(cmd.equals("dwaveloop")) {
			//dwaveloop ch file
			if(args.length < 3) {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
				return null;
			}
			
			int ch = resolveIntArg(args[1]);
			AMatch am = resolveImage(args[2]);
			if(am == null) {
				System.err.println("ERR -- command \"" + fullcmd + "\" asset couldn't be resolved!");
				return null;
			}
			
			StringBuilder sb = new StringBuilder(512);
			sb.append("PLAY_SOUND " + am.asset.getLocalName());
			if(am.asset.isTable()) sb.append(" " + am.idx);
			sb.append(" ANON_CH_" + ch);
			sb.append(" loop=true");
			
			return sb.toString();
		}
		else if(cmd.equals("dwavestop")) {
			//dwaveloop ch file
			if(args.length < 2) {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
				return null;
			}
			
			int ch = resolveIntArg(args[1]);
			return "STOP_SOUND ANON_CH_" + ch;
		}
		else if(cmd.equals("e_a")) {
			//I think this clears the playing sound and resets the sound state to default
			return "RESET_SOUND";
		}
		else if(cmd.equals("e_b") || cmd.equals("e_b1")) {
			return "STOP_SOUND BGM_CH";
		}
		else if(cmd.equals("e_ma")) {
			//Stops just ME tracks, I believe.
			return "STOP_SOUND ME_CH_ALL";
		}
		else if(cmd.equals("e_m1")) {
			return "STOP_SOUND ME_CH_1";
		}
		else if(cmd.equals("e_m2")) {
			return "STOP_SOUND ME_CH_2";
		}
		else if(cmd.equals("e_m3")) {
			return "STOP_SOUND ME_CH_3";
		}
		else if(cmd.equals("eye1") || cmd.equals("eye2") || cmd.equals("eye3")) {
			//Resolve parameters
			//It wants 9
			
			if(args.length < 10) {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
				return null;
			}
			
			StringBuilder sb = new StringBuilder(512);
			sb.append("CALL_METHOD ");
			sb.append(cmd + " ");
			for(int i = 1; i < 10; i++) {
				String sarg = args[i];
				int arg = 0;
				if(sarg.startsWith("%")) {
					int vidx = resolveVarID(sarg);
					Integer val = i_var.get(vidx);
					if(val == null) {
						System.err.println("ERR -- command \"" + fullcmd + "\" invalid args! Coundn't resolve \"" + sarg + "\"");
						return null;
					}
					if(i > 1) sb.append(",");
					sb.append(val);
				}
				else {
					try {arg = Integer.parseInt(sarg);}
					catch(NumberFormatException ex) {
						System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
						return null;
					}
					if(i > 1) sb.append(",");
					sb.append(arg);
				}
			}
			
			return sb.toString();
		}
		else if(cmd.equals("eye12")) {
			return "CALL_METHOD eye12";
		}
		else if(cmd.equals("fede")) {
			//Looks like a audio fadeout custom to this script
			//First param determines channel:
			/*
			 * 0,16 = All sounds
			 * 1 = BGM
			 * 10 = All ME
			 * 11-15 = ME1-5
			 */
			//I think second param is rate - probably millis to full atten?
			
			if(args.length < 3) {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
				return null;
			}
			
			StringBuilder sb = new StringBuilder(512);
			if(args[1].equals("0") || args[1].equals("16")) {
				sb.append("FADEOUT_ALL ");
				sb.append(args[2]);
			}
			else if (args[1].equals("1")) {
				sb.append("FADEOUT_CH ");
				sb.append("BGM_CH_ALL ");
				sb.append(args[2]);
			}
			else if (args[1].equals("10")) {
				sb.append("FADEOUT_CH ");
				sb.append("ME_CH_ALL ");
				sb.append(args[2]);
			}
			else if(args[1].startsWith("1")) {
				sb.append("FADEOUT_CH ");
				sb.append("ME_CH_");
				sb.append(args[1].charAt(1) + " ");
				sb.append(args[2]);
			}
			else {
				System.err.println("ERR -- command \"" + fullcmd + "\" invalid args!");
				return null;
			}
			
			return sb.toString();
		}
		else if(cmd.equals("inc")) {
			int vidx = resolveVarID(args[1]);
			Integer val = i_var.get(vidx);
			if(val == null) val = 1;
			else val++;
			i_var.put(vidx, val);
			System.err.println(args[1] + " = " + val);
			return "";
		}
		else if(cmd.equals("langen")) {
			//Print this string/play the following voice lines if language in English
			//Strings delimited by ^ and ^\ or ^@
			//Find the ^ chars and process everything between them.
			System.err.println("If you are seeing this, something is wrong. Probably not skipping lines correctly.");
			throw new IllegalArgumentException();
			
		}
		else if(cmd.equals("langjp")) {
			//Print this string/play the following voice lines if language in Japanese
			//Lines end in \ or @
			//Okay, looks like @ makes ponscripter wait for a click then writes next line to the same textbox, 
			// while \ waits for a click then starts a new textbox
			//This one isn't nice enough to give us a termination character. So I guess either rely on line end or one of the control characters?
			
			System.err.println("If you are seeing this, something is wrong. Probably not skipping lines correctly.");
			throw new IllegalArgumentException();
		}
		else if(cmd.equals("ld")) {
			//ld (pos)(src)(effect)
			
			if(args.length < 3){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			
			//Use variables for the positions which my sce compiler can calculate from the size of the image.
			
			StringBuilder out = new StringBuilder(1024);
			StringBuilder sb = new StringBuilder(512);
			sb.append("DRAW_2D ");
			String comment = null;
			
			//Asset
			//I wasn't having luck finding the variable assignment defs of these at least in the script.
			//So try using the alias itself as a lookup :D
			//If not a char portrait, probably an effect or something so not exactly sure what to do.
			//Probably another string variable? I'll try pushing it onto the path res method.
			String srcname = args[2];
			String alias = srcname.toLowerCase();
			if(alias.startsWith("$")) alias = alias.substring(1);
			if(alias.length() >= 4 && alias.charAt(3) == '_') {
				//Try as a character portrait
				String chr = alias.substring(0, 3);
				//Get outfit.
				Integer outfit = char_outfit_map.get(chr);
				if(outfit == null) outfit = 1;
				String lookup = alias + "_" + outfit;
				MebAsset a = strta_map.get(lookup);
				if(a == null) {
					//Jess's school uniform outfit is denoted by aliases ending in _U instead of switched with isyou2 for some reason :|
					//Try removing a _U and trying jes outfit two
					if(alias.endsWith("_U")) {
						lookup = alias.substring(0, alias.length()-2) + "_2";
						a = strta_map.get(lookup);
						if(a == null) {
							System.err.println("ERR -- command \"" + fullcmd + "\" -- source argument could not be resolved!");
							return null;
						}
					}
					else {
						System.err.println("ERR -- command \"" + fullcmd + "\" -- source argument could not be resolved!");
						return null;	
					}
				}
				
				sb.append(a.getLocalName() + " ");
				if(a.isTable()) {
					Integer idx = idx_map.get(lookup);	
					if(idx == null) {
						System.err.println("ERR -- command \"" + fullcmd + "\" -- source argument could not be resolved!");
						return null;
					}
					sb.append(idx + " ");
				}
				comment = "  //" + alias;
				
			}
			else {
				AMatch match = resolvePathToAsset(srcname);
				if(match == null) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- source argument could not be resolved!");
					return null;
				}
				sb.append(match.asset.getLocalName() + " " + match.idx);
			}
			sb.append(" LYR_CHRPRT "); //Variable, assigned by sce compiler
			
			//Position
			//Looks like the engine only handles l,c, and r? Maybe?
			//I don't see what else it does with that variable.
			args[1] = args[1].toLowerCase();
			if(args[1].equals("l")) {
				if(char_in_left) {
					//Clear and overwrite
					out.append("CLEAR_LAYER LYR_CHRPRT_LEFT\n");
				}
				sb.append("pos=CHRPRT_LEFT");
				char_in_left = true;
			}
			else if(args[1].equals("c")) {
				if(char_in_center) {
					out.append("CLEAR_LAYER LYR_CHRPRT_CENTER\n");
				}
				sb.append("pos=CHRPRT_CENTER");
				char_in_center = true;
			}
			else if(args[1].equals("r")) {
				if(char_in_right) {
					out.append("CLEAR_LAYER LYR_CHRPRT_RIGHT\n");
				}
				sb.append("pos=CHRPRT_RIGHT");
				char_in_right = true;
			}
			else {
				//I won't force a return, but I'll put a warning and set the location 0,0
				System.err.println("ERR -- command \"" + fullcmd + "\" -- invalid position - setting to 0,0");
				sb.append("pos=0,0");
			}
			
			//Transition (if applicable)
			if(args.length > 3) {
				String a3 = args[3].toLowerCase();
				int e = -1;
				try {e = Integer.parseInt(a3);}
				catch(NumberFormatException ex) {
					System.err.println("Warning -- command \"" + fullcmd + "\" -- effect parameter could not be resolved!");
				}
				
				String lookup = "efe_" + String.format("%02d", e);
				MebAsset a = strta_map.get(lookup);
				if(a == null) {
					if(e == 0) {
						//Instant?
					}
					else if(e == 1) {
						sb.append(" transition=CROSSFADE,250");
					}
					else System.err.println("Warning -- command \"" + fullcmd + "\" -- effect parameter could not be resolved!");
				}
				else{
					Integer idx = idx_map.get(lookup);
					sb.append(" transition=FROMTBL,");
					sb.append(a.getLocalName());
					sb.append("," + idx);
				}
			}
			
			if(comment != null) sb.append(comment);
			out.append(sb.toString());
			//System.err.println(comment);
			return out.toString();
		}
		else if(cmd.equals("lsp")) {
			//handle, image, x, y, (alpha)
			
			if(args.length < 3){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			String handle = args[1].trim();
			StringBuilder sb = new StringBuilder(512);
			
			//If there's a fourth arg, set layer alpha first.
			if(args.length > 4) {
				int alpha = resolveIntArg(args[4]);
				sb.append("SET_OPACITY LYR_SPR_" + handle + " " + alpha + "\n");
			}

			sb.append("DRAW_2D ");
			String comment = null;
			
			String srcname = args[2];
			String alias = srcname.toLowerCase();
			if(alias.startsWith("$")) alias = alias.substring(1);
			if(alias.length() >= 4 && alias.charAt(3) == '_') {
				//Try as a character portrait
				String chr = alias.substring(0, 3);
				//Get outfit.
				Integer outfit = char_outfit_map.get(chr);
				if(outfit == null) outfit = 1;
				String lookup = alias + "_" + outfit;
				MebAsset a = strta_map.get(lookup);
				if(a == null) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- source argument could not be resolved!");
					return null;
				}
				
				sb.append(a.getLocalName() + " ");
				if(a.isTable()) {
					Integer idx = idx_map.get(lookup);	
					if(idx == null) {
						System.err.println("ERR -- command \"" + fullcmd + "\" -- source argument could not be resolved!");
						return null;
					}
					sb.append(idx + " ");
				}
				comment = "  //" + alias;
				
			}
			else {
				AMatch match = resolvePathToAsset(srcname);
				if(match == null) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- source argument could not be resolved!");
					return null;
				}
				sb.append(match.asset.getLocalName() + " " + match.idx);
			}
			sb.append(" LYR_SPR_" + handle + " ");
			
			
			int x = 0; int y = 0;
			x = Integer.parseInt(args[3]);
			y = Integer.parseInt(args[4]);
			
			sb.append("pos=" + x + "," + y);
			if(comment != null) sb.append(comment);
			return sb.toString();	
		}
		else if(cmd.equals("lsp_var")) {
			//Looks like another sprite drawing method.
			//no, img, x, y
			
			if(args.length < 5){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			
			//Not sure what the first param means.
			//StringBuilder out = new StringBuilder(1024);
			StringBuilder sb = new StringBuilder(512);
			sb.append("DRAW_2D ");
			String comment = null;
			
			String srcname = args[2];
			String alias = srcname.toLowerCase();
			if(alias.startsWith("$")) alias = alias.substring(1);
			if(alias.length() >= 4 && alias.charAt(3) == '_') {
				//Try as a character portrait
				String chr = alias.substring(0, 3);
				//Get outfit.
				Integer outfit = char_outfit_map.get(chr);
				if(outfit == null) outfit = 1;
				String lookup = alias + "_" + outfit;
				MebAsset a = strta_map.get(lookup);
				if(a == null) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- source argument could not be resolved!");
					return null;
				}
				
				sb.append(a.getLocalName() + " ");
				if(a.isTable()) {
					Integer idx = idx_map.get(lookup);	
					if(idx == null) {
						System.err.println("ERR -- command \"" + fullcmd + "\" -- source argument could not be resolved!");
						return null;
					}
					sb.append(idx + " ");
				}
				comment = "  //" + alias;
				
			}
			else {
				AMatch match = resolvePathToAsset(srcname);
				if(match == null) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- source argument could not be resolved!");
					return null;
				}
				sb.append(match.asset.getLocalName() + " " + match.idx);
			}
			sb.append(" LYR_SPR_" + args[1] + " ");
			
			int x = 0; int y = 0;
			x = Integer.parseInt(args[3]);
			y = Integer.parseInt(args[4]);
			
			sb.append("pos=" + x + "," + y);
			if(comment != null) sb.append(comment);
			return sb.toString();
		}
		else if(cmd.equals("meplay") || cmd.equals("meplay2") || cmd.equals("me1")|| cmd.equals("me2") 
				|| cmd.equals("me3") || cmd.equals("me1v")|| cmd.equals("me2v")|| cmd.equals("me3v")
				|| cmd.equals("me4v") || cmd.equals("me5v")) {
			//Play ME at specified index (need ME table)
			//meplay ch,idx
			//meplay2 ch,idx,vol
			//ME are mapped as me(idx), so me1, me10 etc.
			
			if(args.length < 2){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			
			int ch = -1; int idx = -1;
			if(cmd.startsWith("mep")) {
				try {
					ch = Integer.parseInt(args[1]);
					idx = Integer.parseInt(args[2]);
				}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
					return null;
				}	
			}
			else {
				try {
					ch = (int)(cmd.charAt(2) - '0');
					idx = Integer.parseInt(args[1]);
				}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
					return null;
				}	
			}
			
			
			//Match asset.
			MebAsset a = strta_map.get("me" + idx);
			if(a == null) {
				System.err.println("ERR -- command \"" + fullcmd + "\" -- source could not be resolved");
				return null;
			}
			StringBuilder sb = new StringBuilder(512);
			sb.append("PLAY_SOUND " + a.getLocalName() + " ME_CH_" + ch);
			
			if(args.length > 3) {
				//Vol
				try {
					int vol = Integer.parseInt(args[3]);
					sb.append(" vol="+vol);
				}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
					return null;
				}
			}
			if(!cmd.startsWith("mep") && args.length > 2) {
				try {
					int vol = Integer.parseInt(args[2]);
					sb.append(" vol="+vol);
				}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
					return null;
				}
			}
			
			return sb.toString();
		}
		else if(cmd.startsWith("mevol")) {
			//mevol, ch, vol
			
			if(args.length < 3){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			
			int ch = -1; int vol = -1;
			try {
				ch = Integer.parseInt(args[1]);
				vol = Integer.parseInt(args[2]);
			}
			catch(NumberFormatException ex) {
				if(!in_for) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
					return null;
				}
			}
			
			if(in_for) {
				StringBuilder sb = new StringBuilder(512);
				sb.append("SET_VOL ME_CH");
				if(ch < 0) sb.append("$FORVAL ");
				else sb.append(ch + " ");
				if(vol < 0) sb.append("$FORVAL");
				else sb.append(vol);
				return sb.toString();
			}
			return "SET_VOL ME_CH" + ch + " " + vol;
		}
		else if(cmd.startsWith("mono")) {
			if(args.length > 1) {
				if(args[1].equals("2")) return "STD_EFFECT MONOCHROME sepia";
				else if(args[1].equals("0")) return "STD_EFFECT MONOCHROME off";
				else if(args[1].equals("1")) return "STD_EFFECT MONOCHROME on";
				return "STD_EFFECT MONOCHROME " + args[1];
			}
			else return "STD_EFFECT MONOCHROME on";
		}
		else if(cmd.startsWith("mov")) {
			//FIIIIINNNNEEEEEE
			if(args.length < 3){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			
			int to = resolveVarID(args[1]);
			String valstr = args[2];
			boolean valisnum = false;
			int ival = 0;
			
			//Resolve value
			if(valstr.startsWith("$")) {
				int from = resolveVarID(valstr);
				String s = s_var.get(from);
				valstr = s;
			}
			else if(valstr.startsWith("%")) {
				valisnum = true;
				int from = resolveVarID(valstr);
				Integer n = i_var.get(from);
				if(n != null) ival = n;
			}
			else if(valstr.startsWith("?")) {
				//Ignore
				return "";
			}
			else {
				try {ival = Integer.parseInt(valstr); valisnum = true;}
				catch(NumberFormatException ex) {}
			}
			
			//Copy value to target
			if(args[1].startsWith("$")) {
				//Check stralias.
				String a = stralias.get(valstr);
				if(a != null) valstr = a;
				
				if(valisnum) s_var.put(to, Integer.toString(ival));
				else s_var.put(to, valstr);
			}
			else if(args[1].startsWith("%")) {
				//Target is an int variable.
				//Check numalias
				String keystr = args[1].substring(1).toLowerCase();
				Integer k = numalias.get(keystr);
				if(k == null) {
					//Assign it some value
					Random r = new Random();
					k = r.nextInt();
					while(i_var.containsKey(k)) k = r.nextInt();
					numalias.put(keystr, k);
				}
				//i_var.put(to, a);
				//System.err.println(args[1] + " = " + a);
				to = k;
				i_var.put(to, ival);
				System.err.println(args[1] + " = " + ival);
			}
			else if(args[1].startsWith("?")) {
				//Ignore
				return "";
			}
			else {
				System.err.println("Warning: move command not resolved -- " + fullcmd);
				return "";
			}
			
		}
		else if(cmd.startsWith("movie") || cmd.startsWith("spoiler_play_movie")) {
			return "PLAY_MOV <INSERT_MOV_NAME>";
		}
		else if(cmd.equals("msp")) {
			//handle, x, y
			if(args.length < 4){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			
			StringBuilder sb = new StringBuilder(512);
			sb.append("MOVE_2D ");
			
			String handle = args[1];
			
			sb.append(" LYR_SPR_" + handle + " ");
			
			int x = 0; int y = 0;
			x = Integer.parseInt(args[2]);
			y = Integer.parseInt(args[3]);
			
			sb.append(x + "," + y);
			
			return sb.toString();
		}
		else if(cmd.startsWith("nega")) {
			if(args.length > 1) {
				if(args[1].equals("0")) return "STD_EFFECT NEGA off";
				else if(args[1].equals("1")) return "STD_EFFECT NEGA on";
				return "STD_EFFECT NEGA " + args[1];
			}
			else return "STD_EFFECT NEGA on";
		}
		else if(cmd.startsWith("quake")) {
			// Shake screen
			//quake(y,x) no,dur
			if(args.length < 3){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			int no = -1; int dur = -1;
			try {
				no = Integer.parseInt(args[1]);
				dur = Integer.parseInt(args[2]);
			}
			catch(NumberFormatException ex) {
				System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
				return null;
			}
			
			StringBuilder sb = new StringBuilder(512);
			sb.append("STD_EFFECT SHAKE " + no + " " + (dur/no) + " " + DEFO_QUAKE_AMT);
			sb.append(no); sb.append(" ");
			sb.append(dur/no); sb.append(" ");
			sb.append(DEFO_QUAKE_AMT);
			if(cmd.endsWith("y")) sb.append(" dir=y");
			else if(cmd.endsWith("x")) sb.append(" dir=x");
			
			return sb.toString();
		}
		else if(cmd.equals("seplay") || cmd.equals("seplay2") || cmd.equals("se1")|| cmd.equals("se2")|| cmd.equals("se3") 
				|| cmd.equals("se1v") || cmd.equals("se2v") || cmd.equals("se3v")) {
			//Same as meplay, except if index is 1100, it plays a random sound I tabled at 1101-1106
			if(args.length < 2){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			
			int ch = -1; int idx = -1;
			if(cmd.startsWith("sep")) {
				try {
					ch = Integer.parseInt(args[1]);
					idx = Integer.parseInt(args[2]);
				}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
					return null;
				}	
			}
			else {
				try {
					ch = (int)(cmd.charAt(2) - '0');
					idx = Integer.parseInt(args[1]);
				}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
					return null;
				}	
			}
			
			StringBuilder sb = new StringBuilder(512);
			if(idx == 1100) {
				//Play random.
				sb.append("PLAY_RANDOM_SOUND 6 ");
				for(int i = 1; i <= 6; i++) {
					MebAsset a = strta_map.get("se" + (1100+i));
					if(a == null) {
						System.err.println("ERR -- command \"" + fullcmd + "\" -- source could not be resolved");
						return null;
					}
					sb.append(a.getLocalName() + " ");
				}
				sb.append("SE_CH_" + ch);
			}
			
			//Match asset.
			MebAsset a = strta_map.get("se" + idx);
			if(a == null) {
				System.err.println("ERR -- command \"" + fullcmd + "\" -- source could not be resolved");
				return null;
			}
			sb.append("PLAY_SOUND " + a.getLocalName() + " SE_CH_" + ch);
			
			
			if(args.length > 3) {
				//Vol
				try {
					int vol = Integer.parseInt(args[3]);
					sb.append(" vol="+vol);
				}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
					return null;
				}
			}
			if(!cmd.startsWith("sep") && args.length > 2) {
				//Vol
				try {
					int vol = Integer.parseInt(args[2]);
					sb.append(" vol="+vol);
				}
				catch(NumberFormatException ex) {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- one or more args is non-numerical");
					return null;
				}
			}
			
			return sb.toString();
		}
		else if(cmd.equals("set_oldnew_bg")) {
			//This appears to be used to show cgs. Hides all the sprites.
			//I'll be lazy and make it paint to the top layer :D
			//set_oldnew_bg cg_img,backup_img,transition,backup-transition
			/*
			 * If the provided first-choice transition param is 2, looks like it calls a dll to do a "breakup" effect
			 * Otherwise, it does the "backup"
			 */
			
			if(args.length < 5){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			
			//Resolve images.
			String i1 = null; String i2 = null;
			args[1] = args[1].toLowerCase();
			args[2] = args[2].toLowerCase();
			
			if(args[1] == "black") i1 = "#000000";
			else if(args[1] == "white") i1 = "#FFFFFF";
			else {
				AMatch am = resolveImage(args[1]);
				if(am == null) {
					//Check backup
					System.err.println("WARNING! cg/bg arg " + args[1] + " could not be resolved - using backup.");
					if(args[2] == "black") i2 = "#000000";
					else if(args[2] == "white") i2 = "#FFFFFF";
					am = resolveImage(args[2]);
					if(am == null) {
						System.err.println("ERR -- cg/bg arg " + args[1] + "backup could not be resolved either.");
						return null;
					}
					if(am.asset.isTable()) {
						i2 = am.asset.getLocalName() + " " + am.idx;
					}
					else i2 = am.asset.getLocalName();
				}
				else {
					if(am.asset.isTable()) {
						i1 = am.asset.getLocalName() + " " + am.idx;
					}
					else i1 = am.asset.getLocalName();
				}
			}
			
			StringBuilder sb = new StringBuilder(512);
			sb.append("DRAW_2D ");
			if(i1 != null) sb.append(i1 + " ");
			else sb.append(i2 + " ");
			sb.append("LYR_CG");
			
			//Resolve effects.
			String estr = args[3].trim().toLowerCase();
			if(estr.equals("2")) sb.append(" transition=CALLBACK,breakup_lrp");
			else {
				String t = resolveEffect(estr);
				if(t != null && !t.isEmpty()) sb.append(" " + t);
			}
			
			sb.append("  //" + args[1]);
			return sb.toString();
		}
		else if(cmd.equals("sl")) {
			//Looks like this clears out the textbox.
			return "CLEAR_TXB " + TXB_ASSET_NAME + " " + TXB_MOD1_NAME;
			//return "SET_TXB_VIS " + TXB_ASSET_NAME + " ALL false";
		}
		else if(cmd.equals("textoff")) {
			//TODO I need to clarify my own commands. What clears the textbox text, and what HIDES it? Specify textbox or all textboxes?
			//Remove textbox from screen
			//txb_on = false;
			//return "CLEAR_TXB";
			return "SET_TXB_VIS " + TXB_ASSET_NAME + " ALL false";
		}
		else if(cmd.equals("wait")) {
			//What's on the box
			if(args.length < 2){
				System.err.println("ERR -- command \"" + fullcmd + "\" -- insufficient args");
				return null;
			}
			
			int amt = -1;
			try {amt = Integer.parseInt(args[1]);}
			catch(NumberFormatException ex) {
				
				//Try int variable.
				if(args[1].startsWith("%")) {
					int idx = resolveVarID(args[1]);
					Integer val = i_var.get(idx);
					if(val != null) amt = val;
					else {
						System.err.println("ERR -- command \"" + fullcmd + "\" -- param is not numerical");
						return null;
					}
				}
				else {
					System.err.println("ERR -- command \"" + fullcmd + "\" -- param is not numerical");
					return null;
				}
			}
			
			return "WAIT " + amt;
		}
		
		//Empty string means ignored command, null means processing error
		//Check if it should be ignored...
		for(int i = 0; i < ignored_cmds.length; i++) {
			if(ignored_cmds[i].equals(cmd)) return "";
		}
		
		bad_cmd_flag = true;
		System.err.println("Command not recognized: " + cmd);
		return "";
	}
	
	private static String processLine(String line) throws IOException {
		String[] cmds = splitLine(line);
		if(cmds == null) {
			System.err.println("Couldn't split into commands: " + line);
			return null;
		}
		
		StringBuilder sb = new StringBuilder(4096);
		for(String cmd : cmds) {
			cmd = cmd.trim();
			String res = processCommand(cmd);
			if(res == null || res.isEmpty()) return "";
			sb.append(res);
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	private static boolean charisAscii(char c) {
		return c <= '~';
	}
	
	private static boolean charisTextControl(char c) {
		if(c == '!') return true;
		if(c == '^') return true;
		if(c == '/') return true;
		if(c == '\\') return true;
		if(c == '@') return true;
		if(c == '#') return true;
		if(c == '~') return true;
		return false;
	}
	
	private static boolean isWhitespacePlus(char c) {
		if(Character.isWhitespace((int)c)) return true;
		//if(Character.isWhitespace(c)) return true;
		if(c == '　') return true;
		return false;
	}
	
	private static String trimplus(String in) {
		in = in.trim();
		int slen = in.length();
		int st = 0; int ed = slen;
		//Find first and last non-whitespace characters...
		for(int i = 0; i < slen; i++) {
			char c = in.charAt(i);
			if(!isWhitespacePlus(c)) break;
			st++;
		}
		for(int i = slen-1; i >= 0; i--) {
			char c = in.charAt(i);
			if(!isWhitespacePlus(c)) break;
			ed--;
		}
		
		if(ed <= st) return "";
		
		return in.substring(st, ed);
	}
	
	private static String processLantextPair(String jpline, String enline) throws IOException {
		//TODO
		/*
		 * Alright, the parsing of these is kind of weird.
		 * -> Switch to text mode when a ^ or non-ASCII character is encountered.
		 * 		After the text command the rest of the line is in text mode - don't need to look for end.
		 * -> Looks like text commands tend to exist outside of ^ enclosings.
		 * 		What I'll do then is modify processTextline to take ^ into account (or just chuck)
		 * -> ...And only forward everything from the first ^ or non-ASCII character to processTextLine
		 * -> Not sure what to do with the other "break" characters. There's 0x09 and 0x10. Are they a timing thing? They aren't positioned 
		 * well for newlines. Maybe just ignore them.
		 */
		//System.err.println("jpline --- " + jpline);
		//System.err.println("enline --- " + enline);
		
		//These often come as a crappy interleave of voice commands and text.
		//First split along colons
		int jcount = 0;
		int ecount = 0;
		List<String> jlist = new LinkedList<String>();
		List<String> elist = new LinkedList<String>();
		strcount_flag = false;
		
		StringBuilder sb = new StringBuilder(4096);
		int slen = jpline.length();
		boolean cmarked = false;
		int i = 0;
		jpline = jpline.trim();
		while(i < slen) {
			char c = jpline.charAt(i++);
			if(cmarked) {
				if(c == '^') cmarked = false;
				sb.append(c);
			}
			else {
				//Colon and semicolon only recognized here.
				switch(c) {
				case '^': cmarked = true; sb.append(c); break;
				case ':':
					jlist.add(sb.toString());
					sb = new StringBuilder(4096);
					break;
				case ';': i = slen; break;
				default: sb.append(c); break;
				}
			}
		}
		if(sb.length() > 0) jlist.add(sb.toString());
		
		sb = new StringBuilder(4096);
		slen = enline.length();
		i = 0;
		enline = enline.trim();
		cmarked = false;
		while(i < slen) {
			char c = enline.charAt(i++);
			if(cmarked) {
				if(c == '^') cmarked = false;
				sb.append(c);
			}
			else {
				//Colon and semicolon only recognized here.
				switch(c) {
				case '^': cmarked = true; sb.append(c); break;
				case ':':
					elist.add(sb.toString());
					sb = new StringBuilder(4096);
					break;
				case ';': i = slen; break;
				default: sb.append(c); break;
				}
			}
		}
		if(sb.length() > 0) elist.add(sb.toString());
		
		//Use the Japanese line to determine how many voice and text commands are there.
		sb = new StringBuilder(4096);
		boolean tmode = false;
		LinkedList<String> voices = new LinkedList<String>();
		String lastvoice = null;
		int vdelay = 0;
		boolean first = true;
		for(String s : jlist) {
			//if(first) System.err.println("j: " + s);
			s = trimplus(s);
			slen = s.length();
			if(s.isEmpty()) continue;
			//System.err.println("j: " + s);
			if(s.startsWith("langjp")) {
				s = trimplus(s.substring(6));
				if(s.isEmpty()) continue;
				slen = s.length();
			}
			
			//Isolate any letters before the first text marker, if there is one.
			StringBuilder cmd = new StringBuilder(256);
			i = 0;
			tmode = false;
			while(!tmode && i < slen) {
				char c = s.charAt(i++);
				if(c == '^' || !charisAscii(c) || charisTextControl(c)) {tmode = true; i--;}
				else cmd.append(c);
			}
			String command = cmd.toString().trim().toLowerCase();
			//System.err.println("command: " + command);
			
			if(!first)sb.append("\n");
			if(command.startsWith("dwave_jp")){
				//voice
				s = s.trim();
				sb.append("STOP_SOUND VOX_CH\n");
				if(vdelay > 0) sb.append("DELAY " + vdelay + "\n");
				vdelay = 0;
				String out = processCommand(s);
				if(out == null) return null;
				else sb.append(out);
				
				int lastcm = s.lastIndexOf(',');
				if(lastcm >= 0) {
					lastvoice = s.substring(lastcm+1).trim();
					voices.add(lastvoice);
					//System.err.println("lastvoice: " + lastvoice);
				}
				first = false;
			}
			else if(command.startsWith("voicedelay")) {
				
				String arg = s.substring(10).trim();
				int amt = 0;
				try {amt = Integer.parseInt(arg);}
				catch(NumberFormatException ex) {
					System.err.println("Warning: \"" + s + "\" skipped - invalid arg");
					continue;
				}
				
				vdelay = amt;
				//sb.append("DELAY " + amt);
			}
			else {
				first = false;
				//Text?
				//System.err.println("j: " + s);
				//sb.append("APPEND_TEXT ");
				//sb.append(TXB_ASSET_NAME + " ");
				//sb.append(TXB_MOD1_NAME + " ");
				//sb.append(stt_name + " " + (stt_jp_idx++));
				
				//Okay let's try this again.
				slen = s.length();
				StringBuilder rawstr = new StringBuilder(slen << 1);
				int j = i; //Start where text marker was found.
				boolean refresh = false;
				boolean firstg = true;
				while(j < slen) {
					char c = s.charAt(j++);
					//Imma just remove carrots.
					//Unless literal
					char d;
					switch(c) {
					case '#': 
						//Only handle if denotes ^@/\ as literal (so it can pass on to string processor)
						d = s.charAt(j);
						rawstr.append(c);
						if(d == '^' || d == '@' || d == '\\' || d == '/') {
							rawstr.append(d); j++;
						}
						refresh=false;
						break;
					case '^': 
						//Eh just ignore.
						refresh=false;
						break;
					case '@': 
						//Write string, add wait for user command
						stt_jp.write(processTextline(rawstr.toString()));
						
						//Check if next character is forward slash...
						if(j < slen) {
							d = s.charAt(j);
							if(d == '/') {
								stt_jp.write("\\a");
								j++;
							}	
						}
						
						refresh = true;
						if(!firstg) sb.append("\n");
						sb.append("APPEND_TEXT ");
						sb.append(TXB_ASSET_NAME + " ");
						sb.append(TXB_MOD1_NAME + " ");
						sb.append(stt_name + " " + (stt_jp_idx++));
						sb.append("\nWAIT_FOR USER_CONT");
						break;
					case '\\': 
						//Write string, add wait for user command & text clear command
						stt_jp.write(processTextline(rawstr.toString()));
						stt_jp.write("\\E");
						refresh = true;
						if(!firstg) sb.append("\n");
						sb.append("APPEND_TEXT ");
						sb.append(TXB_ASSET_NAME + " ");
						sb.append(TXB_MOD1_NAME + " ");
						sb.append(stt_name + " " + (stt_jp_idx++));
						sb.append("\nWAIT_FOR USER_CONT");
						sb.append("\nCLEAR_TXB " + TXB_ASSET_NAME + " " + TXB_MOD1_NAME);
						break;
					case '/': 
						//Write string, appending \a to end
						stt_jp.write(processTextline(rawstr.toString()));
						stt_jp.write("\\a");
						if(!firstg) sb.append("\n");
						sb.append("APPEND_TEXT ");
						sb.append(TXB_ASSET_NAME + " ");
						sb.append(TXB_MOD1_NAME + " ");
						sb.append(stt_name + " " + (stt_jp_idx++));
						refresh = true;
						break;
					case 0x10: 
					case 0x09:
						//I'll ignore these too for now.
						stt_jp.write("\\n");
						refresh=false;
						break;
					default: rawstr.append(c); refresh=false; break;
					//TODO I'm getting this weird 0xe38080 character sometimes. That needs to beat it.
					//Ah, it's a Japanese space. Not getting detected as whitespace.
					//So 1. fix CJK whitespace and 2. there can be command text before main text block (eg. !w800 at line start)
					}
					if(refresh) {
						
						if(lastvoice != null) stt_jp.write("\t" + lastvoice);
						stt_jp.write("\n");
						jcount++;
						
						refresh = false;
						rawstr = new StringBuilder(slen << 1);
						firstg = false;
					}
				}
				if(rawstr.length() > 0) {
					stt_jp.write(processTextline(rawstr.toString()));
					if(lastvoice != null) stt_jp.write("\t" + lastvoice);
					stt_jp.write("\n");
					jcount++;
					
					sb.append("APPEND_TEXT ");
					sb.append(TXB_ASSET_NAME + " ");
					sb.append(TXB_MOD1_NAME + " ");
					sb.append(stt_name + " " + (stt_jp_idx++));
					refresh = false;
				}
				
			}
			//first = false;
		}
		
		
		//Now write the english string to text...
		tmode = false;
		for(String s : elist) {
			//TODO
			s = s.trim();
			//System.err.println("e: " + s);
			if(s.startsWith("langen")) {
				s = s.substring(6).trim();
				if(s.isEmpty()) continue;
			}
			
			//Toss if command.
			if(s.startsWith("dwave_")) continue;
			if(s.startsWith("voicedelay")) continue;
			
			slen = s.length();
			i = 0;
			
			//Fast forward to start mark
			tmode = false;
			while(!tmode && i < slen) {
				char c = s.charAt(i++);
				if(c == '^' || !charisAscii(c) || charisTextControl(c)) {tmode = true; i--;}
			}
			if(i >= slen) continue;
			
			StringBuilder rawstr = new StringBuilder(slen << 1);
			int j = i; //Start where text marker was found.
			boolean refresh = false;
			//System.err.println("e: " + s);
			while(j < slen) {
				char c = s.charAt(j++);
				//Imma just remove carrots.
				//Unless literal
				switch(c) {
				case '#': 
					//Only handle if denotes ^@/\ as literal (so it can pass on to string processor)
					char d = s.charAt(j);
					rawstr.append(c);
					if(d == '^' || d == '@' || d == '\\' || d == '/') {
						rawstr.append(d); j++;
					}
					refresh=false;
					break;
				case '^': 
					//Eh just ignore.
					refresh=false;
					break;
				case '@': 
					//Write string, add wait for user command
					stt_en.write(processTextline(rawstr.toString()));
					refresh = true;
					if(j < slen) {
						d = s.charAt(j);
						if(d == '/') {
							stt_en.write("\\a");
							j++;
						}	
					}
					break;
				case '\\': 
					//Write string, add wait for user command & text clear command
					stt_en.write(processTextline(rawstr.toString()));
					stt_en.write("\\E");
					refresh = true;
					break;
				case '/': 
					//Write string, appending \a to end
					stt_en.write(processTextline(rawstr.toString()));
					stt_en.write("\\a");
					refresh = true;
					break;
				case 0x10: 
				case 0x09:
					//I'll ignore these too for now.
					stt_en.write("\\n");
					refresh=false;
					break;
				default: rawstr.append(c); refresh=false; break;
				}
				if(refresh) {
					
					lastvoice = voices.poll();
					if(lastvoice != null) stt_en.write("\t" + lastvoice);
					stt_en.write("\n");
					ecount++;
					
					refresh = false;
					rawstr = new StringBuilder(slen << 1);
				}
			}
			if(rawstr.length() > 0) {
				stt_en.write(processTextline(rawstr.toString()));
				
				lastvoice = voices.poll();
				if(lastvoice != null) stt_en.write("\t" + lastvoice);
				stt_en.write("\n");
				ecount++;
				
				refresh = false;
			}
			
		}
		
		if(jcount != ecount) {
			System.err.println("Warning -- JP/EN str count mismatch!");
			strcount_flag = true;
		}
		
		//System.exit(2);
		sb.append("\n");
		return sb.toString();
	}
	
	public static void parsePonscript(String path, String writedir, String lbl_start, String lbl_end) throws IOException {
		//TODO
		/*
		 * Keep in mind that the dialog blocks are really gnarled with the two languages
		 * As a result, when encounter a langjp command, need to read as some kind of full block until english
		 * equivalent is done.
		 * 
		 * Reason for this is both the parser, but also that there can be redundant commands.
		 * For example, advchar for the same character or all characters whose dialog is covered in one block may called before
		 * each Japanese string, then CALLED AGAIN in the same order before the English string:
		 * 
		 * eg. 
		 *  advchar "06"
			langjp:dwave_jp 0, hid_1e51:「はぁ〜！@:dwave_jp 0, hid_1e52:　今年も相変わらず、大したもんや…。@:dwave_jp 0, hid_1e53:目の保養とはこのことやで…。」@
			advchar "-1"
			langjp　石段を登りきり、薔薇の庭園に迎えられた人たちは次々の感動を口にする。\

			advchar "06"
			langen:dwave_eng 0, hid_1e51:^"Aaah!^@:dwave_eng 0, hid_1e52:^  It's as beautiful as ever this year...^@:dwave_eng 0, hid_1e53:^  A real delight for the eyes..."^@
			advchar "-1"
			langen^After climbing the stone steps, the people greeted by this rose garden gave voice to their impressions one by one.^\
		 */
		
		//Set state & open writers
		//TODO
		namebox_visible = false;
		char_in_left = false;
		char_in_center = false;
		char_in_right = false;
		char_outfit_map = new HashMap<String, Integer>();
		stt_en_idx = 0; stt_jp_idx = 0;
		st_fnt_sz = DEFO_FONT_SIZE;
		stt_name = "stt_" + lbl_start;
		String outname = writedir + File.separator + stt_name + ".mtsce";
		String outjp = writedir + File.separator + stt_name + "_jp.u16";
		String outen = writedir + File.separator + stt_name + "_we.u8";
		BufferedWriter outscript = new BufferedWriter(new FileWriter(outname));
		stt_en = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outen),"UTF8"));
		stt_jp = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outjp),"UTF-16"));
		
		//Open the script file. Remember to open to UTF8 encoding.
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));
		
		//Fast-forward to start label.
		String line = null;
		int lineno = 0;
		while((line = br.readLine()) != null) {
			lineno++;
			if(line.isEmpty()) continue;
			if(!line.startsWith("*")) continue;
			line = line.toLowerCase();
			if(line.startsWith("*" + lbl_start)) break;
		}
		System.err.println("Parser: found start label at line " + (lineno));
		//System.err.println("endlbl: " + lbl_end);
		
		try {
		while((line = br.readLine()) != null) {
			lineno++;
			if(line.isEmpty()) continue;
			if(line.startsWith(";")) continue;
			if(line.startsWith("*")) {
				//Check if ending label.
				String lower = line.toLowerCase();
				if(lower.startsWith("*"+lbl_end)) break;
			}
			
			//Determine whether texty block or another command
			//String[] commands = null;
			if(line.startsWith("advchar") || line.startsWith("langjp")) {
				//If text block, separate out into individual commands
				//Copy lines until one is found starting with langen
				//System.err.println("line = " + line);
				int linebase = lineno;
				ArrayList<String> lines = new ArrayList<String>(16);
				lines.add(line.trim());
				int jpct = 0; int enct = 0;
				int jp1 = -1; int en1 = -1;
				if(line.startsWith("langjp")) {
					jp1 = 0;
					jpct++;
					//System.err.println("langjp line: " + line);
					//System.err.println("langjp count: " + jpct);
					//System.err.println("jp1: " + jp1);
				}
				while(enct < 1 || enct < jpct) {
					line = br.readLine().trim(); lineno++;
					if(line == null) break;
					if(line.isEmpty()) continue;
					if(line.startsWith(";")) continue;
					
					if(line.startsWith("langjp")) {
						if(jp1 < 0) jp1 = lines.size();
						jpct++;
						//System.err.println("langjp line: " + line);
						//System.err.println("langjp count: " + jpct);
						//System.err.println("jp1: " + jp1);
					}
					else if(line.startsWith("langen")) {
						if(en1 < 0) en1 = lines.size();
						enct++;
						//System.err.println("langen line: " + line);
						//System.err.println("langen count: " + enct);
						//System.err.println("en1: " + en1);
					}
					lines.add(line);
				}
				
				//if(lineno >= 93200) System.exit(2);
				//for(String l : lines) System.err.println("LINE -- " + l);
				//for(int k = 0; k < lines.size(); k++) System.err.println("LINE " + k + " -- " + lines.get(k));
				
				//Eliminate redundant lines.
				LinkedList<String> laterlines = new LinkedList<String>();
				int pj = 0; int pe = en1-jp1;
				while(pj < en1) {
					//System.err.println("pj = " + pj);
					String linej = lines.get(pj);
					//System.err.println("linej = " + linej);
					
					String linee = null;
					if(pe < lines.size()) linee = lines.get(pe);
					else {
						//Pull one more from the reader.
						lineno++;
						linee = br.readLine();
					}
					if(linej.equals(linee)) {
						//Parse this line.
						String str = processLine(linej);
						if(str == null || bad_cmd_flag) {
							System.err.println("ERROR at line " + (linebase + pj) + "! Processing aborted.");
							br.close();
							stt_en.close();
							stt_jp.close();
							outscript.close();
							return;
						}
						if(!str.isEmpty()) outscript.write(str);
					}
					else {
						//Check if text.
						if(linej.startsWith("langjp")) {
							//Make sure partner is langen
							if(!linee.startsWith("langen")) {
								System.err.println("ERROR at line " + (linebase + pe) + "! language command misaligned!");
								br.close();
								stt_en.close();
								stt_jp.close();
								outscript.close();
								return;
							}
							
							String str = processLantextPair(linej, linee);
							if(strcount_flag) {
								System.err.println("--String mismatch around line " + (linebase + pe));
							}
							if(str == null || bad_cmd_flag) {
								System.err.println("ERROR at line " + (linebase + pj) + " or " + (linebase + pe) + "! Processing aborted.");
								br.close();
								stt_en.close();
								stt_jp.close();
								outscript.close();
								return;
							}
							if(!str.isEmpty()) outscript.write(str);
						}
						else {
							//Parse the earlier line, save the later line.
							//I don't think this should happen, but just in case...
							String str = processLine(linej);
							if(str == null || bad_cmd_flag) {
								System.err.println("ERROR at line " + (linebase + pj) + "! Processing aborted.");
								br.close();
								stt_en.close();
								stt_jp.close();
								outscript.close();
								return;
							}
							if(!str.isEmpty()) outscript.write(str);
							if(pe > en1 && !line.startsWith("lang")) laterlines.add(linee);
						}
					}
					pj++; pe++;
				}
				
				for(String l : laterlines) {
					String str = processLine(l);
					if(str == null || bad_cmd_flag) {
						System.err.println("ERROR near line " + (linebase + en1) + "! Processing aborted.");
						br.close();
						stt_en.close();
						stt_jp.close();
						outscript.close();
						return;
					}
					if(!str.isEmpty()) outscript.write(str);
				}
				
			}
			else if(line.startsWith("for ")){
				//Read all lines until the "next" line.
				in_for = true;
				LinkedList<String> looplines = new LinkedList<String>();
				looplines.add(line);
				while(!line.startsWith("next")) {
					lineno++;
					line = br.readLine().trim();
					looplines.add(line);
				}
				outscript.write("FOR ");
				
				//Now parse the loop
				String lline = looplines.poll();
				//Expecting "for %var = st to ed"
				//String var = "";
				int idx = lline.lastIndexOf('=');
				if(idx >= 0) {
					
					//var = lline.substring(3,idx).trim();
					
					String sub = lline.substring(idx+1).trim();
					String[] subs = sub.split("to");
					
					sub = subs[0].trim();
					//Try to parse as int.
					if(sub.startsWith("%")) {
						idx = resolveVarID(sub);
						Integer i = i_var.get(idx);
						if(i == null) {
							System.err.println("Variable couldn't be resolved: \"" + lline + "\"");
							System.err.println("ERROR at line " + (lineno) + "! Processing aborted.");
							br.close();
							stt_en.close();
							stt_jp.close();
							outscript.close();
							return;
						}
						outscript.write(i + " ");
					}
					else {
						try {idx = Integer.parseInt(sub);}
						catch(NumberFormatException ex) {
							System.err.println("Expecting integer parameter: \"" + lline + "\"");
							System.err.println("ERROR at line " + (lineno) + "! Processing aborted.");
							br.close();
							stt_en.close();
							stt_jp.close();
							outscript.close();
							return;
						}
						outscript.write(idx + " ");
					}
					
					sub = subs[1].trim();
					//This is sloppy and I shouldn't copypaste but whatever/
					if(sub.startsWith("%")) {
						idx = resolveVarID(sub);
						Integer i = i_var.get(idx);
						if(i == null) {
							System.err.println("Variable couldn't be resolved: \"" + lline + "\"");
							System.err.println("ERROR at line " + (lineno) + "! Processing aborted.");
							br.close();
							stt_en.close();
							stt_jp.close();
							outscript.close();
							return;
						}
						outscript.write(i + "\n");
					}
					else {
						try {idx = Integer.parseInt(sub);}
						catch(NumberFormatException ex) {
							System.err.println("Expecting integer parameter: \"" + lline + "\"");
							System.err.println("ERROR at line " + (lineno) + "! Processing aborted.");
							br.close();
							stt_en.close();
							stt_jp.close();
							outscript.close();
							return;
						}
						outscript.write(idx + "\n");
					}
					
				}
				else {
					System.err.println("Loop syntax not recognized: \"" + lline + "\"");
					System.err.println("ERROR at line " + (lineno) + "! Processing aborted.");
					br.close();
					stt_en.close();
					stt_jp.close();
					outscript.close();
					return;
				}
				
				//Inner commands...
				while(!looplines.isEmpty()) {
					lline = looplines.poll();
					if(lline.isEmpty()) continue;
					if(lline.startsWith(";")) continue;
					if(lline.startsWith("next")) break;
					
					//See if it has the variable...
					
					String str = processLine(lline);
					if(str == null || bad_cmd_flag) {
						System.err.println("ERROR near line " + (lineno) + "! Processing aborted.");
						br.close();
						stt_en.close();
						stt_jp.close();
						outscript.close();
						return;
					}
					if(!str.isEmpty()) outscript.write(str);
				}
				
				//End
				in_for = false;
				outscript.write("ENDFOR\n");
			}
			else if(line.startsWith("*maria_song_scroll_loop")) {
				//Particular loop in ep1.
				//I don't feel like modifying the parser to handle it, so I'll just manually convert it here.
				//Ignore everything until the next tilde
				while(!line.startsWith("~")) {
					line = br.readLine();
					lineno++;
				}
				
				//Then print manual translation to outscript
				//Moves the image 356 pix left over 7 seconds
				//So, one pix every 20 ms
				outscript.write("FOR 1 356\n");
				outscript.write("MOVE_2D LYR_SPR_bgsp -1,0\n");
				outscript.write("WAIT 20\n");
				outscript.write("ENDFOR\n");
			}
			else {
				//If regular line, split into commands
				//Loop through extracted commands and convert to output text
				//If a command returns a null, throw error (with line#)
				//If a command returns an empty string, check if it's supposed to be an ignored command. If not, throw error (with line#)
				//Write output
				String str = processLine(line);
				if(str == null || bad_cmd_flag) {
					System.err.println("ERROR at line " + (lineno) + "! Processing aborted.");
					br.close();
					stt_en.close();
					stt_jp.close();
					outscript.close();
					return;
				}
				if(!str.isEmpty()) outscript.write(str);
			}
		}}
		catch(Exception ex) {
			br.close();
			stt_en.close();
			stt_jp.close();
			outscript.close();
			System.err.println("Exception at line " + lineno);
			ex.printStackTrace();
			return;
		}
		System.err.println("Parser: found end label at line " + (lineno));
		br.close();
		
		stt_en.close();
		stt_jp.close();
		outscript.close();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String init_file = "D:\\usr\\bghos\\code\\mubuilds\\umineko0\\source\\q_init.txt";
		String script_file = "D:\\usr\\bghos\\code\\mubuilds\\umineko0\\source\\q-mod\\0.u";
		String test_script = "D:\\usr\\bghos\\code\\mubuilds\\umineko0\\source\\umi01_02.txt";
		
		String dat_dir = "D:\\usr\\bghos\\code\\mubuilds\\umineko0\\data";
		String str_dir = dat_dir + "\\str\\workspace";
		
		//Define labels...
		int sec_ch1 = 17;
		int sec_ch2 = 18;
		int sec_ch3 = 18;
		int sec_ch4 = 19;
		
		//Looks like core chapters just plow thru from umi1_opning to umi4_19 ?
		/*Then goes into the 8 teatimes
		 * 
		 * teatime_4 @ 273787
		 * ura_teatime_4 @ 281593
		 * teatime_1 @ 282389
		 * teatime_2 @ 284483
		 * teatime_3 @ 286075
		 * ura_teatime_1 @ 286766
		 * ura_teatime_2 @ 287131
		 * ura_teatime_3 @ 288126
		 * 
		 * (note, what marks end of ura teatime 3? Need to figure that out.....)
		 * use *ep1_scroll
		 * 
		 * god I hate this game layout at least split your scripts and keep your strings in separate tables you scrubs
		 * 
		 */
		
		String[] all_labels = new String[sec_ch1+sec_ch2+sec_ch2+sec_ch4+13];
		int i = 0;
		all_labels[i++] = "umi1_opning";
		for(int j = 0; j < sec_ch1; j++) {
			all_labels[i++] = "umi1_" + (j+1);
		}
		all_labels[i++] = "umi2_opning";
		for(int j = 0; j < sec_ch2; j++) {
			all_labels[i++] = "umi2_" + (j+1);
		}
		all_labels[i++] = "umi3_opning";
		for(int j = 0; j < sec_ch3; j++) {
			all_labels[i++] = "umi3_" + (j+1);
		}
		all_labels[i++] = "umi4_opning";
		for(int j = 0; j < sec_ch4; j++) {
			all_labels[i++] = "umi4_" + (j+1);
		}
		all_labels[i++] = "teatime_4";
		all_labels[i++] = "ura_teatime_4";
		all_labels[i++] = "teatime_1";
		all_labels[i++] = "teatime_2";
		all_labels[i++] = "teatime_3";
		all_labels[i++] = "ura_teatime_1";
		all_labels[i++] = "ura_teatime_2";
		all_labels[i++] = "ura_teatime_3";
		all_labels[i++] = "ep1_scroll";
		

		try {
			//1 - Package and ID assets
			readDefineSection(init_file);
			organizeAssets(dat_dir);
			proj.clean();
			proj.writeProjFile(dat_dir + "\\umineko0." + MuenProject.FE_PROJ); 
			proj.writeInitBin();
			
			int todo = 22;
			for(int j = 0; j < todo; j++) {
				if(j == 17) continue; //ep 1 scroll
				String stlbl = all_labels[j];
				String edlbl = all_labels[j+1];
				
				//Prepare directory
				String outpath = str_dir + "\\" + stlbl;
				if(!FileBuffer.directoryExists(outpath)) {
					Files.createDirectories(Paths.get(outpath));
				}
				
				//Try to parse...
				parsePonscript(script_file, outpath, stlbl, edlbl);
				
				//Register sce and stts with project (later)
				//TODO
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
	}

}
