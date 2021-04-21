package waffleoRai_mubuilderGUI.nodes;

import java.util.Collection;

import javax.swing.tree.TreeNode;

import waffleoRai_mubuilder.MebAsset;
import waffleoRai_mubuilder.MebGroup;

public class MebGroupNode extends MebNode {

	private MebGroup group;
	private MebNode parent;
	
	public MebGroupNode(MebGroup g) {
		group = g;
		
		//Populate child list
		Collection<MebAsset> alist = group.getAssets();
		for(MebAsset a : alist) {
			MebAssetNode an = new MebAssetNode(this, a);
			super.children.put(a.getInstanceID(), an);
		}
	}
	
	protected int sortVal() {return 1;}
	public TreeNode getParent() {return parent;}
	
	public MebGroup getGroup() {return group;}
	
	public void setParent(MebNode node) {parent = node;}
	
	public int compareTo(MebNode o) {
		if(o == null) return 1;
		
		if(!(o instanceof MebGroupNode)) {
			return sortVal() - o.sortVal();
		}
		
		MebGroupNode other = (MebGroupNode)o;
		return this.group.getGroupName().compareTo(other.group.getGroupName());
	}
	
	public int hashCode() {return group.getGroupID();}
	
	public String toString() {
		return group.getGroupName() + " (" + Integer.toHexString(group.getGroupID()) + ")";
	}

}
