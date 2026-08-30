package com.mypdf;

import com.mypdf.ui.MergeView;
import com.mypdf.ui.OrganizeView;
import com.mypdf.ui.SplitView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MyPdfApplication extends Application {
    @Override
    public void start(Stage stage) {
        Label title = new Label("MyPDF");
        title.getStyleClass().add("app-title");
        Label description = new Label("내 PC에서 처리하는 PDF 도구");
        description.getStyleClass().add("app-description");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("PDF 병합", new MergeView(stage)),
                new Tab("PDF 분할", new SplitView(stage)),
                new Tab("PDF 정리", new OrganizeView(stage))
        );
        VBox.setVgrow(tabs, javafx.scene.layout.Priority.ALWAYS);

        VBox root = new VBox(12, title, description, tabs);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 780, 560);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.setTitle("MyPDF");
        stage.setMinWidth(680);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
