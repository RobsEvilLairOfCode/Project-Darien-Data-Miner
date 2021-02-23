import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import javax.imageio.ImageIO;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;

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
	//Checks if one parameter is the same then Averages two sim results 
	public HashMap<Categories,Double> averageResultsWithCheck(SimRun other,String checkKey,boolean returnOther){
		boolean condition =  parameters.get(checkKey).equals(other.getParameters().get(checkKey));
		if(condition)return averageResults(other);
		if(returnOther) return other.getAllFinal();
		return this.getAllFinal();
	}
	//Checks if one parameter is the same then Averages two sim results 
		public HashMap<Categories,Double> averageResultsWithCheck(HashMap<Categories,Double> other,String checkKey, HashMap<String,String> otherParams, boolean returnOther){
			boolean condition =  parameters.get(checkKey).equals(otherParams.get(checkKey));
			if(condition)return averageResults(other);
			if(returnOther) return other;
			return this.getAllFinal();
		}
	public HashMap<Categories,Double> averageResults(SimRun other) {
		HashMap<Categories,Double> results = new HashMap<Categories, Double>();
		
		for(Categories c: Categories.values()) {
			double average = (getAllFinal().get(c) + other.getAllFinal().get(c)) / 2;
			results.put(c,average);
		}
		System.out.println("Averaging " + getFinal(Categories.Recovered) + " of run " + runID + " and  " + other.getFinal(Categories.Recovered) +" of run " +other.getRunID()+ " produced " + results.get(Categories.Recovered));
		return results;
	}
	public HashMap<Categories,Double> averageResults(HashMap<Categories,Double> other) {
		HashMap<Categories,Double> results = new HashMap<Categories, Double>();
		for(Categories c: Categories.values()) {
			double average = (getAllFinal().get(c) + other.get(c)) / 2;
			results.put(c,average);
		}
		System.out.println("Averaging " + getFinal(Categories.Recovered) + " of run " + runID + " and  " + other.get(Categories.Recovered) +" of avg list produced " + results.get(Categories.Recovered));
		return results;
	}
	
	public static TreeMap<String,HashMap<Categories,Double>> averageAllResults(SimRun[] runs){
		TreeMap<String,HashMap<Categories,Double>> results = new TreeMap<String,HashMap<Categories,Double>>(findGreaterInt);
		TreeMap<String,ArrayDeque<SimRun>> sortedRuns = new TreeMap<String,ArrayDeque<SimRun>>(findGreaterInt);//runs sorted by a common compare key value
		for(SimRun run: runs) {
			results.putIfAbsent(run.getParameters().get(CSVManager.compareKey),new HashMap<Categories,Double>());// Fill in keys
			sortedRuns.putIfAbsent(run.getParameters().get(CSVManager.compareKey),new ArrayDeque<SimRun>());// Fill in keys
		}
		//sort the runs into the sorted runs map
		for(String key: results.keySet()) {
			for(SimRun run:runs) {
				if(run.getParameters().get(CSVManager.compareKey).contentEquals(key)) {//first check what the value of the compare key is so it can be sorted correctly
					boolean paramsDiff = false;
					for(String paramKey:CSVManager.targetParams.keySet()) {//check if target params are different
						if(!run.getParameters().get(paramKey).equals(CSVManager.targetParams.get(paramKey))) {
							paramsDiff = true;
						}
					}
					if(!paramsDiff)sortedRuns.get(key).add(run);
				}
			}
		}
		//READ THROUGH SORTEDRUNS AS A CHECK
		for(String key: sortedRuns.keySet()) {
			System.out.println(key +": " + sortedRuns.get(key));
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
		//READ THROUGH results AS A CHECK
		for(String key: results.keySet()) {
			System.out.println(key +": " + results.get(key));
		}

		//procedure
		// Copy all the runs into a set. Sort all the runs by the the compare key value and place them into a map(<String, List<SimRun>>) with the compare key values as the key
		//and lists of filtered sim runs as the values.
		// Make a new loop that itterates through the keyset of the new map. make a new variable that keeps total of the sum. Then  for each filtered sim run list, make a inner loop itterating
		//through the values of SimRun.Categories, in the nested loop. add the value of map.get(key).get(i).finalVals(Category) to the sum, then divide by the length of the list of simruns to get the average
		//the place it on the results map with results.get(key).put(Category,average);
		
		return results;
	}
	
	public static TreeMap<String,HashMap<Categories,Double>> averageAllResultsLegacy(SimRun[] runs){
		TreeMap<String,HashMap<Categories,Double>> results = new TreeMap<String,HashMap<Categories,Double>>(findGreaterInt);
		for(SimRun run: runs) {
			results.putIfAbsent(run.getParameters().get(CSVManager.compareKey),null);// Fill in keys
		}
		//THIS FOLLOWING LOOP ADDS ALOT OF TIME TO THE PROCESS BECAUSE IT IS LAZILY PROGRAMMED. IF THE PROGRAM IS LENGTHY TO EXECUTE, STREAMLINE THE CODE BELOW.
		for(SimRun run: runs) {
			HashMap<Categories,Double> average = run.averageResultsWithCheck(runs[0],"percent-vaccinated",false);//do the first with the current run
			for(int index = 1; index < runs.length;index++) {
				average = runs[index].averageResultsWithCheck(average,"percent-vaccinated",run.getParameters(),true);
			}
			results.put(run.getParameters().get("percent-vaccinated"), average);
		}
		return results;
	}
	public static TreeMap<String,HashMap<Categories,Double>> stdDevResults(TreeMap<String,HashMap<Categories,Double>> averages,SimRun[] runs){
		TreeMap<String,HashMap<Categories,ArrayList<Double>>> offsets = new TreeMap<String,HashMap<Categories,ArrayList<Double>>>();
		TreeMap<String,HashMap<Categories,Double>> variance = new TreeMap<String,HashMap<Categories,Double>>(findGreaterInt);
		for(SimRun run: runs) {
			offsets.putIfAbsent(run.getParameters().get(CSVManager.compareKey),new HashMap<Categories,ArrayList<Double>>());// Fill in keys
			variance.putIfAbsent(run.getParameters().get(CSVManager.compareKey),new HashMap<Categories,Double>());// Fill in keysw
		}
		System.out.println("Compare Key: " + CSVManager.compareKey);
		for(String key: offsets.keySet()) {
			System.out.println(key);
			for(Categories c: Categories.values()) {
				offsets.get(key).put(c, new ArrayList<Double>());
				
			}
		}
		//fill offsets
			for(SimRun run:runs) {
				for(Categories c: Categories.values()) {
				String key = run.getParameters().get(CSVManager.compareKey);//key for variance based on 
				double average = averages.get(key).get(c);
				double offset = Math.abs(average - run.getFinal(c));
				offsets.get(key).get(c).add(offset);
			}
		}
		System.out.println("Average: "+ averages.get("0").get(Categories.Recovered));
		System.out.println("Offset: "+offsets.get("0").get(Categories.Recovered));
			
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
	public static HashMap<Categories,Double> simpleResultAverage(SimRun[] runs) {
		HashMap<Categories,Double> results = new HashMap<Categories,Double>();
		//for(Categories c: Categories.values()) {
		//	results.put(c, null);
		//}//fill the keys with the categories and put null as a placeholder
		
		//For each category make a simple average of all the sim run variables corresponding to the category and write u
		double total = 0;
		double count = 0;
		for(Categories c: Categories.values()) {
			for(SimRun run: runs) {//For each ru
				count += 1.0;
				total += run.getFinal(c);
			}
			results.put(c, (double)(total/count));
		}
		return results;
	}
	private static HashMap<Categories,Double> simpleResultVariance(SimRun[] runs){
		HashMap<Categories,Double> averages = simpleResultAverage(runs);
		HashMap<Categories,Double> variance = new HashMap<Categories,Double>();
	
		for(Categories c: Categories.values()) {
			ArrayList<Double> offsets = new ArrayList<Double>(); 
			for(SimRun run:runs) {
				Double d = Math.abs(run.getFinal(c) - averages.get(c));
				offsets.add(d);
			}
			offsets.replaceAll(new UnaryOperator<Double>() {

				@Override
				public Double apply(Double t) {
					
					return Math.pow(t, 2);
				}});
			
			double sum = 0;
			for(Double d: offsets) {
				sum += d;
			}
			
			 variance.put(c, sum / (double)(offsets.size() - 1));
		}
		
		return variance;
	}
	public static HashMap<Categories,Double> simpleResultStandardDeviation(SimRun[] runs){
		HashMap<Categories,Double> variance = simpleResultVariance(runs);
		variance.replaceAll(new BiFunction<Categories,Double,Double>(){

			@Override
			public Double apply(Categories t, Double u) {
				
				return Math.sqrt(u);
			}});
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
		
	
	//Creates a graph with vaccine increment of 5; Averages similar runs
	public static Task<Pane> createVaccineStepGraph = new Task<Pane>(){

		@Override
		protected Pane call() throws Exception {
			SimRun[] runs = CSVManager.simRunData;
			StackPane parent = new StackPane();
			VBox headerPane = new VBox();//Contains title and parameters
			HBox graph = new HBox(5);// HBox that contains each individual bar
			Pane ticks = makeVerticalTickMark(200,5,0,100);
			ticks.setViewOrder(0);
			graph.getChildren().add(ticks); //Adds the tick mark before everything else
			graph.setOnMouseClicked(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent arg0) {
					 try {
						ClassLoader.getPlatformClassLoader().loadClass("javafx.swing.embed.SwingFXUtils");
					} catch (ClassNotFoundException e2) {
						// TODO Auto-generated catch block 
						e2.printStackTrace();
					}
					 WritableImage snapshot = graph.snapshot(null, null);
					 BufferedImage image = SwingFXUtils.fromFXImage(snapshot, null);
					 File out = new File(System.getProperty("user.home")+"/graph.png");
					 try {
						out.createNewFile();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					 try {
						ImageIO.write(image,"png",out);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}});			
			HashMap<String, SimRun> data = averageAll(runs);
			System.out.println("Recovered: "+data.get("0").getMean(Categories.Recovered));
			System.out.println("Exposed: "+data.get("0").getMean(Categories.Exposed));
			System.out.println("Healthy: "+data.get("0").getMean(Categories.Healthy));
			System.out.println("Hospitalized: "+data.get("0").getMean(Categories.Hospitalized));
			System.out.println("Infected: "+data.get("0").getMean(Categories.Infected));
			System.out.println("Vaccinated: "+data.get("0").getMean(Categories.Vaccinated));
			
			int count = 0;
			for(String key: data.keySet()) {
				VBox bar = (VBox)createBar(data.get(key));
				graph.getChildren().add(bar);
				count++;
				updateProgress(count,data.size());
				
			}
			return graph;
		}};
	
	public static Pane createBar(SimRun data) {
		VBox bar = new VBox(0);
		for(Categories c: Categories.values()) {
			final double width = 20;
			final double height = 2 * data.getMean(c);
			Color color = null;
			switch(c) {
			case Exposed:
				color = Color.GREEN;
				break;
			case Healthy:
				color = Color.GREEN;
				break;
			case Hospitalized:
				color = Color.RED;
				break;
			case Infected:
				color = Color.ORANGE;
				break;
			case Recovered:
				color = Color.ORANGE;
				break;
			case Vaccinated:
				color = Color.BLUE;
				break;
			default:
				break;
			
			}
			
			Rectangle rect = new Rectangle(width,height,Paint.valueOf(color.toString()));
			bar.getChildren().add(rect);
		}
		return bar;
	}
	public static HashMap<String, SimRun> averageAll(SimRun[] runs){
		HashMap<String, SimRun> data = new HashMap<String, SimRun>();
		for(int index = 0; index < runs.length; index++) {
			data.putIfAbsent(runs[index].getParameters().get("percent-vaccinated"), runs[index]);//set a run for each percent vaccinated
		}
		
		for(String key: data.keySet()) {// For each key..
			for(int index = 0; index < runs.length; index++) {
				data.put(key, data.get(key).averageIfSimilarParams(runs[index]));//Got through the data and average all the sim runs that share that data.
			}
		}
		System.out.println("Dayduh Size: "+data.size());
		System.out.println("Keys: "+data.keySet());
		return data;
	}

	public static void adjustSimRunList() {
		if(CSVManager.targetParams == null) { //When selecting 'all data option'
			CSVManager.adjustedSimRunData = CSVManager.simRunData.clone();
			return;
		}
		ArrayList<SimRun> adjustedRuns = new ArrayList<SimRun>();
		for(SimRun run:CSVManager.simRunData) {// goes though each run in the list, filters by targetparameters, ignores compareKey parameter
			boolean viable = true;
			for(String key:CSVManager.targetParams.keySet()) {
				if(key.contentEquals(CSVManager.compareKey)) continue;
				if(!run.getParameters().get(key).contentEquals(CSVManager.targetParams.get(key))) {
					viable = false;
				}
			}
			if(viable) adjustedRuns.add(run);
		}
		System.out.println("Removed " + (CSVManager.simRunData.length - adjustedRuns.size()) + " items" );
	}
	
	private static Pane makeVerticalTickMark(double height, int step, double min, double max) {
		Pane parent = new Pane();
		parent.setPrefHeight(height);
		parent.setMaxHeight(height);
		
		final double tickLength = height / 20.0;
		final double lineWidth = 1;
		
		Line spine = new Line();
		spine.setStartX(0);
		spine.setStartY(0);
		spine.setEndX(0);
		spine.setEndY(height);
		
		spine.setStrokeWidth(lineWidth);
		
		parent.getChildren().add(spine);
		
		double increment = height/((max-min)/(int)step); //visual spacing of the tick marks
		System.out.println(increment);
		double count = min;//Text
		for(double currentHeight = height; currentHeight >= 0; currentHeight -= increment) {//places tick marks along the spine
			Line tick = new Line();
			Label label = new Label();//Labels the text
			
			if(count == min || count == max)label.setText(Double.toString(count));
			tick.setStartX(0);
			tick.setEndX(tickLength);
			tick.setStartY(currentHeight);
			tick.setEndY(currentHeight);
			tick.setStroke(Paint.valueOf(Color.DARKRED.toString()));
			
			label.setMaxHeight(10);
			label.setTranslateX(tickLength + 5);
			label.setTranslateY(currentHeight - label.getMaxHeight());
			
			count+=step;//Increment Count for the next loop
			parent.getChildren().addAll(tick,label);
		}
		return parent;
	}

}
