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
	public double getFinal(Categories c) {
		ArrayList<Double> list = variables.get(c);
		return list.get(list.size() - 1);
	
	}
	public HashMap<Categories,Double> getAllFinal() {
		HashMap<Categories,Double> results = new HashMap<Categories, Double>();
		for(Categories c:Categories.values()) {
			results.put(c,getFinal(c));
		}
		return results;
	}

	public static TreeMap<String,HashMap<Categories,Double>> averageAllResults(SimRun[] runs, RunArguments runArgs){
		TreeMap<String,HashMap<Categories,Double>> results = new TreeMap<String,HashMap<Categories,Double>>(findGreaterInt);
		TreeMap<String,ArrayDeque<SimRun>> sortedRuns = new TreeMap<String,ArrayDeque<SimRun>>(findGreaterInt);//runs sorted by a common compare key value
		for(SimRun run: runs) {
			results.putIfAbsent(run.getParameters().get(runArgs.getCompareKey()),new HashMap<Categories,Double>());// Fill in keys
			sortedRuns.putIfAbsent(run.getParameters().get(runArgs.getCompareKey()),new ArrayDeque<SimRun>());// Fill in keys
		}
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
		//averages the sorted runs
		for(String key: sortedRuns.keySet()) {
			for(Categories c: Categories.values()) {
				double sum = 0;
				double count = sortedRuns.get(key).size();
			
				for(SimRun run:sortedRuns.get(key)) {
					sum += run.getFinal(c);
				}
				results.get(key).put(c, sum/count);
			}
		}
		//procedure
		// Copy all the runs into a set. Sort all the runs by the the compare key value and place them into a map(<String, List<SimRun>>) with the compare key values as the key
		//and lists of filtered sim runs as the values.
		// Make a new loop that itterates through the keyset of the new map. make a new variable that keeps total of the sum. Then  for each filtered sim run list, make a inner loop itterating
		//through the values of SimRun.Categories, in the nested loop. add the value of map.get(key).get(i).finalVals(Category) to the sum, then divide by the length of the list of simruns to get the average
		//the place it on the results map with results.get(key).put(Category,average);
		
		return results;
	}

	public static TreeMap<String,HashMap<Categories,Double>> stdDevResults(TreeMap<String,HashMap<Categories,Double>> averages,SimRun[] runs,RunArguments runArgs){
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
}
