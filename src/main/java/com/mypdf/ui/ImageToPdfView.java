package com.mypdf.ui;

import com.mypdf.service.ImageToPdfService;
import com.mypdf.service.ImageToPdfService.Margin;
import com.mypdf.service.ImageToPdfService.ImageSource;
import com.mypdf.service.ImageToPdfService.Options;
import com.mypdf.service.ImageToPdfService.Orientation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ImageToPdfView extends BorderPane {
    private final Window owner;
    private final ImageToPdfService service = new ImageToPdfService();
    private final ObservableList<ImageSource> images = FXCollections.observableArrayList();
    private final ListView<ImageSource> list = new ListView<>(images);
    private final RadioButton portrait = new RadioButton("세로");
    private final ComboBox<String> margin = new ComboBox<>();
    private final CheckBox merge = new CheckBox("모든 이미지를 하나의 PDF로 병합");
    private final Label status = new Label();

    public ImageToPdfView(Window owner) {
        this.owner = owner;
        setPadding(new Insets(18, 0, 0, 0));

        Button add = new Button("이미지 추가");
        add.setOnAction(event -> addImages());
        Label hint = new Label("위에서 아래 순서로 한 장씩 PDF 페이지가 됩니다.");
        hint.setWrapText(true);
        VBox top = new VBox(8, new HBox(12, add, status), hint);
        top.setPadding(new Insets(0, 0, 12, 0));
        setTop(top);

        list.setPlaceholder(new Label("JPG·PNG 이미지를 추가해주세요."));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(ImageSource item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);
                if (empty || item == null) return;
                File file = item.file();

                double width = portrait.isSelected() ? 78 : 110;
                double height = portrait.isSelected() ? 110 : 78;
                double inset = margin.getSelectionModel().getSelectedIndex() * 10 * width
                        / (portrait.isSelected() ? 210 : 297);
                ImageView preview = new ImageView(new Image(file.toURI().toString(), 156, 220, true, true, true));
                preview.setPreserveRatio(true);
                boolean swapped = item.clockwiseDegrees() % 180 != 0;
                preview.setFitWidth((swapped ? height : width) - inset * 2);
                preview.setFitHeight((swapped ? width : height) - inset * 2);
                preview.setRotate(item.clockwiseDegrees());
                StackPane paper = new StackPane(new Group(preview));
                paper.setCursor(Cursor.HAND);
                paper.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) showPreview(item);
                    event.consume();
                });
                paper.setMinSize(width, height);
                paper.setMaxSize(width, height);
                paper.setPrefSize(width, height);
                paper.setStyle("-fx-background-color: white; -fx-border-color: #bdbdbd;");
                Label name = new Label((getIndex() + 1) + ". " + file.getName());
                name.setWrapText(true);
                name.setMaxWidth(230);
                name.setCursor(Cursor.HAND);
                name.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) showPreview(item);
                    event.consume();
                });
                MenuButton menu = new MenuButton("회전");
                menu.setAccessibleText("이미지 옵션: " + file.getName());
                MenuItem left = new MenuItem("왼쪽으로 90° 회전");
                left.setOnAction(event -> rotate(item, -90));
                MenuItem right = new MenuItem("오른쪽으로 90° 회전");
                right.setOnAction(event -> rotate(item, 90));
                menu.getItems().addAll(left, right);
                BorderPane card = new BorderPane();
                card.setLeft(paper);
                card.setCenter(name);
                BorderPane.setMargin(name, new Insets(0, 12, 0, 12));
                BorderPane.setAlignment(name, Pos.CENTER_LEFT);
                card.setRight(menu);
                BorderPane.setAlignment(menu, Pos.TOP_RIGHT);
                card.setPadding(new Insets(6));
                card.prefWidthProperty().bind(list.widthProperty().subtract(40));
                setGraphic(card);
            }
        });
        setCenter(list);

        ToggleGroup direction = new ToggleGroup();
        RadioButton landscape = new RadioButton("가로");
        portrait.setToggleGroup(direction);
        landscape.setToggleGroup(direction);
        portrait.setSelected(true);
        direction.selectedToggleProperty().addListener((obs, oldValue, newValue) -> list.refresh());

        margin.getItems().addAll("여백 없음", "좁게 (10 mm)", "넓게 (20 mm)");
        margin.getSelectionModel().selectFirst();
        margin.valueProperty().addListener((obs, oldValue, newValue) -> list.refresh());
        margin.setMaxWidth(Double.MAX_VALUE);
        merge.setSelected(true);
        merge.setWrapText(true);
        Label explanation = new Label("A4 · 이미지 비율 유지\n체크 해제 시 이미지마다 PDF를 저장합니다.");
        explanation.getStyleClass().add("hint");
        explanation.setWrapText(true);
        VBox options = new VBox(14, new Label("페이지 방향"), new HBox(14, portrait, landscape),
                new Label("여백"), margin, merge, explanation);
        options.setPrefWidth(220);
        options.setMinWidth(200);
        options.setPadding(new Insets(8, 0, 0, 16));
        setRight(options);

        Button up = new Button("위로");
        up.setOnAction(event -> move(-1));
        Button down = new Button("아래로");
        down.setOnAction(event -> move(1));
        Button remove = new Button("제거");
        remove.setOnAction(event -> {
            int index = list.getSelectionModel().getSelectedIndex();
            if (index >= 0) images.remove(index);
            list.refresh();
        });
        Button clear = new Button("전체 비우기");
        clear.setOnAction(event -> images.clear());
        Button convert = new Button("PDF로 변환");
        convert.getStyleClass().add("primary-button");
        convert.setOnAction(event -> convert());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(8, up, down, remove, clear, spacer, convert);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(12, 0, 0, 0));
        setBottom(bottom);
    }

    private void addImages() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("PDF로 변환할 이미지 선택");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "이미지 (*.jpg, *.jpeg, *.png)", "*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"));
        List<File> selected = chooser.showOpenMultipleDialog(owner);
        if (selected == null) return;
        for (File file : selected) {
            if (ImageToPdfService.isSupported(file)) images.add(new ImageSource(file, 0));
            else Alerts.error(owner, "JPG 또는 PNG 파일만 추가할 수 있습니다: " + file.getName());
        }
    }

    private void move(int offset) {
        int index = list.getSelectionModel().getSelectedIndex();
        int target = index + offset;
        if (index < 0 || target < 0 || target >= images.size()) return;
        ImageSource file = images.remove(index);
        images.add(target, file);
        list.getSelectionModel().select(target);
        list.scrollTo(target);
        list.refresh();
    }

    private void rotate(ImageSource item, int degrees) {
        // Identity distinguishes duplicate entries that happen to have the same file and rotation.
        for (int index = 0; index < images.size(); index++) {
            if (images.get(index) == item) {
                images.set(index, item.rotate(degrees));
                list.getSelectionModel().select(index);
                return;
            }
        }
    }

    private void showPreview(ImageSource item) {
        for (int index = 0; index < images.size(); index++) {
            if (images.get(index) == item) {
                list.getSelectionModel().select(index);
                break;
            }
        }
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(item.file().getName() + " · 이미지 확인");
        Image image = new Image(item.file().toURI().toString(), 2400, 2400, true, true, true);
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setRotate(item.clockwiseDegrees());
        StackPane canvas = new StackPane(new Group(view));
        canvas.setMinSize(0, 0);
        canvas.setStyle("-fx-background-color: #eeeeee;");
        boolean swapped = item.clockwiseDegrees() % 180 != 0;
        view.fitWidthProperty().bind((swapped ? canvas.heightProperty() : canvas.widthProperty())
                .subtract(32).map(value -> Math.max(1, value.doubleValue())));
        view.fitHeightProperty().bind((swapped ? canvas.widthProperty() : canvas.heightProperty())
                .subtract(32).map(value -> Math.max(1, value.doubleValue())));
        Label loading = new Label("이미지를 불러오는 중...");
        canvas.getChildren().add(loading);
        image.progressProperty().addListener((obs, before, progress) -> {
            if (progress.doubleValue() >= 1) loading.setVisible(false);
        });
        image.errorProperty().addListener((obs, before, error) -> {
            if (error) {
                loading.setText("이미지를 열 수 없습니다.");
                loading.setVisible(true);
            }
        });
        if (image.isError()) loading.setText("이미지를 열 수 없습니다.");
        else loading.setVisible(image.getProgress() < 1);
        Button close = new Button("닫기");
        close.setOnAction(event -> dialog.close());
        Label help = new Label("회전 " + item.clockwiseDegrees() + "° · 창 크기에 맞춰 표시합니다.");
        HBox footer = new HBox(16, help, close);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12));
        BorderPane root = new BorderPane(canvas);
        root.setBottom(footer);
        dialog.setScene(new Scene(root, 800, 650));
        dialog.setMinWidth(360);
        dialog.setMinHeight(300);
        dialog.show();
    }

    private void convert() {
        if (images.isEmpty()) {
            Alerts.error(owner, "변환할 이미지를 먼저 추가해주세요.");
            return;
        }
        boolean combined = merge.isSelected();
        File destination;
        if (combined) {
            FileChooser chooser = MergeView.pdfChooser("이미지 PDF 저장");
            chooser.setInitialFileName("images.pdf");
            destination = chooser.showSaveDialog(owner);
            if (destination != null && !destination.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                // Keep the exact chooser destination so its overwrite confirmation stays valid.
                Alerts.error(owner, "파일명 끝에 .pdf를 붙여 저장해주세요.");
                return;
            }
        } else {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("이미지별 PDF 저장 폴더 선택");
            destination = chooser.showDialog(owner);
        }
        if (destination == null) return;
        List<ImageSource> sources = List.copyOf(images);
        Options options = new Options(portrait.isSelected() ? Orientation.PORTRAIT : Orientation.LANDSCAPE,
                Margin.values()[margin.getSelectionModel().getSelectedIndex()]);
        File output = destination;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (combined) service.mergeImages(sources, output, options);
                else service.saveImagesSeparately(sources, output, options);
                return null;
            }
        };
        setDisable(true);
        status.setText("변환 중... " + sources.size() + "장");
        task.setOnSucceeded(event -> {
            setDisable(false);
            status.setText("");
            Alerts.info(owner, "완료", combined ? "이미지 " + sources.size() + "장을 하나의 PDF로 저장했습니다."
                    : "PDF " + sources.size() + "개를 저장했습니다.\n같은 이름의 파일에는 번호를 붙였습니다.");
        });
        task.setOnFailed(event -> {
            setDisable(false);
            status.setText("");
            Alerts.error(owner, "이미지 PDF 변환 중 오류가 발생했습니다.\n" + task.getException().getMessage());
        });
        Thread worker = new Thread(task, "mypdf-image-converter");
        worker.setDaemon(true);
        worker.start();
    }
}
