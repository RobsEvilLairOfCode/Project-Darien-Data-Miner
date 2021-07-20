package LePackage;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;

public class SimRun {
	public enum Categories{
		Healthy,Recovered,Hospitalized,Vaccinated,Infected,Exposed
	}
	private int runID;
	private HashMap<String, String> parameters;
	private HashMap<Categories, ArrayList<Double>> variables;
	public SimRun(int runID, HashMap<String, String> parameters, HashMap<Categories, ArrayList<Double>> variables) {
		super();
		this.runID = runID;
		this.parameters = parameters;
		this.variables = variables;
	}
	public int getRunID() {
		return runID;
	}
	public void setRunID(int runID) {
		this.runID = runID;
	}
	public HashMap<String, String> getParameters() {
		return parameters;
	}
	@Override
	public String toString() {
		return "SimRun [runID=" + runID+"]";
	}
	public void setParameters(HashMap<String, String> parameters) {
		this.parameters = parameters;
	}
	public HashMap<Categories, ArrayList<Double>> getVariables() {
		return variables;
	}
	public void setVariables(HashMap<Categories, ArrayList<Double>> variables) {
		this.variables = variables;
	}
	public double getMean(Categories category) {
		double sum = 0;
		for(Double value: variables.get(category)) {
			sum+= value;
		}
		return sum / (double)variables.get(category).size();
	}
	//Returns a simrun with the data being averaged of the other simruns.
	public SimRun averageIfSimilarParams(SimRun other) {
		if(this.parameters.equals(other.getParameters())) return averageVariables(other);
		return this;
	}
	
	public SimRun averageVariables(SimRun other) {

		HashMap<Categories, ArrayList<Double>> newVariables = new HashMap<Categories, ArrayList<Double>>();
		
		int length = Math.min(variables.get(Categories.Healthy).size(), other.getVariables().get(Categories.Healthy).size());
		
		newVariables.put(Categories.Exposed, new ArrayList<Double>());
		newVariables.put(Categories.Healthy, new ArrayList<Double>());
		newVariables.put(Categories.Hospitalized, new ArrayList<Double>());
		newVariables.put(Categories.Infected, new ArrayList<Double>());
		newVariables.put(Categories.Recovered, new ArrayList<Double>());
		newVariables.put(Categories.Vaccinated, new ArrayList<Double>());
		
		for(int tick = 0; tick < length; tick++) {//fill with 0 first
			newVariables.get(Categories.Exposed).add(0.0);
			newVariables.get(Categories.Healthy).add(0.0);
			newVariables.get(Categories.Hospitalized).add(0.0);
			newVariables.get(Categories.Infected).add(0.0);
			newVariables.get(Categories.Recovered).add(0.0);
			newVariables.get(Categories.Vaccinated).add(0.0);
			
		}
		for(int tick = 0; tick < length; tick++) {
			newVariables.get(Categories.Exposed).set(tick,average(other,Categories.Exposed,tick));
			newVariables.get(Categories.Healthy).set(tick,average(other,Categories.Healthy,tick));
			newVariables.get(Categories.Hospitalized).set(tick,average(other,Categories.Hospitalized,tick));
			newVariables.get(Categories.Infected).set(tick,average(other,Categories.Infected,tick));
			newVariables.get(Categories.Recovered).set(tick,average(other,Categories.Recovered,tick));
			newVariables.get(Categories.Vaccinated).set(tick,average(other,Categories.Vaccinated,tick));
		}
		
		return new SimRun(runID,parameters,newVariables);
	}
	private double average(SimRun other,Categories category, int tick) {
		return (variables.get(category).get(tick) + other.getVariables().get(category).get(tick))/2.0;
	}
	public double getDay(Categories c,int day) {
		ArrayList<Double> list = variables.get(c);
		if(day==0)return list.get(list.size() - 1);
		return list.get(day - 1);
	}	
	public double getFinal(Categories c) {
		return getDay(c,0);
	}
	public int getFinalDayNumber() {
		return variables.get(Categories.Exposed).size() - 1;
	}
	public static int getFinalDayNumber(SimRun[] runs) {
		int least = Integer.MAX_VALUE;
		for(SimRun run: runs) {
			least = Math.min(run.getFinalDayNumber(), least);
		}
		return least;
	}
	public HashMap<Categories,Double> getAllFinal() {
		HashMap<Categories,Double> results = new HashMap<Categories, Double>();
		for(Categories c:Categories.values()) {
			results.put(c,getFinal(c));
		}
		return results;
	}
	public HashMap<Categories,Double> getAllDay(int day) {
		HashMap<Categories,Double> results = new HashMap<Categories, Double>();
		for(Categories c:Categories.values()) {
			results.put(c,getDay(c,day));
		}
		return results;
	}
	public static TreeMap<String,HashMap<Categories,Double>> averageAllResults(SimRun[] runs, RunArguments runArgs){
		System.out.println(1);
		TreeMap<String,HashMap<Categories,Double>> results = new TreeMap<String,HashMap<Categories,Double>>(findGreaterInt);
		TreeMap<String,ArrayDeque<SimRun>> sortedRuns = new TreeMap<String,ArrayDeque<SimRun>>(findGreaterInt);//runs sorted by a common compare key value
		System.out.println(2);
		for(SimRun run: runs) {
			results.putIfAbsent(run.getParameters().get(runArgs.getCompareKey()),new HashMap<Categories,Double>());// Fill in keys
			sortedRuns.putIfAbsent(run.getParameters().get(runArgs.getCompareKey()),new ArrayDeque<SimRun>());// Fill in keys
		}
		System.out.println(3);
		//sort the runs into the sorted runs map
		for(String key: results.keySet()) {
			for(SimRun run:runs) {
				if(run.getParameters().get(runArgs.getCompareKey()).contentEquals(key)) {//first check what the value of the compare key is so it can be sorted correctly
					boolean paramsDiff = false;
					for(String paramKey:runArgs.getTargetParams().keySet()) {//check if target params are different
						if(!run.getParameters().get(paramKey).equals(runArgs.getTargetParams().get(paramKey))) {
							paramsDiff = true;
						}
					}
					if(!paramsDiff)sortedRuns.get(key).add(run);
				}
			}
		}
		System.out.println(4);
		//averages the sorted runs
		for(String key: sortedRuns.keySet()) {
			for(Categories c: Categories.values()) {
				System.out.println("Running");
				double sum = 0;
				double count = sortedRuns.get(key).size();
				System.out.println("Running");
				for(SimRun run:sortedRuns.get(key)) {
					sum += run.getDay(c,0);
				}
				System.out.println("Running");
				results.get(key).put(c, sum/count);
			}
		}
		System.out.println(5);
		//procedure
		// Copy all the runs into a set. Sort all the runs by the the compare key value and place them into a map(<String, List<SimRun>>) with the compare key values as the key
		//and lists of filtered sim runs as the values.
		// Make a new loop that itterates through the keyset of the new map. make a new variable that keeps total of the sum. Then  for each filtered sim run list, make a inner loop itterating
		//through the values of SimRun.Categories, in the nested loop. add the value of map.get(key).get(i).finalVals(Category) to the sum, then divide by the length of the list of simruns to get the average
		//the place it on the results map with results.get(key).put(Category,average);
		
		return results;
	}

	public static TreeMap<String,HashMap<Categories,Double>> stdDevResults(TreeMap<String,HashMap<Categories,Double>> averages,SimRun[] runs, RunArguments runArgs){
		TreeMap<String,HashMap<Categories,ArrayList<Double>>> offsets = new TreeMap<String,HashMap<Categories,ArrayList<Double>>>();
		TreeMap<String,HashMap<Categories,Double>> variance = new TreeMap<String,HashMap<Categories,Double>>(findGreaterInt);
		for(SimRun run: runs) {
			offsets.putIfAbsent(run.getParameters().get(runArgs.getCompareKey()),new HashMap<Categories,ArrayList<Double>>());// Fill in keys
			variance.putIfAbsent(run.getParameters().get(runArgs.getCompareKey()),new HashMap<Categories,Double>());// Fill in keysw
		}
		for(String key: offsets.keySet()) {
			for(Categories c: Categories.values()) {
				offsets.get(key).put(c, new ArrayList<Double>());
				
			}
		}
		//fill offsets
			for(SimRun run:runs) {
				for(Categories c: Categories.values()) {
				String key = run.getParameters().get(runArgs.getCompareKey());//key for variance based on 
				double average = averages.get(key).get(c);
				double offset = Math.abs(average - run.getFinal(c));
				offsets.get(key).get(c).add(offset);
			}
		}

		//calculate variance
		for(String key: offsets.keySet()) {
			for(Categories c:Categories.values()) {
				offsets.get(key).get(c).replaceAll(new UnaryOperator<Double>() {

					@Override
					public Double apply(Double t) {
						return Math.pow(t, 2);
					}});
			}
		}
		//sum up results and put them in variance list
		for(String key: offsets.keySet()) {
			for(Categories c: Categories.values()) {
				double sum = 0;
				double count = 0;
				for(Double d: offsets.get(key).get(c)) {
					sum += d;
					count++;
				}
				
				variance.get(key).put(c, sum / count);
			}
		}
		for(String key: variance.keySet()) {
		variance.get(key).replaceAll(new BiFunction<Categories,Double,Double>(){

			@Override
			public Double apply(Categories t, Double u) {
				
				return Math.sqrt(u);
			}});
		}
		return variance;
	}

//	public String[][] toStringArray(){
	//	String[][] ret = new String[this.parameters.size()][6];
//		return ret;
	//}
	
	public String[] paramstoString() {
			return (String[])this.parameters.values().toArray();
	}
	
	////	String[][] ret = new String[6][6];
	//	ret[0] = Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
	//}
	//
	public static Comparator<String> findGreaterInt= new Comparator<String>() {

		@Override
		public int compare(String o1, String o2) {
			if(!isAllDigit(o1) || !isAllDigit(o2)) return o1.compareTo(o2);
			int first = Integer.parseInt(o1);
			int second = Integer.parseInt(o2);
			
			return Integer.compare(first, second);
		}
		
		private boolean isAllDigit(String str) {
			boolean alldigit = true;
			for(char c: str.toCharArray()) {
				if(!Character.isDigit(c)) {
					alldigit = false;
				}
			}
			return alldigit;
		}
	};
	public static HashMap<HashMap<String,String>,ArrayList<SimRun>> sortRunsByParameter(SimRun[] runs){//Map<Parameter,Runs Associated>
		HashMap<HashMap<String,String>,ArrayList<SimRun>> sortedRuns = new HashMap<HashMap<String,String>,ArrayList<SimRun>>();
		for(SimRun run: runs) {
			HashMap<String,String> params = run.getParameters();
			if(sortedRuns.get(params) == null) {
				sortedRuns.put(params,new ArrayList<SimRun>());
			}
			sortedRuns.get(params).add(run);
		}
		return sortedRuns;
	}
	public static SimRun compressRuns(SimRun[] runs) {
		int maxCount= SimRun.getFinalDayNumber(runs);
		HashMap<Categories, ArrayList<Double>> variables = new HashMap<Categories, ArrayList<Double>>();
		for(Categories c: Categories.values()) {
			ArrayList<Double> values= new ArrayList<Double>();
			for(int day = 0; day < maxCount; day++) {
				double sum = 0;
				for(int run =0; run < runs.length; run++) {
					sum+=runs[run].getDay(c, day);
				}
				double average = sum / (double)runs.length;
				values.add(average);
			}
			variables.put(c, values);
		}
		SimRun simrun = new SimRun(0,runs[0].getParameters(), variables);
		return simrun;
	}
	public String[][] tableForm(){
		String[][] table = new String[Categories.values().length][this.getFinalDayNumber() + 2];
		Categories[] values = SimRun.Categories.values();
		for(int i = 0; i <  Categories.values().length; i++) {
			table[i][0] = values[i].name();
		}
		for(int i = 0; i < values.length; i++) {
			for(int j = 0; j < variables.get(values[i]).size();j++) {
				table[i][j+1] = variables.get(values[i]).get(j).toString();
			}
		}
		return table;
	}
	
}
