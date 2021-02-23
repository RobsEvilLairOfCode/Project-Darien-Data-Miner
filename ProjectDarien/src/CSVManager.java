import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeMap;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;

public class CSVManager{
	private File CSVFile = null;//Full File
	private String[] CSVRows = null;//Divide by Rows
	private String[][] CSVCells = null;//Divide by Cells
	
	private SimRun[] simRunData = null;
	
	private File analysisFile = null;
	private String[][] analysisCells = null;
	private RunArguments runArgs = null;
	public CSVManager(File file,RunArguments runArgs) {
		this.CSVFile = file;
		this.runArgs = runArgs;
	}
	
	public void setRunArgs(RunArguments runArgs) {
		this.runArgs = runArgs;
	}

	public Task<Void> init = new Task<Void>() {
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
	public int getLongestRow(){//Requires CSVRows to be initiallized
		int cells = 0;
		for(int line = 0; line < CSVRows.length; line++) {
			cells = Math.max(CSVRows[line].split(",").length, cells);
		}
		return cells;
	}
	public boolean isInitialized() {//All Variables are Filled
		return CSVFile != null && CSVRows != null && CSVCells != null;
	}
	public void wipe() {//Use if a new CSV File must be loaded
		CSVFile = null;
		CSVRows = null;
		CSVCells = null;
	}
	public int getAmtRuns() {
			int row = runArgs.getRunIDRow();
			int largest = -1;
			for(int col = 1; col < CSVCells[row].length; col++) {
				String cell = CSVCells[row][col];
				int get = Integer.parseInt(cell);
				largest = Math.max(get, largest);
			}
			return largest;
	}
	public String[] getParameterNames() {
			String[] params = new String[runArgs.getNumofParams()];
			for(int row = 0; row < runArgs.getNumofParams(); row++) {
				params[row] = CSVCells[row + runArgs.getParamsRow()][runArgs.getParamsCol()];
			}
			return params;
	};
	public Task<Void> mine = new Task<Void>() {
		@Override
		protected Void call() throws Exception {
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
			
			updateTargetParams();
			return null;
		}
		@SuppressWarnings("unchecked")
		public void updateTargetParams() {
			HashMap<String, String> params = (HashMap<String, String>) simRunData[0].getParameters().clone();
			params.remove(runArgs.getCompareKey());
			runArgs.setTargetParams(params);
		}
	};
	
	public Task<Void> analyize = new Task<Void>() {

		@Override
		protected Void call() throws Exception {
			TreeMap<String,HashMap<SimRun.Categories,Double>> averages = SimRun.averageAllResults(simRunData,runArgs);//SimRun.averageAllResults(adjustedSimRunData);
			TreeMap<String,HashMap<SimRun.Categories,Double>> stdDev = SimRun.stdDevResults(averages, simRunData,runArgs);
			
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
	public void applyHeader() {
		for(int row = 0; row <= 5; row++) {
			for(int col = 0; col <= 4; col++)
				analysisCells[row][col] = CSVCells[row][col];
		}
		analysisCells[6][0]= CSVFile.getName();
	}
	public void applyParams() {
		analysisCells[7][0] = "Compare Key";
		analysisCells[7][1] = runArgs.getCompareKey();
		
		if(runArgs.getTargetParams() == null) {
			analysisCells[8][0] = "Using Multiple Parameters";
		}else {
			int count = 8;
			for(String key: runArgs.getTargetParams().keySet()) {
				analysisCells[count][0] = key;
				analysisCells[count][1] = runArgs.getTargetParams().get(key);
				count++;
			}
		}
	}
	
	public File getCSVFile() {
		return CSVFile;
	}
	public String[] getCSVRows() {
		return CSVRows;
	}
	public String[][] getCSVCells() {
		return CSVCells;
	}

	public void applyAverageCheck(TreeMap<String,HashMap<SimRun.Categories,Double>> averages) {
		int row = 24;
		
		analysisCells[row][0]= "Averages";
				
		row++;
		
		//Compare key and its values
		analysisCells[row][0] = runArgs.getCompareKey();
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
	public File getAnalysisFile() {
		return analysisFile;
	}
	public void applyStdDevCheck(TreeMap<String,HashMap<SimRun.Categories,Double>> stdDev) {
		int row = 33;
		
		analysisCells[row][0]= "Standard Deviations";
				
		row++;
		
		//Compare key and its values
		analysisCells[row][0] = runArgs.getCompareKey();
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
	
	public void saveAnalysis() {
		FileChooser fc = new FileChooser();
		fc.setInitialDirectory(new File(System.getProperty("user.home")+"/Downloads"));
		fc.setTitle("Chooser Location to Analysis File");
		//File path = fc.showSaveDialog(new Stage());
		File path = new File(CSVFile.getParentFile().getAbsolutePath()+"/Analysis");
		path.mkdirs();
		saveAnalysis(path);
	}
	public void saveAnalysis(File path) {
		analysisFile = new File(path.getAbsolutePath() + "/"+ CSVFile.getName() + "_Analysis.csv");
		try {
			analysisFile.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(analysisFile);
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
		System.out.println("Saved Analysis File at " + analysisFile.getAbsolutePath());
		//try {
		//	Runtime.getRuntime().exec("explorer.exe "+ file.getAbsolutePath());
		//} catch (IOException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}
	}
}
