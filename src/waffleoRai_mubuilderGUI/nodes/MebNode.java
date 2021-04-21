package waffleoRai_mubuilderGUI.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.TreeNode;

import waffleoRai_Utils.Treenumeration;

public abstract class MebNode implements TreeNode, Comparable<MebNode>{
	
	protected Map<Long, MebAssetNode> children;
	protected ArrayList<MebAssetNode> clist;
	
	protected MebNode() {
		children = new HashMap<Long, MebAssetNode>();
	}
	
	protected void updateClist() {
		if(clist != null) clist.clear();
		if(clist == null) {
			clist = new ArrayList<MebAssetNode>(children.size()+1);
		}
		clist.addAll(children.values());
		Collections.sort(clist);
	}
	
	protected abstract int sortVal();
	
	public TreeNode getChildAt(int childIndex) {
		updateClist();
		if(childIndex < 0 || childIndex >= clist.size()) return null;
		return clist.get(childIndex);
	}

	public int getChildCount() {return children.size();}
	public TreeNode getParent() {return null;}
	public boolean getAllowsChildren() {return true;}
	public boolean isLeaf() {return !children.isEmpty();}

	public int getIndex(TreeNode node) {
		updateClist();
		int i = 0;
		for(MebAssetNode n : clist) {
			if(n == node) return i;
			i++;
		}
		return -1;
	}

	public Enumeration<TreeNode> children() {
		updateClist();
		ArrayList<TreeNode> copy = new ArrayList<TreeNode>(clist.size()+1);
		copy.addAll(clist);
		
		return new Treenumeration(copy);
	}

}
