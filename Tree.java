package tree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Console;
import java.io.InputStreamReader;
import java.util.Arrays;


import org.jdom.output.XMLOutputter;
import org.jdom.Document;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import org.jdom.Element;

// TODO: Auto-generated Javadoc
/**
 * The Class Tree.
 */
public class Tree {
	
	/** The dlunit. */
	DLUnit dlunit;
	
	/** The thing name. */
	public  Element thingName = new Element("Concept");
	
	/** The thing concept. */
	public  Element thingConcept = new Element("Concept");
		
	/** The things children. */
	public  ArrayList<TreeNode> thingsChildren = new ArrayList<TreeNode>();
	
	/** The things constants children. */
	public  ArrayList<TreeNode> thingsConstantsChildren = new ArrayList<TreeNode>();

	
	/** The THING. */
	public TreeNode THING = new TreeNode(thingName , thingConcept, thingsChildren, thingsConstantsChildren);
	
	public static void printAtomicConceptString (Element e){
		System.out.println(e.getAttributeValue("text"));
	}

	public static void printConcept (Element e){
		XMLOutputter serializer = new XMLOutputter();
//		Get the most general subsumee list
		try {
			serializer.output(e, System.out);
		} catch (IOException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
		}
	}

	/**
	 * Instantiates a new tree.
	 */
	public Tree(Document doc)
	{
		thingConcept.setAttribute("text", "Thing");
		thingName.setAttribute("text", "Thing");
		dlunit = new DLUnit(doc);
		dlunit.processProgram();
	}

	/**
	 * Gets the parent list.
	 *
	 * @param node the node
	 * @return the parent list
	 */
	public ArrayList<TreeNode> getParentList(TreeNode node)
	{
		ArrayList<TreeNode> nodes = new ArrayList<TreeNode>();
		ArrayList<TreeNode> parentList = new ArrayList<TreeNode>();
		ArrayList<TreeNode> temp = new ArrayList<TreeNode>();
		nodes.add(this.THING);
		while(!nodes.isEmpty())
		{
			temp.clear();
//			System.out.println("getParentlIst");
			for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
				TreeNode treeNode = (TreeNode) iterator.next();
				temp.add(treeNode);
				if(treeNode.children.contains(node))
				{
					parentList.add(treeNode);
				}
			}
			nodes.removeAll(temp);
			for (TreeNode treeNode : temp) {
				if(treeNode!=null & treeNode.children.size() !=0)
					nodes.addAll(treeNode.children);
			}
		}
		return parentList;
	}
	
	
//				System.out.println("MGS structure match");
//				try {
//					serializer.output(treeNode.concept, System.out);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.out.println();
//				try {
//					serializer.output(givenConcept, System.out);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.out.println (dlunit.structureMatch(treeNode.concept, givenConcept));
	
	/**
	 * Gets the most general subsumeees.
	 *
	 * @param mostSpecific the most specific
	 * @param givenConcept the given concept
	 * @return the most general subsumeees
	 */
	public ArrayList<TreeNode> getMostGeneralSubsumeees(ArrayList<TreeNode> mostSpecific, Element givenConcept)
	{
		ArrayList<TreeNode> mostGenericSubsumees = (ArrayList<TreeNode>)mostSpecific.clone();
		ArrayList<TreeNode> temp = new ArrayList<TreeNode>();
		ArrayList<TreeNode> temp2 = new ArrayList<TreeNode>();
		boolean childrenLeft = true;
		XMLOutputter serializer = new XMLOutputter();
		
		System.out.println("MGS");
		printConcept(givenConcept);
		System.out.println();
		//Get the most general subsumee list
		while(childrenLeft)
		{
			temp = new ArrayList<TreeNode>();
			System.out.println("curr MSG: " + mostGenericSubsumees);
			childrenLeft =  false;
			for (TreeNode treeNode : mostGenericSubsumees) {
				// If the current treeNode is not subsumed by the givenConcept, remove it and add all its children later.
				if(!(dlunit.structureMatch(treeNode.concept, givenConcept)))
				{
					temp.add(treeNode);
				}
			}
			//Temp list is created because without it the code gives concurrent modification exception
			//All the elements the temp List are removed from the generalSubsumee list and all their children are added
			for (TreeNode treeNode : temp) {
				mostGenericSubsumees.remove(treeNode);
				for (TreeNode treeNodeIt : treeNode.children) {
					childrenLeft = true;
					mostGenericSubsumees.add(treeNodeIt);
				}
			}
		}

		//Remove elements whose parents are also there in the generalsubsumee list
		for (Iterator iterator = mostGenericSubsumees.iterator(); iterator.hasNext();) {
			TreeNode treeNode = (TreeNode) iterator.next();
			ArrayList<TreeNode> parentList = getParentList(treeNode);

			for (Iterator iterator2 = parentList.iterator(); iterator2
			.hasNext();) {
				TreeNode treeNode1 = (TreeNode) iterator2.next();
				if(mostGenericSubsumees.contains(treeNode1))
				{
					temp2.add(treeNode1);
				}
			}
		}
		for (Iterator iterator2 = temp2.iterator(); iterator2
		.hasNext();) {
			TreeNode treeNode = (TreeNode) iterator2.next();
			mostGenericSubsumees.remove(treeNode);
		}
		return mostGenericSubsumees;
	}
	
	/**
	 * Gets the most specific subsumers.
	 *
	 * @param newConcept the new concept
	 * @return the most specific subsumers
	 */
	public ArrayList<TreeNode> getMostSpecificSubsumers (Element newConcept){
		ArrayList<TreeNode> subsumerList = new ArrayList<TreeNode>();
		subsumerList.add(this.THING);
		Boolean childrenLeft = true;
		ArrayList<TreeNode> currChildren = null;
		Boolean removeParent = false;
		XMLOutputter serializer = new XMLOutputter();
		while (childrenLeft){
			childrenLeft = false;
			for (TreeNode currSubsumer : subsumerList) {
				removeParent = false;
				for (TreeNode currChild : currSubsumer.children) {
//					System.out.println("MSS structure match");
//					try {
//						serializer.output(newConcept, System.out);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					System.out.println();
//					try {
//						serializer.output(currChild.concept, System.out);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					System.out.println((dlunit.structureMatch(newConcept, currChild.concept)));
					// Add subsuming children of currSubsumer to subsumerList
					if ((dlunit.structureMatch(newConcept, currChild.concept))){
						childrenLeft = true;
						removeParent = true;
						subsumerList.add (currChild);
					}
				}
				if (removeParent){
					subsumerList.remove(currSubsumer);
				}
			}
		}
		return subsumerList;
	}
	
	/**
	 * Adds the node from sentence.
	 *
	 * @param elem1 the elem1
	 * @param elem2 the elem2
	 * @param relation the relation
	 */
	public void addNodeFromSentence (Element elem1, Element elem2, String relation){
		System.out.println(relation);
		
		if (relation == "==") {
			TreeNode newNode = new TreeNode(elem1, elem2, null, null);
//			System.out.println("adding sentence eqto");
			ArrayList<TreeNode> mostSpecificSubsumersList = this.getMostSpecificSubsumers(elem2);
			ArrayList<TreeNode> mostGeneralSubsumeesList = this.getMostGeneralSubsumeees(mostSpecificSubsumersList,elem2);
			
			printAtomicConceptString (elem1);
			printConcept(elem2);
			System.out.println();
			System.out.println(mostSpecificSubsumersList);
			System.out.println(mostGeneralSubsumeesList);
			
			
			//		ArrayList<TreeNode> mostGeneralSubsumeesList = this.getMostSpecificSubsumers(elem2);
			ArrayList<TreeNode> intersection = (ArrayList<TreeNode>) mostSpecificSubsumersList.clone();
			intersection.retainAll(mostGeneralSubsumeesList);

			if (intersection.isEmpty ()) {
				for (TreeNode subsumerNode : mostSpecificSubsumersList) {
					// Remove links between mostSpecificSubsumersList and mostGeneralSubsumeesList
					subsumerNode.children.removeAll(mostGeneralSubsumeesList);

					// Add the new node as a child to each subsumer
					subsumerNode.children.add(newNode);
				}
				// Add links from newNode to all the subsumees.
				newNode.children.addAll(mostGeneralSubsumeesList);

				// Handling constants
				// Get constants that are children of all the subsumers.
				ArrayList<TreeNode> constantsList = null;
				if (!mostSpecificSubsumersList.isEmpty()) {
					constantsList = mostSpecificSubsumersList.get(0).constantsChildren;
					for (TreeNode currNode : mostSpecificSubsumersList) {
						constantsList.retainAll(currNode.constantsChildren);
					}
				}

				// Get all constants subsumed by mostGeneralSubsumeesList
				ArrayList<TreeNode> subsumedConstantsList = null;
				if (!mostGeneralSubsumeesList.isEmpty()) {
					subsumedConstantsList = mostGeneralSubsumeesList.get(0).constantsChildren;
					for (TreeNode currNode : mostGeneralSubsumeesList) {
						subsumedConstantsList.addAll(currNode.constantsChildren);
					}
				}

				// Eliminate those constants that appear in the	constants children of 
				// mostGeneralSubsumeesList
				if(constantsList!=null &  subsumedConstantsList!=null)
					constantsList.removeAll(subsumedConstantsList);

				// Hourglass structure with newNode in between constants and the specific thigies.
			}
		}
		if(relation == "((")
		{
			TreeNode newNode = new TreeNode(elem1, elem2, null, null);
//			System.out.println("adding sentence ((");
			ArrayList<TreeNode> mostSpecificSubsumersList = this.getMostSpecificSubsumers(elem2);
			System.out.println("S : " + mostSpecificSubsumersList);
			for (Iterator iterator = mostSpecificSubsumersList.iterator(); iterator
			.hasNext();) {
				TreeNode treeNode = (TreeNode) iterator.next();
				treeNode.children.add(newNode);	
			}

		}

		if(relation == "->")
		{
			TreeNode newNode = new TreeNode(elem1, elem2, null, null);
//			System.out.println("adding sentence ->");
			ArrayList<TreeNode> mostSpecificSubsumersList = this.getMostSpecificSubsumers(elem2);
			for (Iterator iterator = mostSpecificSubsumersList.iterator(); iterator
			.hasNext();) {
				TreeNode treeNode = (TreeNode) iterator.next();
				treeNode.constantsChildren.add(newNode);
			}
		}
	}

	/**
	 * Prints the tree.
	 */
	public void printTree()
	{
		ArrayList<TreeNode> nodes = new ArrayList<TreeNode>();
		ArrayList<TreeNode> temp = new ArrayList<TreeNode>();
		nodes.add(this.THING);
		boolean flag = true;
		int level = 0;
		while(flag)
		{
			temp.clear();
			for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
				TreeNode treeNode = (TreeNode) iterator.next();
//				System.out.println("gen" + "level = "+ level);
				temp.add(treeNode);
//				System.out.println(treeNode+"");

			}
			level ++ ;
			nodes.removeAll(temp);
//			System.out.println("Nodes Length" + nodes.size());
			for (Iterator iterator = temp.iterator(); iterator.hasNext();) {
				TreeNode treeNode = (TreeNode) iterator.next();
//				System.out.println("Tree Node's children " +  treeNode.children );
				if(treeNode!=null & treeNode.children.size() !=0)
					nodes.addAll(treeNode.children);
			}
//			System.out.println("Nodes nodes after " + nodes + nodes.size());
			if(nodes.size() == 0)
			{
				flag = false;
			}
		}
	}
	
	/**
	 * Get Dot graph representation of the Tree.
	 *
	 * This uses GraphViz Java API to generate the DOT graph.
	 * @return the string containing the Dot source.
	 */
	public String convertToDot(){
		// GraphViz
		GraphViz gv = new GraphViz();
		gv.addln(gv.start_graph());
		
		LinkedList<TreeNode> bfsQueue = new LinkedList<TreeNode> ();
		TreeNode currNode = null;
		bfsQueue.add(this.THING);
		
		while(!bfsQueue.isEmpty())
		{
			currNode = bfsQueue.remove();
			for (TreeNode child_node : currNode.children) {
				gv.addln(currNode + " -> \"" + child_node + "\";");
			}
			bfsQueue.addAll(currNode.children);
		}
		
		gv.addln(gv.end_graph());
		System.out.println(gv.getDotSource());

		String type = "gif";
		File out = new File("output." + type);
		gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), out );
		return "";
	}
	
	public void outputXml()
	{
		Element root =  new Element("graph");
		root.setAttribute("id", "G");
		root.setAttribute("edgedefault", "undirected");

		ArrayList<TreeNode> nodes = new ArrayList<TreeNode>();
		ArrayList<TreeNode> temp = new ArrayList<TreeNode>();
		nodes.add(this.THING);
		boolean flag = true;
		int level = 0;
		while(flag)
		{
			temp.clear();
			for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
				TreeNode treeNode = (TreeNode) iterator.next();
				Element node = new Element("node");
				node.setAttribute("id", treeNode.toString());
				root.addContent(node);
				temp.add(treeNode);

			}
			level ++ ;
			nodes.removeAll(temp);
			for (Iterator iterator = temp.iterator(); iterator.hasNext();) {
				TreeNode treeNode = (TreeNode) iterator.next();

//				System.out.println("Parent" + treeNode);
				for (TreeNode treeNodeChild : treeNode.children) {
//					System.out.println("Child" + treeNodeChild);

					Element edge = new Element("edge");
					edge.setAttribute("source", treeNode.toString());
					edge.setAttribute("target", treeNodeChild.toString());
					root.addContent(edge);

				}
				if(treeNode!=null & treeNode.children.size() !=0)
					nodes.addAll(treeNode.children);
			}
			if(nodes.size() == 0)
			{
				flag = false;
			}
		}

		Document doc1 = new Document(root);
		// serialize it onto System.out
		try {
			FileWriter fstream = new FileWriter("xml-output.xml");
			BufferedWriter out = new BufferedWriter(fstream);
			XMLOutputter serializer = new XMLOutputter();
			serializer.output(doc1, out);
		}
		catch (IOException e) {
			System.err.println(e);
		}

	}
}


