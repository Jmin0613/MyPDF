package com.mypdf.ui;

import com.mypdf.service.PdfMergeService;
import com.mypdf.service.PdfSplitService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MergeView extends BorderPane {
    private final Window owner;
    private final ObservableList<PdfFileItem> items = FXCollections.observableArrayList();
    private final TableView<PdfFileItem> table = new TableView<>(items);
    private final PdfMergeService mergeService = new PdfMergeService();
    private final PdfSplitService documentService = new PdfSplitService();

    public MergeView(Window owner) {
        this.owner = owner;
        setPadding(new Insets(18, 0, 0, 0));

        Label help = new Label("위에서 아래 순서대로 PDF를 병합합니다.");
        Button add = new Button("PDF 추가");
        add.setOnAction(event -> addFiles());
        HBox header = new HBox(12, help, add);
        header.setAlignment(Pos.CENTER_LEFT);

        TableColumn<PdfFileItem, String> order = new TableColumn<>("순서");
        order.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                Integer.toString(table.getItems().indexOf(data.getValue()) + 1)));
        order.setPrefWidth(60);
        TableColumn<PdfFileItem, String> name = new TableColumn<>("파일명");
        name.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().fileName()));
        name.setPrefWidth(400);
        TableColumn<PdfFileItem, Integer> pages = new TableColumn<>("페이지 수");
        pages.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().pageCount()).asObject());
        pages.setPrefWidth(100);
        TableColumn<PdfFileItem, Void> actionColumn = new TableColumn<>("제거");
        actionColumn.setPrefWidth(80);
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button remove = new Button("제거");
            {
                remove.setOnAction(event -> {
                    items.remove(getTableRow().getItem());
                    table.refresh();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : remove);
            }
        });
        table.getColumns().add(order);
        table.getColumns().add(name);
        table.getColumns().add(pages);
        table.getColumns().add(actionColumn);
        table.setPlaceholder(new Label("추가된 PDF가 없습니다."));

        Button up = new Button("위로 이동");
        up.setOnAction(event -> move(-1));
        Button down = new Button("아래로 이동");
        down.setOnAction(event -> move(1));
        HBox controls = new HBox(8, up, down);

        Button merge = new Button("PDF 병합");
        merge.getStyleClass().add("primary-button");
        merge.setOnAction(event -> merge());
        HBox action = new HBox(merge);
        action.setAlignment(Pos.CENTER_RIGHT);
        VBox bottom = new VBox(12, controls, action);
        bottom.setPadding(new Insets(12, 0, 0, 0));

        setTop(header);
        BorderPane.setMargin(table, new Insets(12, 0, 0, 0));
        setCenter(table);
        setBottom(bottom);
    }

    private void addFiles() {
        FileChooser chooser = pdfChooser("병합할 PDF 선택");
        List<File> files = chooser.showOpenMultipleDialog(owner);
        if (files == null) return;
        for (File file : files) {
            if (!isPdf(file)) {
                Alerts.error(owner, "PDF 파일만 선택할 수 있습니다: " + file.getName());
                continue;
            }
            try {
                items.add(new PdfFileItem(file, documentService.getPageCount(file)));
            } catch (IOException | RuntimeException e) {
                Alerts.error(owner, "PDF를 열 수 없습니다: " + file.getName());
            }
        }
        table.refresh();
    }

    private void move(int offset) {
        int selected = table.getSelectionModel().getSelectedIndex();
        int target = selected + offset;
        if (selected < 0 || target < 0 || target >= items.size()) return;
        PdfFileItem item = items.remove(selected);
        items.add(target, item);
        table.getSelectionModel().select(target);
        table.refresh();
    }

    private void merge() {
        if (items.isEmpty()) {
            Alerts.error(owner, "병합할 PDF를 먼저 추가해주세요.");
            return;
        }
        FileChooser chooser = pdfChooser("병합 PDF 저장");
        chooser.setInitialFileName("merged.pdf");
        File destination = chooser.showSaveDialog(owner);
        if (destination == null) return;
        if (items.stream().anyMatch(item -> sameFile(item.file(), destination))) {
            Alerts.error(owner, "원본 PDF와 다른 위치 또는 파일명으로 저장해주세요.");
            return;
        }
        try {
            mergeService.merge(items.stream().map(PdfFileItem::file).toList(), destination);
            Alerts.info(owner, "완료", "PDF 병합이 완료되었습니다.");
        } catch (IOException | RuntimeException e) {
            Alerts.error(owner, "PDF 병합 중 오류가 발생했습니다.\n" + safeMessage(e));
        }
    }

    static FileChooser pdfChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF 파일 (*.pdf)", "*.pdf"));
        return chooser;
    }

    static boolean isPdf(File file) {
        return file.isFile() && file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf");
    }

    static String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    static boolean sameFile(File first, File second) {
        try {
            return first.getCanonicalFile().equals(second.getCanonicalFile());
        } catch (IOException e) {
            return first.getAbsoluteFile().equals(second.getAbsoluteFile());
        }
    }
}
