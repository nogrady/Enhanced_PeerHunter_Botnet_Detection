package iastate.durham314.PeerHunter.mcg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class Generate_Mutual_Contact_Graph_Runnable implements Runnable {
	private String node1IPSet;
	private String node2IPSet;
	private String OutputFolder;
	private int node1;
	private int node2;
	private Thread t;
	private double mutual_contact_score_threshold;

	Generate_Mutual_Contact_Graph_Runnable(String OutputFolder_, String node1IPSet_, String node2IPSet_, int node1_,
			int node2_, double mutual_contact_score_threshold_) {
		node1IPSet = node1IPSet_;
		node2IPSet = node2IPSet_;
		node1 = node1_;
		node2 = node2_;
		OutputFolder = OutputFolder_;
		mutual_contact_score_threshold = mutual_contact_score_threshold_;
	}

	@Override
	public void run() {
		String[] st1 = node1IPSet.split(", ");
		String[] st2 = node2IPSet.split(", ");
		double score = -1;
		Set<String> set1 = new HashSet<String>();
		for (String line : st1) {
			set1.add(line);
		}
		Set<String> set2 = new HashSet<String>();
		for (String line : st2) {
			set2.add(line);
		}

		Set<String> maxset = set1;
		Set<String> minset = set2;

		if (set1.size() < set2.size()) {
			maxset = set2;
			minset = set1;
		}
		int a = minset.size();
		int b = maxset.size();
		minset.retainAll(maxset);
		int c = minset.size();

		set1.clear();
		set2.clear();
		minset.clear();
		maxset.clear();
		set1 = null;
		set2 = null;
		minset = null;
		maxset = null;
		score = (double) (c) / (double) (a + b - c);

		if (c > 0 && score > mutual_contact_score_threshold) {
			try {
				PrintWriter writer = new PrintWriter(
						new FileOutputStream(new File(OutputFolder + "LouvainInput.txt"), true));
				writer.println(node1 + "\t" + node2 + "\t" + score);
				writer.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public void start() {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}
}