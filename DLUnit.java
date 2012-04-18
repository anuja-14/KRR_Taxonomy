package tree;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

import org.jgrapht.*;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import java.util.*;
import java.io.*;

import krr.main.ProcessDL;

/**
 * @author Vikram Rao S, M Vijay Karthik
 * 
 */
public class DLUnit {
	Document doc;
	List<String> topSortedConcepts;
	HashMap<String, Element> conceptMap;
	DirectedGraph<String, DefaultEdge> dependencies;
	Element ThingConcept;
	int newConceptSuffix = 1;
	String kbString;

	/**
	 * Reads from a file and returns its content
	 * 
	 * @param fileName
	 * @return Contents of the file
	 */
	String readFromFile(String fileName) {
		String s = "";
		try {
			FileInputStream r = new FileInputStream(fileName);
			DataInputStream dis = new DataInputStream(r);
			BufferedReader br = new BufferedReader(new InputStreamReader(r));
			String temp;
			// br.readLine();
			// br.readLine();
			while ((temp = br.readLine()) != null)
				s += temp + "\n";
			dis.close();
			r.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return s;
	}

	Element getNewConcept() {
		Element ret = new Element("CONCEPT");
		ret.setAttribute("text", "AnonymousConcept-" + (newConceptSuffix++));
		return ret;
	}

	/**
	 * Run the DL System on the KB specified in the given file.
	 * 
	 * @param fileName
	 */
	public DLUnit(String fileName) {
		ThingConcept = new Element("CONCEPT");
		ThingConcept.setAttribute("text", "Thing");

		ProcessDL process = ProcessDL.getInstance();
		process.debug();
		PrintStream old_out = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
		process.run(fileName);
		System.setOut(old_out);
		String s = baos.toString();

		int ind = s.indexOf("PROGRAM");
		s = s.substring(ind + 7);
		ind = s.indexOf("PROGRAM");
		s = s.substring(ind - 1);

		// String s = readFromFile( fileName );
		s = s.replace("<<", "((");

		kbString = readFromFile(fileName);
		
		System.out.println(kbString);

		System.out.println(s);

		SAXBuilder sb = new SAXBuilder();
		try {
			doc = sb.build(new ByteArrayInputStream(s.getBytes()));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * For use when no KB is needed. For example, to use Structure Matching or
	 * Normalization, this constructor can be called.
	 */
	public DLUnit( Document document ) {
		doc = document;
	}

	/**
	 * Given an XML tree, process it.
	 * 
	 * @param doc
	 *            - XML Tree
	 */
	void processProgram() {
		Element root = doc.getRootElement();
		List<Element> statements = root.getChildren();

		conceptMap = buildConceptMap(root);

		dependencies = new SimpleDirectedGraph<String, DefaultEdge>(
				DefaultEdge.class);
		try {
			getDependencyTree(statements, dependencies);
		} catch (Exception e) {
			e.printStackTrace();
		}

		TopologicalOrderIterator<String, DefaultEdge> top = new TopologicalOrderIterator<String, DefaultEdge>(
				dependencies);

		topSortedConcepts = new LinkedList<String>();

		while (top.hasNext()) {
			topSortedConcepts.add(top.next());
		}

		for (Iterator<String> i = topSortedConcepts.iterator(); i.hasNext();) {
			String n = i.next();
			if (conceptMap.containsKey(n))
				normalize(conceptMap.get(n));
		}

		XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
		try {
			xo.output(doc, System.out);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// System.out.println( "\n\n" + structureMatch(
		// (Element) statements.get(0).getChildren().get(1),
		// (Element) statements.get(1).getChildren().get(1) ) );
	}

	HashMap<String, Element> buildConceptMap(Element root) {
		HashMap<String, Element> cm = new HashMap<String, Element>();
		List<Element> rc = root.getChildren();
		for (Element e : rc) {
			if (e.getName().equals("EQUIVALENTTO")) {
				if (((Element) e.getChildren().get(0)).getName().equals(
						"CONCEPT"))
					cm.put(e.getChild("CONCEPT").getAttributeValue("text"), e);
				// System.out.println(
				// e.getChild("CONCEPT").getAttributeValue("text") + "\n" + e );
			} else if (e.getName().equals("ISA")) {
				List<Element> ch = e.getChildren();

				if (ch.get(0).getName().equals("CONCEPT")) {
					e.setName("EQUIVALENTTO");
					e.setAttribute("text", "==");

					Element ch1 = ch.get(1);

					ch.get(1).detach();
					Element wrapper = new Element("AND");
					wrapper.addContent(ch1);
					wrapper.addContent(getNewConcept());
					e.addContent(2, wrapper);

					cm.put(ch.get(0).getAttributeValue("text"), e);
				}
			} else if (e.getName().equals("SUBSUMES")) {
				List<Element> ch = e.getChildren();

				if (ch.get(1).getName().equals("CONCEPT")) {
					e.setName("EQUIVALENTTO");
					e.setAttribute("text", "==");

					Element ch0 = ch.get(0), ch1 = ch.get(1);

					ch.get(1).detach();
					ch.get(0).detach();

					Element wrapper = new Element("AND");
					wrapper.addContent(ch0);
					wrapper.addContent(getNewConcept());

					e.addContent(ch1);
					e.addContent(wrapper);

					cm.put(ch.get(0).getAttributeValue("text"), e);
				}
			}
		}
		// System.out.println("\n");
		return cm;
	}

	void getDependencyTree(List<Element> sentences,
			DirectedGraph<String, DefaultEdge> graph) throws Exception {
		for (Element s : sentences) {
			String name = s.getName();
			List<Element> c = s.getChildren();
			String cname = c.get(0).getAttributeValue("text");
			Element descr = c.get(1);

			if (name.equals("EQUIVALENTTO")) {
				List<String> childConcepts = new LinkedList<String>();
				findChildConcepts(descr, childConcepts);

				for (String e : childConcepts) {
					if (!graph.containsVertex(cname))
						graph.addVertex(cname);
					if (!graph.containsVertex(e))
						graph.addVertex(e);
					graph.addEdge(e, cname);
				}
			} else {
				// System.err
				// .println("ERROR : Not an == statement. What do I do ?");
			}
		}
	}

	void findChildConcepts(Element root, List<String> ans) {
		List<Element> children = root.getChildren();

		if (root.getName().equals("CONCEPT"))
			ans.add(root.getAttributeValue("text"));

		if (children != null && children.size() != 0) {
			for (Element c : children) {
				findChildConcepts(c, ans);
			}
		}
	}

	void replacePreviousConcepts(Element root, List<Element> prevCon) {
		List<Element> children = root.getChildren();

		// TODO : Only if it is a previous concept.
		if (root.getName().equals("CONCEPT")) {
			prevCon.add(root);
		}

		if (children != null && children.size() != 0) {
			for (Element c : children) {
				replacePreviousConcepts(c, prevCon);
			}
		}
	}

	void normalize(Element root) {
		List<Element> children = root.getChildren();
		Element con = children.get(1);

		// Replace all previous concept names with their trees.
		normalizePreviousConcepts(con);

		// Do AND flattening.
		flattenAnds(con);

		// Do ALL combination.
		combineAlls(con);

		// Do EXISTS combination.
		combineExists(con);

		// Remove redundancies.
		List<Element> removeThese = new LinkedList<Element>();
		removeRedundancies(con, removeThese);

		for (Element e : removeThese) {
			e.detach();
		}

		// Remove AND in cases where there are 0 or 1 children.
		fixAnds(con);
	}

	void fixAnds(Element c) {
		List<Element> children = c.getChildren();
		String name = c.getName();
		if (name.equals("ALL")) {
			fixAnds(children.get(1));
		}

		if (name.equals("AND")) {
			if (children.size() == 0) {
				if (c.getParentElement().getName().equals("EQUIVALENTTO"))
					c.getParentElement().addContent(
							(Element) ThingConcept.clone());
				c.detach();
			}

			else if (children.size() == 1) {
				Element child = children.get(0);
				child.detach();
				Element parent = c.getParentElement();
				int index = parent.indexOf(c);
				c.detach();
				parent.addContent(index, child);
			}

			else {
				for (Element ch : children) {
					fixAnds(ch);
				}
			}
			
			children = c.getChildren();
			
			if( children.size() >= 2 ) {
				List<Element> toRemove = new LinkedList<Element>();
				
				for (Element ch : children) {
					if( ch.getName().equals("CONCEPT")
							&& ch.getAttributeValue("text").equals("Thing") )
						toRemove.add(ch);
				}
				
				for( Element ch : toRemove )
					ch.detach();
			}
		}
	}

	boolean elementsEqual(Element a, Element b) {
		String aname = a.getName(), bname = b.getName();
		List<Element> ac = a.getChildren(), bc = b.getChildren();

		if (!aname.equals(bname))
			return false;
		if (aname.equals("CONCEPT"))
			return (a.getAttributeValue("text").equals(b
					.getAttributeValue("text")));
		if (aname.equals("FILLS") || aname.equals("EXISTS")) {
			return ac.get(0).getAttributeValue("text")
					.equals(bc.get(0).getAttributeValue("text"))
					&& ac.get(1).getAttributeValue("text")
							.equals(bc.get(1).getAttributeValue("text"));
		}
		if (aname.equals("ALL")) {
			return ac.get(0).getAttributeValue("text")
					.equals(bc.get(0).getAttributeValue("text"))
					&& elementsEqual(ac.get(1), bc.get(1));
		}
		if (aname.equals("AND")) {
			if (ac.size() != bc.size())
				return false;
			for (Element ach : ac) {
				Element toDel = null;
				for (Element bch : bc) {
					if (elementsEqual(ach, bch)) {
						toDel = bch;
						break;
					}
				}

				if (toDel == null)
					return false;
				else
					bc.remove(toDel);
			}
			return bc.isEmpty();
		}
		return false;
	}

	
	void removeRedundancies(Element con, List<Element> removeThese) {
		String cname = con.getName();
		List<Element> children = con.getChildren();

		if (cname.equals("ALL")) {
			removeRedundancies(children.get(1), removeThese);

			// Remove [ALL r Thing].
			Element conCopy = con;
			List<Element> childrenCopy = conCopy.getChildren();
			if (childrenCopy.get(1).getName().equals("CONCEPT")
					&& childrenCopy.get(1).getAttributeValue("text")
							.equals("Thing")) {

				removeThese.add(conCopy);

				 conCopy = conCopy.getParentElement();
				 while( conCopy != null && conCopy.getName().equals("ALL") ) {
					 conCopy.addContent(1, (Element) ThingConcept.clone());
					 removeThese.add( conCopy );
					 conCopy = conCopy.getParentElement();
				 }
				 
				 if( conCopy.getName().equals("EQUIVALENTTO") )
					 conCopy.addContent(2, (Element) ThingConcept.clone() );
			}

		} else if (cname.equals("AND")) {
			for (Element e : children) {
				removeRedundancies(e, removeThese);
			}
			children = con.getChildren();
			List<Element> toBeRemoved = new LinkedList<Element>();
			for (int iit = 0; iit < children.size(); iit++) {
				Element i = children.get(iit);
				for (int jit = iit + 1; jit < children.size(); jit++) {
					Element j = children.get(jit);
					if (j != i && elementsEqual(i, j))
						toBeRemoved.add(j);
				}
			}

			removeThese.addAll(toBeRemoved);
		}
	}

	void combineExists(Element con) {
		String cname = con.getName();
		List<Element> children = con.getChildren();

		if (cname.equals("ALL"))
			combineExists(children.get(1));
		else if (cname.equals("AND")) {
			HashMap<String, List<Element>> map = new HashMap<String, List<Element>>();
			for (Element e : children) {
				if (e.getName().equals("EXISTS")) {
					String roleName = ((Element) e.getChildren().get(1))
							.getAttributeValue("text");
					if (map.containsKey(roleName))
						map.get(roleName).add(e);
					else {
						List<Element> l = new LinkedList<Element>();
						l.add(e);
						map.put(roleName, l);
					}
				}
			}

			for (String roleName : map.keySet()) {
				List<Element> roles = map.get(roleName);
				Element firstRole = roles.get(0);

				int max = Integer.parseInt(((Element) firstRole.getChildren()
						.get(0)).getAttributeValue("text"));

				for (Element r : roles) {
					if (r == firstRole)
						continue;
					r.detach();
					int cur = Integer.parseInt(((Element) r.getChildren()
							.get(0)).getAttributeValue("text"));
					if (cur > max)
						max = cur;
				}

				((Element) firstRole.getChildren().get(0)).setAttribute("text",
						(new Integer(max)).toString());
			}
		}
	}

	void combineAlls(Element con) {
		String cname = con.getName();
		List<Element> children = con.getChildren();

		if (cname.equals("ALL"))
			combineAlls(children.get(1));
		else if (cname.equals("AND")) {
			HashMap<String, List<Element>> map = new HashMap<String, List<Element>>();
			for (Element e : children) {
				if (e.getName().equals("ALL")) {
					String roleName = ((Element) e.getChildren().get(0))
							.getAttributeValue("text");
					if (map.containsKey(roleName))
						map.get(roleName).add(e);
					else {
						List<Element> l = new LinkedList<Element>();
						l.add(e);
						map.put(roleName, l);
					}
				}
			}

			for (String roleName : map.keySet()) {
				List<Element> roles = map.get(roleName);
				Element firstRole = roles.get(0);
				Element newConcept = new Element("AND");

				Element frc = (Element) firstRole.getChildren().get(1);
				// frc.detach();
				// newConcept.addContent(frc);

				for (Element r : roles) {
					if (r == firstRole)
						continue;
					r.detach();
					Element c = (Element) r.getChildren().get(1);
					c.detach();
					newConcept.addContent(c);
				}
				flattenAnds(newConcept);
				combineAlls(newConcept);

				if (newConcept.getChildren().size() == 0)
					;
				else {
					frc.detach();
					newConcept.addContent(frc);

					firstRole.addContent(2, newConcept);
				}
			}
		}
	}

	void flattenAnds(Element con) {
		String cname = con.getName();
		List<Element> c = con.getChildren();

		if (cname == "ALL")
			flattenAnds(c.get(1));
		else if (cname == "AND") {
			for (Element e : c) {
				flattenAnds(e);
			}

			List<Element> toModify = new LinkedList<Element>();
			c = con.getChildren();
			for (Element e : c) {
				if (e.getName().equals("AND")) {
					// Remove the AND.
					toModify.add(e);
				}
			}

			for (Element e : toModify) {
				int index = con.indexOf(e);
				e.detach();

				List<Element> andChildren = e.getChildren();

				List<Element> toModifyAgain = new LinkedList<Element>();

				for (Element ac : andChildren) {
					toModifyAgain.add(ac);
				}

				for (Element i : toModifyAgain) {
					i.detach();
					con.addContent(index++, i);
				}
			}
		}
	}

	void normalizePreviousConcepts(Element con) {
		List<Element> prevCon = new LinkedList<Element>();
		replacePreviousConcepts(con, prevCon);

		// TODO : Get prevCon from the dependency graph itself.

		for (Iterator<Element> e = prevCon.iterator(); e.hasNext();) {
			// Replace with their descriptions.
			Element cur = e.next(), par = cur.getParentElement();

			if (conceptMap.containsKey(cur.getAttributeValue("text"))) {
				int index = par.indexOf(cur);
				Element prevConcept = (Element) ((Element) conceptMap.get(cur
						.getAttributeValue("text"))).getChildren().get(1);
				prevConcept = (Element) prevConcept.clone();

				cur.detach();
				par.addContent(index, prevConcept);
			}
		}
	}

	boolean structureMatch(Element subsumee, Element subsumer) {
		
		if( subsumer.getName().equals("CONCEPT")
				&& subsumer.getAttributeValue("text").equals("Thing") )
			return true;
		
		Element d = subsumee, e = subsumer;

		String dname = d.getName(), ename = e.getName();

		if (dname.equals("CONCEPT") && ename.equals("CONCEPT")) {
			if (d.getAttributeValue("text").equals(e.getAttributeValue("text")))
				return true;
		} else if ((dname.equals("FILLS") && ename.equals("FILLS"))
				|| (dname.equals("EXISTS") && ename.equals("EXISTS"))) {
			List<Element> dc = d.getChildren(), ec = e.getChildren();

			// TODO : We assume that constants are represented in the "text"
			// attribute. Is this correct ?
			if (dc.get(0).getAttributeValue("text")
					.equals(ec.get(0).getAttributeValue("text"))
					&& dc.get(1).getAttributeValue("text")
							.equals(ec.get(1).getAttributeValue("text")))
				return true;
		} else if (dname.equals("FILLS") && ename.equals("EXISTS")) {
			List<Element> dc = d.getChildren(), ec = e.getChildren();
			if (Integer.parseInt(ec.get(0).getAttributeValue("text")) == 1
					&& dc.get(0).getAttributeValue("text")
							.equals(ec.get(1).getAttributeValue("text")))
				return true;
		} else if (dname.equals("ALL") && ename.equals("ALL")) {
			List<Element> dc = d.getChildren(), ec = e.getChildren();
			if (dc.get(0).getAttributeValue("text")
					.equals(ec.get(0).getAttributeValue("text"))
					&& structureMatch(dc.get(1), ec.get(1)))
				return true;
		} else if (dname.equals("AND") ) {
			List<Element> dc, ec;
			dc = d.getChildren();
			
			if( ename.equals("AND"))
				ec = e.getChildren();
			else {
				ec = new LinkedList<Element>();
				ec.add(e);
			}
			
			for (Element ech : ec) {
				boolean done = false;
				for (Element dch : dc) {
					if (structureMatch(dch, ech)) {
						done = true;
						break;
					}
				}

				if (!done)
					return false;
			}
			return true;
		}
		return false;
	}

	public static void main(String args[]) {
		DLUnit t = new DLUnit(
				args[0]);
		t.processProgram();

		BufferedReader input = new BufferedReader(new InputStreamReader(
				System.in));
		String command;
		String kb = t.kbString;
		while (true) {
			try {
				command = input.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			command = command.replaceAll("^ *", "");
			command = command.replaceAll(" *$", "");

			if (command.equals("EXIT"))
				break;
			else if( command.charAt(0) == '?' ) {
				StringTokenizer st = new StringTokenizer(command, " ");
				
				st.nextToken();
				String left = st.nextToken(),
					   op = st.nextToken(),
					   right = st.nextToken();
				
				
				Element leftElement,
						rightElement;
				
				try {
					leftElement = (Element) t.conceptMap.get(left).getChildren().get(1);
				}
				catch( Exception e ) {
					leftElement = new Element("CONCEPT");
					leftElement.setAttribute( "text", left );
				}
				
				try {
					rightElement = (Element) t.conceptMap.get(right).getChildren().get(1);
				}
				catch( Exception e ) {
					rightElement = new Element("CONCEPT");
					rightElement.setAttribute( "text", right );
				}
				

				if( op.equals("<<") )
					System.out.println( t.structureMatch( leftElement, rightElement ) );
				else if( op.equals(">>") )
					System.out.println( t.structureMatch( rightElement, leftElement ) );
				else if( op.equals("==") )
					System.out.println( t.structureMatch( leftElement, rightElement )
							&& t.structureMatch( rightElement, leftElement ) );
				
			}
			else {
				kb += command;
				kb += "\n";
			}
		}

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(
					"/home/vikrams/Desktop/test.txt", false);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		try {
			fos.write(kb.getBytes());
			fos.close();
			
			if( ! kb.equals( t.kbString ) ) {
				String ar[] = { "/home/vikrams/Desktop/test.txt" };
				DLUnit.main( ar );
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}
}