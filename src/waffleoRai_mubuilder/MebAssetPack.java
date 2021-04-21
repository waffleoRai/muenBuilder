package waffleoRai_mubuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import waffleoRai_Encryption.AES;
import waffleoRai_Utils.BinFieldSize;
import waffleoRai_Utils.FileBuffer;

public class MebAssetPack {
	
	public static void writeAssetPackHeader(MebGroup group, String abs_path, byte[] aeskey, byte[] iv) throws IOException {
		//Get asset list.
		Collection<MebAsset> alist = group.getAssets();
		
		//Three temp files
		FileBuffer hdr = new FileBuffer(32, false);
		int tnamesize = 0;
		
		for(MebAsset a : alist) {
			tnamesize += 2;
			String ename = a.getExportName();
			if(ename != null) tnamesize += ename.length()+1;
		}
		FileBuffer ntablehdr = new FileBuffer(4 + (alist.size() << 2), false);
		FileBuffer ntable = new FileBuffer(tnamesize, false);
		FileBuffer atable = new FileBuffer(4 + (90*alist.size()), false);
		
		//Do in reverse order, because that's easiest.
		atable.addToFile(alist.size());
		for(MebAsset a : alist) {
			atable.addToFile(a.getTypeID());
			atable.addToFile(a.getGroupID());
			atable.addToFile(a.getInstanceID());
			
			int flags = 0x0001; //This tool always xors unless in debug mode.
			if(MuenProject.DEBUGOP_NO_ASSP_XOR != 0) flags = 0x0000;
			if(a.getCompFlag()) flags |= 0x0002;
			atable.addToFile((short)flags);
			atable.addToFile((short)0);
			atable.addToFile(a.getPackageIndex());
			atable.addToFile(a.getOffset());
			atable.addToFile(a.getCompressedSize());
			atable.addToFile(a.getFullSize());
			byte[] hash = a.getHash();
			for(int i = 0; i < 32; i++) atable.addToFile(hash[i]);
		}
		
		//Name table.
		int noff = 4 + (alist.size() << 2);
		ntablehdr.addToFile(alist.size());
		for(MebAsset a : alist) {
			ntablehdr.addToFile(noff);
			String ename = a.getExportName();
			if(ename != null) {
				ntable.addVariableLengthString(ename, BinFieldSize.WORD, 2);
				noff += ename.length() + 2;
				if((noff % 2) != 0) noff++;
			}
			else {
				ntable.addToFile((short)0);
				noff+=2;
			}
		}
		
		//ASSH File Header
		hdr.printASCIIToFile(MuenProject.MAGIC_ASSH);
		hdr.addToFile(MuenProject.VER_ASSH);
		
		long hdrsz = 0x18;
		long offtoatbl = hdrsz + ntablehdr.getFileSize() + ntable.getFileSize();
		long tsz = offtoatbl + atable.getFileSize();
		hdr.addToFile(tsz);
		hdr.addToFile(hdrsz);
		hdr.addToFile(offtoatbl);
		
		//Write (encrypt if not in debug mode)
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(abs_path));
		if(MuenProject.DEBUGOP_NO_ASSH_ENCRYPT == 0) {
			//Needs encryption
			String temppath = FileBuffer.generateTemporaryPath("muenb_asshwriter");
			BufferedOutputStream bos2 = new BufferedOutputStream(new FileOutputStream(temppath));
			hdr.writeToStream(bos2);
			ntablehdr.writeToStream(bos2);
			ntable.writeToStream(bos2);
			atable.writeToStream(bos2);
			bos2.close();
			
			AES aes = new AES(aeskey);
			aes.initEncrypt(iv);
			aes.setCBC();
			
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(temppath));
			byte[] arr = new byte[16];
			long remain = FileBuffer.fileSize(temppath);
			while(remain > 16) {
				bis.read(arr);
				remain -= 16;
				byte[] enc = aes.encryptBlock(arr, false);
				bos.write(enc);
			}
			
			//Last block.
			Arrays.fill(arr, (byte)0);
			bis.read(arr, 0, (int)remain);
			byte[] enc = aes.encryptBlock(arr, true);
			bos.write(enc);
			
			bis.close();
			Files.deleteIfExists(Paths.get(temppath));
		}
		else {
			hdr.writeToStream(bos);
			ntablehdr.writeToStream(bos);
			ntable.writeToStream(bos);
			atable.writeToStream(bos);
		}
		bos.close();
		
	}
	
	public static void writeAssetPack(MebPackage pkg) {
		//TODO
	}
	
}
