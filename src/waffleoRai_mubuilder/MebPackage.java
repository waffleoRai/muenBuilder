package waffleoRai_mubuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import waffleoRai_Utils.BinFieldSize;
import waffleoRai_Utils.FileBuffer;

public class MebPackage {
	
	/*----- Constants -----*/
	
	/*----- Instance Variables -----*/
	
	private int index;
	
	private String pkg_name;
	private String pkg_path;
	
	private Map<Long, MebAsset> assetMap;
	
	/*----- Initialization -----*/
	
	public MebPackage() {
		assetMap = new TreeMap<Long, MebAsset>();
	}
	
	/*----- Serialization -----*/
	
	public void writeToProjFile(OutputStream str) throws IOException {

		if(pkg_name == null || pkg_name.isEmpty()) pkg_name = Long.toHexString(new Random().nextLong());
		if(pkg_path == null || pkg_path.isEmpty()) pkg_path = "./" + pkg_name + ".assp";
		
		int allocsz = 4 + pkg_name.length() + 3 + pkg_path.length() + 3;
		List<Long> aidlist = new ArrayList<Long>(assetMap.size()+1);
		aidlist.addAll(assetMap.keySet());
		Collections.sort(aidlist);
		for(Long aid : aidlist) {
			MebAsset a = assetMap.get(aid);
			allocsz += 16; //TGI
			String aname = a.getLocalName();
			if(aname == null || aname.isEmpty()) {
				aname = Long.toHexString(a.getInstanceID());
				a.setLocalName(aname);
			}
			//a src can be empty, but there will be issues building...
			allocsz += 3 + (aname.length() << 2);
			
			String asrc = a.getSourcePath();
			allocsz += 2;
			if(asrc != null) allocsz += 1 + (asrc.length() << 2);
		}
		
		FileBuffer buff = new FileBuffer(allocsz, false);
		buff.addToFile(assetMap.size());
		buff.addVariableLengthString(pkg_name, BinFieldSize.WORD, 2);
		buff.addVariableLengthString(pkg_path, BinFieldSize.WORD, 2);
		
		for(Long aid : aidlist) {
			MebAsset a = assetMap.get(aid);
			buff.addToFile(a.getTypeID());
			buff.addToFile(a.getGroupID());
			buff.addToFile(a.getInstanceID());
			buff.addVariableLengthString("UTF8", a.getLocalName(), BinFieldSize.WORD, 2);
			buff.addVariableLengthString("UTF8", a.getSourcePath(), BinFieldSize.WORD, 2);
		}
		
		//Write to output stream
		buff.writeToStream(str);
		
	}
	
	/*----- Getters -----*/
	
	public String getName() {return pkg_name;}
	public String getTargetPath() {return pkg_path;}
	public int getIndex() {return index;}
	
	public int countAssets() {return assetMap.size();}
	
	public Collection<MebAsset> getAssets(){
		List<MebAsset> list = new ArrayList<MebAsset>(assetMap.size()+1);
		list.addAll(assetMap.values());
		Collections.sort(list);
		return list;
	}
	
	/*----- Setters -----*/
	
	public void setName(String name) {pkg_name = name;}
	public void setTargetPath(String path) {pkg_path = path;}
	
	public void addAsset(MebAsset a) {
		if(a == null) return;
		assetMap.put(a.getInstanceID(), a);
	}
	
	public boolean removeAsset(long inst_id) {
		MebAsset a = assetMap.remove(inst_id);
		return a != null;
	}

	public void setAssetPkgIndexTo(int idx) {
		index = idx;
		for(MebAsset a : assetMap.values()) {
			a.setPackageIndex(idx);
		}
	}
	
}
