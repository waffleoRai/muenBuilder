package waffleoRai_mubuilderGUI.nodes;

import java.util.Collection;

import javax.swing.tree.TreeNode;

import waffleoRai_mubuilder.MebAsset;
import waffleoRai_mubuilder.MebPackage;

public class MebPackNode extends MebNode{
	
	public MebNode parent;
	private MebPackage pack;
	
	public MebPackNode(MebPackage p) {
		pack = p;
		
		//Populate child list
		Collection<MebAsset> alist = pack.getAssets();
		for(MebAsset a : alist) {
			MebAssetNode an = new MebAssetNode(this, a);
			super.children.put(a.getInstanceID(), an);
		}
	}
	
	protected int sortVal() {return 2;}
	public TreeNode getParent() {return parent;}
	
	public MebPackage getPackage() {return pack;}
	
	public void setParent(MebNode node) {parent = node;}
	
	public int compareTo(MebNode o) {
		if(o == null) return 1;
		
		if(!(o instanceof MebGroupNode)) {
			return sortVal() - o.sortVal();
		}
		
		MebPackNode other = (MebPackNode)o;
		return this.pack.getName().compareTo(other.pack.getName());
	}
	
	public String toString() {
		return pack.getName();
	}

}
