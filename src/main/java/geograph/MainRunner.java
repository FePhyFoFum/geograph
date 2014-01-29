package geograph;

import gnu.trove.set.hash.TLongHashSet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import org.opentree.graphdb.GraphDatabaseAgent;

import treethings.TreeImporter;
import treethings.TreeJanitor;
import jade.tree.*;

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
		gda.shutdownDb();
	}
	
	//args[1] database
	private static void printMap(String [] args){
		GraphDatabaseAgent gda = new GraphDatabaseAgent(args[1]);
		MapPrinter mp = new MapPrinter(gda);
		//mp.printSimpleMap(50., -95.,true, "Bio01");
		mp.printSimpleMap(51.,-90., false, "");
		gda.shutdownDb();
	}
	
	//args[1] database
	private static void printPhyloDivMap(String [] args){
		GraphDatabaseAgent gda = new GraphDatabaseAgent(args[1]);
		MapPrinter mp = new MapPrinter(gda);
		HashSet<String> ths = new HashSet<String>();ths.add("interrupta");ths.add("subspicata");
		TLongHashSet limits = mp.get_mrca_taxa_limits(ths);
		mp.printPhylogeneticDiversityMap(51.,-125.,limits);
		gda.shutdownDb();
	}
	
	//args[1] database, args[2] startlat, args[3] startlong, args...limits
	private static void printPhyloDivMapFullArgs(String [] args){
		GraphDatabaseAgent gda = new GraphDatabaseAgent(args[1]);
		MapPrinter mp = new MapPrinter(gda);
		double startlat = Double.valueOf(args[2]);
		double startlong = Double.valueOf(args[3]);
		HashSet<String> ths = null;
		TLongHashSet limits = null;
		if(args.length > 4){
			ths = new HashSet<String>();
			for(int i=4;i<args.length;i++){
				ths.add(args[i]);
			}
			limits = mp.get_mrca_taxa_limits(ths);
		}
		mp.printPhylogeneticDiversityMap(startlat,startlong,limits);
		gda.shutdownDb();
	}
	
	//args[1] newick file, args[2] database
	private static void readNewickTree(String [] args){
		GraphDatabaseAgent gda = new GraphDatabaseAgent(args[2]);
		String filename = args[1];
		TreeReader tr = new TreeReader ();
		JadeTree tree = null;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filename));
			tree = tr.readTree(br.readLine());
			tree.assocObject("treename", filename);
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TreeImporter ti = new TreeImporter(gda);
		ti.loadTree(tree);
		gda.shutdownDb();
	}
	
	//args [1] name (indexed as source), args[2] database
	private static void deleteTree(String [] args){
		GraphDatabaseAgent gda = new GraphDatabaseAgent(args[2]);
		String source = args[1];
		TreeJanitor tj = new TreeJanitor(gda);
		tj.deleteTreeFromSource(source);
		gda.shutdownDb();
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
		else if(args[0].equals("printphylomap") && args.length == 2)
			MainRunner.printPhyloDivMap(args);
		else if(args[0].equals("printphylomap") && args.length >= 4)
			MainRunner.printPhyloDivMapFullArgs(args);
		else if(args[0].equals("loadnewick") && args.length == 3)
			MainRunner.readNewickTree(args);
		else if(args[0].equals("deletetree") && args.length == 3)
			MainRunner.deleteTree(args);
			
	}

}
