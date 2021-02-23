import java.util.HashMap;

public class RunArguments {
	//Locating the Data
	private int sectionLen; //The length of each section(default = 6)
	private int runIDRow;// what row the runid's are located on (default = 6)
	private int runIDCol;// what column the parameters are located on (default = 0)
	private int paramsRow;// what row the parameters are located on (default = 7)
	private int paramsCol;// what column the runid's are located on (default = 0)
	private int numofParams;//The number of parameters present(default = 7)
	private int dataRow; // what row the data starts at (default = 21)
	private int dataCol; // what column the data starts at not counting labels (default = 0)
	private int sheetVersion;
	
	public RunArguments(int sectionLen, int runIDRow, int runIDCol, int paramsRow, int paramsCol, int numofParams,
			int dataRow, int dataCol,int sheetVersion) {
		super();
		this.sectionLen = sectionLen;
		this.runIDRow = runIDRow;
		this.runIDCol = runIDCol;
		this.paramsRow = paramsRow;
		this.paramsCol = paramsCol;
		this.numofParams = numofParams;
		this.dataRow = dataRow;
		this.dataCol = dataCol;
		this.sheetVersion = sheetVersion;
	}

	public static RunArguments getDefault() {
		String[][] CSV = CSVManager.CSVCells;
		if(CSV[6][10].contains("count students with")){
			return null;
		}else {
			return new RunArguments(6,6,0,7,0,8,22,0,1);
		}
		
	}
	
	public static RunArguments autoGenerate() {
		String[][] CSV = CSVManager.CSVCells;
		if(CSV[6][10].contains("count students with")){
			return null;
		}else {
			return autoGenerateV1();
		}
	}
	public static RunArguments autoGenerateV2() {
		int sl = -1,ridr = -1,ridc = -1,pr = -1,pc = -1,nop = -1,dr = -1,dc = -1;// If the value could not be determined, -1 will be returned
		String[][] CSV = CSVManager.CSVCells;
		
		for(int row = 0; row < CSV.length; row++) {
			for(int column = 0; column < CSV[0].length; column++) {
				if(CSV[row][column].contains("run number")) {
					ridr = row;
					ridc = column;
					
					pr = row;
					pc = column + 1;
					
					dr = row;
				}
				if(CSV[row][column].contains("count students")) {
					dc = column - 1;
					sl = 0;
					for(int col = column; col < CSV[row].length; col++) {
						if(CSV[row][col].contains("count students")) {
							sl++;
						}else {
							break;
						}
					}
				}
				
				if(pc != -1 && pr != -1) {
					nop = 0;
					for(int col = 0; col < CSV[pr].length; col++) {
						if(CSV[pr][col].contains("count students with"))break;
						nop++;
					}
				}
			}
		}
		return new RunArguments(sl,ridr,ridc,pr,pc,nop,dr,dc,2);
	}
	//Reads a 2D array of each cell in the CSV
	public static RunArguments autoGenerateV1() {
		int sl = -1,ridr = -1,ridc = -1,pr = -1,pc = -1,nop = -1,dr = -1,dc = -1;// If the value could not be determined, -1 will be returned
		boolean urv = false;//Abbreviations of the parameters
		String[][] CSV = CSVManager.CSVCells;
		
		for(int row = 0; row < CSV.length; row++) {
			for(int column = 0; column < CSV[0].length; column++) {
				String currentCell = CSV[row][column];//Ease of Use
				
				//Finding the Section Length and Run ID Pos
				if(currentCell.contains("run number")) {
					if(column + 1 < CSVManager.getLongestRow() && !CSV[row][column + 1].isBlank()) { //Avoid going out of bounds
						boolean allDigits = true;
						for(char c: CSV[row][column + 1].toCharArray()) {// is the text in the next cell all digits? If so, we know the following cells are numerical
							if(!Character.isDigit(c)) {
								allDigits = false;
								break;
							}
						}
						if(allDigits) {
							System.out.println("Column: " + column);
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
						if(CSV[ro][pc + 1].isBlank()) break;//If the Cell is empty, stop the loop form executing more that it should
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
				//findhing dr,dc
				if(currentCell.contains("run") && currentCell.contains("data")) {
					//
					dr = row;
					dc = column;
				}
				
				if(currentCell.contains("mean")) {
					if(column + 1 < CSVManager.getLongestRow() && !CSV[row][column + 1].isBlank() || !CSV[row][column + 1].isEmpty()) { //Avoid going out of bounds
						boolean allDigits = true;
						for(char c: CSV[row][column + 1].toCharArray()) {// is the text in the next cell all digits? If so, we know the following cells are numerical
							if(!Character.isDigit(c) || c == '.') {
								allDigits = false;
								break;
							}
						}
						if(allDigits) {
							urv = true;
						}
					}
				
				
				}
			}
		}
		return new RunArguments(sl,ridr,ridc,pr,pc,nop,dr,dc,1);
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

	public int getVersion() {
		return this.sheetVersion;
	}
	
	@Override
	public String toString() {
		return "RunArguments [sectionLen=" + sectionLen + ", runIDRow=" + runIDRow + ", runIDCol=" + runIDCol
				+ ", paramsRow=" + paramsRow + ", paramsCol=" + paramsCol + ", numofParams=" + numofParams
				+ ", dataRow=" + dataRow + ", dataCol=" + dataCol + "]";
	}
}

