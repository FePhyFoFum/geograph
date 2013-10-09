package geograph;

import org.opentree.graphdb.GraphDatabaseAgent;

public class MainRunner {
	
	//args[1] layer file, args[2] name,  args[3] database
	private static void readAsciClimateLayer(String [] args){
		GraphDatabaseAgent gda = new GraphDatabaseAgent(args[3]);
		AsciLayerReader alr = new AsciLayerReader(gda);
		alr.readFile(args[2],args[1]);
		alr.doneWithReader();
	}

	//args[1] record file, args[2] database
	private static void readTabRecords(String [] args){
		GraphDatabaseAgent gda = new GraphDatabaseAgent(args[2]);
		RecordLoader rl = new RecordLoader(gda);
		rl.loadTABrecord(args[1]);
	}
	
	private static void printMap(String [] args){
		GraphDatabaseAgent gda = new GraphDatabaseAgent(args[1]);
		MapPrinter mp = new MapPrinter(gda);
		//mp.printSimpleMap(50., -95.,true, "Bio01");
		mp.printSimpleMap(50.,-125., false, "");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args[0].equals("loadlayer") && args.length == 4)
			MainRunner.readAsciClimateLayer(args);
		else if(args[0].equals("loadrecords") && args.length == 3)
			MainRunner.readTabRecords(args);
		else if(args[0].equals("printmap") && args.length == 2)
			MainRunner.printMap(args);
			
	}

}
