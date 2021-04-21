package waffleoRai_mubuilderGUI.nodes;

import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import waffleoRai_Utils.Treenumeration;
import waffleoRai_mubuilder.MebAsset;

public class MebAssetNode implements TreeNode, Comparable<MebAssetNode>{
	
	//For GUI lists and trees
	
	public MebNode parent; //Only one at a time!
	public MebAsset asset;
	
	public MebAssetNode(MebNode p, MebAsset a) {
		parent = p;
		asset = a;
	}

	public int compareTo(MebAssetNode o) {
		if(o == null) return 1;
		return asset.compareTo(o.asset);
	}

	public TreeNode getChildAt(int childIndex) {return null;}
	public int getChildCount() {return 0;}
	public TreeNode getParent() {return parent;}
	public int getIndex(TreeNode node) {return -1;}
	public boolean getAllowsChildren() {return false;}
	public boolean isLeaf() {return true;}

	public Enumeration<TreeNode> children() {
		TreeNode[] n = null;
		return new Treenumeration(n);
	}
	
	public String toString() {
		return asset.getLocalName() + String.format(" (%08x:%08x:&016x)", asset.getTypeID(), asset.getGroupID(), asset.getInstanceID());
	}
	

}
