package com.mypdf.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfSplitService {
    public int getPageCount(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            return document.getNumberOfPages();
        }
    }

    public void extractPages(File source, List<Integer> oneBasedPages, File destination) throws IOException {
        if (oneBasedPages == null || oneBasedPages.isEmpty()) {
            throw new IllegalArgumentException("저장할 페이지가 없습니다.");
        }
        try (PDDocument input = Loader.loadPDF(source); PDDocument output = new PDDocument()) {
            int pageCount = input.getNumberOfPages();
            for (int pageNumber : oneBasedPages) {
                if (pageNumber < 1 || pageNumber > pageCount) {
                    throw new IllegalArgumentException("페이지 번호가 문서 범위를 벗어났습니다: " + pageNumber);
                }
                output.importPage(input.getPage(pageNumber - 1)); // 사용자 페이지 번호(1부터)를 PDFBox 인덱스(0부터)로 변환
            }
            output.save(destination);
        }
    }
}
