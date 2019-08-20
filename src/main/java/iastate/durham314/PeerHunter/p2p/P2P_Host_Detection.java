package iastate.durham314.PeerHunter.p2p;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.log4j.BasicConfigurator;

import iastate.durham314.PeerHunter.conf.PeerHunter_Conf;
import iastate.durham314.PeerHunter.main.FileModi;

public class P2P_Host_Detection {

	public static class Generate_Mutual_Contact_Sets_Mapper extends Mapper<Object, Text, Text, Text> {
		@Override
		protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String[] sets = value.toString().split("\t");
			String[] set_1 = sets[1].split(",");
			context.write(new Text(sets[0] + "," + set_1[0] + "," + set_1[2] + "," + set_1[3]), new Text(set_1[1]));

		}
	}
	public static class Generate_Mutual_Contact_Sets_Reducer extends Reducer<Text, Text, Text, Text> {
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			Set<String> nodeSet = new HashSet<String>();
			for (Text i : values) {
				nodeSet.add(i.toString());
			}
			if (nodeSet.size() > 0) {
				context.write(new Text(key.toString()), new Text(nodeSet.toString()));
			}
			nodeSet.clear();
		}
	}

	public static class P2P_Host_Detection_Mapper extends Mapper<Object, Text, Text, Text> {
		@Override
		protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String[] line = value.toString().split("\t");
			String[] sets = line[1].split(",");
			context.write(new Text(line[0] + "," + sets[0] + "," + sets[2] + "," + sets[3]), new Text(sets[1]));
		}
	}

	public static class P2P_Host_Detection_Mapper2 extends Mapper<Object, Text, Text, Text> {
		@Override
		protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			context.write(new Text(value.toString().split("\t")[0]), new Text("1"));
		}
	}

	public static class P2P_Host_Detection_Reducer extends Reducer<Text, Text, Text, Text> {
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			Set<String> Distinct16Set = new HashSet<String>();
			Set<String> Distinct32Set = new HashSet<String>();
			List<String> cache = new ArrayList<String>();
			int flag = -1;
			for (Text i : values) {
				String[] sets = i.toString().split("\\.");
				Distinct16Set.add(sets[0] + "." + sets[1]);
				Distinct32Set.add(i.toString());
				cache.add(i.toString());
				if (Distinct16Set.size() >= p2p_host_detection_threshold
						&& Distinct32Set.size() >= p2p_host_detection_threshold_number_of_ips) {
					flag = 1;
					break;
				}
			}
			if (flag == 1) {
				String[] sets = key.toString().split(",");
				for (String i : cache) {
					context.write(new Text(sets[0]), new Text(sets[1] + "," + i + "," + sets[2] + "," + sets[3]));
				}
				for (Text i : values) {
					context.write(new Text(sets[0]),
							new Text(sets[1] + "," + i.toString() + "," + sets[2] + "," + sets[3]));
				}
			} else {
				cache.clear();
			}

		}
	}

	public static class P2P_Host_Detection_Reducer2 extends Reducer<Text, Text, Text, Text> {
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			if (IP_MAP.containsKey(key.toString())) {
				context.write(new Text(key.toString()), new Text(IP_MAP.get(key.toString())));
			} else {
				context.write(new Text(key.toString()), new Text("Normal"));
			}
		}
	}

	private static int p2p_host_detection_threshold = PeerHunter_Conf.P2P_HOST_DETECTION_THRESHOLD_DEFAULT;

	private static int p2p_host_detection_threshold_number_of_ips = PeerHunter_Conf.P2P_HOST_DETECTION_THRESHOLD_NumberOfIPs;

	private static HashMap<String, String> IP_MAP;

	public static void run(String ID) throws IllegalArgumentException, IOException {
		String Graph = "Graph_" + ID;

		String InputFolder = PeerHunter_Conf.ROOT_LOCATION + Graph + "/INPUT/P2P_Legi_Map";
		File folder = new File(InputFolder + "/");
		File[] listOfFiles = folder.listFiles();
		IP_MAP = new HashMap<String, String>();

		for (File file : listOfFiles) {
			if (file.isFile() && !file.getName().substring(0, 1).equals(".")) {
				BufferedReader br = new BufferedReader(new FileReader(InputFolder + "/" + file.getName()));
				String line = "";
				while ((line = br.readLine()) != null && line.contains(".")) {
					String[] lines = line.split("\t");
					IP_MAP.put(lines[1], lines[2]);
				}
				br.close();
			}
		}

		BasicConfigurator.configure();
		JobConf conf = new JobConf(P2P_Host_Detection.class);

		FileModi.deleteDir(new File(PeerHunter_Conf.ROOT_LOCATION + Graph + "/p2p_host_detection"));

		Job Job_p2p_host_detection = Job.getInstance();
		Job_p2p_host_detection.setJobName("Job_p2p_host_detection_" + Graph);
		Job_p2p_host_detection.setJarByClass(P2P_Host_Detection.class);
		Job_p2p_host_detection.setMapperClass(P2P_Host_Detection_Mapper.class);
		Job_p2p_host_detection.setReducerClass(P2P_Host_Detection_Reducer.class);
		Job_p2p_host_detection.setOutputKeyClass(Text.class);
		Job_p2p_host_detection.setOutputValueClass(Text.class);
		ControlledJob ctrl_Job_p2p_host_detection = new ControlledJob(conf);
		ctrl_Job_p2p_host_detection.setJob(Job_p2p_host_detection);
		FileInputFormat.addInputPath(Job_p2p_host_detection,
				new Path(PeerHunter_Conf.ROOT_LOCATION + Graph + "/INPUT/Legi"));
		FileInputFormat.addInputPath(Job_p2p_host_detection,
				new Path(PeerHunter_Conf.ROOT_LOCATION + Graph + "/INPUT/P2P"));
		FileInputFormat.addInputPath(Job_p2p_host_detection,
				new Path(PeerHunter_Conf.ROOT_LOCATION + Graph + "/INPUT/Wild_P2P"));
		FileOutputFormat.setOutputPath(Job_p2p_host_detection,
				new Path(PeerHunter_Conf.ROOT_LOCATION + Graph + "/p2p_host_detection"));
		Job_p2p_host_detection.setNumReduceTasks(18);

		FileModi.deleteDir(new File(PeerHunter_Conf.ROOT_LOCATION + Graph + "/mutual_contact_sets"));
		Job Job_Generate_Mutual_Contact_Sets = Job.getInstance();
		Job_Generate_Mutual_Contact_Sets.setJobName("Job_Generate_Mutual_Contact_Sets_" + Graph);
		Job_Generate_Mutual_Contact_Sets.setJarByClass(P2P_Host_Detection.class);
		Job_Generate_Mutual_Contact_Sets.setMapperClass(Generate_Mutual_Contact_Sets_Mapper.class);
		Job_Generate_Mutual_Contact_Sets.setReducerClass(Generate_Mutual_Contact_Sets_Reducer.class);
		Job_Generate_Mutual_Contact_Sets.setOutputKeyClass(Text.class);
		Job_Generate_Mutual_Contact_Sets.setOutputValueClass(Text.class);
		ControlledJob ctrl_Job_Generate_Mutual_Contact_Sets = new ControlledJob(conf);
		ctrl_Job_Generate_Mutual_Contact_Sets.setJob(Job_Generate_Mutual_Contact_Sets);
		FileInputFormat.addInputPath(Job_Generate_Mutual_Contact_Sets,
				new Path(PeerHunter_Conf.ROOT_LOCATION + Graph + "/p2p_host_detection"));
		FileOutputFormat.setOutputPath(Job_Generate_Mutual_Contact_Sets,
				new Path(PeerHunter_Conf.ROOT_LOCATION + Graph + "/mutual_contact_sets"));
		ctrl_Job_Generate_Mutual_Contact_Sets.addDependingJob(ctrl_Job_p2p_host_detection);
		Job_Generate_Mutual_Contact_Sets.setNumReduceTasks(1);

		JobControl jobCtrl = new JobControl("ctrl_p2p_host_detection");
		jobCtrl.addJob(ctrl_Job_p2p_host_detection);
		jobCtrl.addJob(ctrl_Job_Generate_Mutual_Contact_Sets);
		Thread t = new Thread(jobCtrl);
		t.start();

		while (true) {

			if (jobCtrl.allFinished()) {
				System.out.println(jobCtrl.getSuccessfulJobList());
				jobCtrl.stop();
				break;
			}
		}
	}
}