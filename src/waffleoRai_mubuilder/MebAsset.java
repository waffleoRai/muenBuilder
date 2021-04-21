package waffleoRai_mubuilder;

public class MebAsset implements Comparable<MebAsset>{

	/*----- Constants -----*/
	
	/*----- Static Variables -----*/
	
	public static boolean sort_by_name = false;
	
	/*----- Instance Variables -----*/
	
	private int type_id;
	private int group_id;
	private long instance_id;
	
	private String build_name;
	private String export_name; //If set, visible to engine at runtime. MUST be ASCII
	private String src_path;
	
	private int pkg_idx;
	
	//Temp fields for building
	private boolean iscomped;
	private long offset;
	private long fullsz;
	private long compsz;
	private byte[] hash;
	
	/*----- Init -----*/
	
	public MebAsset() {
		pkg_idx = -1;
	}
	
	public MebAsset(int typeID, String name, String srcpath) {
		type_id = typeID;
		src_path = srcpath;
		
		int hash1 = name.hashCode();
		int hash2 = srcpath.hashCode();
		
		instance_id = ((long)hash1) << 32;
		instance_id |= (long)hash2;
		
		pkg_idx = -1;
	}
	
	/*----- Getters -----*/
	
	public int getTypeID() {return type_id;}
	public int getGroupID() {return group_id;}
	public long getInstanceID() {return instance_id;}
	public String getLocalName() {return build_name;}
	public String getExportName() {return export_name;}
	public String getSourcePath() {return src_path;}
	public int getPackageIndex() {return pkg_idx;}

	public boolean getCompFlag() {return iscomped;}
	public long getOffset() {return offset;}
	public long getFullSize() {return fullsz;}
	public long getCompressedSize() {return compsz;}
	public byte[] getHash() {return hash;}
	
	public String toString() {
		return this.build_name + String.format("(%08x:%08x:%016x)", type_id, group_id, instance_id);
	}
	
	public boolean isTable() {
		//Needs index to access references??
		
		//This is a very inefficient way to do this but eh.
		switch(type_id) {
		case MebTypes._AUT_DEF: return true;
		case MebTypes._EFT_DEF: return true;
		case MebTypes._IMT_DEF: return true;
		case MebTypes._SPR_RAW: return true;
		case MebTypes._STT_U16: return true;
		case MebTypes._STT_UN8: return true;
		case MebTypes._STT_UNI: return true;
		}
		
		return false;
	}
	
	public byte[] getXorVal() {
		byte[] arr = new byte[16];
		
		long l = instance_id;
		for(int i = 0; i < 8; i++) {
			arr[i] = (byte)(l&0xFF);
			l = l >>> 8;
		}
		
		int n = group_id;
		for(int i = 0; i < 4; i++) {
			arr[i+8] = (byte)(n&0xFF);
			n = n >>> 8;
		}
		
		n = type_id;
		for(int i = 0; i < 4; i++) {
			arr[i+12] = (byte)(n&0xFF);
			n = n >>> 8;
		}
		
		return arr;
	}
	
	/*----- Setters -----*/
	
	public void setTypeID(int id) {type_id = id;}
	public void setGroupID(int id) {group_id = id;}
	public void setInstanceID(long id) {instance_id = id;}
	public void setLocalName(String name) {build_name = name;}
	public void setExportName(String name) {export_name = name;}
	public void setSourcePath(String path) {src_path = path;}
	public void setPackageIndex(int val) {pkg_idx = val;}
	
	public void setCompFlag(boolean b) {iscomped = b;}
	public void setOffset(long val) {offset = val;}
	public void setFullSize(long val) {fullsz = val;}
	public void setCompressedSize(long val) {compsz = val;}
	public void setHash(byte[] b) {hash = b;}
	
	/*----- Sorting -----*/
	
	public boolean equals(Object o) {
		if(o == null) return false;
		if(this == o) return true;
		if(!(o instanceof MebAsset)) return false;
		
		MebAsset other = (MebAsset)o;
		if(this.instance_id != other.instance_id) return false;
		if(this.group_id != other.group_id) return false;
		if(this.type_id != other.type_id) return false;
		
		return true;
	}
	
	public int hashCode() {
		int lo = (int)instance_id;
		int hi = (int)(instance_id >> 32);
		return hi ^ lo ^ group_id;
	}
	
	public int compareTo(MebAsset o) {

		if(o == null) return 1;
		if(o == this) return 0;
		
		if(sort_by_name) {
			if(build_name == null) {
				if(o.build_name != null) return -1;
			}
			else {
				if(o.build_name == null) return 1;
				return this.build_name.compareTo(o.build_name);
			}
		}
		
		if(this.type_id != o.type_id) return Integer.compareUnsigned(this.type_id, o.type_id);
		if(this.group_id != o.group_id) return Integer.compareUnsigned(this.group_id, o.group_id);
		if(this.instance_id != o.instance_id) return Long.compareUnsigned(this.instance_id, o.instance_id);
		
		return 0;
	}
	
}
