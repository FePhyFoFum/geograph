package treethings;

import jade.tree.JadeNode;

import java.util.ArrayList;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

import geograph.RelType;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TLongHashSet;

public class TreeUtils {
	
	public static Node getTreeMRCA(ArrayList<Node> innodes){
		if (innodes.size() == 1) {
    		return innodes.get(0);
    	}
		Node cur1 = innodes.get(0);
		innodes.remove(0);
		Node cur2 = null;
		Node tempmrca = null;
		while (innodes.size() > 0) {
			cur2 = innodes.get(0);
			innodes.remove(0);
			tempmrca = getMRCATraverse(cur1,cur2);
			cur1 = tempmrca;
		}
		return cur1;
	}
	
	private static Node getMRCATraverse(Node curn1, Node curn2) {
		//get path to root for first node
		TLongArrayList path1 = new TLongArrayList();
		Node parent = curn1;
		while (parent != null) {
			path1.add(parent.getId());
			if (curn1.hasRelationship(Direction.OUTGOING, RelType.IS_CHILDOF)){
				parent = curn1.getSingleRelationship(RelType.IS_CHILDOF, Direction.OUTGOING).getEndNode();				
			}else{
				break;
			}
		}
		//find first match between this node and the first one
		parent = curn2;
		while (parent != null) {
			if (path1.contains(parent.getId())) {
				return parent;
			}
			if (curn1.hasRelationship(Direction.OUTGOING, RelType.IS_CHILDOF)){
				parent = curn1.getSingleRelationship(RelType.IS_CHILDOF, Direction.OUTGOING).getEndNode();				
			}else{
				break;
			}
		}
		return null;
	}
	
	public static TLongArrayList getNodesFromClade(Node anode){
		TLongArrayList tlist = new TLongArrayList();
		TraversalDescription ISCHILDOF_TRAVERSAL = Traversal.description()
                .relationships(RelType.IS_CHILDOF, Direction.INCOMING);
		for(Node tnode: ISCHILDOF_TRAVERSAL.traverse(anode).nodes()){
			tlist.add(tnode.getId());
		}
		return tlist;
	}
}
