package LePackage;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class GraphMaker {
	final double BGWIDTH = 1600;
	final double BGHEIGHT = 900;
	
	final double GWIDTH = 1200;
	final double GHEIGHT = 700;
	
	final double HPADDING = 10;
	
	CSVManager manager;
	String[][] A;
	
	public GraphMaker(CSVManager manager) {
		this.manager = manager;
		this.A = manager.getAnalysisCells();
	}
	
//	public Pane createGraph() {
	//	BorderPane parent = new BorderPane();
	//	parent.setBackground(getBG());
	//	parent.setBottom(createGraphBaseLine());
		
	//	Stage stg = new Stage();
	///	Scene s = new Scene(parent);
	//	stg.setScene(s);
	//	stg.show();
		
	//	return parent;
	//}
	//public Background getBG() {
	//	Stop[] stops = new Stop[] { new Stop(0, Color.BLACK.brighter()), new Stop(0.5, Color.DARKGREY),new Stop(1, Color.BLACK.brighter())};
	//	LinearGradient lngnt = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
	//	Background bg = new Background(new BackgroundFill(Paint.valueOf(lngnt.toString()),CornerRadii.EMPTY,Insets.EMPTY));
	//	
	//	return bg;
	//}
	
	public StackedBarChart<String,Number> getChart() {
		CategoryAxis ckAxis = new CategoryAxis();//x
		NumberAxis avgAxis = new NumberAxis();//y
		StackedBarChart<String,Number> chart = new StackedBarChart<String,Number>(ckAxis, avgAxis);
		chart.setTitle(manager.getRunArgs().getCompareKey() + " chart");
		ckAxis.setLabel(this.manager.getRunArgs().getCompareKey());
		avgAxis.setLabel("Population(%)");
		ArrayList<String> category = new ArrayList<String>();
		for(int i = 1; i < A[25].length;i++) {
			System.out.println(A[25][i]);
			if(!A[25][i].isBlank()) {
				
				category.add(A[25][i]);
			}else {
				break;
			}
		}
		
		ckAxis.setCategories(FXCollections.<String>observableArrayList(category));
		for(int y = 26; y < 32;y++) {
			XYChart.Series<String, Number> series = new XYChart.Series<String, Number>();
			series.setName(A[y][0]);
			for(int x = 1; x < category.size() + 1;x++) {
				series.getData().add(new XYChart.Data<String, Number>(A[25][x],Double.parseDouble(A[y][x])));
			}
			System.out.println(series.getData());
			chart.getData().add(series);
		}
		return chart;
		
	}
	public Image chartImage() {
		StackedBarChart<String,Number> chart = getChart();
		return chart.snapshot(null, null);
	}
	public void showChart() {
		Image chart = chartImage();
		ImageView iv = new ImageView(chart);
		Stage stage = new Stage();
		Pane pane = new Pane();
		Scene scene = new Scene(pane);
		pane.getChildren().add(getChart());
		stage.setScene(scene);
		stage.show();
		
		
	}
	
	public ArrayList<Byte> fileHeader(Image chart){
		int w = (int)chart.getWidth();
		int h = (int)chart.getHeight();
		
		byte[] start = {(byte)0xFF,(byte)0xD8};
		byte[] app = {(byte)0xFF,(byte)0xE0};
		byte[] len = ByteBuffer.allocate(2).putInt(16 + 3 * w * h).array();
		byte[] identifier = {(byte)0x4A,(byte)0x46,(byte)0x49,(byte)0x46,(byte)0x00};
		byte[] version = {(byte)0x01,(byte)0x02};
		return null;
	}
	public void saveChart() {
		
	}
}
