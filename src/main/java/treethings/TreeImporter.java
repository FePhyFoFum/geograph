package treethings;

import geograph.RelType;
import gnu.trove.list.array.TLongArrayList;
import jade.tree.*;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.opentree.graphdb.*;

/**
 * This is rediculously simple and should probably be replaced with something
 * more feature full like OTU. However, this gets the job done for now
 * 
 * @author smitty
 *
 */

public class TreeImporter {

	private GraphDatabaseAgent gda; 
	
	private Index<Node> speciesIndex;
	private Index<Node> rootIndex;
	
	public TreeImporter(GraphDatabaseAgent gda){
		this.gda = gda;
		speciesIndex = this.gda.getNodeIndex("species","type","exact");
		rootIndex = this.gda.getNodeIndex("root", "type","exact");
	}
	
	public void loadTree(JadeTree tree){
		Node cnode = null;//root
		Transaction tx = null;
		try{
			tx = gda.beginTx();
			cnode = gda.createNode();
			rootIndex.add(cnode, "source", tree.getObject("treename"));
			preOrderLoadTree(tree.getRoot(),cnode);
			tx.success();
		}finally{
			tx.finish();
		}
		initMrcas(cnode);
	}
	
	private void preOrderLoadTree(JadeNode curjnode, Node curnode){
		for(int i=0;i<curjnode.getChildCount();i++){
			JadeNode tchild = curjnode.getChild(i);
			Node cnode = null;
			if(tchild.getName().length() < 2 || tchild.getChildCount() > 0){
				cnode = gda.createNode();
			}else{
				IndexHits<Node> hits = speciesIndex.get("species", tchild.getName().replace("_"," "));
				if(hits.hasNext()){
					cnode = hits.next();
				}else{
					cnode = gda.createNode();
					cnode.setProperty("name", tchild.getName().replace("_", " "));
					speciesIndex.add(cnode, "species", tchild.getName().replace("_", " "));
				}
			}
			Relationship trel = cnode.createRelationshipTo(curnode, RelType.IS_CHILDOF);
			//brlen
			trel.setProperty("brlen", tchild.getBL());
			preOrderLoadTree(tchild,cnode);
		}
	}
	
	/**
	 * Adds the MRCA_CHILDOF and STREE_CHILDOF relationships to an existing taxonomy.
	 * 
	 * Assumes the structure of the graphdb where the taxonomy is stored alongside the graph of life
	 * and therefore the graph is initialized, in this case, with the taxonomy relationships
	 * 
	 * for a more general implementation, could just go through and work on the preferred nodes
	 * 
	 * @throws TaxonNotFoundException 
	 */
	private void initMrcas(Node startnode){
		Transaction tx = null;
		// start the mrcas
		System.out.println("calculating mrcas");
		try {
			tx = gda.beginTx();
			postorderAddMRCAs(startnode);
			tx.success();
		} finally {
			tx.finish();
		}
		//NOTE: outmrcas don't exist for taxchild of nodes because they are assumed to be the whole thing
	}
	
	/**
	 * for initial tree processing.  adds a mrca->long[]  property
	 *	to the node and its children (where the elements of the array are the ids
	 *	of graph of life nodes). The property is is the union of the mrca properties
	 *	for the subtree. So the leaves of the tree must already have their mrca property
	 *	filled in!
	 *
	 * @param dbnode should be a node in the graph-of-life (has incoming MRCACHILDOF relationship)
	 */
	private void postorderAddMRCAs(Node dbnode) {
		//traversal incoming and record all the names
		for (Relationship rel: dbnode.getRelationships(Direction.INCOMING,RelType.IS_CHILDOF)) {
			Node tnode = rel.getStartNode();
			postorderAddMRCAs(tnode);
		}
		//could make this a hashset if dups become a problem
		TLongArrayList mrcas = new TLongArrayList();
		mrcas.add(dbnode.getId());
		if (dbnode.hasProperty("mrca") == false) {
			for (Relationship rel: dbnode.getRelationships(Direction.INCOMING,RelType.IS_CHILDOF)) {
				Node tnode = rel.getStartNode();
				mrcas.addAll((long[]) tnode.getProperty("mrca"));
			}
			mrcas.sort();
		}
		dbnode.setProperty("mrca", mrcas.toArray());
	}
	
}
