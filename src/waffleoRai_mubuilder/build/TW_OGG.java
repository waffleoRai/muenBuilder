package waffleoRai_mubuilder.build;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;

import javax.imageio.ImageIO;

import waffleoRai_Utils.FileBuffer;
import waffleoRai_Utils.FileUtils;
import waffleoRai_mubuilder.MebAsset;

public class TW_OGG extends AssetWriterCallback{
	
	public static boolean wav2ogg(String inpath, String outpath) {
		//TODO
		return false;
	}
	
	public boolean writeAssetTo(MebAsset a, OutputStream os, MessageDigest sha) {
		//Can read in any Java compatible image.
		try {
			//Convert input to ogg and put in temp file
			String tpath = FileBuffer.generateTemporaryPath("muenb_ogg_writer");
			//Right now, only accepts wav and ogg (I'll add others... eventually)
			String srcpath = a.getSourcePath().toLowerCase();
			if(srcpath.endsWith(".ogg")) {
				//Just copy.
				Files.copy(Paths.get(srcpath), Paths.get(tpath));
			}
			else if(srcpath.endsWith(".wav")) {
				//Will need to convert...
				//TODO
			}
			else {return false;}
			
			//Get hash and sizes from temp file
			a.setCompFlag(false);
			a.setCompressedSize(FileBuffer.fileSize(tpath));
			a.setFullSize(a.getCompressedSize());
			
			FileBuffer buff = FileBuffer.createBuffer(tpath);
			a.setHash(FileUtils.getSHA256Hash(buff.getBytes())); //Generously assuming smallish file..........
			sha.digest(buff.getBytes());
			
			//Copy off back to original output stream
			buff.writeToStream(os);
			Files.delete(Paths.get(tpath));
		}
		catch(Exception x) {
			x.printStackTrace();
			return false;
		}
		
		return true;
	}

}
