package com.mypdf.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

public class PdfOrganizeService {
    private static final float THUMBNAIL_SCALE = 0.28f;

    public List<RenderedPage> renderThumbnails(File source, IntConsumer progress,
                                                BooleanSupplier cancelled) throws IOException {
        List<RenderedPage> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(source)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            if (pageCount < 1) {
                throw new IOException("페이지가 없는 PDF입니다.");
            }
            for (int index = 0; index < pageCount; index++) {
                if (cancelled.getAsBoolean()) {
                    return List.of();
                }
                BufferedImage thumbnail = renderer.renderImage(index, THUMBNAIL_SCALE, ImageType.RGB);
                pages.add(new RenderedPage(index, thumbnail));
                progress.accept(index + 1);
            }
        }
        return List.copyOf(pages);
    }

    public void save(File source, List<Integer> originalPageIndexes, File destination) throws IOException {
        if (originalPageIndexes == null || originalPageIndexes.isEmpty()) {
            throw new IllegalArgumentException("PDF에는 최소 1페이지가 남아 있어야 합니다.");
        }
        try (PDDocument input = Loader.loadPDF(source); PDDocument output = new PDDocument()) {
            int pageCount = input.getNumberOfPages();
            for (int originalIndex : originalPageIndexes) {
                if (originalIndex < 0 || originalIndex >= pageCount) {
                    throw new IllegalArgumentException("원본 페이지 인덱스가 범위를 벗어났습니다: " + originalIndex);
                }
                output.importPage(input.getPage(originalIndex));
            }
            output.save(destination);
        }
    }

    public record RenderedPage(int originalPageIndex, BufferedImage thumbnail) {
    }
}
