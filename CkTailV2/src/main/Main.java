package main;

import java.io.File;

public class Main {

	public static String log;
	public static String regex;
	public static String output;
	static boolean timerMode;
	public static String mode;
	
	public static void main(String[] args) {
		try {
			FullOptions.setOptions(args);
		} catch (Exception e) {
			System.err.println("pb option");
			System.exit(3);
		}
		createDir();
		String tmp = output + "/splitres";
		final long timeSplit1 = System.currentTimeMillis();
		String[] argsSplit = {"-i", log, "-r", regex, "-o", tmp, mode};
		main.split.MainSplit.main(argsSplit);
		final long timeSplit2 = System.currentTimeMillis();
		
		final long timeGen1 = System.currentTimeMillis();
		String[] argsGen = {"-i", tmp+"/traces", "-o", output};
		try {
			main.modelGen.MainGen.main(argsGen);
		} catch (Exception e) {
			System.err.println("failed to infer model from traces");
			e.printStackTrace();
		}
		final long timeGen2 = System.currentTimeMillis();

		if (timerMode) {
			System.out.println("Split Duration: " + (timeSplit2 - timeSplit1) + " ms");
			System.out.println("modle Generation Duration: " + (timeGen2 - timeGen1) + " ms");
        }	
	}

	// create the directory that will contain the nex traces
		private static String createDir() {
			String tmpName = null, fName = "RESULTS/" + output;
			int i = 1;
			File x = new File(fName);
			while(x.exists()) {
				tmpName = fName+i;
				x = new File(tmpName);
				i++;
			}
			if (tmpName != null) {
				fName = tmpName;
			}
			output = fName;
			x = new File(fName);
			x.mkdirs();
			return fName;
		}
	
}
