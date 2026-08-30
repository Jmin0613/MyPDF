package com.mypdf.service;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfMergeService {
    public void merge(List<File> sourceFiles, File destination) throws IOException {
        if (sourceFiles == null || sourceFiles.isEmpty()) {
            throw new IllegalArgumentException("병합할 PDF가 없습니다.");
        }
        PDFMergerUtility merger = new PDFMergerUtility();
        for (File sourceFile : sourceFiles) {
            merger.addSource(sourceFile);
        }
        merger.setDestinationFileName(destination.getAbsolutePath());
        merger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
    }
}
