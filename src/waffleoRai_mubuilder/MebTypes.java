package waffleoRai_mubuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MebTypes {
	
	//Image/Sprites
	public static final int _IMG_RAW = 0x11694d47; //Any image format, converted on packaging (defaults to this type if input is not png or jpg)
	public static final int _IMG_PNG = 0x11694d48; //Any image format, converted on packaging (defaults if input is png)
	public static final int _IMG_JPG = 0x11694d49; //Any image format, converted on packaging (defaults if input is jpg)
	
	public static final int _SPR_RAW = 0x11735052; //Directory of any Java ready image format ending in .sprs
	public static final int _TLS_RAW = 0x11746353; //Directory of any Java ready image format ending in .mutls
	public static final int _IM8_RAW = 0x11694d08; //
	public static final int _PLT_8BT = 0x11504c38; //Generated in builder
	public static final int _ANM_SPR = 0x11694e4d; //Generated in builder
	
	//Layout
	public static final int _TLM_DEF = 0x1174634d; //Generated in builder
	public static final int _TXB_DEF = 0x1a545842; //Generated in builder
	public static final int _LYO_DEF = 0x1a4c594f; //Generated in builder
	
	//Font
	public static final int _FNT_TTF = 0x464e5474; //Any ttf import
	public static final int _FNT_OTF = 0x464e546f; //Any otf import
	
	//Strings
	public static final int _STT_UNI = 0x53747200; //Converted from properly formatted tsv (tbl with subtbls that are either UTF8/UTF16)
	public static final int _STT_UN8 = 0x53747208; //Converted from properly formatted tsv
	public static final int _STT_U16 = 0x53747210; //Converted from properly formatted tsv
	
	//Tables
	public static final int _IMT_DEF = 0x2a696d54; //Converted from properly formatted tsv
	public static final int _AUT_DEF = 0x2a617554; //Converted from properly formatted tsv
	public static final int _EFT_DEF = 0x2aefefef; //Converted from properly formatted tsv - basic effect tbl
	
	//Sound
	public static final int _SND_RAW = 0x5d734e44; //WAV or AIFF, converted on packaging (defaults to this type if wav or aif input)
	public static final int _SND_OGG = 0x5d734e46; //OGG input, though will accept FLAC, WAV, or AIFF which are converted on packaging
	public static final int _SND_FLC = 0x5d734e45; //FLAC input, though will accept OGG, WAV, or AIFF which are converted on packaging
	
	public static final int _SEQ_CTM = 0x5d734551; //Midi input, convert on packaging. Will also accept some game formats like seqp or sseq
	public static final int _BNK_CTM = 0x5d624e4b; //Convert on packaging - accepts sf2 mainly. Accepts some game formats like vab. May accept dls?
	public static final int _WAR_CTM = 0x52734152; //Directory of sounds ending in .muwar - all converted to flac on packaging
	
	//AV
	public static final int _MOV_MP4 = 0x436d7034; //mp4 import - no conversion done. Codecs must be readable by program
	public static final int _MOV_MKV = 0x436d6b76; //mkv import - no conversion done. Codecs must be readable by program
	public static final int _MOV_AVI = 0x43617669; //avi import - no conversion done. Codecs must be readable by program

	//Script
	public static final int _SCE_CUT = 0x2a736365; //Generated in builder
	
	//Code
	public static final int _DLL_NAT = 0xc0de6e74; //Imported DLL or SO. Relies on program to note arch/OS
	public static final int _DLL_NET = 0xc0de4323; //Imported .NET DLL
	public static final int _JAR_DEF = 0xc0de6a76; //Imported JAR
	public static final int _PYS_PY3 = 0xc0de7079; //Imported Python 3 script
	
	/*----- String Map -----*/
	
	private static Map<Integer, String> strid_map;
	private static Set<Integer> auto_comp; //Types to compress on packaging.
	
	static {
		populateStringMap();
		populateAutocompSet();
	}
	
	private static void populateAutocompSet() {
		auto_comp.add(_IMG_RAW);
		auto_comp.add(_SPR_RAW);
		auto_comp.add(_TLS_RAW);
		auto_comp.add(_IM8_RAW);
		auto_comp.add(_PLT_8BT);
		auto_comp.add(_ANM_SPR);
		auto_comp.add(_TLM_DEF);
		auto_comp.add(_TXB_DEF);
		auto_comp.add(_LYO_DEF);
		auto_comp.add(_STT_UNI);
		auto_comp.add(_STT_UN8);
		auto_comp.add(_STT_U16);
		auto_comp.add(_IMT_DEF);
		auto_comp.add(_AUT_DEF);
		auto_comp.add(_EFT_DEF);
		auto_comp.add(_SND_RAW);
		auto_comp.add(_BNK_CTM);
		auto_comp.add(_SCE_CUT);
		auto_comp.add(_PYS_PY3);
	}
	
	private static void populateStringMap() {
		strid_map = new TreeMap<Integer,String>();
		
		strid_map.put(_IMG_RAW, "_IMG RAW");
		strid_map.put(_IMG_PNG, "_IMG PNG");
		strid_map.put(_IMG_JPG, "_IMG JPEG");
		
		strid_map.put(_SPR_RAW, "_SPR");
		strid_map.put(_TLS_RAW, "_TLS");
		strid_map.put(_IM8_RAW, "_IM8");
		strid_map.put(_PLT_8BT, "_PLT 8BIT");
		strid_map.put(_ANM_SPR, "_ANM Sprite");
		
		strid_map.put(_TLM_DEF, "_TLM");
		strid_map.put(_TXB_DEF, "_TXB");
		strid_map.put(_LYO_DEF, "_LYO");
		
		strid_map.put(_FNT_TTF, "_FNT ttf");
		strid_map.put(_FNT_OTF, "_FNT otf");
		
		strid_map.put(_STT_UNI, "_STT UNI");
		strid_map.put(_STT_UN8, "_STT UTF8");
		strid_map.put(_STT_U16, "_STT UTF16LE");
		
		strid_map.put(_IMT_DEF, "_IMT");
		strid_map.put(_AUT_DEF, "_AUT");
		
		strid_map.put(_SND_RAW, "_SND Raw PCM");
		strid_map.put(_SND_OGG, "_SND OGG");
		strid_map.put(_SND_FLC, "_SND FLAC");
		
		strid_map.put(_SEQ_CTM, "_SEQ");
		strid_map.put(_BNK_CTM, "_BNK");
		strid_map.put(_WAR_CTM, "_WAR");
		
		strid_map.put(_MOV_MP4, "_MOV MP4");
		strid_map.put(_MOV_MKV, "_MOV MKV");
		strid_map.put(_MOV_AVI, "_MOV AVI");
		
		strid_map.put(_SCE_CUT, "_SCE");
		
	}
	
	public boolean autocompType(int type) {
		return auto_comp.contains(type);
	}
	
	public static List<Integer> getAllTypes(){
		List<Integer> list = new ArrayList<Integer>(strid_map.size()+1);
		list.addAll(strid_map.keySet());
		return list;
	}

	public static String getTypeString(int type) {
		return strid_map.get(type);
	}
	
	
}
