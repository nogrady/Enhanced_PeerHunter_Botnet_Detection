package iastate.durham314.PeerHunter.conf;

public class PeerHunter_Conf {

	public static int[] P2P_HOST_DETECTION_THRESHOLD_SET = { 2, 5, 10, 13, 15, 17, 20, 25, 30, 35, 40, 45, 50, 70, 100,
			130, 150, 180, 200, 500, 1000, 5000, 10000, 12500 };
	public static int P2P_HOST_DETECTION_THRESHOLD_DEFAULT = 30;
	public static int P2P_HOST_DETECTION_THRESHOLD_NumberOfIPs = 0;
	public static double MUTUAL_CONTACT_SCORE_THRESHOLD = 0;
	public static double LOUVAIN_COMMUNITY_DETECTION_RESOLUTION = 1.0;
	public static double BOTNET_DETECTION_THRESHOLD_BGP = 0.5;
	public static double BOTNET_DETECTION_THRESHOLD_MCS = 0.2;
	public static String ROOT_LOCATION = "data/";
	public static double MMK_ATT_RATIO = 0.0;
	public static double[] MMK_ATT_RATIO_SET = { 0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6, 0.7,
			0.8, 0.9, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.4, 2.8, 3.0, 4.5, 6.0 };
	public static double[] BOTNET_DETECTION_THRESHOLD_BGP_SET = { 0.5 };
	public static double[] BOTNET_DETECTION_THRESHOLD_MCS_SET = { 0.2 };
}
