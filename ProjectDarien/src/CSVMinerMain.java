import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;

public class CSVMinerMain extends Application{
	public static void main(String[] args) {
		launch(args);
	}

	public final double WINDOWSIZEX = 400;
	public final double WINDOWSIZEY = 400;
	public final double PADDING = 20.0;
	
	public static RunArguments runArguments = RunArguments.getDefault();
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Project Darien Data Averager");
		
		BorderPane primaryPane = new BorderPane();
		
		ImageView banner = new ImageView(new Image("udellogo.jpg"));
		
		StackPane nextbackPane = new StackPane();
		
		Button nextButton = new Button("Next");
		Button backButton = new Button("Back");
		
		Pane fileSelectionPane = getFileSelectionPane(nextButton);
		Pane runArgumentPane = getRunArgumentPane();
		Pane compareKeyandParamPane = null;
		

		nextButton.setDisable(true);
		nextButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				if(fileSelectionPane.getParent() == primaryPane) {//If the fileSelectPane is open, go to the run argument pane
					if(!CSVManager.isInitialized()) {//Ensure CSVManager is initialized
						ProgressBar pb = new ProgressBar();
						Label progtext = new Label("0%");
						pb.setPrefWidth(WINDOWSIZEX);
						nextbackPane.getChildren().addAll(pb,progtext);
						StackPane.setAlignment(pb, Pos.BOTTOM_CENTER);
						StackPane.setAlignment(progtext, Pos.BOTTOM_CENTER);
						pb.progressProperty().bind(CSVManager.init.progressProperty());
						pb.progressProperty().addListener(new ChangeListener<Number>() {

							@Override
							public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
								progtext.setText((int)Math.round(arg2.doubleValue() * 100) + "%");
								if(arg2.doubleValue() >= 1) {
									pb.setVisible(false);
									progtext.setVisible(false);
									nextbackPane.getChildren().removeAll(pb,progtext);
								}
							}});
						new Thread(CSVManager.init).start();
					}
					CSVManager.init.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

						@Override
						public void handle(WorkerStateEvent arg0) {
							primaryPane.getChildren().remove(fileSelectionPane);
							primaryPane.setCenter(runArgumentPane);
						}});
					backButton.setDisable(false);
					
					
					
				}else if(runArgumentPane.getParent() == primaryPane) {
					ProgressBar pb = new ProgressBar();
					Label progtext = new Label("Mining - 0%");
					pb.setPrefWidth(WINDOWSIZEX);
					StackPane.setAlignment(pb, Pos.BOTTOM_CENTER);
					StackPane.setAlignment(progtext, Pos.BOTTOM_CENTER);
					pb.progressProperty().bind(CSVManager.mine.progressProperty());
					nextbackPane.getChildren().addAll(pb,progtext);
					pb.progressProperty().addListener(new ChangeListener<Number>() {

						@Override
						public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
							progtext.setText("Mining - " + (int)Math.round(arg2.doubleValue() * 100) + "%");
						}});
					new Thread(CSVManager.mine).start();
					CSVManager.mine.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

						@Override
						public void handle(WorkerStateEvent arg0) {
							pb.setVisible(false);
							progtext.setVisible(false);
							nextbackPane.getChildren().removeAll(pb,progtext);
							primaryPane.getChildren().remove(runArgumentPane);
							Pane comparePane = getCompareKeyandParamPane();
							comparePane.setId("comparePane");
							primaryPane.setCenter(comparePane);
						}});
					
				}else if(primaryPane.getCenter().getId().equals("comparePane")) {//When it is the getCompareKeyandParamPane;
					new Thread(CSVManager.analyize).start();
				}
				
			}});
		backButton.setDisable(true);
		backButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				if(runArgumentPane.getParent() == primaryPane) {
					CSVManager.wipe();
					backButton.setDisable(true);
					primaryPane.getChildren().remove(runArgumentPane);
					primaryPane.setCenter(fileSelectionPane);
				}
				
			}});	
		nextbackPane.getChildren().addAll(nextButton,backButton);
		
		StackPane.setAlignment(nextButton,Pos.CENTER_RIGHT);
		StackPane.setAlignment(backButton, Pos.CENTER_LEFT);

		primaryPane.setTop(banner);
		primaryPane.setCenter(fileSelectionPane);
		primaryPane.setBottom(nextbackPane);
		
		
		
		Scene primaryScene = new Scene(primaryPane,WINDOWSIZEX,WINDOWSIZEY);
		
		primaryStage.setScene(primaryScene);
		
		if(this.getParameters().getRaw().isEmpty()) {
			primaryStage.show();
		}else {
			handleCMD();
		}
	}
	public void handleCMD() {
		List<String> params = this.getParameters().getRaw();
		String filePath = params.get(0);
		String runOp = params.get(1);
		String compareKey = params.get(2);

		CSVManager.setCSVFile(new File(filePath));
		CSVManager.init.run();
		if(runOp.contentEquals("-d"))CSVMinerMain.runArguments = RunArguments.getDefault();
		else if(runOp.contentEquals("-a"))CSVMinerMain.runArguments = RunArguments.autoGenerate();
		
		CSVManager.mine.run();
			
		CSVManager.compareKey = compareKey;
		CSVManager.targetParams = null;
		
		CSVManager.analyize.run();
		System.exit(0);
	}
	public Pane getFileSelectionPane(Button nextButton) {//First Pane
		StackPane parent = new StackPane();
		Label header = new Label();
		TextField pathField = new TextField();
		Button fileChooserButton = new Button();
		
		header.setText("Enter the path of the CSV file, or click the 'Select File' button to open the file chooser.");
		header.setWrapText(true);
		header.setMaxWidth(WINDOWSIZEX - PADDING);
		header.setMaxHeight(WINDOWSIZEY - PADDING);
		
		if(CSVManager.CSVFile != null) {
			pathField.setText(CSVManager.CSVFile.getAbsolutePath());
		}else {
			pathField.setText("");
		}
		pathField.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {//Disables the next button until there is a proper input
				if(new File(pathField.getText()).exists() && pathField.getText().contains(".csv")) {
					nextButton.setDisable(false);
				}else {
					nextButton.setDisable(true);
				}
			}});
		
		
		fileChooserButton.setText("Select File");
		fileChooserButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
				fileChooser.setTitle("Please select the CSV File");
				fileChooser.getExtensionFilters().add(new ExtensionFilter("Comma Seperated Variables (*.csv)","*.csv"));
				File choice = fileChooser.showOpenDialog(new Stage());
				CSVManager.setCSVFile(choice);
				pathField.setText(choice.getAbsolutePath());
			}});
		
		parent.getChildren().addAll(header,pathField,fileChooserButton);
		StackPane.setAlignment(header, Pos.TOP_LEFT);
		StackPane.setAlignment(pathField, Pos.BOTTOM_LEFT);
		StackPane.setAlignment(fileChooserButton, Pos.BOTTOM_RIGHT);
		
		parent.setPadding(new Insets(PADDING));
		
		return parent;
	}
	public Pane getRunArgumentPane() {//Second Pane
		VBox parent = new VBox();
		Label header = new Label();
		
		header.setText("Please enter the following values so that the program may properly locate the required information, or click the 'Autodetect' button to attempt to locate the information automatically");
		header.setWrapText(true);
		header.setMaxWidth(WINDOWSIZEX - PADDING);
		header.setMaxHeight(200);
		header.setPadding(new Insets(7));
		
		ScrollPane valuescrollPane = new ScrollPane();
		BorderPane valuesBox = new BorderPane();
		
		VBox valuesBoxLeft = new VBox(20);
		VBox valuesBoxRight = new VBox(20);
		
		BorderPane buttonsPane = new BorderPane();
		Button defaultButton = new Button("Default Values");
		Button autogenButton = new Button("AutoDetect");
		
		TextField secLen = new TextField();
		secLen.setPromptText("Section Length");
		secLen.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				for(char c: arg2.toCharArray()) {//check if all chars are digits
					if(!Character.isDigit(c)) {
						secLen.setText(arg1);
						return;
					}
				}
				if(!arg2.isBlank() && Integer.parseInt(arg2) > CSVManager.CSVRows.length) {
					secLen.setText(arg1);
					return;
				}
				runArguments.setSectionLen(Integer.parseInt(arg2));
				System.out.println(runArguments.toString());
			}});
		
		TextField runIDRow = new TextField();
		runIDRow.setPromptText("Run ID Row");
		runIDRow.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				for(char c: arg2.toCharArray()) {//check if all chars are digits
					if(!Character.isDigit(c)) {
						runIDRow.setText(arg1);
						return;
					}
				}
				if(!arg2.isBlank() && Integer.parseInt(arg2) > CSVManager.CSVRows.length) {
					runIDRow.setText(arg1);
					return;
				}
				runArguments.setRunIDRow(Integer.parseInt(arg2));
				System.out.println(runArguments.toString());
			}});
		
		TextField runIDCol = new TextField();
		runIDCol.setPromptText("Run ID Column");
		runIDCol.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				for(char c: arg2.toCharArray()) {//check if all chars are digits
					if(!Character.isDigit(c)) {
						runIDCol.setText(arg1);
						return;
					}
				}
				if(!arg2.isBlank() && Integer.parseInt(arg2) > CSVManager.CSVRows.length) {
					runIDCol.setText(arg1);
					return;
				}
				runArguments.setRunIDCol(Integer.parseInt(arg2));
				System.out.println(runArguments.toString());
			}});
		
		TextField filler1 = new TextField();//Useless, Not Visible
		filler1.setVisible(false);
		filler1.setDisable(true);

		TextField paramsRow = new TextField();
		paramsRow.setPromptText("Parameters Row");
		paramsRow.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				for(char c: arg2.toCharArray()) {//check if all chars are digits
					if(!Character.isDigit(c)) {
						paramsRow.setText(arg1);
						return;
					}
				}
				if(!arg2.isBlank() && Integer.parseInt(arg2) > CSVManager.CSVRows.length) {
					paramsRow.setText(arg1);
					return;
				}
				runArguments.setParamsRow(Integer.parseInt(arg2));
				System.out.println(runArguments.toString());
			}});

		TextField paramsCol = new TextField();
		paramsCol.setPromptText("Parameters Column");
		paramsCol.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				for(char c: arg2.toCharArray()) {//check if all chars are digits
					if(!Character.isDigit(c)) {
						paramsCol.setText(arg1);
						return;
					}
				}
				if(!arg2.isBlank() && Integer.parseInt(arg2) > CSVManager.CSVRows.length) {
					paramsCol.setText(arg1);
					return;
				}
				runArguments.setParamsCol(Integer.parseInt(arg2));
				System.out.println(runArguments.toString());
			}});
		
		TextField filler2 = new TextField();//Useless, Not Visible
		filler2.setVisible(false);
		filler2.setDisable(true);
		
		TextField numOfParams = new TextField();
		numOfParams.setPromptText("Number Of Parameters");
		numOfParams.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				for(char c: arg2.toCharArray()) {//check if all chars are digits
					if(!Character.isDigit(c)) {
						numOfParams.setText(arg1);
						return;
					}
				}
				if(!arg2.isBlank() && Integer.parseInt(arg2) > CSVManager.CSVRows.length) {
					numOfParams.setText(arg1);
					return;
				}
				runArguments.setNumofParams(Integer.parseInt(arg2));
				System.out.println(runArguments.toString());
			}});
		
		TextField dataRow = new TextField();
		dataRow.setPromptText("Data Row");
		dataRow.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				for(char c: arg2.toCharArray()) {//check if all chars are digits
					if(!Character.isDigit(c)) {
						dataRow.setText(arg1);
						return;
					}
				}
				if(!arg2.isBlank() && Integer.parseInt(arg2) > CSVManager.CSVRows.length) {
					dataRow.setText(arg1);
					return;
				}
				runArguments.setDataRow(Integer.parseInt(arg2));
				System.out.println(runArguments.toString());
			}});
		

		TextField dataCol = new TextField();
		dataCol.setPromptText("Data Column");
		dataCol.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				for(char c: arg2.toCharArray()) {//check if all chars are digits
					if(!Character.isDigit(c)) {
						dataCol.setText(arg1);
						return;
					}
				}
				if(!arg2.isBlank() && Integer.parseInt(arg2) > CSVManager.CSVRows.length) {
					dataCol.setText(arg1);
					return;
				}
				runArguments.setDataCol(Integer.parseInt(arg2));
				System.out.println(runArguments.toString());
			}});
		
		autogenButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				runArguments = RunArguments.autoGenerate();
				
				secLen.setText(runArguments.getSectionLen() + "");
				runIDRow.setText(runArguments.getRunIDRow() + "");
				runIDCol.setText(runArguments.getRunIDCol() + "");
				paramsRow.setText(runArguments.getParamsRow() + "");
				paramsCol.setText(runArguments.getParamsCol() + "");
				numOfParams.setText(runArguments.getNumofParams() + "");
				dataRow.setText(runArguments.getDataRow() + "");
				dataCol.setText(runArguments.getDataCol() + "");
			}});
		
		defaultButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				runArguments = RunArguments.getDefault();
				
				secLen.setText(runArguments.getSectionLen() + "");
				runIDRow.setText(runArguments.getRunIDRow() + "");
				runIDCol.setText(runArguments.getRunIDCol() + "");
				paramsRow.setText(runArguments.getParamsRow() + "");
				paramsCol.setText(runArguments.getParamsCol() + "");
				numOfParams.setText(runArguments.getNumofParams() + "");
				dataRow.setText(runArguments.getDataRow() + "");
				dataCol.setText(runArguments.getDataCol() + "");
			}});
		
		valuesBox.setLeft(valuesBoxLeft);
		valuesBox.setRight(valuesBoxRight);
		valuesBox.setPrefWidth(WINDOWSIZEX);
		valuesBox.setPadding(new Insets(10));
		
		valuesBoxLeft.setMaxWidth(WINDOWSIZEX/2);
		valuesBoxRight.setMaxWidth(WINDOWSIZEX/2);
		valuesBoxLeft.setAlignment(Pos.CENTER);
		valuesBoxRight.setAlignment(Pos.CENTER);
		valuesBoxLeft.getChildren().addAll(secLen,runIDRow,paramsRow,numOfParams,dataRow);
		valuesBoxRight.getChildren().addAll(filler1,runIDCol,paramsCol,filler2,dataCol);
		
		valuescrollPane.setContent(valuesBox);
		valuescrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		
		buttonsPane.setLeft(defaultButton);
		buttonsPane.setRight(autogenButton);
		parent.getChildren().addAll(header,valuescrollPane,buttonsPane);

		return parent;
	}
	public Pane getCompareKeyandParamPane() {
		VBox parent = new VBox(20);
		Label header = new Label();
		HBox compareKeyPane = new HBox(5);
			Label compareKeyLabel = new Label("Compare Key: ");
			Spinner<String> compareKeySpinner = new Spinner<String>();
		ScrollPane paramPaneScroll = new ScrollPane();
		HBox paramPane = new HBox(20);
		
		header.setText("Enter the compare key into the spinner, and then select the exclusive params, or pick 'All data' to use all data in analysis.");
		header.setWrapText(true);
		
		SpinnerValueFactory.ListSpinnerValueFactory<String> compareKeyFactory = new SpinnerValueFactory.ListSpinnerValueFactory<String>(FXCollections.observableList(new ArrayList<String>(CSVManager.simRunData[0].getParameters().keySet())));
		compareKeySpinner.setValueFactory(compareKeyFactory);
		
		compareKeySpinner.valueProperty().addListener(new ChangeListener<String>() {//Populates the paramPane with radioButtons represent the param constraints

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				CSVManager.compareKey = arg2;
				paramPane.getChildren().clear();
				
				HashSet<HashMap<String, String>> paramList = new HashSet<HashMap<String, String>>();
				for(SimRun run: CSVManager.simRunData) { // remove the compare key from the lists
					HashMap<String, String> params = (HashMap<String, String>) run.getParameters().clone();
					params.remove(CSVManager.compareKey);
					paramList.add(params);
				}
				System.out.println( "Removed key:"+ arg2 + " for "+ paramList.toString());
				ToggleGroup toggleGroup = new ToggleGroup();//
				//Add in all data button first
				RadioButton addAllButton = new RadioButton();
				addAllButton.setToggleGroup(toggleGroup);
				addAllButton.setText("Add All");
				toggleGroup.selectToggle(addAllButton);
				addAllButton.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent arg0) {
						CSVManager.targetParams = null;
						SimRun.adjustSimRunList();
					}});
				paramPane.getChildren().add(addAllButton);
				for(HashMap<String, String> params: paramList) {
					RadioButton button = new RadioButton();
					button.setToggleGroup(toggleGroup);
					String text = "";
					text = params.toString().replace(',', '\n');
					button.setText(text);
					button.setUserData(params);
					button.setOnAction(new EventHandler<ActionEvent>() {

						@Override
						public void handle(ActionEvent arg0) {
							CSVManager.targetParams = params;
							SimRun.adjustSimRunList();
						}});
					paramPane.getChildren().add(button);
				}
			}
		});
		
		paramPaneScroll.setMaxHeight(200);
		paramPaneScroll.setPrefHeight(200);
		compareKeyPane.getChildren().addAll(compareKeyLabel, compareKeySpinner);
		paramPaneScroll.setContent(paramPane);
		parent.getChildren().addAll(header,compareKeyPane,paramPaneScroll);
		
		
		return parent;
	}
}
