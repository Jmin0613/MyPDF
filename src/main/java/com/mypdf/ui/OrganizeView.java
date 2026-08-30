package com.mypdf.ui;

import com.mypdf.service.PdfOrganizeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OrganizeView extends BorderPane {
    private static final DataFormat PAGE_INDEX = new DataFormat("application/x-mypdf-page-index");

    private final Window owner;
    private final PdfOrganizeService service = new PdfOrganizeService();
    private final ObservableList<PdfPageItem> pages = FXCollections.observableArrayList();
    private final List<PdfPageItem> originalPages = new ArrayList<>();
    private final Label fileInfo = new Label("선택된 PDF가 없습니다.");
    private final Label status = new Label();
    private final CheckBox selectAll = new CheckBox("모두 선택");
    private final Button reset = new Button("초기화");
    private final Button save = new Button("PDF 저장");
    private final FlowPane pagePane = new FlowPane(12, 12);
    private File source;
    private Task<List<PdfOrganizeService.RenderedPage>> renderTask;
    private boolean updatingSelectAll;

    public OrganizeView(Window owner) {
        this.owner = owner;
        setPadding(new Insets(18, 0, 0, 0));

        Button choose = new Button("PDF 선택");
        choose.setOnAction(event -> chooseFile());
        selectAll.setOnAction(event -> applySelectAll());
        reset.setOnAction(event -> reset());
        save.setOnAction(event -> save());
        save.getStyleClass().add("primary-button");

        HBox fileRow = new HBox(12, choose, fileInfo);
        fileRow.setAlignment(Pos.CENTER_LEFT);
        HBox actions = new HBox(8, selectAll, reset, save);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox top = new VBox(12, fileRow, actions, status);

        pagePane.setPadding(new Insets(12));
        ScrollPane scrollPane = new ScrollPane(pagePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        pagePane.prefWrapLengthProperty().bind(scrollPane.widthProperty().subtract(30));
        scrollPane.getStyleClass().add("organize-scroll");
        BorderPane.setMargin(scrollPane, new Insets(12, 0, 0, 0));

        setTop(top);
        setCenter(scrollPane);
        setNoDocumentControls();
    }

    private void chooseFile() {
        File selected = MergeView.pdfChooser("정리할 PDF 선택").showOpenDialog(owner);
        if (selected == null) return;
        if (!MergeView.isPdf(selected)) {
            Alerts.error(owner, "PDF 파일만 선택할 수 있습니다.");
            return;
        }
        startRendering(selected);
    }

    private void startRendering(File selected) {
        cancelRendering();
        source = null;
        pages.clear();
        originalPages.clear();
        pagePane.getChildren().clear();
        fileInfo.setText(selected.getName());
        status.setText("페이지를 불러오는 중...");
        setControlsAvailable(false);

        Task<List<PdfOrganizeService.RenderedPage>> task = new Task<>() {
            @Override
            protected List<PdfOrganizeService.RenderedPage> call() throws IOException {
                return service.renderThumbnails(selected,
                        completed -> updateMessage("페이지를 불러오는 중... " + completed + "페이지"),
                        this::isCancelled);
            }
        };
        renderTask = task;
        status.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(event -> {
            status.textProperty().unbind();
            if (task.isCancelled()) return;
            source = selected;
            for (PdfOrganizeService.RenderedPage rendered : task.getValue()) {
                PdfPageItem item = new PdfPageItem(rendered.originalPageIndex(),
                        SwingFXUtils.toFXImage(rendered.thumbnail(), null));
                item.selectedProperty().addListener((observable, oldValue, newValue) -> updateSelectAllState());
                pages.add(item);
                originalPages.add(item);
            }
            rebuildPagePane();
            fileInfo.setText(selected.getName() + "  ·  전체 " + pages.size() + "페이지");
            status.setText("");
            setControlsAvailable(true);
        });
        task.setOnFailed(event -> {
            status.textProperty().unbind();
            source = null;
            pages.clear();
            originalPages.clear();
            pagePane.getChildren().clear();
            fileInfo.setText("선택된 PDF가 없습니다.");
            status.setText("");
            setNoDocumentControls();
            Throwable error = task.getException();
            Alerts.error(owner, "PDF 페이지를 불러오지 못했습니다.\n" + safeThrowableMessage(error));
        });

        Thread thread = new Thread(task, "mypdf-thumbnail-renderer");
        thread.setDaemon(true);
        thread.start();
    }

    private void cancelRendering() {
        if (renderTask != null && renderTask.isRunning()) renderTask.cancel();
        if (status.textProperty().isBound()) status.textProperty().unbind();
    }

    private void rebuildPagePane() {
        pagePane.getChildren().clear();
        for (int index = 0; index < pages.size(); index++) {
            PdfPageItem item = pages.get(index);
            item.setDisplayOrder(index + 1);
            pagePane.getChildren().add(createPageNode(item));
        }
        updateSelectAllState();
    }

    private Node createPageNode(PdfPageItem item) {
        CheckBox selected = new CheckBox();
        selected.selectedProperty().bindBidirectional(item.selectedProperty());
        Button delete = new Button("삭제");
        delete.getStyleClass().add("page-delete-button");
        delete.setOnAction(event -> deletePage(item));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox pageActions = new HBox(6, selected, spacer, delete);
        pageActions.setAlignment(Pos.CENTER_LEFT);

        ImageView thumbnail = new ImageView(item.thumbnail());
        thumbnail.setFitWidth(135);
        thumbnail.setFitHeight(185);
        thumbnail.setPreserveRatio(true);
        thumbnail.setSmooth(true);

        Label number = new Label();
        number.textProperty().bind(item.displayOrderProperty().asString());
        VBox node = new VBox(6, pageActions, thumbnail, number);
        node.setAlignment(Pos.TOP_CENTER);
        node.getStyleClass().add("pdf-page-item");
        configureDragAndDrop(node, item);
        return node;
    }

    private void configureDragAndDrop(VBox node, PdfPageItem item) {
        node.setOnDragDetected(event -> {
            if (event.getTarget() instanceof CheckBox || event.getTarget() instanceof Button) return;
            ClipboardContent content = new ClipboardContent();
            content.put(PAGE_INDEX, Integer.toString(item.originalPageIndex()));
            node.startDragAndDrop(TransferMode.MOVE).setContent(content);
            event.consume();
        });
        node.setOnDragOver(event -> {
            if (event.getDragboard().hasContent(PAGE_INDEX)
                    && !Integer.toString(item.originalPageIndex()).equals(event.getDragboard().getContent(PAGE_INDEX))) {
                event.acceptTransferModes(TransferMode.MOVE);
                showDropPosition(node, event.getX() >= node.getWidth() / 2);
            }
            event.consume();
        });
        node.setOnDragExited(event -> clearDropPosition(node));
        node.setOnDragDropped(event -> {
            clearDropPosition(node);
            Object content = event.getDragboard().getContent(PAGE_INDEX);
            if (content == null) return;
            int originalIndex = Integer.parseInt(content.toString());
            PdfPageItem dragged = pages.stream()
                    .filter(page -> page.originalPageIndex() == originalIndex).findFirst().orElse(null);
            if (dragged == null || dragged == item) return;
            int sourceIndex = pages.indexOf(dragged);
            int insertionIndex = pages.indexOf(item) + (event.getX() >= node.getWidth() / 2 ? 1 : 0);
            pages.remove(sourceIndex);
            if (sourceIndex < insertionIndex) insertionIndex--;
            pages.add(Math.max(0, Math.min(insertionIndex, pages.size())), dragged);
            rebuildPagePane();
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void showDropPosition(Node node, boolean after) {
        clearDropPosition(node);
        node.getStyleClass().add(after ? "drop-after" : "drop-before");
    }

    private void clearDropPosition(Node node) {
        node.getStyleClass().removeAll("drop-before", "drop-after");
    }

    private void applySelectAll() {
        if (updatingSelectAll) return;
        boolean selected = selectAll.isIndeterminate() || selectAll.isSelected();
        pages.forEach(page -> page.setSelected(selected));
        updateSelectAllState();
    }

    private void updateSelectAllState() {
        updatingSelectAll = true;
        long selectedCount = pages.stream().filter(PdfPageItem::isSelected).count();
        selectAll.setIndeterminate(selectedCount > 0 && selectedCount < pages.size());
        selectAll.setSelected(!pages.isEmpty() && selectedCount == pages.size());
        updatingSelectAll = false;
    }

    private void deletePage(PdfPageItem item) {
        if (pages.size() <= 1) {
            Alerts.error(owner, "PDF에는 최소 1페이지가 남아 있어야 합니다.");
            return;
        }
        pages.remove(item);
        rebuildPagePane();
    }

    private void reset() {
        if (source == null) {
            Alerts.error(owner, "초기화할 PDF를 먼저 선택해주세요.");
            return;
        }
        pages.setAll(originalPages);
        pages.forEach(page -> page.setSelected(true));
        rebuildPagePane();
    }

    private void save() {
        if (source == null) {
            Alerts.error(owner, "저장할 PDF를 먼저 선택해주세요.");
            return;
        }
        List<Integer> indexes = pages.stream()
                .filter(PdfPageItem::isSelected)
                .map(PdfPageItem::originalPageIndex)
                .toList();
        if (indexes.isEmpty()) {
            Alerts.info(owner, "안내", "저장할 페이지를 선택해주세요.");
            return;
        }
        FileChooser chooser = MergeView.pdfChooser("정리한 PDF 저장");
        chooser.setInitialFileName(baseName(source.getName()) + "_organized.pdf");
        File destination = chooser.showSaveDialog(owner);
        if (destination == null) return;
        if (MergeView.sameFile(source, destination)) {
            Alerts.error(owner, "원본 PDF와 다른 위치 또는 파일명으로 저장해주세요.");
            return;
        }
        try {
            service.save(source, indexes, destination);
            Alerts.info(owner, "완료", "정리한 PDF를 저장했습니다.");
        } catch (IOException | RuntimeException e) {
            Alerts.error(owner, "PDF 저장 중 오류가 발생했습니다.\n" + MergeView.safeMessage(e));
        }
    }

    private void setControlsAvailable(boolean available) {
        selectAll.setDisable(!available);
        reset.setDisable(!available);
        save.setDisable(!available);
    }

    private void setNoDocumentControls() {
        selectAll.setDisable(true);
        reset.setDisable(false);
        save.setDisable(false);
    }

    private static String baseName(String name) {
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
                ? name.substring(0, name.length() - 4) : name;
    }

    private static String safeThrowableMessage(Throwable error) {
        if (error == null) return "알 수 없는 오류";
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
