package waffleoRai_mubuilderGUI.nodes;

public class MebRootNode extends MebNode {

	private String str;
	
	public MebRootNode(String string) {
		str = string;
	}
	
	public int compareTo(MebNode o) {
		if(o == null) return 1;
		if(!(o instanceof MebRootNode)) return -1;
		
		return this.toString().compareTo(o.toString());
	}
	
	protected int sortVal() {
		return -1;
	}
	
	public String toString() {return str;}

}
