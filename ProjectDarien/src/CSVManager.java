import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeMap;

import javafx.concurrent.Task;
import javafx.scene.control.ScrollPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class CSVManager{
	public static File CSVFile = null;//Full File
	public static String[] CSVRows = null;//Divide by Rows
	public static String[][] CSVCells = null;//Divide by Cells
	
	public static SimRun[] simRunData = null;
	public static SimRun[] adjustedSimRunData = null;
	
	public static String compareKey = null;
	public static HashMap<String, String> targetParams = null;
	
	public static File analysisFile = null;
	public static String analysisRows = null;
	public static String[][] analysisCells = null;
	public static void setCSVFile(File file) {
		CSVFile = file;
	}
	
	public static Task<Void> init = new Task<Void>() {
		@Override
		protected Void call() throws Exception {
			if(isInitialized()) return null;//if the variables are already filled, dont execute
			//Divide Rows
			try {
				double bytesRead = 0;
				
				FileInputStream fis = null;
				fis = new FileInputStream(CSVFile);
				Scanner sc = new Scanner(fis);
				CSVRows = new String[0];
				while(sc.hasNext()) {
					CSVRows = Arrays.copyOf(CSVRows, CSVRows.length + 1);
					CSVRows[CSVRows.length - 1] = sc.nextLine();
					bytesRead += CSVRows[CSVRows.length -1].length();
					this.updateProgress(((bytesRead/CSVFile.length()) * 0.33) * 100, 100);
				}
				sc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("e");
			//Divide Columns
			CSVCells = new String[CSVRows.length][getLongestRow()];
			for(int line = 0; line < CSVRows.length; line++) {//Fill CSVCELLS with empty Strings
				Arrays.fill(CSVCells[line],"");
				this.updateProgress((((double)line / (double)CSVRows.length) * 0.33 + 0.33) * 100, 100);
			}
			for(int line = 0; line < CSVRows.length; line++) {//Overwrite Cells
				String[] items = CSVRows[line].split(","); 
				for(int item = 0; item < items.length; item++) {
					CSVCells[line][item] = items[item].replace("\"", "");
				}
				this.updateProgress((((double)line / (double)CSVRows.length) * 0.33 + 0.66) * 100, 100);
			}
			this.updateProgress(1, 1);
			return null;
		}
	}; 
	public static int getLongestRow(){//Requires CSVRows to be initiallized
		int cells = 0;
		for(int line = 0; line < CSVRows.length; line++) {
			cells = Math.max(CSVRows[line].split(",").length, cells);
		}
		return cells;
	}
	public static boolean isInitialized() {//All Variables are Filled
		return CSVFile != null && CSVRows != null && CSVCells != null;
	}
	public static void wipe() {//Use if a new CSV File must be loaded
		CSVFile = null;
		CSVRows = null;
		CSVCells = null;
	}
	public static int getAmtRuns() {
		int ver = CSVMinerMain.runArguments.getVersion();
		if(ver == 1) {
			int row = CSVMinerMain.runArguments.getRunIDRow();
			int largest = -1;
			for(int col = 1; col < CSVCells[row].length; col++) {
				String cell = CSVCells[row][col];
				int get = Integer.parseInt(cell);
				largest = Math.max(get, largest);
			}
			return largest;
		}
		for(int i = CSVCells.length - 1; i > 0; i++) {
			if(!CSVCells[i][0].isBlank()) {
				return Integer.parseInt(CSVCells[i][0]);
			}
		}
		return 0;
	}
	public static String[] getParameterNames() {
		int ver = CSVMinerMain.runArguments.getVersion();
		if(ver == 1) {
			String[] params = new String[CSVMinerMain.runArguments.getNumofParams()];
			for(int row = 0; row < CSVMinerMain.runArguments.getNumofParams(); row++) {
				params[row] = CSVCells[row + CSVMinerMain.runArguments.getParamsRow()][CSVMinerMain.runArguments.getParamsCol()];
			}
			return params;
		}
		
		String[] params = new String[CSVMinerMain.runArguments.getNumofParams()];
		int count = 0;
		for(int i = CSVMinerMain.runArguments.getParamsRow(); i < CSVMinerMain.runArguments.getNumofParams();i++) {
			params[count] = CSVCells[CSVMinerMain.runArguments.getParamsRow()][i];
		}
		return params;
	};
	public static Task<Void> mine = new Task<Void>() {
		@Override
		protected Void call() throws Exception {
			if(CSVMinerMain.runArguments.getVersion() == 1) mineV1();
			else mineV2();
			return null;
		}
		
		private void mineV1() {
			RunArguments runArgs = CSVMinerMain.runArguments;
			ArrayList<SimRun> runs = new ArrayList<SimRun>();
			for(int run = 0; run < getAmtRuns();run++) {//for every run
				updateProgress((double)run * 2, (double)getAmtRuns() * 2);
				HashMap<String, String> parameters = new HashMap<String,String>();
				HashMap<SimRun.Categories, ArrayList<Double>> variables = new HashMap<SimRun.Categories,ArrayList<Double>>();
				
				//add parameter info first.
				for(int row = runArgs.getParamsRow(); row < runArgs.getParamsRow() + runArgs.getNumofParams(); row++) {
					String key = getParameterNames()[row - runArgs.getParamsRow()];//pulls from the parameter names
					String value = CSVCells[row][run * runArgs.getSectionLen() + runArgs.getParamsCol() + 1];
					parameters.put(key, value);							
				}
				updateProgress((double)run * 2  + 1, (double)getAmtRuns() * 2);
				//collect and average data
				final int labelOffset = 1;//Accounts for the amount of labels needed to skip
				for(int col = 0; col < runArgs.getSectionLen(); col++) {//for every column in each run, collect the data
					SimRun.Categories key = SimRun.Categories.values()[col];
					ArrayList<Double> value = new ArrayList<Double>();
					for(int row = runArgs.getDataRow() + 1; row < CSVCells.length;row++) {//gets each value under a category and adds it into the array
						String numString = CSVCells[row][col + labelOffset + run * runArgs.getSectionLen()];
						if(numString.isBlank())break;//end when there are no more numbers
						Double num = Double.valueOf(numString);
						value.add(num);
					}
					variables.put(key,value);
				}
				runs.add(new SimRun(run + 1,parameters,variables));
			}
			updateProgress(1, 1);
			simRunData = runs.toArray(new SimRun[0]);
		}
		
		private void mineV2() {
			RunArguments runArgs = CSVMinerMain.runArguments;
			ArrayList<SimRun> runs = new ArrayList<SimRun>();
			for(int run = 0; run < getAmtRuns();run++) {//for every run
				updateProgress((double)run * 2, (double)getAmtRuns() * 2);
				HashMap<String, String> parameters = new HashMap<String,String>();
				HashMap<SimRun.Categories, ArrayList<Double>> variables = new HashMap<SimRun.Categories,ArrayList<Double>>();
				
				int runRow = -1;
				int runSize = -1;
				try {
					runRow = findRunRow(run);
					runSize = findRunLength(run);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				//add parameter info first.
				for(int col = runArgs.getParamsCol(); col < runArgs.getParamsCol() + runArgs.getNumofParams(); col++) {
					String key = getParameterNames()[col - runArgs.getParamsRow()];//pulls from the parameter names
					String value = CSVCells[runRow][col];
					parameters.put(key, value);							
				}
				updateProgress((double)run * 2  + 1, (double)getAmtRuns() * 2);
				
				final int labelOffset = 0;//Accounts for the amount of labels needed to skip
				for(int col = runArgs.getDataCol() +1; col < runRow + runArgs.getSectionLen(); col++) {//for every column in each run, collect the data
					SimRun.Categories key = SimRun.Categories.values()[col];
					ArrayList<Double> value = new ArrayList<Double>();
					for(int row = runRow; row < runRow + runSize;row++) {//gets each value under a category and adds it into the array
						String numString = CSVCells[row][col + labelOffset];
						if(numString.isBlank())break;//end when there are no more numbers
						Double num = Double.valueOf(numString);
						value.add(num);
					}
					variables.put(key,value);
				}
				runs.add(new SimRun(run + 1,parameters,variables));
			}
			updateProgress(1, 1);
			simRunData = runs.toArray(new SimRun[0]);
		}
		
		private int findRunRow(int run) throws Exception {//only use in v2
			if(run > getAmtRuns()) {
				throw new Exception("run # too high");
			}
			for(int row = CSVMinerMain.runArguments.getDataRow(); row < CSVCells.length; row++) {
				if(Integer.parseInt(CSVCells[row][0]) == run) return row;
			}
			return -1;
		}
		
		private int findRunLength(int run) throws Exception {
			int row = findRunRow(run);
			boolean stop = false;
			int size = 0;
			while(!stop) {
				if(Integer.parseInt(CSVCells[row + size][0])==run)size++;
				else stop = true;
			}
			return size;
		}
	};
	
	public static Task<Void> analyize = new Task<Void>() {

		@Override
		protected Void call() throws Exception {
			TreeMap<String,HashMap<SimRun.Categories,Double>> averages = SimRun.averageAllResults(simRunData);//SimRun.averageAllResults(adjustedSimRunData);
			TreeMap<String,HashMap<SimRun.Categories,Double>> stdDev = SimRun.stdDevResults(averages, simRunData);
//			for(int i = 0; i <= 100; i+= 5) {
//				HashMap<SimRun.Categories,Double> results = averages.get(Integer.toString(i));
//				for(SimRun.Categories c:SimRun.Categories.values()) {
//					System.out.println("Of " + i + "% vaccinated. In category "+ c.name() + ", the average was: "+  results.get(c));
//				}
//			}
			analysisCells = new String[100][100];
			for(int row = 0; row < analysisCells.length; row++) {// fill with empty Strings to prevent error
				Arrays.fill(analysisCells[row],"");
			}
			applyHeader();
			applyParams();

			applyAverageCheck(averages);
			applyStdDevCheck(stdDev);
			saveAnalysis();
			
			return null;
		}
		
	};
	public static void applyHeader() {
		for(int row = 0; row <= 5; row++) {
			for(int col = 0; col <= 4; col++)
				analysisCells[row][col] = CSVCells[row][col];
		}
		analysisCells[6][0]= CSVFile.getName();
	}
	public static void applyParams() {
		analysisCells[7][0] = "Compare Key";
		analysisCells[7][1] = CSVManager.compareKey;
		
		if(CSVManager.targetParams == null) {
			analysisCells[8][0] = "Using Multiple Parameters";
		}else {
			int count = 8;
			for(String key: CSVManager.targetParams.keySet()) {
				analysisCells[count][0] = key;
				analysisCells[count][1] = CSVManager.targetParams.get(key);
				count++;
			}
		}
	}
	
	public static void applyAverageandStandardDeviation() {
		analysisCells[12][0] = "Final Results,averaged/std";
		int count = 13;
		System.out.println(CSVManager.adjustedSimRunData);
		System.out.println(CSVManager.simRunData);
		HashMap<SimRun.Categories,Double> averages = SimRun.simpleResultAverage(CSVManager.simRunData);
		HashMap<SimRun.Categories,Double> stdDev = SimRun.simpleResultStandardDeviation(CSVManager.simRunData);
		for(SimRun.Categories c:SimRun.Categories.values()) {
			analysisCells[count][0] = c.toString();
			analysisCells[count][1] = averages.get(c).toString();
			analysisCells[count][2] = stdDev.get(c).toString();
			count++;
		}
	}
	
	public static void applyAverageCheckLegacy(TreeMap<String,HashMap<SimRun.Categories,Double>> averages) {
		analysisCells[24][0] = "Aver";
		
		if(CSVManager.compareKey != null)analysisCells[25][0] = CSVManager.compareKey;
		for(int col = 0; col < averages.keySet().size();col++) {//Writes the vaccinated increments
			analysisCells[25][col + 1] =  averages.keySet().toArray(new String[0])[col];
		}
		
		analysisCells[26][0] = "Susceptible";
		for(int col = 0; col < averages.keySet().size();col++) {
			String key =  averages.keySet().toArray(new String[0])[col];
			HashMap<SimRun.Categories,Double> averageList = averages.get(key);
			analysisCells[26][col + 1] = Double.toString(averageList.get(SimRun.Categories.Healthy) + averageList.get(SimRun.Categories.Exposed));
					
		}
		analysisCells[27][0] = "Infected & Recovered";
		for(int col = 0; col < averages.keySet().size();col++) {
			String key =  averages.keySet().toArray(new String[0])[col];
			HashMap<SimRun.Categories,Double> averageList = averages.get(key);
			analysisCells[27][col + 1] = Double.toString(averageList.get(SimRun.Categories.Infected) + averageList.get(SimRun.Categories.Recovered));
		}
		
		analysisCells[28][0] = "Hospitalized";
		for(int col = 0; col < averages.keySet().size();col++) {
			String key =  averages.keySet().toArray(new String[0])[col];
			HashMap<SimRun.Categories,Double> averageList = averages.get(key);
			analysisCells[28][col + 1] = Double.toString(averageList.get(SimRun.Categories.Hospitalized));
		}
		
		analysisCells[29][0] = "Vaccinated";
		for(int col = 0; col < averages.keySet().size();col++) {
			String key =  averages.keySet().toArray(new String[0])[col];
			HashMap<SimRun.Categories,Double> averageList = averages.get(key);
			analysisCells[29][col + 1] = Double.toString(averageList.get(SimRun.Categories.Vaccinated));
		}
		
		analysisCells[30][0] = "Total";
		for(int col = 1; col < averages.keySet().size(); col++) {
			double total = 0;
			for(int row  = 26; row <= 29; row++) {
				total += Double.parseDouble(analysisCells[row][col]);
			}
			analysisCells[30][col] = Double.toString(total);
		}
	}
	
	public static void applyAverageCheck(TreeMap<String,HashMap<SimRun.Categories,Double>> averages) {
		int row = 24;
		
		analysisCells[row][0]= "Averages";
				
		row++;
		
		//Compare key and its values
		analysisCells[row][0] = CSVManager.compareKey;
		for(int col = 0; col < averages.keySet().size();col++) {
			analysisCells[row][col + 1]= averages.keySet().toArray(new String[0])[col];
		}
		
		row++;
		
		//averaged values
		for(SimRun.Categories c: SimRun.Categories.values()) {
			for(int col = -1; col < averages.keySet().size();col++) {
				if(col == -1) analysisCells[row][0] = c.name();
				else analysisCells[row][col + 1]  = Double.toString(averages.get(averages.keySet().toArray(new String[0])[col]).get(c));
			}
			row++;
		}
		
		//totals
		analysisCells[row][0] = "Total";
		for(int col = 1; col < averages.keySet().size() + 1; col++) {
			double total = 0;
			for(int r  = 26; r <= 31; r++) {
				total += Double.parseDouble(analysisCells[r][col]);
			}
			analysisCells[32][col] = Double.toString(total);
		}
		
		
	}
	public static void applyStdDevCheck(TreeMap<String,HashMap<SimRun.Categories,Double>> stdDev) {
		int row = 33;
		
		analysisCells[row][0]= "Standard Deviations";
				
		row++;
		
		//Compare key and its values
		analysisCells[row][0] = CSVManager.compareKey;
		for(int col = 0; col < stdDev.keySet().size();col++) {
			analysisCells[row][col + 1]= stdDev.keySet().toArray(new String[0])[col];
		}
		
		row++;
		
		//averaged values
		for(SimRun.Categories c: SimRun.Categories.values()) {
			for(int col = -1; col < stdDev.keySet().size();col++) {
				if(col == -1) analysisCells[row][0] = c.name();
				else analysisCells[row][col + 1]  = Double.toString(stdDev.get(stdDev.keySet().toArray(new String[0])[col]).get(c));
			}
			row++;
		}
		
	}
	
	public static void applyStdDevCheckLegacy(TreeMap<String,HashMap<SimRun.Categories,Double>> stdDev) {
		analysisCells[31][0] = "Standard Deviation";
		
		if(CSVManager.compareKey != null)analysisCells[32][0] = CSVManager.compareKey;
		for(int col = 0; col < stdDev.keySet().size();col++) {//Writes the vaccinated increments
			analysisCells[32][col + 1] =  stdDev.keySet().toArray(new String[0])[col];
		}
		
		analysisCells[33][0] = "Susceptible";
		for(int col = 0; col < stdDev.keySet().size();col++) {
			String key =  stdDev.keySet().toArray(new String[0])[col];
			HashMap<SimRun.Categories,Double> averageList = stdDev.get(key);
			analysisCells[33][col + 1] = Double.toString(averageList.get(SimRun.Categories.Healthy) + averageList.get(SimRun.Categories.Exposed));
					
		}
		analysisCells[34][0] = "Infected & Recovered";
		for(int col = 0; col < stdDev.keySet().size();col++) {
			String key =  stdDev.keySet().toArray(new String[0])[col];
			HashMap<SimRun.Categories,Double> averageList = stdDev.get(key);
			analysisCells[34][col + 1] = Double.toString(averageList.get(SimRun.Categories.Infected) + averageList.get(SimRun.Categories.Recovered));
		}
		
		analysisCells[35][0] = "Hospitalized";
		for(int col = 0; col < stdDev.keySet().size();col++) {
			String key =  stdDev.keySet().toArray(new String[0])[col];
			HashMap<SimRun.Categories,Double> averageList = stdDev.get(key);
			analysisCells[35][col + 1] = Double.toString(averageList.get(SimRun.Categories.Hospitalized));
		}
		
		analysisCells[36][0] = "Vaccinated";
		for(int col = 0; col < stdDev.keySet().size();col++) {
			String key =  stdDev.keySet().toArray(new String[0])[col];
			HashMap<SimRun.Categories,Double> averageList = stdDev.get(key);
			analysisCells[36][col + 1] = Double.toString(averageList.get(SimRun.Categories.Vaccinated));
		}
		
		analysisCells[37][0] = "Total";
		for(int col = 1; col < stdDev.keySet().size(); col++) {
			double total = 0;
			for(int row  = 26; row <= 29; row++) {
				total += Double.parseDouble(analysisCells[row][col]);
			}
			analysisCells[37][col] = Double.toString(total);
		}
	}
	
	public static void saveAnalysis() {
		FileChooser fc = new FileChooser();
		fc.setInitialDirectory(new File(System.getProperty("user.home")+"/Downloads"));
		fc.setTitle("Chooser Location to Analysis File");
		//File path = fc.showSaveDialog(new Stage());
		File path = new File(System.getProperty("user.home")+"/Downloads");
		saveAnalysis(path);
	}
	public static void saveAnalysis(File path) {
		File file = new File(path.getAbsolutePath() + "/"+ CSVFile.getName() + "_Analysis.csv");
		try {
			file.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PrintWriter pw = new PrintWriter(fos);
		for(int row = 0; row < analysisCells.length; row++) {
			for(int col = 0; col < analysisCells[row].length;col++) {
				pw.write(analysisCells[row][col] + ",");
			}
			pw.write("\n");
		}
		pw.close();
		System.out.println("Saved Analysis File");
		try {
			Runtime.getRuntime().exec("explorer.exe "+ file.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}
}
