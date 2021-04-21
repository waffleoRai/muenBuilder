package waffleoRai_mubuilder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import waffleoRai_Utils.BinFieldSize;
import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileUtils;
import waffleoRai_Utils.FileBuffer.UnsupportedFileTypeException;
import waffleoRai_Utils.SerializedString;
import waffleoRai_mubuilder.build.AssetWriterCallback;

public class MuenProject {
	
	/*----- Constants -----*/
	
	public static final String MAGIC_PROJ = "muenPROJ";
	
	public static final short VER_PROJ = 2;
	public static final short VER_INIBIN = 1;
	public static final int VER_ASSH = 2;
	public static final int VER_ASSP = 1;
	
	public static final String MAGIC_ASSH = "assH";
	public static final String MAGIC_ASSP = "assP";
	
	public static final String FN_INIBIN = "init.bin";
	public static final String FE_PROJ = "muprj";
	
	public static final long DEFO_MEM_LIMIT = 0x7fffffff;
	
	protected static int DEBUGOP_NO_ASSH_ENCRYPT = 1;
	protected static int DEBUGOP_NO_ASSP_XOR = 1;
	
	/*----- Instance Variables -----*/
	
	private String build_dir; //Target directory
	
	private int defo_pkg = 0;
	private int defo_grp = 0xdef0def0;
	
	private char[] gamecode;
	private short ver_maj;
	private short ver_min;
	private short ver_bld;
	
	//Uses default settings for flags - ASSH encrypted and by group, ASSPs not encrypted (but XORd with TGI)
	
	//private long timestamp_epochsec; //What's stored
	private ZonedDateTime timestamp;
	private byte[] aeskey;
	private long mem_limit; //Defaults to 2GB
	
	//	----- Project Builder Fields
	
	private ArrayList<MebPackage> packages;
	private Map<Integer, MebGroup> groupMap;
	private Map<Long, MebAsset> assetMap;
	private Map<String, MebAsset> nameMap;
	
	/*----- Initialization -----*/
	
	private MuenProject() {
		gamecode = new char[8];
		aeskey = new byte[16];
		groupMap = new TreeMap<Integer, MebGroup>();
		assetMap = new TreeMap<Long, MebAsset>();
		nameMap = new HashMap<String, MebAsset>();
	}
	
	public static MuenProject newProject(String gcode, String builddir) {
		MuenProject proj = new MuenProject();
		proj.build_dir = builddir;
		
		int gchar = 0;
		if(gcode != null) {
			for(int i = 0; i < gcode.length(); i++) proj.gamecode[gchar++] = gcode.charAt(i);
		}
		Random rand = new Random();
		while(gchar < 8) {
			//Generate random characters
			int i = rand.nextInt(3); //Num, lower, upper
			switch(i) {
			case 0:
				proj.gamecode[gchar++] = (char) ('0' + (char)rand.nextInt(10));
				break;
			case 1:
				proj.gamecode[gchar++] = (char) ('a' + (char)rand.nextInt(26));
				break;
			case 2:
				proj.gamecode[gchar++] = (char) ('A' + (char)rand.nextInt(26));
				break;
			}
		}
		
		proj.timestamp = ZonedDateTime.now();
		
		SecureRandom srand = new SecureRandom();
		srand.nextBytes(proj.aeskey);
		
		
		proj.mem_limit = DEFO_MEM_LIMIT;
		proj.packages = new ArrayList<MebPackage>();
		
		return proj;
	}
	
	public static MuenProject readProject(String proj_path) throws IOException, UnsupportedFileTypeException {
		MuenProject proj = new MuenProject();
		
		//Read project file
		FileBuffer file = FileBuffer.createBuffer(proj_path, false);
		
		//Header
		if(file.findString(0, 0x10, MAGIC_PROJ) != 0L) throw new UnsupportedFileTypeException("Muen Project magic number not found!");
		long cpos = 8;
		int ver = Short.toUnsignedInt(file.shortFromFile(cpos)); cpos+=2;
		int flags = Short.toUnsignedInt(file.shortFromFile(cpos)); cpos += 2;
		//For now, not bothering with compression.
		if((flags & 0x1) != 0) {
			//Compressed
			//TODO
		}
		
		if(ver >= 2) {
			proj.defo_pkg = Short.toUnsignedInt(file.shortFromFile(cpos)); cpos+=2;
			proj.defo_grp = file.intFromFile(cpos); cpos += 4;
		}
		
		//Data
		SerializedString ss = file.readVariableLengthString("UTF8", cpos, BinFieldSize.WORD, 2);
		proj.build_dir = ss.getString();
		cpos += ss.getSizeOnDisk();
		
		int pkgcount = file.intFromFile(cpos); cpos+=4;
		proj.packages = new ArrayList<MebPackage>(pkgcount+8);
		for(int i = 0; i < pkgcount; i++) {
			int itemcount = file.intFromFile(cpos); cpos+=4;
			MebPackage pkg = new MebPackage();
			pkg.setAssetPkgIndexTo(i);
			proj.packages.add(pkg);
			
			ss = file.readVariableLengthString(cpos, BinFieldSize.WORD, 2);
			pkg.setName(ss.getString());
			cpos += ss.getSizeOnDisk();
			
			ss = file.readVariableLengthString(cpos, BinFieldSize.WORD, 2);
			pkg.setTargetPath(ss.getString());
			cpos += ss.getSizeOnDisk();
			
			for(int j = 0; j < itemcount; j++) {
				MebAsset a = new MebAsset();
				a.setTypeID(file.intFromFile(cpos)); cpos+=4;
				a.setGroupID(file.intFromFile(cpos)); cpos+=4;
				a.setInstanceID(file.longFromFile(cpos)); cpos+=8;
				
				ss = file.readVariableLengthString("UTF8", cpos, BinFieldSize.WORD, 2);
				a.setLocalName(ss.getString());
				cpos += ss.getSizeOnDisk();
				
				ss = file.readVariableLengthString("UTF8", cpos, BinFieldSize.WORD, 2);
				a.setSourcePath(ss.getString());
				cpos += ss.getSizeOnDisk();
				
				a.setPackageIndex(i);
				
				pkg.addAsset(a);
				proj.assetMap.put(a.getInstanceID(), a);
				proj.nameMap.put(a.getLocalName(), a);
			}
		}
		
		int gcount = file.intFromFile(cpos); cpos+=4;
		for(int i = 0; i < gcount; i++) {
			int gid = file.intFromFile(cpos); cpos+=4;
			MebGroup g = new MebGroup(gid);
			
			ss = file.readVariableLengthString("UTF8", cpos, BinFieldSize.WORD, 2);
			g.setGroupName(ss.getString());
			cpos += ss.getSizeOnDisk();
			
			proj.groupMap.put(gid, g);
		}
		
		//Link assets to groups
		for(MebAsset a : proj.assetMap.values()) {
			int gid = a.getGroupID();
			if(gid != 0) {
				MebGroup g = proj.groupMap.get(gid);
				if(g == null) {
					g = new MebGroup(gid);
					proj.groupMap.put(gid, g);
				}
				g.addAsset(a);
			}
		}
		
		//Read inibin
		String inibin_path = proj.build_dir + File.separator + FN_INIBIN;
		file = FileBuffer.createBuffer(inibin_path, false);
		
		cpos = 4; //Skip version
		for(int i = 0; i < 8; i++) {
			proj.gamecode[i] = (char)file.getByte(cpos++);
		}
		
		proj.ver_maj = file.shortFromFile(cpos); cpos+=2;
		proj.ver_min = file.shortFromFile(cpos); cpos+=2;
		proj.ver_bld = file.shortFromFile(cpos); cpos+=2;
		cpos += 2; //Skip flags as builder only does one type
		long sepoch = file.longFromFile(cpos); cpos+=8;
		proj.timestamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(sepoch), ZoneId.systemDefault());
		cpos += 4; //Reserved
		proj.aeskey = file.getBytes(cpos, cpos+16); cpos+=16;
		cpos +=16; //Not used
		proj.mem_limit = file.longFromFile(cpos); cpos+=8;
		
		//Path table not read by builder (more detailed version already in proj file)
		int sz = file.intFromFile(cpos); cpos+=4;
		cpos += sz;
		
		gcount = file.intFromFile(cpos); cpos+=4;
		for(int i = 0; i < gcount; i++) {
			int gid = file.intFromFile(cpos); cpos+=4;
			MebGroup g = proj.groupMap.get(gid);
			
			ss = file.readVariableLengthString(cpos, BinFieldSize.WORD, 2);
			if(g != null) g.setASSHPath(ss.getString());
			cpos += ss.getSizeOnDisk();
		}
		
		return proj;
	}
	
	/*----- Serialization -----*/
	
	public void writeInitBin() throws IOException {
		String path = build_dir + File.separator + FN_INIBIN;
		
		FileBuffer hdr = new FileBuffer(80, false);
		hdr.addToFile((int)VER_INIBIN);
		for(int i = 0; i < 8; i++) hdr.addToFile((byte)gamecode[i]);
		hdr.addToFile(ver_maj);
		hdr.addToFile(ver_min);
		hdr.addToFile(ver_bld);
		hdr.addToFile((short)0x0005);
		
		hdr.addToFile(timestamp.toEpochSecond());
		hdr.addToFile(0);
		for(int i = 0; i < 16; i++) hdr.addToFile(aeskey[i]);
		for(int i = 0; i < 4; i++) hdr.addToFile(0);
		hdr.addToFile(mem_limit);
		
		//Path table
		ArrayList<String> pathlist = new ArrayList<String>(packages.size()+1);
		ArrayList<Integer> offlist = new ArrayList<Integer>(packages.size()+1);
		int ptsz = 0;
		for(MebPackage pkg : packages) {
			String pp = pkg.getTargetPath();
			if(pp == null || pp.isEmpty()) {
				//Generate from name.
				String pn = pkg.getName();
				if(pn == null || pn.isEmpty()) {
					//Generate a name
					pn = Long.toHexString(new Random().nextLong());
					pkg.setName(pn);
				}
				pp = "./" + pn + ".assp";
				pkg.setTargetPath(pp);
			}
			pathlist.add(pp);
			offlist.add(ptsz);
			ptsz += pp.length() + 2;
			if((ptsz % 2) != 0) ptsz++;
		}
		int otsz = offlist.size() << 2;
		FileBuffer pathtbl = new FileBuffer(8 + otsz + ptsz, false);
		pathtbl.addToFile(4 + otsz+ptsz);
		pathtbl.addToFile(pathlist.size());
		for(Integer o : offlist) pathtbl.addToFile(o+otsz);
		for(String p : pathlist) pathtbl.addVariableLengthString(p, BinFieldSize.WORD, 2);
		
		//Group table
		List<Integer> glist = new ArrayList<Integer>(groupMap.size()+1);
		glist.addAll(groupMap.keySet());
		Collections.sort(glist);
		int alloc = 4;
		for(Integer gid : glist) {
			MebGroup g = groupMap.get(gid);
			String p = g.getASSHPath();
			if(p == null || p.isEmpty()) {
				//Generate from group name
				String gname = g.getGroupName();
				if(gname == null || gname.isEmpty()) {
					gname = "G" + Integer.toHexString(gid);
					g.setGroupName(gname);
				}
				p = "./" + gname + ".assh";
				g.setASSHPath(p);
			}
			alloc += 4 + p.length() + 2;
			if((alloc % 2) != 0) alloc++;
		}
		FileBuffer gtbl = new FileBuffer(alloc, false);
		gtbl.addToFile(glist.size());
		for(Integer gid : glist) {
			gtbl.addToFile(gid);
			MebGroup g = groupMap.get(gid);
			String p = g.getASSHPath();
			gtbl.addVariableLengthString(p, BinFieldSize.WORD, 2);
		}
		
		//Write
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
		hdr.writeToStream(bos);
		pathtbl.writeToStream(bos);
		gtbl.writeToStream(bos);
		bos.close();
	}
	
	public void writeProjFile(String path) throws IOException {
		//TODO No compression atm
		
		FileBuffer hdr = new FileBuffer(30 + (build_dir.length() << 2), false);
		hdr.printASCIIToFile(MAGIC_PROJ);
		hdr.addToFile(VER_PROJ);
		hdr.addToFile((short)0x0); //No flags
		
		//Version 2+ - default pkg and grp
		hdr.addToFile((short)defo_pkg);
		hdr.addToFile(defo_grp);
		
		//Path
		//System.err.println("DEBUG: build_dir = " + build_dir);
		hdr.addVariableLengthString("UTF8", build_dir, BinFieldSize.WORD, 2);
		hdr.addToFile(packages.size());
		
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
		hdr.writeToStream(bos);
		
		//Arc table.
		for(MebPackage pkg : packages) pkg.writeToProjFile(bos);
		
		//Group table.
		List<Integer> gidlist = new ArrayList<Integer>(groupMap.size()+1);
		gidlist.addAll(groupMap.keySet());
		Collections.sort(gidlist);
		int alloc = 4;
		for(Integer gid : gidlist) {
			MebGroup g = groupMap.get(gid);
			alloc += 4;
			String gname = g.getGroupName();
			if(gname == null || gname.isEmpty()) {
				gname = "G" + Integer.toHexString(gid);
				g.setGroupName(gname);
			}
			alloc += 3 + (gname.length() << 2);
		}
		hdr = new FileBuffer(alloc, false);
		hdr.addToFile(gidlist.size());
		for(Integer gid : gidlist) {
			MebGroup g = groupMap.get(gid);
			hdr.addToFile(gid);
			hdr.addVariableLengthString("UTF8", g.getGroupName(), BinFieldSize.WORD, 2);
		}
		hdr.writeToStream(bos);
		
		bos.close();
	}
	
	/*----- Getters -----*/
	
	public String getBuildDir() {return build_dir;}
	public char[] getGamecode() {return gamecode;}
	public short getMajorVersion() {return ver_maj;}
	public short getMinorVersion() {return ver_min;}
	public short getBuildVersion() {return ver_bld;}
	public ZonedDateTime getTimestamp() {return timestamp;}
	public byte[] getAESKey() {return aeskey;}
	public long getMemLimit() {return mem_limit;}
	
	public String getGamecodeString() {
		StringBuilder sb = new StringBuilder(10);
		for(char c : gamecode)sb.append(c);
		return sb.toString();
	}

	public String getVersionString() {
		StringBuilder sb = new StringBuilder(128);
		sb.append(ver_maj + ".");
		sb.append(ver_min + ".");
		sb.append(ver_bld);
		return sb.toString();
	}
	
	public List<MebAsset> getAllAssets(){
		ArrayList<MebAsset> list = new ArrayList<MebAsset>(assetMap.size()+1);
		list.addAll(assetMap.values());
		Collections.sort(list);
		return list; 
	}
	
	public List<MebGroup> getAllGroups(){
		ArrayList<MebGroup> list = new ArrayList<MebGroup>(groupMap.size()+1);
		list.addAll(groupMap.values());
		Collections.sort(list);
		return list; 
	}
	
	public List<MebPackage> getAllPackages(){
		ArrayList<MebPackage> list = new ArrayList<MebPackage>(packages.size()+1);
		list.addAll(packages);
		return list; 
	}
	
	/*----- Setters -----*/
	
	public void setDefaultPackage(int package_idx) {this.defo_pkg = package_idx;}
	public void setDefaultGroup(int group_id) {this.defo_grp = group_id;}
	
	public void updateTimestamp() {timestamp = ZonedDateTime.now();}
	
	public void rebuildNameMap() {
		nameMap.clear();
		List<MebAsset> list = new ArrayList<MebAsset>(assetMap.size()+1);
		list.addAll(assetMap.values());
		for(MebAsset a: list) {
			nameMap.put(a.getLocalName(), a);
		}
	}
	
	public int newGroup(String name, String path) {
		if(name == null) return 0;
		int id = name.hashCode();
		while(groupMap.containsKey(id)) id++;
		
		MebGroup grp = new MebGroup(id, name, path);
		groupMap.put(id, grp);
		
		updateTimestamp();
		return id;
	}
	
	public int newPackage(String name, String path) {
		MebPackage pkg = new MebPackage();
		pkg.setName(name);
		pkg.setTargetPath(path);
		int idx = packages.size();
		pkg.setAssetPkgIndexTo(idx);
		packages.add(pkg);
		
		//System.err.println("DEBUG || Package " + pkg.getName() + " added at index " + idx);
		updateTimestamp();
		return idx;
	}
	
	public MebAsset newAsset(int pkg, int group, String srcpath) {
		//Generate instance id from src path.
		byte[] hash = FileUtils.getSHA1Hash(srcpath.getBytes());
		long iid = 0;
		for(int i = 0; i < 8; i++) {
			iid <<= 8;
			iid |= Byte.toUnsignedLong(hash[i]);
		}
		
		MebAsset a = new MebAsset();
		a.setInstanceID(iid);
		a.setSourcePath(srcpath);
		a.setPackageIndex(pkg);
		MebGroup g = groupMap.get(group);
		if(g == null) {
			g = new MebGroup(group);
			groupMap.put(group, g);
		}
		g.addAsset(a);
		
		if(pkg >= 0 && packages.size() > pkg) {
			MebPackage p = packages.get(pkg);
			p.addAsset(a);
		}
		
		assetMap.put(iid, a);
		updateTimestamp();
		return a;
	}
	
	public void moveAssetToPackage(MebAsset a, int pkg) {
		if(a == null || pkg < 0 || pkg >= packages.size()) return;
		
		int nowpack = a.getPackageIndex();
		if(nowpack >= 0 && nowpack < packages.size()) {
			//Remove from current package
			MebPackage pack = packages.get(a.getPackageIndex());	
			pack.removeAsset(a.getInstanceID());
		}
		a.setPackageIndex(pkg);
		MebPackage pack = packages.get(pkg);
		pack.addAsset(a);
		updateTimestamp();
	}
	
	public MebAsset removeAsset(long inst_uid) {
		MebAsset a = assetMap.remove(inst_uid);
		if(a == null) return null;
		
		//Remove from name map.
		nameMap.remove(a.getLocalName());
		
		//Remove from package
		int pidx = a.getPackageIndex();
		if(pidx >= 0 && pidx < packages.size()) {
			MebPackage p = packages.get(pidx);
			p.removeAsset(a.getInstanceID());
		}
		
		//Remove from group
		int gid = a.getGroupID();
		MebGroup g = groupMap.get(gid);
		if(g != null) {
			g.removeAsset(a.getInstanceID());
		}
		
		updateTimestamp();
		return a;
	}
	
	public MebGroup removeGroup(int gid) {
		MebGroup g = groupMap.remove(gid);
		if(g == null) return null;
		
		//Move all assets within to the default group.
		MebGroup defo = groupMap.get(defo_grp);
		if(defo == null) {
			if(defo_grp == 0) defo_grp = 0xdef0def0;
			while(groupMap.containsKey(defo_grp)) defo_grp++;
			
			defo = new MebGroup(defo_grp, "default_group", "./defogrp.assh");
			groupMap.put(defo_grp, defo);
		}
		
		Collection<MebAsset> alist = g.getAssets();
		for(MebAsset a : alist) {
			defo.addAsset(a);
		}
		
		updateTimestamp();
		return g;
	}
	
	public MebPackage removePackage(int pack_idx) {
		if(packages == null || pack_idx < 0 || pack_idx >= packages.size()) return null;
		MebPackage pack = packages.get(pack_idx);
		
		//Remove from list...
		int i = 0;
		int j = 0;
		ArrayList<MebPackage> plist = new ArrayList<MebPackage>(packages.size()+1);
		for(MebPackage pkg : packages) {
			if(i != pack_idx) {
				plist.add(pkg);
				pkg.setAssetPkgIndexTo(j++);
			}
			i++;
		}
		packages = plist;
		
		//Move assets to default package
		pack.setAssetPkgIndexTo(defo_pkg);
		
		updateTimestamp();
		return pack;
	}
	
	public void removeEmptyGroups() {
		List<Integer> keepgroups = new LinkedList<Integer>();
		for(Integer gid : groupMap.keySet()) {
			MebGroup grp = groupMap.get(gid);
			if(grp.getAssetCount() > 0) keepgroups.add(gid);
		}
		
		Map<Integer, MebGroup> nmap = new TreeMap<Integer, MebGroup>();
		for(Integer k : keepgroups) {
			nmap.put(k, groupMap.get(k));
		}
		
		groupMap.clear();
		groupMap = nmap;
		updateTimestamp();
	}
	
	public void removeEmptyPackages() {
		int i = 0;
		ArrayList<MebPackage> plist = new ArrayList<MebPackage>(packages.size()+1);
		for(MebPackage pkg : packages) {
			if(pkg.countAssets() > 0) {
				plist.add(pkg);
				pkg.setAssetPkgIndexTo(i++);
			}
		}
		packages = plist;
		updateTimestamp();
	}
	
	public void clean() {
		removeEmptyPackages();
		removeEmptyGroups();
		rebuildNameMap();
	}

	/*----- Build -----*/
	
	public byte[] genAESIV() {
		String ivcommon = "muEngine";
		byte[] iv = new byte[16];
		for(int i = 0; i < 8; i++) iv[i] = (byte)ivcommon.charAt(i);
		for(int i = 0; i < 8; i++) iv[i+8] = (byte)gamecode[i];
		
		return iv;
	}
	
	public String pathToAbs(String projpath) {
		if(projpath.startsWith("./")) {
			if(File.separatorChar == '/') return build_dir + projpath.substring(1);
			return build_dir + projpath.substring(1).replace('/', File.separatorChar);
		}
		
		return projpath.replace('/', File.separatorChar);
	}
	
	public boolean buildMe() throws IOException {
	
		ver_bld++;
		
		//Inibin
		writeInitBin();
		
		//Packages
		for(MebPackage pkg : packages) {
			//Make sure file directories exist.
			String targpath = pkg.getTargetPath();
			targpath = pathToAbs(targpath);
			int lastslash = targpath.lastIndexOf(File.separatorChar);
			if(lastslash >= 0) {
				String targdir = targpath.substring(0, lastslash);
				if(!FileBuffer.directoryExists(targdir)) Files.createDirectories(Paths.get(targdir));
			}
			
			//Init hasher
			MessageDigest sha = null;
			try {sha = MessageDigest.getInstance("SHA-256");} 
			catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				throw new IOException("Hash init failed!");
			}
			
			//It'll first write to a temp file then copy back (so can hash)
			String temppath = FileBuffer.generateTemporaryPath("muenbuilder_assp");
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(temppath));
			
			//Write
			long off = 0;
			Collection<MebAsset> alist = pkg.getAssets();
			for(MebAsset a : alist) {
				a.setOffset(off+48);
				String srcpath = a.getSourcePath();
				
				MessageDigest shaf = null;
				try {shaf = MessageDigest.getInstance("SHA-256");} 
				catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
					throw new IOException("Hash init failed!");
				}
				
				//Try to get writer
				AssetWriterCallback aw = AssetWriterCallback.getWriter(a.getTypeID());
				if(aw != null) {
					aw.writeAssetTo(a, bos, sha);
					
					//a.setFullSize(szs[0]);
					//a.setCompressedSize(szs[1]);
					//a.setCompFlag(a.getFullSize() != a.getCompressedSize());
					off += a.getCompressedSize();
				}
				else {
					//Copy the file as-is.
					long size = FileBuffer.fileSize(srcpath);
					long sz = 0;
					FileBuffer buff = FileBuffer.createBuffer(srcpath);
					
					if(DEBUGOP_NO_ASSP_XOR == 0) {
						//Write 16 bytes at a time.
						byte[] xorval = a.getXorVal();
						int[] xori = new int[16];
						for(int i = 0; i < 16; i++) xori[i] = Byte.toUnsignedInt(xorval[i]);
						long cpos = 0;
						while(cpos < size) {
							for(int i = 0; i < 16; i++) {
								int b = 0;
								if(cpos < size) b = Byte.toUnsignedInt(buff.getByte(cpos++));
								shaf.update((byte)b);
								int x = b ^ xori[i];
								sz++;
								bos.write(x);
								sha.update((byte)x);
							}
						}
					}
					else {
						long cpos = 0;
						while(cpos < size) {
							byte b = buff.getByte(cpos++);
							sha.update(b);
							shaf.update(b);
							bos.write(Byte.toUnsignedInt(b));
						}
						sz = size;
					}
					
					a.setCompFlag(false);
					a.setFullSize(sz);
					a.setCompressedSize(sz);
					a.setHash(shaf.digest());
					
					off += sz;
				}
				
			}
			
			bos.close();
			
			//Header
			byte[] hash = sha.digest();
			FileBuffer hdr = new FileBuffer(48,false);
			hdr.printASCIIToFile(MAGIC_ASSP);
			hdr.addToFile(VER_ASSP);
			hdr.addToFile(0L);
			for(int i = 0; i < 32; i++) hdr.addToFile(hash[i]);
			
			bos = new BufferedOutputStream(new FileOutputStream(targpath));
			hdr.writeToStream(bos);
			FileBuffer buff = FileBuffer.createBuffer(temppath);
			buff.writeToStream(bos);
			bos.close();
			Files.delete(Paths.get(temppath));
		}
		
		//Groups
		byte[] iv = this.genAESIV();
		for(MebGroup g : groupMap.values()) {
			String targpath = g.getASSHPath();
			targpath = pathToAbs(targpath);
			MebAssetPack.writeAssetPackHeader(g, targpath, aeskey, iv);
		}
		
		return true;
	}
	
}
