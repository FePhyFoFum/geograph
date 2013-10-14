package geograph;

import java.io.*;
import java.util.HashSet;

import org.apache.lucene.search.TermRangeQuery;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.*;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.index.lucene.ValueContext;
import org.opentree.graphdb.*;


public class AsciLayerReader {

	private String NODATA_value;
	private double cellsize;
	private double xllcorner;
	private double yllcorner;
	private int nrows;
	private int ncols;
	private GraphDatabaseAgent gda;
	Index<Node> nodeIndexLatLong;
	Index<Node> nodeIndexLat;
	Index<Node> nodeIndexLong;
	Index<Node> metaDataIndex;
	
	public AsciLayerReader(GraphDatabaseAgent gda){
		this.gda = gda;
		nodeIndexLatLong = this.gda.getNodeIndex("latlong","type","exact");
		nodeIndexLat = this.gda.getNodeIndex("latitude","type", "exact");
		nodeIndexLong = this.gda.getNodeIndex("longitude","type", "exact");
		metaDataIndex = this.gda.getNodeIndex("source", "type", "exact");
	}
	
	/**
	 * Assumes a standard ASCI geography file with the header lines like (values are examples)
	 * ncols         720
	 * nrows         280
	 * xllcorner     -180
	 * yllcorner     -56
	 * cellsize      0.5
	 * NODATA_value  -9999
	 * 
	 * also assumes that the data contained within are doubles
	 * 
	 * @param sourcename
	 * @param filename
	 */
	public void readFile(String sourcename, String filename){
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			boolean first = true;
			Node metadata = null;
			Transaction tx = null;
			try{
				tx = gda.beginTx();
				int linecount = 0; 
				while(line != null){
					String [] spls = line.split(" ");
					if (linecount == 0){
						ncols = Integer.valueOf(spls[spls.length-1]);
					}else  if(linecount == 1){
						nrows = Integer.valueOf(spls[spls.length-1]);
					}else  if(linecount == 2){
						xllcorner = Double.valueOf(spls[spls.length-1]);
					}else  if(linecount == 3){
						yllcorner = Double.valueOf(spls[spls.length-1]);
					}else  if(linecount == 4){
						cellsize = Double.valueOf(spls[spls.length-1]);
					}else  if(linecount == 5){
						NODATA_value = spls[spls.length-1];//better compare as string
						//at this point we can create the metadatanode
						metadata = gda.createNode();
						metadata.setProperty("sourcename",sourcename);
						metadata.setProperty("ncols", ncols);
						metadata.setProperty("nrows", nrows);
						metadata.setProperty("xllcorner", xllcorner);
						metadata.setProperty("yllcorner", yllcorner);
						metadata.setProperty("cellsize", cellsize);
						metadata.setProperty("NODATA_value", NODATA_value);
						metaDataIndex.add(metadata, "sourcename", sourcename);
					}else{
						int mlinecount = linecount - 5;
						double latitude = (yllcorner + ((nrows-mlinecount) * cellsize));
						if (first == true){
							metadata.setProperty("yulcorner",latitude);
							first = false;
						}
						for(int i=0;i<spls.length;i++){
							double longitude = (xllcorner + ((ncols-(spls.length-i)) * cellsize));
							if(spls[i].equals(NODATA_value)==false){
								//check to see if the node exists
								boolean exists = false;
								Node nd = null;
								//index check
								double stlat = latitude;
								double stoplat = latitude+cellsize;
								double stlong = longitude;
								double stoplong = longitude+cellsize;
								IndexHits <Node> lathits = nodeIndexLat.query(QueryContext.numericRange("start", stlat, stoplat,true,false));
								IndexHits <Node> longhits = nodeIndexLong.query(QueryContext.numericRange("start", stlong, stoplong,true,false));
								HashSet<Node> latnodes = new HashSet<Node>();
								HashSet<Node> keepers = new HashSet<Node>();
								while(lathits.hasNext()){
									latnodes.add(lathits.next());
								}
								while(longhits.hasNext()){
									Node ln = longhits.next();
									if(cellsize != (Double)ln.getProperty("cellsize")){
										//if it is small, keep as a child that the other should potentially connect to if 
										// we are creating a new node
										continue;
									}
									if(latnodes.contains(ln)){
										keepers.add(ln);
									}
								}
								lathits.close();
								longhits.close();
								//end index check
								if(keepers.size()>0){
//									System.out.println("exists: "+keepers);
									exists = true;
									nd = keepers.iterator().next();
								}
								if(exists == false){
									//create a node for the lat and long
									nd = gda.createNode();
									//add some properties
									nd.setProperty("longitude", longitude);
									nd.setProperty("latitude", latitude);
									nd.setProperty("stop_longitude",longitude+cellsize);
									nd.setProperty("stop_latitude", latitude+cellsize);
									nd.setProperty("cellsize", cellsize);
									//add children to it
									
									//add to parents
									
									nodeIndexLat.add( nd, "start", new ValueContext( latitude ).indexNumeric() );
									nodeIndexLat.add( nd, "stop", new ValueContext( latitude+cellsize ).indexNumeric() );
									nodeIndexLong.add( nd, "start", new ValueContext( longitude ).indexNumeric() );
									nodeIndexLong.add( nd, "stop", new ValueContext( longitude+cellsize ).indexNumeric() );
									nodeIndexLatLong.add( nd, "startlat", new ValueContext( latitude ).indexNumeric() );
									nodeIndexLatLong.add( nd, "stoplat", new ValueContext( latitude+cellsize ).indexNumeric() );
									nodeIndexLatLong.add( nd, "startlong", new ValueContext( longitude ).indexNumeric() );
									nodeIndexLatLong.add( nd, "stoplong", new ValueContext( longitude+cellsize ).indexNumeric() );
								}
								nd.setProperty(sourcename, Double.valueOf(spls[i]));
							}
						}
					}
					linecount += 1;
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
		System.out.println("ncols: "+ncols);
		System.out.println("nrows: "+nrows);
		System.out.println("xllcorner: "+xllcorner);
		System.out.println("yllcorner: "+yllcorner);
		System.out.println("cellsize: "+cellsize);
		System.out.println("NODATA_value: "+NODATA_value);
	}
	
	public void doneWithReader(){
		gda.shutdownDb();
	}

}
