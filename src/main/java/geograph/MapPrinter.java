package geograph;

import gnu.trove.set.hash.TLongHashSet;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.lucene.queryParser.QueryParser.Operator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;
import org.opentree.graphdb.GraphDatabaseAgent;

public class MapPrinter {

	private GraphDatabaseAgent gda;
	Index<Node> nodeIndexLatLong;
	Index<Node> nodeIndexLat;
	Index<Node> nodeIndexLong;
	Index<Node> speciesIndex;
	Index<Node> metaDataIndex;
	
	public MapPrinter(GraphDatabaseAgent gda){
		this.gda = gda;
		nodeIndexLatLong = this.gda.getNodeIndex("latlong","type","exact");
		nodeIndexLat = this.gda.getNodeIndex("latitude","type", "exact");
		nodeIndexLong = this.gda.getNodeIndex("longitude","type", "exact");
		speciesIndex = this.gda.getNodeIndex("species","type","exact");//should be replaced with the taxonomy from otu
		metaDataIndex = this.gda.getNodeIndex("source", "type", "exact");
	}
	
	public String printSimpleMap(double latitude, double longitude, boolean layer, String data){
		StringBuffer sb = new StringBuffer("");
		String prop = data;
		int range = 40;
		int cellmultiplier = 1;
		double cellsize = 0;
		//all for now have the same cell size, so just getting one of the metadata nodes and getting the cell size
		IndexHits<Node> hitssources = metaDataIndex.query("sourcename","*");
		//System.out.println(hitssources.size());
		Node metadata = hitssources.next();
		cellsize = (Double)metadata.getProperty("cellsize")*cellmultiplier;
		hitssources.close();
		boolean exists = false;
		Node nd = null;
		//index check
		double curlatitude = latitude;
		try {
			FileWriter fw  = new FileWriter("views/data2.tsv");
			fw.write("day\thour\tvalue\n");
			for(int i=0;i<range;i++){
				double stlat = curlatitude-cellsize;
				double stoplat = curlatitude;
				//		double stlong = longitude-cellsize;
				//		double stoplong = longitude;
				IndexHits <Node> lathits = nodeIndexLatLong.query(QueryContext.numericRange("startlat", stlat, stoplat,true,false).sortNumeric("startlong", false));
				//		IndexHits <Node> longhits = nodeIndexLong.query(QueryContext.numericRange("start", stlong, stoplong,true,false).sortNumeric("start", false));
				//		System.out.println("long: "+longitude+" lat: "+latitude+" stlat: "+stlat+" stoplat: "+stoplat+ " stlong: "+stlong+" stoplong: "+stoplong);
				//System.out.println(lathits.size());
				double values[] = new double[range];
				Arrays.fill(values, -1);
				double curlong = Math.round(longitude);
				if(lathits.size()!= 0){
					Node curnode = lathits.next();
					double curnodelong = (Double)curnode.getProperty("longitude");
					//System.out.println(curnodelong+" "+curlong);
					while(curnodelong < longitude && lathits.hasNext()){
						curnode = lathits.next();
						curnodelong = (Double)curnode.getProperty("longitude");
					}
					if(curnodelong < longitude || curnodelong > (longitude+(cellsize*range))){
						//just keep going, there is no overlap
					}else{
						for(int j =0; j < range; j++){
							if(curlong == curnodelong){
								/*
								 * this is where the different values would be changed
								 */
								if (layer == true){
									values[j] = (Double)curnode.getProperty(prop);
								}else{//records or measures
									values[j] = 0;//giving the record 1 so that there is some color for this
									for(Relationship rel: curnode.getRelationships(RelType.IS_LOCATED)){
										values[j] += 1;
									}
								}
								if(lathits.hasNext()){
									curnode = lathits.next();
									curnodelong = (Double)curnode.getProperty("longitude");
								}
							}
							curlong += cellsize;
							//System.out.println(curnodelong+" "+curlatitude+","+curlong);
						}
					}
				}
				for(int j=0;j<values.length;j++){
					fw.write((i+1)+"\t"+(j+1)+"\t"+values[j]+"\n");
				}
				lathits.close();
				curlatitude -= cellsize;
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	/**
	 * Currently this is just measuring the phylogenetic diversity as measured
	 * by the sum of the branch lengths in the area. This can take a while because
	 * it needs to measure it for each of the taxa in the area.
	 * 
	 * TODO: 
	 * 		add the clade name or clade node for filtering only species in your group
	 * 		add the ability to calculate different ways
	 * @param latitude
	 * @param longitude
	 * @return
	 */
	public String printPhylogeneticDiversityMap(double latitude, double longitude, TLongHashSet taxa_limits){ //add MRCA
		StringBuffer sb = new StringBuffer("");
		int range = 40;
		int cellmultiplier = 1;
		double cellsize = 0;
		boolean limit_taxa = false;
		if(taxa_limits != null){
			limit_taxa = true;
			System.out.println("limiting to "+taxa_limits);
		}
		//all for now have the same cell size, so just getting one of the metadata nodes and getting the cell size
		IndexHits<Node> hitssources = metaDataIndex.query("sourcename","*");
		//System.out.println(hitssources.size());
		Node metadata = hitssources.next();
		cellsize = (Double)metadata.getProperty("cellsize")*cellmultiplier;
		hitssources.close();
		boolean exists = false;
		Node nd = null;
		//index check
		double curlatitude = latitude;
		try {
			FileWriter fw  = new FileWriter("views/data2.tsv");
			fw.write("day\thour\tvalue\n");
			for(int i=0;i<range;i++){
				double stlat = curlatitude-cellsize;
				double stoplat = curlatitude;
				//		double stlong = longitude-cellsize;
				//		double stoplong = longitude;
				IndexHits <Node> lathits = nodeIndexLatLong.query(QueryContext.numericRange("startlat", stlat, stoplat,true,false).sortNumeric("startlong", false));
				//		IndexHits <Node> longhits = nodeIndexLong.query(QueryContext.numericRange("start", stlong, stoplong,true,false).sortNumeric("start", false));
				//		System.out.println("long: "+longitude+" lat: "+latitude+" stlat: "+stlat+" stoplat: "+stoplat+ " stlong: "+stlong+" stoplong: "+stoplong);
				//System.out.println(lathits.size());
				double values[] = new double[range];
				Arrays.fill(values, -1);
				double curlong = Math.round(longitude);
				if(lathits.size()!= 0){
					Node curnode = lathits.next();
					double curnodelong = (Double)curnode.getProperty("longitude");
					//System.out.println(curnodelong+" "+curlong);
					while(curnodelong < longitude && lathits.hasNext()){
						curnode = lathits.next();
						curnodelong = (Double)curnode.getProperty("longitude");
					}
					if(curnodelong < longitude || curnodelong > (longitude+(cellsize*range))){
						//just keep going, there is no overlap
					}else{
						for(int j =0; j < range; j++){
							if(curlong == curnodelong){
								/*
								 * this is where the different values would be changed
								 */
								values[j] = 0;//giving the record 1 so that there is some color for this
								//TODO: CALCULATE THE PHYLOGENETIC DIVERSITY HERE
								HashSet<Node> species_to_calc = new HashSet<Node> (); 
								for(Relationship rel: curnode.getRelationships(RelType.IS_LOCATED)){
									try{
										Iterable<Relationship> trels = rel.getStartNode().getRelationships(RelType.HAS_RECORD);//.getEndNode();
										for(Relationship trel: trels){
											if(limit_taxa == true){
												if(taxa_limits.contains(trel.getStartNode().getId())){
													species_to_calc.add(trel.getStartNode());
												}
											}else{
												species_to_calc.add(trel.getStartNode());
											}
										}
									}catch(Exception e){
										continue;
									}
								}
								//given the list of species, this is where the phylodiversity would be calculated
								double value = calc_basic_phylo_div(species_to_calc);
								values[j] = value;//species_to_calc.size();
								if(lathits.hasNext()){
									curnode = lathits.next();
									curnodelong = (Double)curnode.getProperty("longitude");
								}
							}
							curlong += cellsize;
							//System.out.println(curnodelong+" "+curlatitude+","+curlong);
						}
					}
				}
				for(int j=0;j<values.length;j++){
					fw.write((i+1)+"\t"+(j+1)+"\t"+values[j]+"\n");
				}
				lathits.close();
				curlatitude -= cellsize;
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	double calc_basic_phylo_div(HashSet<Node> species_to_calc){
		double retval = 0;
		if(species_to_calc.size() < 1){
			return 0;
		}else if(species_to_calc.size() == 1){
			Node curnode = species_to_calc.iterator().next();
			Relationship currel = curnode.getSingleRelationship(RelType.IS_CHILDOF, Direction.OUTGOING);
			if(currel != null){
				if(currel.hasProperty("brlen")){
					return (Double)currel.getProperty("brlen");
				}else{
					return 1;
				}
			}else{
				return 1;
			}
		}
		TLongHashSet visitedNodes = new TLongHashSet();
		TLongHashSet nodeIds = new TLongHashSet();
		for(Node tnd : species_to_calc){
			nodeIds.add(tnd.getId());
		}
		for(Node tnd : species_to_calc){
			boolean going = true;
			Node curnode = tnd;
			Relationship currel = null;
			visitedNodes.add(curnode.getId());
			while(going){
				try{
					currel = curnode.getSingleRelationship(RelType.IS_CHILDOF, Direction.OUTGOING);
					curnode = currel.getEndNode();
				}catch(Exception e){
					System.out.println(curnode +" not in tree");
					going = false;
					break;
				}
				if(visitedNodes.contains(curnode.getId())){
					going = false;
					break;
				}else{
					visitedNodes.add(curnode.getId());
					if(currel.hasProperty("brlen")){
						retval += (Double)currel.getProperty("brlen");
					}else{
						retval += 1;
					}
					TLongHashSet tmrcas = new TLongHashSet((long []) curnode.getProperty("mrca"));
					if(tmrcas.containsAll(nodeIds)){
						going = false;
						break;
					}
				}
			}
		}
		return retval;
	}
	
	/**
	 * 
	 * @return
	 */
	TLongHashSet get_mrca_taxa_limits(HashSet<String> taxa_names){
		TLongHashSet mrcas = new TLongHashSet();
		TLongHashSet nodeIds = new TLongHashSet();
		Node curnode = null;
		for(String tst: taxa_names){
			IndexHits<Node> hit = speciesIndex.get("species", tst);
			Node tnode = hit.getSingle();
			if(tnode.hasRelationship(RelType.IS_CHILDOF, Direction.OUTGOING)){
				curnode = tnode;
			}
			nodeIds.add(tnode.getId());
		}
		boolean going = true;
		while(going){
			mrcas = new TLongHashSet((long []) curnode.getProperty("mrca"));
			if(mrcas.containsAll(nodeIds)){
				going = false;
				break;
			}	
			try{
				Relationship currel = curnode.getSingleRelationship(RelType.IS_CHILDOF, Direction.OUTGOING);
				curnode = currel.getEndNode();
			}catch(Exception e){
				System.out.println(curnode +" not in tree");
				going = false;
				break;
			}
		}
		return mrcas;
	}
}
