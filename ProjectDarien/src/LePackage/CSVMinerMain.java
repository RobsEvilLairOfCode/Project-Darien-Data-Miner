package LePackage;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import LePackage.RunArguments.MODE;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SpinnerValueFactory.ListSpinnerValueFactory;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;

public class CSVMinerMain extends Application{
	public final double WINDOWSIZEX = 400;
	public final double WINDOWSIZEY = 400;
	public final double PADDING = 20.0;
	
	private RunArguments runArguments = null;
	private CSVManager manager = null;
	public static void main(String[] args) {
		launch(args);
	}
	@Override
	public void start(Stage primaryStage) throws Exception {
		
		primaryStage.setTitle("Project Darien Data Averager");
		
		Scene primaryScene = null;
		BorderPane primaryPane = new BorderPane();
		
		ImageView banner = new ImageView(new Image(this.getClass().getResourceAsStream("udellogo.jpg")));
		
		StackPane nextbackPane = new StackPane();
		
		Button nextButton = new Button("Next");
		Button backButton = new Button("Back");
		
		Pane fileSelectionPane = getFileSelectionPane(nextButton);
		Pane runArgumentPane = getRunArgumentPane();
		

		nextButton.setDisable(true);
		nextButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				if(fileSelectionPane.getParent() == primaryPane) {//If the fileSelectPane is open, go to the run argument pane
					if(manager.getCSVFile().isDirectory()) {
						File dir = manager.getCSVFile();
						handleCMD(dir);
					}
					if(!manager.isInitialized()) {//Ensure CSVManager is initialized
						ProgressBar pb = new ProgressBar();
						Label progtext = new Label("0%");
						pb.setPrefWidth(WINDOWSIZEX);
						nextbackPane.getChildren().addAll(pb,progtext);
						StackPane.setAlignment(pb, Pos.BOTTOM_CENTER);
						StackPane.setAlignment(progtext, Pos.BOTTOM_CENTER);
						pb.progressProperty().bind(manager.init.progressProperty());
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
						new Thread(manager.init).start();
					}
					manager.init.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

						@Override
						public void handle(WorkerStateEvent arg0) {
							primaryPane.getChildren().remove(fileSelectionPane);
							primaryPane.setCenter(runArgumentPane);
						}});
					backButton.setDisable(false);
					
					
					
				}else if(runArgumentPane.getParent() == primaryPane) {
					runArguments.update(manager);
					
					ProgressBar pb = new ProgressBar();
					Label progtext = new Label("Mining - 0%");
					pb.setPrefWidth(WINDOWSIZEX);
					StackPane.setAlignment(pb, Pos.BOTTOM_CENTER);
					StackPane.setAlignment(progtext, Pos.BOTTOM_CENTER);
					pb.progressProperty().bind(manager.mine.progressProperty());
					nextbackPane.getChildren().addAll(pb,progtext);
					pb.progressProperty().addListener(new ChangeListener<Number>() {

						@Override
						public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
							progtext.setText("Mining - " + (int)Math.round(arg2.doubleValue() * 100) + "%");
						}});
					new Thread(manager.mine).start();
					manager.mine.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

						@Override
						public void handle(WorkerStateEvent arg0) {
							if(manager.getRunArgs().getMode()== RunArguments.MODE.Average) {
								manager.analyize.run();	
								System.out.println("Manger: "+manager.getAnalysisFile());
								progtext.setText("Saved Analysis Sheet: " + manager.getAnalysisFile().getAbsolutePath());
							}
							else if(manager.getRunArgs().getMode() == RunArguments.MODE.Compress) {
								manager.compress.run();
								System.out.println("Manger: "+manager.getCompressFile());
								System.out.println(manager.compress.getException());
								progtext.setText("Saved Compress Sheet: " + manager.getCompressFile().getAbsolutePath());
							}
							
							// new GraphMaker(manager).showChart();
							//pb.setVisible(false);
							//progtext.setVisible(false);
							//nextbackPane.getChildren().removeAll(pb,progtext);
							//primaryPane.getChildren().remove(runArgumentPane);

						}});
					
				}
			}});
		backButton.setDisable(true);
		backButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				if(runArgumentPane.getParent() == primaryPane) {
					manager.wipe();
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
		
		
		
		primaryScene = new Scene(primaryPane,WINDOWSIZEX,WINDOWSIZEY);
		
		primaryStage.setScene(primaryScene);
		primaryStage.setResizable(false);
		
		if(this.getParameters().getRaw().isEmpty()) {
			primaryStage.show();
		}else {
			handleCMD();
		}
	}
	public void handleCMD() {
		System.out.println("Using Command Line Mode");
		List<String> params = this.getParameters().getRaw();
		String filePath = params.get(0);
		if(filePath.contentEquals("-help")) printHelp();
		if(new File(filePath).isDirectory()) {
			int completed = 0;
			int len = new File(filePath).listFiles().length;
			for(File file: new File(filePath).listFiles()) {
				if(file.getName().contains(".csv")) {
					try {
						handleFileCMD(file);
					} catch (Exception e) {
						System.err.println("Epic Fail!");
						e.printStackTrace();
					}
				}
				completed++;
				System.out.println(completed + " out of " + len);
			}
		}else {
			try {
				handleFileCMD(new File(filePath));
				System.gc();
			} catch (Exception e) {
				System.err.println("Epic Fail!");
				e.printStackTrace();
			}
		}
		System.exit(0);
	}
	public void handleCMD(File dir) {//Over ride if directory is selected in gui
		System.out.println("Switching to Command Line Mode");
			int completed = 0;
			int len = dir.listFiles().length;
			System.out.println(completed + " out of " + len);
			for(File file: dir.listFiles()) {
				if(file.getName().contains(".csv")) {
					try {
						handleFileCMD(file);
					} catch (Exception e) {
						System.err.println("Epic Fail!");
						e.printStackTrace();
					}
				}
				completed++;
				System.out.println(completed + " out of " + len);
			}
		System.exit(0);
	}
	public void handleFileCMD(File csv) throws Exception{
		RunArguments runArgumentsCMD = null;
		CSVManager managerCMD = new CSVManager(csv,runArguments);
		managerCMD.init.run();
		runArgumentsCMD = RunArguments.autoGenerate(managerCMD);
		runArgumentsCMD.update(managerCMD);
		managerCMD.mine.run();
			
		managerCMD.analyize.run();
	}
	public void printHelp() {
		System.out.println("Jar Usage: java -jar PDMine.jar [FILEPATH]");
		System.out.println("EXE Usage: PDMine.exe [FILEPATH]");
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
		
		pathField.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {//Disables the next button until there is a proper input
				if(new File(pathField.getText()).exists() && (pathField.getText().contains(".csv") || new File(pathField.getText()).isDirectory())) {
					nextButton.setDisable(false);
					manager = new CSVManager(new File(pathField.getText()),runArguments);
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
				if(!arg2.isEmpty() && Integer.parseInt(arg2) > manager.getCSVRows().length) {
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
				if(!arg2.isEmpty() && Integer.parseInt(arg2) > manager.getCSVRows().length) {
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
				if(!arg2.isEmpty() && Integer.parseInt(arg2) > manager.getCSVRows().length) {
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
				if(!arg2.isEmpty() && Integer.parseInt(arg2) > manager.getCSVRows().length) {
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
				if(!arg2.isEmpty() && Integer.parseInt(arg2) > manager.getCSVRows().length) {
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
				if(!arg2.isEmpty() && Integer.parseInt(arg2) > manager.getCSVRows().length) {
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
				if(!arg2.isEmpty() && Integer.parseInt(arg2) > manager.getCSVRows().length) {
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
				if(!arg2.isEmpty() && Integer.parseInt(arg2) > manager.getCSVRows().length) {
					dataCol.setText(arg1);
					return;
				}
				runArguments.setDataCol(Integer.parseInt(arg2));
				System.out.println(runArguments.toString());
			}});
		TextField compareKey = new TextField();
		compareKey.setPromptText("Compare Key");
		compareKey.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				runArguments.setCompareKey(arg2);
				System.out.println(runArguments.toString());
			}});

		TextField day = new TextField();
		day.setPromptText("Day");
		day.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				for(char c: arg2.toCharArray()) {//check if all chars are digits
					if(!Character.isDigit(c)) {
						dataCol.setText(arg1);
						return;
					}
				}
				if(!arg2.isEmpty() && Integer.parseInt(arg2) > manager.getCSVRows().length) {
					dataCol.setText(arg1);
					return;
				}
				runArguments.setDay(Integer.parseInt(arg2));
				System.out.println(runArguments.toString());
			}});
		
		TextField filler3 = new TextField();//Useless, Not Visible
		filler3.setVisible(false);
		filler3.setDisable(true);
		
		TextField filler4 = new TextField();//Useless, Not Visible
		filler4.setVisible(false);
		filler4.setDisable(true);
		
		 CheckBox mode = new CheckBox();
		 mode.setText("Compress Mode");
		 mode.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				runArguments.setMode((arg2.booleanValue())?RunArguments.MODE.Compress:RunArguments.MODE.Average);
				System.out.println(runArguments.toString());
			}});
		
		
		autogenButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				runArguments = RunArguments.autoGenerate(manager);
				
				secLen.setText(runArguments.getSectionLen() + "");
				runIDRow.setText(runArguments.getRunIDRow() + "");
				runIDCol.setText(runArguments.getRunIDCol() + "");
				paramsRow.setText(runArguments.getParamsRow() + "");
				paramsCol.setText(runArguments.getParamsCol() + "");
				numOfParams.setText(runArguments.getNumofParams() + "");
				dataRow.setText(runArguments.getDataRow() + "");
				dataCol.setText(runArguments.getDataCol() + "");
				day.setText(runArguments.getDay() + "");
				compareKey.setText(runArguments.getCompareKey() + "");
			}});
		
		defaultButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				runArguments = RunArguments.getDefault(manager);
				
				secLen.setText(runArguments.getSectionLen() + "");
				runIDRow.setText(runArguments.getRunIDRow() + "");
				runIDCol.setText(runArguments.getRunIDCol() + "");
				paramsRow.setText(runArguments.getParamsRow() + "");
				paramsCol.setText(runArguments.getParamsCol() + "");
				numOfParams.setText(runArguments.getNumofParams() + "");
				dataRow.setText(runArguments.getDataRow() + "");
				dataCol.setText(runArguments.getDataCol() + "");
				day.setText(runArguments.getDay() + "");
				compareKey.setText(runArguments.getCompareKey() + "");
			}});
		
		valuesBox.setLeft(valuesBoxLeft);
		valuesBox.setRight(valuesBoxRight);
		valuesBox.setPrefWidth(WINDOWSIZEX);
		valuesBox.setPadding(new Insets(10));
		
		valuesBoxLeft.setMaxWidth(WINDOWSIZEX/2);
		valuesBoxRight.setMaxWidth(WINDOWSIZEX/2);
		valuesBoxLeft.setAlignment(Pos.CENTER);
		valuesBoxRight.setAlignment(Pos.CENTER);
		valuesBoxLeft.getChildren().addAll(secLen,runIDRow,paramsRow,numOfParams,dataRow,compareKey,day);
		valuesBoxRight.getChildren().addAll(filler1,runIDCol,paramsCol,filler2,dataCol,filler3,mode);
		
		valuescrollPane.setContent(valuesBox);
		valuescrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		
		buttonsPane.setLeft(defaultButton);
		buttonsPane.setRight(autogenButton);
		parent.getChildren().addAll(header,valuescrollPane,buttonsPane);

		return parent;
	}

}
