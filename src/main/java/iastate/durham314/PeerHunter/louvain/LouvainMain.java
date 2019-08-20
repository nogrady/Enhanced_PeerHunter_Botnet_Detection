package iastate.durham314.PeerHunter.louvain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import iastate.durham314.PeerHunter.conf.PeerHunter_Conf;
import iastate.durham314.PeerHunter.main.FileModi;

public class LouvainMain {

	public static void Louvain(String ID, double resolution_) throws IOException {
		String Graph = "Graph_" + ID;
		boolean printOutput, update;
		double modularity, maxModularity, resolution, resolution2;
		int algorithm, i, j, modularityFunction, nClusters, nIterations, nRandomStarts;
		int[] cluster;
		long beginTime, endTime, randomSeed;
		Network network;
		Random random;

		File f = new File(PeerHunter_Conf.ROOT_LOCATION + Graph + "/louvain_communities_detection");
		if (!f.exists()) {
			if (f.mkdir()) {
				System.out.println("Directory " + PeerHunter_Conf.ROOT_LOCATION + Graph
						+ "/louvain_communities_detection" + " is created!");
			} else {
				System.out.println("Failed to create directory!");
			}
		}

		modularityFunction = 1;
		resolution = resolution_;
		algorithm = 1;
		nRandomStarts = 100;
		nIterations = 100;
		randomSeed = 0;
		printOutput = false; // (1 > 0);

		String inputFileName = PeerHunter_Conf.ROOT_LOCATION + Graph + "/mutual_contact_graph/LouvainInput.txt";
		String outputFileName = PeerHunter_Conf.ROOT_LOCATION + Graph + "/louvain_communities_detection/" + Graph + "_"
				+ resolution + ".txt";

		if (printOutput) {
			System.out.println("Reading input file...");
			System.out.println();
		}

		network = readInputFile(inputFileName, modularityFunction);

		if (printOutput) {
			System.out.format("Number of nodes: %d%n", network.getNNodes());
			System.out.format("Number of edges: %d%n", network.getNEdges() / 2);
			System.out.println();
			System.out.println("Running " + ((algorithm == 1) ? "Louvain algorithm"
					: ((algorithm == 2) ? "Louvain algorithm with multilevel refinement"
							: "smart local moving algorithm"))
					+ "...");
			System.out.println();
		}

		resolution2 = ((modularityFunction == 1) ? (resolution / network.getTotalEdgeWeight()) : resolution);

		beginTime = System.currentTimeMillis();
		cluster = null;
		nClusters = -1;
		maxModularity = Double.NEGATIVE_INFINITY;
		random = new Random(randomSeed);
		for (i = 0; i < nRandomStarts; i++) {
			if (printOutput && (nRandomStarts > 1))
				System.out.format("Random start: %d%n", i + 1);

			network.initSingletonClusters();

			j = 0;
			update = true;
			do {
				if (printOutput && (nIterations > 1))
					System.out.format("Iteration: %d%n", j + 1);

				if (algorithm == 1)
					update = network.runLouvainAlgorithm(resolution2, random);
				else if (algorithm == 2)
					update = network.runLouvainAlgorithmWithMultilevelRefinement(resolution2, random);
				else if (algorithm == 3)
					network.runSmartLocalMovingAlgorithm(resolution2, random);
				j++;

				modularity = network.calcQualityFunction(resolution2);

				if (printOutput && (nIterations > 1))
					System.out.format("Modularity: %.4f%n", modularity);
			} while ((j < nIterations) && update);

			if (modularity > maxModularity) {
				network.orderClustersByNNodes();
				cluster = network.getClusters();
				nClusters = network.getNClusters();
				maxModularity = modularity;
			}

			if (printOutput && (nRandomStarts > 1)) {
				if (nIterations == 1)
					System.out.format("Modularity: %.4f%n", modularity);
				System.out.println();
			}
		}
		endTime = System.currentTimeMillis();

		if (printOutput) {
			if (nRandomStarts == 1) {
				if (nIterations > 1)
					System.out.println();
				System.out.format("Modularity: %.4f%n", maxModularity);
			} else
				System.out.format("Maximum modularity in %d random starts: %.4f%n", nRandomStarts, maxModularity);
			System.out.format("Number of communities: %d%n", nClusters);
			System.out.format("Elapsed time: %d seconds%n", Math.round((endTime - beginTime) / 1000.0));
			System.out.println();
			System.out.println("Writing output file...");
			System.out.println();
		}

		writeOutputFile(outputFileName, cluster);

		System.out.println("Communities are OK!");
	}

	private static Network readInputFile(String fileName, int modularityFunction) throws IOException {
		BufferedReader bufferedReader;
		double[] edgeWeight1, edgeWeight2, nodeWeight;
		int i, j, nEdges, nLines, nNodes;
		int[] firstNeighborIndex, neighbor, nNeighbors, node1, node2;
		Network network;
		String[] splittedLine;

		bufferedReader = new BufferedReader(new FileReader(fileName));

		nLines = 0;
		while (bufferedReader.readLine() != null)
			nLines++;

		bufferedReader.close();

		bufferedReader = new BufferedReader(new FileReader(fileName));

		node1 = new int[nLines];
		node2 = new int[nLines];
		edgeWeight1 = new double[nLines];
		i = -1;
		for (j = 0; j < nLines; j++) {
			splittedLine = bufferedReader.readLine().split("\t");
			node1[j] = Integer.parseInt(splittedLine[0]);
			if (node1[j] > i)
				i = node1[j];
			node2[j] = Integer.parseInt(splittedLine[1]);
			if (node2[j] > i)
				i = node2[j];
			edgeWeight1[j] = (splittedLine.length > 2) ? Double.parseDouble(splittedLine[2]) : 1;
		}
		nNodes = i + 1;

		bufferedReader.close();

		nNeighbors = new int[nNodes];
		for (i = 0; i < nLines; i++)
			if (node1[i] < node2[i]) {
				nNeighbors[node1[i]]++;
				nNeighbors[node2[i]]++;
			}

		firstNeighborIndex = new int[nNodes + 1];
		nEdges = 0;
		for (i = 0; i < nNodes; i++) {
			firstNeighborIndex[i] = nEdges;
			nEdges += nNeighbors[i];
		}
		firstNeighborIndex[nNodes] = nEdges;

		neighbor = new int[nEdges];
		edgeWeight2 = new double[nEdges];
		Arrays.fill(nNeighbors, 0);
		for (i = 0; i < nLines; i++)
			if (node1[i] < node2[i]) {
				j = firstNeighborIndex[node1[i]] + nNeighbors[node1[i]];
				neighbor[j] = node2[i];
				edgeWeight2[j] = edgeWeight1[i];
				nNeighbors[node1[i]]++;
				j = firstNeighborIndex[node2[i]] + nNeighbors[node2[i]];
				neighbor[j] = node1[i];
				edgeWeight2[j] = edgeWeight1[i];
				nNeighbors[node2[i]]++;
			}

		if (modularityFunction == 1) {
			nodeWeight = new double[nNodes];
			for (i = 0; i < nEdges; i++)
				nodeWeight[neighbor[i]] += edgeWeight2[i];
			network = new Network(nNodes, firstNeighborIndex, neighbor, edgeWeight2, nodeWeight);
		} else
			network = new Network(nNodes, firstNeighborIndex, neighbor, edgeWeight2);

		return network;
	}

	public static void run(String ID) throws IllegalArgumentException, IOException {
		String Graph = "Graph_" + ID;
		FileModi.deleteDir(new File(PeerHunter_Conf.ROOT_LOCATION + Graph + "/louvain_communities_detection"));
		double i = PeerHunter_Conf.LOUVAIN_COMMUNITY_DETECTION_RESOLUTION;
		LouvainMain.Louvain(ID, i);
	}

	private static void writeOutputFile(String fileName, int[] cluster) throws IOException {
		BufferedWriter bufferedWriter;
		int i;

		bufferedWriter = new BufferedWriter(new FileWriter(fileName));

		for (i = 0; i < cluster.length; i++) {
			bufferedWriter.write(i + "," + Integer.toString(cluster[i]));
			bufferedWriter.newLine();
		}

		bufferedWriter.close();
	}
}
