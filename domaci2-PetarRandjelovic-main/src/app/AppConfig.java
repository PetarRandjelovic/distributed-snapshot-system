package app;

import app.snapshot_bitcake.LYSnapshotResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class contains all the global application configuration stuff.
 * @author bmilojkovic
 *
 */
public class AppConfig {

	/**
	 * Convenience access for this servent's information
	 */
	public static ServentInfo myServentInfo;

	public static List<ServentInfo> serventInfoList = new ArrayList<>();

	/**
	 * If this is true, the system is a clique - all nodes are each other's
	 * neighbors. 
	 */
	public static boolean IS_CLIQUE;

	public static AtomicBoolean isWhite = new AtomicBoolean(true);


	public static ArrayList<Integer> inicijatorListApp = new ArrayList<>();

	public static Object colorLock = new Object();

	public static CopyOnWriteArrayList<Integer> rejectedList= new CopyOnWriteArrayList<>();

	public static Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();

	public static Map<Integer, LYSnapshotResult> uzeteVrednosti = new ConcurrentHashMap<>();

	public static ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> parentMap = new ConcurrentHashMap<>();

	public static AtomicInteger supervisordId = new AtomicInteger(-1);

	public static AtomicInteger test1 = new AtomicInteger(0);

	public static AtomicInteger sveKomsijeSuOdgovorili = new AtomicInteger(0);

	public static AtomicInteger snapshotVersion = new AtomicInteger(0);

	public static AtomicInteger parentId = new AtomicInteger(-1);

	public static AtomicBoolean canStartCounting = new AtomicBoolean(false);

	public static AtomicBoolean startCountingNowOvo = new AtomicBoolean(false);

	public static AtomicInteger brojacZaMeeting = new AtomicInteger(0);

	/**
	 * Print a message to stdout with a timestamp
	 * @param message message to print
	 */
	public static void timestampedStandardPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();

		System.out.println(timeFormat.format(now) + " - " + message);
	}

	/**
	 * Print a message to stderr with a timestamp
	 * @param message message to print
	 */
	public static void timestampedErrorPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();

		System.err.println(timeFormat.format(now) + " - " + message);
	}

	/**
	 * Reads a config file. Should be called once at start of app.
	 * The config file should be of the following format:
	 * <br/>
	 * <code><br/>
	 * servent_count=3 			- number of servents in the system <br/>
	 * clique=false 			- is it a clique or not <br/>
	 * fifo=false				- should sending be fifo
	 * servent0.port=1100 		- listener ports for each servent <br/>
	 * servent1.port=1200 <br/>
	 * servent2.port=1300 <br/>
	 * servent0.neighbors=1,2 	- if not a clique, who are the neighbors <br/>
	 * servent1.neighbors=0 <br/>
	 * servent2.neighbors=0 <br/>
	 *
	 * </code>
	 * <br/>
	 * So in this case, we would have three servents, listening on ports:
	 * 1100, 1200, and 1300. This is not a clique, and:<br/>
	 * servent 0 sees servent 1 and 2<br/>
	 * servent 1 sees servent 0<br/>
	 * servent 2 sees servent 0<br/>
	 *
	 * @param configName name of configuration file
	 */
	public static void readConfig(String configName){
		rejectedList.add(-2);
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(configName)));

		} catch (IOException e) {
			timestampedErrorPrint("Couldn't open properties file. Exiting...");
			System.exit(0);
		}

		int serventCount = -1;
		try {
			serventCount = Integer.parseInt(properties.getProperty("servent_count"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading servent_count. Exiting...");
			System.exit(0);
		}

		IS_CLIQUE = Boolean.parseBoolean(properties.getProperty("clique", "false"));
		String snapshotType = properties.getProperty("snapshot");
		if (snapshotType == null) {
			snapshotType = "none";
		}

		String inicijatorList = properties.getProperty("initiators");
		String[] inicijatorListArr = inicijatorList.split(",");

		for (int i = 0; i < inicijatorListArr.length; i++) {
			if(inicijatorListApp.contains(Integer.parseInt(inicijatorListArr[i]))){
				continue;
			}

			inicijatorListApp.add(Integer.parseInt(inicijatorListArr[i]));
		}

		for (int i = 0; i < serventCount; i++) {
			String portProperty = "servent"+i+".port";

			int serventPort = -1;

			try {
				serventPort = Integer.parseInt(properties.getProperty(portProperty));
			} catch (NumberFormatException e) {
				timestampedErrorPrint("Problem reading " + portProperty + ". Exiting...");
				System.exit(0);
			}

			List<Integer> neighborList = new ArrayList<>();
			if (IS_CLIQUE) {
				for(int j = 0; j < serventCount; j++) {
					if (j == i) {
						continue;
					}

					neighborList.add(j);
				}
			} else {
				String neighborListProp = properties.getProperty("servent"+i+".neighbors");

				if (neighborListProp == null) {
					timestampedErrorPrint("Warning: graph is not clique, and node " + i + " doesnt have neighbors");
				} else {
					String[] neighborListArr = neighborListProp.split(",");

					try {
						for (String neighbor : neighborListArr) {
							neighborList.add(Integer.parseInt(neighbor));
						}
					} catch (NumberFormatException e) {
						timestampedErrorPrint("Bad neighbor list for node " + i + ": " + neighborListProp);
					}
				}
			}


			//	ServentInfo newInfo = new ServentInfo("localhost", i, serventPort, neighborList);
			ServentInfo newInfo = new ServentInfo("localhost", i, serventPort, neighborList,AppConfig.snapshotVersion.get());
			serventInfoList.add(newInfo);
		}
	}

	/**
	 * Get info for a servent selected by a given id.
	 * @param id id of servent to get info for
	 * @return {@link ServentInfo} object for this id
	 */
	public static ServentInfo getInfoById(int id) {
		if (id >= getServentCount()) {
			throw new IllegalArgumentException(
					"Trying to get info for servent " + id + " when there are " + getServentCount() + " servents.");
		}
		return serventInfoList.get(id);
	}

//	public static int getVersionId() {
//		return myServentInfo.getVersionId();
//	}

	/**
	 * Get number of servents in this system.
	 */
	public static int getServentCount() {
		return serventInfoList.size();
	}

	public int getVersionIds(){
		return snapshotVersion.get();
	}

	public static CopyOnWriteArrayList<Integer> getRejectedList() {
		return rejectedList;
	}
}