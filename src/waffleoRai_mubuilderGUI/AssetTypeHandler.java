package waffleoRai_mubuilderGUI;

import java.awt.Frame;

import javax.swing.JPanel;

import waffleoRai_mubuilder.MebAsset;

public interface AssetTypeHandler {
	
	public JPanel genPreviewPanel(String srcPath);
	public boolean isEditable();
	public void editButtonCallback(Frame parent, MebAsset asset);
	
	public static AssetTypeHandler getHandler(int type) {
		//TODO
		return null;
	}

}
