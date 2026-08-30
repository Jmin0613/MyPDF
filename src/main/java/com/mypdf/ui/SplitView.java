package com.mypdf.ui;

import com.mypdf.service.PdfSplitService;
import com.mypdf.util.PageRangeParser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SplitView extends VBox {
    private final Window owner;
    private final PdfSplitService splitService = new PdfSplitService();
    private final Label selectedFile = new Label("선택된 PDF가 없습니다.");
    private final TextField range = new TextField();
    private File source;
    private int pageCount;

    public SplitView(Window owner) {
        this.owner = owner;
        setPadding(new Insets(18, 0, 0, 0));
        setSpacing(14);

        Button select = new Button("PDF 선택");
        select.setOnAction(event -> selectFile());
        HBox fileRow = new HBox(12, select, selectedFile);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        Label rangeLabel = new Label("추출할 페이지");
        range.setPromptText("예: 1-5,8,10-15");
        range.setMaxWidth(380);
        Label hint = new Label("입력한 순서대로 선택한 페이지를 하나의 PDF로 저장합니다.");
        hint.getStyleClass().add("hint");

        Button save = new Button("선택 페이지 저장");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> save());

        getChildren().addAll(fileRow, rangeLabel, range, hint, save);
    }

    private void selectFile() {
        File file = MergeView.pdfChooser("분할할 PDF 선택").showOpenDialog(owner);
        if (file == null) return;
        if (!MergeView.isPdf(file)) {
            Alerts.error(owner, "PDF 파일만 선택할 수 있습니다.");
            return;
        }
        try {
            int pages = splitService.getPageCount(file);
            source = file;
            pageCount = pages;
            selectedFile.setText(file.getName() + "  ·  전체 " + pages + "페이지");
        } catch (IOException | RuntimeException e) {
            Alerts.error(owner, "PDF를 열 수 없습니다.\n" + MergeView.safeMessage(e));
        }
    }

    private void save() {
        if (source == null) {
            Alerts.error(owner, "분할할 PDF를 먼저 선택해주세요.");
            return;
        }
        final List<Integer> pages;
        try {
            pages = PageRangeParser.parse(range.getText(), pageCount);
        } catch (IllegalArgumentException e) {
            Alerts.error(owner, e.getMessage());
            return;
        }
        FileChooser chooser = MergeView.pdfChooser("선택 페이지 저장");
        chooser.setInitialFileName(baseName(source.getName()) + "_pages.pdf");
        File destination = chooser.showSaveDialog(owner);
        if (destination == null) return;
        if (MergeView.sameFile(source, destination)) {
            Alerts.error(owner, "원본 PDF와 다른 위치 또는 파일명으로 저장해주세요.");
            return;
        }
        try {
            splitService.extractPages(source, pages, destination);
            Alerts.info(owner, "완료", "선택한 페이지를 저장했습니다.");
        } catch (IOException | RuntimeException e) {
            Alerts.error(owner, "PDF 저장 중 오류가 발생했습니다.\n" + MergeView.safeMessage(e));
        }
    }

    private static String baseName(String name) {
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
                ? name.substring(0, name.length() - 4) : name;
    }
}
