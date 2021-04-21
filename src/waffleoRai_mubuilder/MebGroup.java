package waffleoRai_mubuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class MebGroup implements Comparable<MebGroup>{

	/*----- Constants -----*/
	
	/*----- Instance Variables -----*/
	
	private int group_id;
	private String group_name;
	private String assh_path; //Relative, Unix style
	
	private Map<Long, MebAsset> assetMap;
	
	/*----- Initialization -----*/
	
	public MebGroup() {
		this(new Random().nextInt());
	}
	
	public MebGroup(int id) {
		group_id = id;
		group_name = "G" + Integer.toHexString(group_id);
		assh_path = "./" + group_name + ".assh";
		assetMap = new TreeMap<Long, MebAsset>();
	}
	
	public MebGroup(int id, String name, String asshPath) {
		group_id = id;
		group_name = name;
		assh_path = asshPath;
		assetMap = new TreeMap<Long, MebAsset>();
	}
	
	/*----- Getters -----*/
	
	public int getGroupID() {return group_id;}
	public String getGroupName() {return group_name;}
	public String getASSHPath() {return assh_path;}
	
	public int getAssetCount() {
		return assetMap.size();
	}
	
	public Collection<MebAsset> getAssets(){
		List<MebAsset> list = new ArrayList<MebAsset>(assetMap.size()+1);
		list.addAll(assetMap.values());
		Collections.sort(list);
		return list;
	}
	
	/*----- Setters -----*/
	
	public void setGroupName(String name) {group_name = name;}
	public void setASSHPath(String path) {assh_path = path;}
	
	public void addAsset(MebAsset a) {
		if(a == null) return;
		a.setGroupID(group_id);
		assetMap.put(a.getInstanceID(), a);
	}
	
	public boolean removeAsset(long inst_id) {
		MebAsset a = assetMap.remove(inst_id);
		if(a != null) {
			a.setGroupID(0);
			return true;
		}
		return false;
	}
	
	/*----- Compare -----*/
	
	public int hashCode() {return this.group_id;}
	
	public boolean equals(Object o) {
		if(o == null) return false;
		if(o == this) return true;
		
		if(!(o instanceof MebGroup)) return false;
		
		MebGroup og = (MebGroup)o;
		return og.group_id == this.group_id;
	}

	public int compareTo(MebGroup o) {
		//Sort by name, then id
		
		if(o == null) return 1;
		if(this == o) return 0;
		
		if(this.group_name == null) {
			if(o.group_name != null) return -1;
		}
		else {
			int c = group_name.compareTo(o.group_name);
			if(c != 0) return c;
		}
		
		return Integer.compareUnsigned(group_id, o.group_id);
	}
	
}
