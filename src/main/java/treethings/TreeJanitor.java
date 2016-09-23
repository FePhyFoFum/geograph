package treethings;

import geograph.RelType;
import gnu.trove.list.array.TLongArrayList;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.*;
import org.opentree.graphdb.GraphDatabaseAgent;

public class TreeJanitor {
	private GraphDatabaseAgent gda; 
	
	private Index<Node> rootIndex;
	
	public TreeJanitor(GraphDatabaseAgent gda){
		this.gda = gda;
		rootIndex = this.gda.getNodeIndex("root", "type","exact");
	}
	
	public boolean deleteTreeFromSource(String sourcename){
		boolean success = false;
		
		IndexHits<Node> hits = rootIndex.get("source",sourcename);
		Node root = null;
		if(hits.hasNext()){
			success = true;
			root = hits.next();
		}
		TLongArrayList nodeids = TreeUtils.getNodesFromClade(root);
		Transaction tx = null;
		try {
			tx = gda.beginTx();
			for(int i=0;i<nodeids.size();i++){
				Node tnode = gda.getNodeById(nodeids.get(i));
				for(Relationship rel: tnode.getRelationships(RelType.IS_CHILDOF)){
					rel.delete();
				}
				if(tnode.getRelationships().iterator().hasNext()==false){
					tnode.delete();
				}
			}
			rootIndex.remove(root);
			tx.success();
		} finally {
			tx.finish();
		}
		return success;
	}
}
