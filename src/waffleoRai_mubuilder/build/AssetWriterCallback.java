package waffleoRai_mubuilder.build;

import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

import waffleoRai_mubuilder.MebAsset;
import waffleoRai_mubuilder.MebTypes;

public abstract class AssetWriterCallback {

	//Returns {fullsize, compsize}
	public abstract boolean writeAssetTo(MebAsset a, OutputStream os, MessageDigest sha);
	
	/*--- Map by type ---*/
	
	private static Map<Integer, AssetWriterCallback> typemap;
	
	static {
		initTypemap();
	}
	
	private static void initTypemap() {
		typemap = new TreeMap<Integer, AssetWriterCallback>();

		typemap.put(MebTypes._IMG_PNG, new TW_PNG());
		
	}
	
	public static AssetWriterCallback getWriter(int typeid) {
		//If it returns null, just write the asset as-is
		AssetWriterCallback w = typemap.get(typeid);
		return w;
	}
	
	
}
