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

public class TW_PNG extends AssetWriterCallback{

	public boolean writeAssetTo(MebAsset a, OutputStream os, MessageDigest sha) {
		//Can read in any Java compatible image.
		try {
			BufferedImage img = ImageIO.read(new File(a.getSourcePath()));
			
			//Have to write to a temp first so can get hash & size...
			String tpath = FileBuffer.generateTemporaryPath("muenb_png_writer");
			ImageIO.write(img, "png", new File(tpath));

			a.setCompFlag(false);
			a.setCompressedSize(FileBuffer.fileSize(tpath));
			a.setFullSize(a.getCompressedSize());
			
			FileBuffer buff = FileBuffer.createBuffer(tpath);
			a.setHash(FileUtils.getSHA256Hash(buff.getBytes())); //Generously assuming smallish file..........
			sha.digest(buff.getBytes());
			
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
