package geograph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.lucene.queryParser.QueryParser.Operator;
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
	
}
