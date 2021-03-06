package tree;

import java.util.ArrayList;

import org.jdom.Element;

public class TreeNode {
	public Element name = null;
	public Element concept = null;
	public ArrayList<TreeNode> children = null;
	public ArrayList<TreeNode> constantsChildren = null;
	
	
	public TreeNode(Element name, Element concept,
			ArrayList<TreeNode> children, ArrayList<TreeNode> constantsChildren) {
		this.name = name;
		this.concept = concept;
		if(children != null)
			this.children = children;
		else
			this.children =  new ArrayList<TreeNode>();
		if(constantsChildren !=null)
			this.constantsChildren = constantsChildren;
		else
			this.constantsChildren = new ArrayList<TreeNode>();
	}
	
	public String toString (){
		if (name.getName() == "CONCEPT" && name.getAttributeValue("text").equals("Thing")){
			return "THING";
		}
		return name.getAttributeValue("text");
	}
}
