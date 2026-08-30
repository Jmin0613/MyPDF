package com.mypdf.ui;

import java.io.File;

record PdfFileItem(File file, int pageCount) {
    String fileName() {
        return file.getName();
    }
}
