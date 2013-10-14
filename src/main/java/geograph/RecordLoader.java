package geograph;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;
import org.opentree.graphdb.GraphDatabaseAgent;

public class RecordLoader {

	private GraphDatabaseAgent gda;
	Index<Node> nodeIndexLat;
	Index<Node> nodeIndexLong;
	Index<Node> speciesIndex;
	Index<Node> metaDataIndex;

	
	public RecordLoader(GraphDatabaseAgent gda){
		this.gda = gda;
		nodeIndexLat = this.gda.getNodeIndex("latitude","type", "exact");
		nodeIndexLong = this.gda.getNodeIndex("longitude","type", "exact");
		speciesIndex = this.gda.getNodeIndex("species","type","exact");//should be replaced with the taxonomy from otu
		metaDataIndex = this.gda.getNodeIndex("source", "type", "exact");
	}
	
	/**
	 * This assumes that the file is in the format
	 * speciesname\tlongitude\tlatitude.......
	 */
	public void loadTABrecord(String filename){
		double cellsize = 0;
		//all for now have the same cell size, so just getting one of the metadata nodes and getting the cell size
		IndexHits<Node> hitssources = metaDataIndex.query("sourcename","*");
		System.out.println(hitssources.size());
		Node metadata = hitssources.next();
		cellsize = (Double)metadata.getProperty("cellsize");
		hitssources.close();
		int counter = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			Transaction tx = null;
			try{
				tx = gda.beginTx();
				while(line != null){
					String [] spls = line.split("\t");
					String speciesname = spls[0];
					Double longitude = Double.valueOf(spls[1]);
					Double latitude = Double.valueOf(spls[2]);
					Node species = null;
					
					//in the future this will need something on the front end to match to the taxonomy 
					// so that things will match uniquely
					IndexHits<Node> hits = speciesIndex.get("species", speciesname);
					if(hits.hasNext()){
						species = hits.next();
					}else{
						species = gda.createNode();
						species.setProperty("name",speciesname);
						speciesIndex.add(species, "species", speciesname);
					}
					hits.close();
					//create the record
					Node record = gda.createNode();
					record.setProperty("longitude", longitude);
					record.setProperty("latitude", latitude);
					//connect the species to the record
					species.createRelationshipTo(record, RelType.HAS_RECORD);
					
					//find the geographic area on which to connect
					boolean exists = false;
					Node nd = null;
					//index check
					double stlat = latitude-cellsize;
					double stoplat = latitude;
					double stlong = longitude-cellsize;
					double stoplong = longitude;
					IndexHits <Node> lathits = nodeIndexLat.query(QueryContext.numericRange("start", stlat, stoplat,true,false));
					IndexHits <Node> longhits = nodeIndexLong.query(QueryContext.numericRange("start", stlong, stoplong,true,false));
					//System.out.println("long: "+longitude+" lat: "+latitude+" stlat: "+stlat+" stoplat: "+stoplat+ " stlong: "+stlong+" stoplong: "+stoplong);
					//System.out.println(lathits.size()+ " " +longhits.size());
					HashSet<Node> latnodes = new HashSet<Node>();
					HashSet<Node> keepers = new HashSet<Node>();
					while(lathits.hasNext()){
						Node ltnd = lathits.next();
					//	System.out.println(ltnd.getProperty("latitude")+" "+ltnd.getProperty("stop_latitude")+" "+ltnd.getProperty("longitude")+" "+ltnd.getProperty("stop_longitude"));
						latnodes.add(ltnd);
					}
					while(longhits.hasNext()){
						Node ln = longhits.next();
						if(latnodes.contains(ln)){
							keepers.add(ln);
						}
					}
					lathits.close();
					longhits.close();
					//end index check
					if(keepers.size()>0){
						//System.out.println("exists: "+keepers);
						exists = true;
						nd = keepers.iterator().next();
					}
					if(nd != null)
						record.createRelationshipTo(nd, RelType.IS_LOCATED);
					counter += 1;
					if(counter % 10000 == 0){
						System.out.println(counter);
						tx.success();
						tx.finish();
						tx = gda.beginTx();
					}
					line = br.readLine();
				}
				tx.success();
			}finally{
				tx.finish();
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
