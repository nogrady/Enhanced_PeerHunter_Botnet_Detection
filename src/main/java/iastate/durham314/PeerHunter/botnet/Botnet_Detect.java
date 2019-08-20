package iastate.durham314.PeerHunter.botnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import iastate.durham314.PeerHunter.conf.PeerHunter_Conf;
import iastate.durham314.PeerHunter.main.FileModi;

public class Botnet_Detect {

	public static Set<String> PotentialIP = new HashSet<String>();

	public static double botnet_detection_threshold_bgp;
	public static double botnet_detection_threshold_mcs;

	public static void Botnet_Detection(String Graph) throws IOException {
		FileModi.deleteDir(new File(PeerHunter_Conf.ROOT_LOCATION + Graph + "/botnet_detection"));
		File f = new File(PeerHunter_Conf.ROOT_LOCATION + Graph + "/botnet_detection");

		if (!f.exists()) {
			if (f.mkdir()) {
				System.out.println(
						"Directory " + PeerHunter_Conf.ROOT_LOCATION + Graph + "/botnet_detection" + " is created!");
			} else {
				System.out.println("Failed to create directory!");
			}
		}

		PotentialIP.clear();
		File folder = new File(PeerHunter_Conf.ROOT_LOCATION + Graph + "/louvain_communities_detection");
		File[] listOfFiles = folder.listFiles();

		FileModi.deleteDir(new File(PeerHunter_Conf.ROOT_LOCATION + Graph + "/communities_scores_calculate"));
		f = new File(PeerHunter_Conf.ROOT_LOCATION + Graph + "/communities_scores_calculate");

		if (!f.exists()) {
			if (f.mkdir()) {
				System.out.println("Directory " + PeerHunter_Conf.ROOT_LOCATION + Graph
						+ "/communities_scores_calculate" + " is created!");
			} else {
				System.out.println("Failed to create directory!");
			}
		}

		for (File file : listOfFiles) {
			if (file.isFile() && !file.getName().substring(0, 1).equals(".")) {

				String line = "";
				BufferedReader br = new BufferedReader(new FileReader(file.getPath())); // Read the Community Detection
																						// Result

				HashMap<String, ArrayList<String>> louvain_results = new HashMap<String, ArrayList<String>>();

				while ((line = br.readLine()) != null) {

					String[] str = line.split(",");
					if (louvain_results.containsKey(str[1])) {
						louvain_results.get(str[1]).add(str[0]);
					} else {
						ArrayList<String> temp = new ArrayList<String>();
						temp.add(str[0]);
						louvain_results.put(str[1], temp);
					}
				}
				br.close();

				for (Entry<String, ArrayList<String>> entry : louvain_results.entrySet()) {
					String com_id = entry.getKey();
					ArrayList<String> nodes = entry.getValue();

					HashSet<String> nodes_ips = new HashSet<String>();

					if (nodes.size() > 2) {
						double resolution = Double.parseDouble(file.getName().split("_")[2].split("\\.")[0]);
						PrintWriter pw = new PrintWriter(
								new FileOutputStream(
										new File(PeerHunter_Conf.ROOT_LOCATION + Graph
												+ "/communities_scores_calculate/" + Graph + "_" + resolution + ".txt"),
										true));
						PrintWriter pw_2 = new PrintWriter(new FileOutputStream(new File(PeerHunter_Conf.ROOT_LOCATION
								+ Graph + "/communities_scores_calculate/" + Graph + "_" + resolution + "_2.txt"),
								true));

						double Sum_BGP = 0;
						double Sum_MCS = 0;

						BufferedReader br_BGP = new BufferedReader(new FileReader(
								PeerHunter_Conf.ROOT_LOCATION + Graph + "/mutual_contact_graph/IDtoIP.txt"));

						while ((line = br_BGP.readLine()) != null) {
							if (nodes.contains(line.split("\t")[0])) {
								Sum_BGP += Double.parseDouble(line.split("\t")[3])
										/ Double.parseDouble(line.split("\t")[5]);
								nodes_ips.add(line.split("\t")[1] + "," + line.split("\t")[2] + ","
										+ line.split("\t")[3] + "," + line.split("\t")[4] + "," + line.split("\t")[5]);

							}
						}
						br_BGP.close();

						BufferedReader br_MCS = new BufferedReader(new FileReader(
								PeerHunter_Conf.ROOT_LOCATION + Graph + "/mutual_contact_graph/LouvainInput.txt"));
						while ((line = br_MCS.readLine()) != null) {

							if (nodes.contains(line.split("\t")[0]) && nodes.contains(line.split("\t")[1])) {
								Sum_MCS += Double.parseDouble(line.split("\t")[2]);
							}
						}
						br_MCS.close();

						double m = nodes.size();
						double n = m * (m - 1) / 2;

						pw.println(com_id + "," + m + "," + n + "," + Sum_BGP / m + "," + Sum_MCS / n);

						if (Sum_BGP / m > botnet_detection_threshold_bgp
								&& Sum_MCS / n > botnet_detection_threshold_mcs) {
							pw_2.println(com_id + "," + m + "," + n + "," + Sum_BGP / m + "," + Sum_MCS / n);
							pw_2.println(nodes);
							pw_2.println(nodes_ips);
							PotentialIP.addAll(nodes);

							PrintWriter pw_ = new PrintWriter(
									new FileOutputStream(new File(PeerHunter_Conf.ROOT_LOCATION + Graph
											+ "/botnet_detection/bot_detection_input.txt"), true));
							BufferedReader br_ = new BufferedReader(new FileReader(
									PeerHunter_Conf.ROOT_LOCATION + Graph + "/mutual_contact_graph/LouvainInput.txt"));
							Set<String> Edges = new HashSet<String>();
							while ((line = br_.readLine()) != null) {
								if (nodes.contains(line.split("\t")[0]) && nodes.contains(line.split("\t")[1])) {
									Edges.add(line.split("\t")[0] + "," + line.split("\t")[1]);
								}
							}
							pw_.println(nodes.toString() + "\t" + Edges.toString());
							br_.close();
							pw_.close();
						}
						pw_2.close();
						pw.close();
					}
				}
			}
		}

		PrintWriter pw = new PrintWriter(
				PeerHunter_Conf.ROOT_LOCATION + Graph + "/botnet_detection/botnet_detection.txt");
		BufferedReader br = new BufferedReader(
				new FileReader(PeerHunter_Conf.ROOT_LOCATION + Graph + "/mutual_contact_graph/IDtoIP.txt"));
		String line = "";
		Set<String> PotentialIP_Set = new HashSet<String>();
		while ((line = br.readLine()) != null) {
			if (PotentialIP.contains(line.split("\t")[0])) {
				pw.println(line.split("\t")[0] + "\t" + line.split("\t")[1] + "\t" + line.split("\t")[2]);
				PotentialIP_Set.add(line.split("\t")[1].split(",")[0] + "\t" + line.split("\t")[2]);
			}
		}
		br.close();
		pw.close();

		pw = new PrintWriter(PeerHunter_Conf.ROOT_LOCATION + Graph + "/botnet_detection/botnet_detection_2.txt");
		for (String i : PotentialIP_Set) {
			pw.println(i);
		}
		pw.close();
	}

	public static void run(String ID) throws IllegalArgumentException, IOException {
		for (double botnet_detection_threshold_bgp_double : PeerHunter_Conf.BOTNET_DETECTION_THRESHOLD_BGP_SET) {
			for (double botnet_detection_threshold_mcs_double : PeerHunter_Conf.BOTNET_DETECTION_THRESHOLD_MCS_SET) {
				botnet_detection_threshold_bgp = botnet_detection_threshold_bgp_double;
				botnet_detection_threshold_mcs = botnet_detection_threshold_mcs_double;

				String Graph = "Graph_" + ID;

				Botnet_Detection(Graph);

			}
		}
	}
}