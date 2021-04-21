package waffleoRai_poneater;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import waffleoRai_Files.FileNodeModifierCallback;
import waffleoRai_Files.tree.DirectoryNode;
import waffleoRai_Files.tree.FileNode;
import waffleoRai_Utils.BitStreamer;
import waffleoRai_Utils.FileBuffer;

public class PonsNSA {
	
	public static final int COMP_NONE = 0;
	public static final int COMP_SPB = 1; //Think this is for bmps?
	public static final int COMP_LZSS = 2;
	public static final int COMP_NBZ = 4;
	
	public static final String METAKEY_COMPTYPE = "NSA_COMPTYPE";
	public static final String METAKEY_DECOMPSZ = "NSA_DECOMPSZ";

	private DirectoryNode root; //It's technically flat, but might as well take advantage if it has organization.
	private boolean stat;
	
	private PonsNSA() {}
	
	public static PonsNSA readNSA(String path) throws IOException {
		if(path == null) return null;
		FileBuffer file = FileBuffer.createBuffer(path, true);
		PonsNSA nsa = new PonsNSA();
		nsa.root = new DirectoryNode(null, "");
		
		file.setCurrentPosition(0);
		int fcount = Short.toUnsignedInt(file.nextShort());
		long baseoff = Integer.toUnsignedLong(file.nextInt());
		
		System.err.println("Total Files: " + fcount);
		System.err.println("Base offset: 0x" + Long.toHexString(baseoff));
		
		//File table
		for(int i = 0; i < fcount; i++) {
			
			//Go through directories in path to reduce to file name
			//Making dir nodes as found
			DirectoryNode parent = nsa.root;
			
			//Name (look for null byte... assuming ascii for now because I'm lazy)
			StringBuilder sb = new StringBuilder(1024);
			byte b = -1;
			while((b = file.nextByte()) != 0) {
				char c = (char)b;
				if(c == '\\') {
					//See if that dir exists.
					String str = sb.toString();
					FileNode check = parent.getNodeAt("./" + str);
					DirectoryNode d = null;
					if(check != null && check instanceof DirectoryNode) {
						d = (DirectoryNode)check;
					}
					else {
						d = new DirectoryNode(parent, sb.toString());
					}
					parent = d;
					sb = new StringBuilder(1024);
				}
				else if(c == '+') continue;
				else sb.append(c);
			}
			
			FileNode fn = new FileNode(parent, sb.toString());
			int comp = Byte.toUnsignedInt(file.nextByte());
			long off = Integer.toUnsignedLong(file.nextInt());
			long sz = Integer.toUnsignedLong(file.nextInt());
			long decompsz = Integer.toUnsignedLong(file.nextInt());
			
			fn.setOffset(baseoff + off);
			fn.setLength(sz);
			if(comp != COMP_NONE) {
				fn.setMetadataValue(METAKEY_COMPTYPE, Integer.toString(comp));
				fn.setMetadataValue(METAKEY_DECOMPSZ, Long.toHexString(decompsz));
			}
			
			fn.setSourcePath(path);
			
			//System.err.print("File found: " + fn.getFullPath() + " @ 0x" + Long.toHexString(fn.getOffset()));
			/*if(comp != COMP_NONE) {
				System.err.print("Compressed file found: " + fn.getFullPath() + " @ 0x" + Long.toHexString(fn.getOffset()));
				System.err.print(" | Compression: ");
				switch(comp) {
				case COMP_SPB: System.err.print("SPB"); break;
				case COMP_LZSS: System.err.print("LZSS"); break;
				case COMP_NBZ: System.err.print("NBZ"); break;
				}
				System.err.println();
			}*/
			//System.err.println();
			
			//System.err.println("My name: " + fn.getFileName());
		}
		
		System.err.println("NSA Scanned. Nodes: " + nsa.root.getAllDescendants(true).size());
		return nsa;
	}
	
	public static void extractSPB(FileBuffer src, OutputStream dst) throws IOException {
		//DirectReader.cpp -> DirectReader::decodeSPB
		
		src.setCurrentPosition(0);
		int width = Short.toUnsignedInt(src.nextShort());
		int height = Short.toUnsignedInt(src.nextShort());
		
		int width_pad = (4 - width * 3 % 4) % 4;
		int total_size = (width * 3 + width_pad) * height + 54;
		
		//bmp header
		FileBuffer buff = new FileBuffer(54, false);
		buff.printASCIIToFile("BM");
		buff.addToFile(total_size);
		buff.addToFile(0); //Reserved
		buff.addToFile(54); //Data offset
		buff.addToFile(40); //Hdr size
		buff.addToFile(width);
		buff.addToFile(height);
		buff.addToFile((short)1); //planes
		buff.addToFile((short)24); //bit depth
		buff.addToFile(0);
		buff.addToFile(total_size - 54); //img size
		
		//0 fill to 54 bytes, then write.
		while(buff.getFileSize() < 54) buff.addToFile((byte)0x00);
		buff.writeToStream(dst);
		
		//Image data
		BitStreamer bitstr = new BitStreamer(src, 4, true);
		int ct = 0;
		int wh = width*height;
		int m = 0;
		int k = 0;
		byte[] decbuff = new byte[wh+4];
		byte[] outbuff = new byte[total_size - 54];
		for (int i = 0; i < 3; i++) {
	        ct = 0;
	        
	        //Get next 8 bits.
	        byte c = bitstr.readToByte(8);
	        decbuff[ct++] = c;
	        while(ct < wh) {
	        	int n = bitstr.readToInt(3);
	        	if(n == 0) {
	        		for(int j = 0; j < 4; j++) decbuff[ct++] = c;
	        		continue;
	        	}
	        	else if(n == 7) {
	        		m = bitstr.readToInt(1) + 1;
	        	}
	        	else m = n+2;
	        	
	        	for(int j = 0; j < 4; j++) {
	        		if(m==8) {
	        			c = bitstr.readToByte(8);
	        		}
	        		else {
	        			k = bitstr.readToInt(m);
	        			if ((k&0x1) != 0) c += (k >>> 1) + 1;
	        			else c -= (k >>> 1);
	        		}
	        		decbuff[ct++] = c;
	        	}
	        }
	        
	        int ooff = (width * 3 + width_pad) * (height - 1) + i;
	        int doff = 0;
	        
	        boolean flip = false;
	        for(int j = 0; j < height; j++) {
	        	if(flip) {
	        		for(int l = 0; l < width; ooff-=3) outbuff[ooff] = decbuff[doff++];
	        		ooff -= width * 3 + width_pad - 3;
	        	}
	        	else {
	        		for(int l = 0; l < width; ooff+=3) outbuff[ooff] = decbuff[doff++];
	        		ooff -= width * 3 + width_pad + 3;
	        	}
	        	flip = !flip;
	        } 
	    }
		
		dst.write(outbuff);
	}
	
	public static void extractLZSS(FileBuffer src, OutputStream dst, long oglen) {
		//TODO
		//#define EI 8
		//#define EJ 4
		//#define P 1  /* If match length <= P then output one character */
		//#define N (1 << EI)  /* buffer size */
		//#define F ((1 << EJ) + P)  /* lookahead buffer size */
		
		//N = 256 (1 << 8)
		//F = 17 [(1 << 4) + 1]
		//N-F = 239
		
		//Looks like...
		/*
		 * -> Check bit. 
		 * -> If set...
		 * 	-> Break if EOF
		 * 	-> Copy next 8 bits as byte to buffer
		 * -> If unset...
		 * 	-> Get 8 bits (break if EOF)
		 * 	-> Get 4 bits (break if EOF)
		 * 	-> Copy j+1 bytes from backpos specified by i.
		 *	-> ...Not clear to me what i is? Seems to be fixed pos in back buffer, rather than relative???
		 * 
		 */
		
		System.err.println("LZSS item, skipping for now...");
	}
	
	public static void extractNBZ(FileBuffer src, OutputStream dst) throws IOException {
		//src.writeToStream(dst);
		//Looks like it keeps the decomp size right there at the beginning of the stream...
		ByteArrayInputStream bastr = new ByteArrayInputStream(src.getBytes(4, src.getFileSize()));
		BZip2CompressorInputStream str = new BZip2CompressorInputStream(bastr);
		
		int b = -1;
		while((b = str.read()) != -1) dst.write(b);
		
		str.close();
	}
	
	public boolean extractTo(String dir) {

		if(root == null) return false;
		System.err.println("Root found. Nodes: " + root.getAllDescendants(true).size());
		stat = true;
		root.doForTree(new FileNodeModifierCallback() {

			public void doToNode(FileNode node) {
				if(!stat) return;
				if(node.isDirectory()) {
					//Create directory
					String me = node.getFullPath().replace("/", File.separator);
					me = dir + me;
					//System.err.println("Directory: " + me);
					if(!FileBuffer.directoryExists(me)) {
						try {
							Files.createDirectories(Paths.get(me));
						} 
						catch (IOException e) {
							stat = false;
							e.printStackTrace();
						}
					}
				}
				else {
					//Extract
					String path = node.getFullPath().replace("/", File.separator);
					path = dir + path;
					//System.err.println("Writing: " + path);
					try {
						FileBuffer dat = node.loadData();
						//Check compression
						String cstr = node.getMetadataValue(METAKEY_COMPTYPE);
						if(cstr != null) {
							int comp = Integer.parseInt(cstr);
							BufferedOutputStream bos = null;
							switch(comp) {
							case COMP_SPB:
								bos = new BufferedOutputStream(new FileOutputStream(path));
								extractSPB(dat, bos);
								bos.close();
								break;
							case COMP_LZSS:
								bos = new BufferedOutputStream(new FileOutputStream(path));
								long dlen = 0;
								String vstr = node.getMetadataValue(METAKEY_DECOMPSZ);
								if(vstr == null) {
									bos.close();
									throw new IOException("Need decomp len for LZSS!");
								}
								dlen = Long.parseLong(vstr, 16);
								extractLZSS(dat, bos, dlen);
								bos.close();
								break;
							case COMP_NBZ:
								bos = new BufferedOutputStream(new FileOutputStream(path));
								extractNBZ(dat, bos);
								bos.close();
								break;
							}

						}
						else dat.writeFile(path);
					} 
					catch (IOException e) {
						stat = false;
						e.printStackTrace();
					}
					
				}
			}
			
		});
		
		return stat;
	}
	
	public static void main(String[] args) {
		//TODO
		
		String dir = "D:\\usr\\bghos\\code\\mubuilds\\umineko0\\source";
		String inpath = dir + "\\q-mod\\arc5.nsa";
		String outpath = dir + "\\data\\qm_arc5";
		
		//Let's see what happens..
		
		try {
			PonsNSA nsa = PonsNSA.readNSA(inpath);
			nsa.extractTo(outpath);
		}
		catch(Exception x) {
			x.printStackTrace();
			System.exit(1);
		}
		
	}
	
}
