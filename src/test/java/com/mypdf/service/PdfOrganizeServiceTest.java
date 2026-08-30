package com.mypdf.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfOrganizeServiceTest {
    @TempDir Path tempDir;

    @Test
    void savesOnlySelectedPagesInCurrentDisplayOrder() throws Exception {
        File source = createNumberedPdf();
        File result = tempDir.resolve("organized.pdf").toFile();

        // 화면 순서 0, 3, 1, 5 중 선택된 항목을 화면 순서대로 필터링한 결과
        new PdfOrganizeService().save(source, List.of(0, 1, 5), result);

        try (PDDocument document = Loader.loadPDF(result)) {
            assertEquals(3, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
            assertEquals("PAGE-1 PAGE-2 PAGE-6", text);
        }
    }

    private File createNumberedPdf() throws Exception {
        File file = tempDir.resolve("source.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int pageNumber = 1; pageNumber <= 6; pageNumber++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(72, 720);
                    content.showText("PAGE-" + pageNumber);
                    content.endText();
                }
            }
            document.save(file);
        }
        return file;
    }
}
