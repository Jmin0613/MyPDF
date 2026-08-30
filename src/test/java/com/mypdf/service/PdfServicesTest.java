package com.mypdf.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfServicesTest {
    @TempDir Path tempDir;

    @Test
    void mergesAndExtractsPages() throws Exception {
        File first = createPdf("first.pdf", 2);
        File second = createPdf("second.pdf", 3);
        File merged = tempDir.resolve("merged.pdf").toFile();
        new PdfMergeService().merge(List.of(first, second), merged);
        assertEquals(5, count(merged));

        File extracted = tempDir.resolve("extracted.pdf").toFile();
        new PdfSplitService().extractPages(merged, List.of(5, 1, 3), extracted);
        assertEquals(3, count(extracted));
    }

    private File createPdf(String name, int pages) throws Exception {
        File file = tempDir.resolve(name).toFile();
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pages; i++) document.addPage(new PDPage());
            document.save(file);
        }
        return file;
    }

    private int count(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            return document.getNumberOfPages();
        }
    }
}
