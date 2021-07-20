package LePackage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class RunArguments {
	//Locating the Data
	public enum MODE{
		Average,Compress
	}
	private MODE mode;//mode the application will run in
	public MODE getMode() {
		return mode;
	}

	public void setMode(MODE mode) {
		this.mode = mode;
	}

	private int sectionLen; //The length of each section(default = 6)
	private int runIDRow;// what row the runid's are located on (default = 6)
	private int runIDCol;// what column the parameters are located on (default = 0)
	private int paramsRow;// what row the parameters are located on (default = 7)
	private int paramsCol;// what column the runid's are located on (default = 0)
	private int numofParams;//The number of parameters present(default = 7)
	private int dataRow; // what row the data starts at (default = 21)
	private int dataCol; // what column the data starts at not counting labels (default = 0)
	private int day;//If left blank, last day is assumed(-1)
	private String compareKey;
	private HashMap<String, String> targetParams;

	
	public RunArguments(MODE mode,int sectionLen, int runIDRow, int runIDCol, int paramsRow, int paramsCol, int numofParams,
			int dataRow, int dataCol, int day, String compareKey) {
		super();
		this.mode = mode;
		this.sectionLen = sectionLen;
		this.runIDRow = runIDRow;
		this.runIDCol = runIDCol;
		this.paramsRow = paramsRow;
		this.paramsCol = paramsCol;
		this.numofParams = numofParams;
		this.dataRow = dataRow;
		this.dataCol = dataCol;
		this.day = day;
		this.compareKey = compareKey;
		
	}

	public static RunArguments getDefault(CSVManager manager) {
		if(manager.getCSVCells()[6][10].contains("count students with")){
			return null;
		}else {
			return new RunArguments(MODE.Average,6,6,0,7,0,8,22,0,0,"percent-vaccinated");
		}
		
	}

		public static RunArguments autoGenerate(CSVManager manager) {
			int sl = -1,ridr = -1,ridc = -1,pr = -1,pc = -1,nop = -1,dr = -1,dc = -1;// If the value could not be determined, -1 will be returnedf
			String[][] CSV = manager.getCSVCells();

			
			for(int row = 0; row < CSV.length; row++) {
				for(int column = 0; column < CSV[0].length; column++) {
					String currentCell = CSV[row][column];//Ease of Use
					
					//Finding the Section Length and Run ID Pos
					if(currentCell.contains("run number")) {
						if(column + 1 < manager.getLongestRow() && !CSV[row][column + 1].isEmpty()) { //Avoid going out of bounds
							boolean allDigits = true;
							for(char c: CSV[row][column + 1].toCharArray()) {// is the text in the next cell all digits? If so, we know the following cells are numerical
								if(!Character.isDigit(c)) {
									allDigits = false;
									break;
								}
							}
							if(allDigits) {
								ridr = row;
								ridc = column;//Column next to label column contains offset
								pr = row +1;//Next Row down contain parameters
								pc = column;//Column that contains Labels
							}
							String text = CSV[row][ridc + 1];//Stores the String of the cell and compares it to the following columns until it doesnt match
							int amtOfMatches = 0; //amount of matching boxes after
							for(int col = ridc + 1; col < CSV[0].length; col++) {//getting sl
								if(CSV[row][col].contentEquals(text)) {
									amtOfMatches++;
								}else {
									break;
								}
							}
							if(amtOfMatches != 0)sl = amtOfMatches;
						}
					}
					
					//finding nop
					if(pr != -1 && pc != -1 && nop == -1) {
						for(int ro = pr; ro < CSV.length; ro++) {//Simply count the number of parameters
							//Checking the value cell to ensure there is a proper value
							if(CSV[ro][pc + 1].isEmpty()) break;//If the Cell is empty, stop the loop form executing more that it should
							boolean allDigits = true;
							for(char c: CSV[ro][pc + 1].toCharArray()) {// is the text in the next cell all digits? If so, we know the following cells are numerical
								if(!Character.isDigit(c)) {
									allDigits = false;
									break;
								}
							}
							if(allDigits || CSV[ro][pc + 1].toLowerCase().contentEquals("true") || CSV[ro][pc + 1].toLowerCase().contentEquals("false")) {
								if(nop == -1) nop = 1;//First passed check changes it from -1 to 1
								else nop++;
							}else {
								break;
							}
						}
					}
					//finding dr,dc
					if(currentCell.contains("run") && currentCell.contains("data")) {
						dr = row;
						dc = column;
					}
				}
			}
		
		String ck = "percent-vaccinated";
		String tptext = manager.getCSVCells()[2][0].toLowerCase().substring(0, manager.getCSVCells()[2][0].toLowerCase().indexOf(" tipping point")).strip();
		
		System.out.println(tptext);
		switch(tptext) {
			case "vaccine":
				ck = "percent-vaccinated";
				break;
			case "initial infected":
				ck = "percent-initially-infected";
				break;
			case "off campus":
				ck = "%offcampus-activities";
				break;
			case "masking":
				ck = "%mask-compliance";
				break;
			case "varient":
				ck = "varient";
				break;
			case "variant":
				ck = "variant";
				break;
			default:
				ck = "percent-vaccinated";
				break;
		};
		/*
		 * System.out.println("Tptext: "+ tptext); for(int i = pr; i < pr + nop; i++) {
		 * System.out.println(manager.getCSVCells()[i][pc]); //if(closeMatch(tptext,
		 * manager.getCSVCells()[i][pc].replace(" ","")) || closeMatch(tptext,
		 * manager.getCSVCells()[i][pc].replace("-",""))) {
		 * 
		 * ck = manager.getCSVCells()[i][pc]; break; } }
		 */
		return new RunArguments(MODE.Average,sl,ridr,ridc,pr,pc,nop,dr,dc,-1,ck);
	}

	public HashMap<String, String> getTargetParams() {
		return targetParams;
	}

	public void setTargetParams(HashMap<String, String> targetParams) {
		this.targetParams = targetParams;
	}

	public int getSectionLen() {
		return sectionLen;
	}


	public void setSectionLen(int sectionLen) {
		this.sectionLen = sectionLen;
	}


	public int getRunIDRow() {
		return runIDRow;
	}


	public void setRunIDRow(int runIDRow) {
		this.runIDRow = runIDRow;
	}


	public int getRunIDCol() {
		return runIDCol;
	}


	public void setRunIDCol(int runIDCol) {
		this.runIDCol = runIDCol;
	}


	public int getParamsRow() {
		return paramsRow;
	}


	public void setParamsRow(int paramsRow) {
		this.paramsRow = paramsRow;
	}
	public int getDay() {
		return this.day;
	}
	
	public void setDay(int day) {
		this.day = day;
	}


	public int getParamsCol() {
		return paramsCol;
	}


	public void setParamsCol(int paramsCol) {
		this.paramsCol = paramsCol;
	}


	public int getNumofParams() {
		return numofParams;
	}


	public void setNumofParams(int numofParams) {
		this.numofParams = numofParams;
	}


	public int getDataRow() {
		return dataRow;
	}


	public void setDataRow(int dataRow) {
		this.dataRow = dataRow;
	}


	public int getDataCol() {
		return dataCol;
	}


	public void setDataCol(int dataCol) {
		this.dataCol = dataCol;
	}
	public String getCompareKey() {
		return compareKey;
	}

	public void setCompareKey(String compareKey) {
		this.compareKey = compareKey;
	}
	
	@Override
	public String toString() {
		return "RunArguments [mode=" + mode + ", sectionLen=" + sectionLen + ", runIDRow=" + runIDRow + ", runIDCol="
				+ runIDCol + ", paramsRow=" + paramsRow + ", paramsCol=" + paramsCol + ", numofParams=" + numofParams
				+ ", dataRow=" + dataRow + ", dataCol=" + dataCol + ", day=" + day + ", compareKey=" + compareKey
				+ ", targetParams=" + targetParams + "]";
	}
	
	public void update(CSVManager manager) {
		manager.setRunArgs(this);
	}


}

