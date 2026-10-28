package com.mypdf;

import com.mypdf.ui.MergeView;
import com.mypdf.ui.ImageToPdfView;
import com.mypdf.ui.OrganizeView;
import com.mypdf.ui.SplitView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;

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
                new Tab("PDF 정리", new OrganizeView(stage)),
                new Tab("이미지 → PDF", new ImageToPdfView(stage))
        );
        VBox.setVgrow(tabs, javafx.scene.layout.Priority.ALWAYS);

        VBox root = new VBox(12, title, description, tabs);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 780, 560);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.setTitle("MyPDF");
        applyApplicationIcon(stage);
        stage.setMinWidth(680);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    private void applyApplicationIcon(Stage stage) {
        URL iconResource = getClass().getResource("/images/mypdf-icon.png");
        if (iconResource == null) return;

        try {
            Image icon = new Image(iconResource.toExternalForm());
            if (!icon.isError()) stage.getIcons().add(icon);
        } catch (RuntimeException ignored) {
            // 아이콘을 불러오지 못해도 애플리케이션은 정상적으로 실행한다.
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
