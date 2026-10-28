package com.mypdf.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import org.apache.pdfbox.util.Matrix;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** One image per A4 page, fitted without cropping or changing its aspect ratio. */
public class ImageToPdfService {
    /** Rotation belongs to this list entry, so even duplicate files can rotate independently. */
    public record ImageSource(File file, int clockwiseDegrees) {
        public ImageSource {
            Objects.requireNonNull(file, "이미지 파일이 필요합니다.");
            if (clockwiseDegrees % 90 != 0) throw new IllegalArgumentException("90도 단위로 회전해주세요.");
            clockwiseDegrees = Math.floorMod(clockwiseDegrees, 360);
        }

        public ImageSource rotate(int degrees) {
            return new ImageSource(file, clockwiseDegrees + degrees);
        }
    }

    public enum Orientation { PORTRAIT, LANDSCAPE }

    public enum Margin {
        NONE(0), NARROW(10), WIDE(20);

        private final float points;

        Margin(float millimeters) {
            points = millimeters * 72 / 25.4f;
        }

        public float points() { return points; }
    }

    public record Options(Orientation orientation, Margin margin) {
        public Options {
            Objects.requireNonNull(orientation, "페이지 방향이 필요합니다.");
            Objects.requireNonNull(margin, "여백 설정이 필요합니다.");
        }

        public static Options defaults() {
            return new Options(Orientation.PORTRAIT, Margin.NONE);
        }
    }

    public static boolean isSupported(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return file.isFile() && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"));
    }

    public void merge(List<File> images, File destination, Options options) throws IOException {
        validate(images, options);
        mergeImages(images.stream().map(file -> new ImageSource(file, 0)).toList(), destination, options);
    }

    public void mergeImages(List<ImageSource> images, File destination, Options options) throws IOException {
        validateSources(images, options);
        for (ImageSource image : images) {
            if (image.file().getCanonicalFile().equals(destination.getCanonicalFile())) {
                throw new IllegalArgumentException("원본 이미지와 다른 파일로 저장해주세요.");
            }
        }
        write(images, destination.toPath(), options, true);
    }

    /** Existing PDFs are preserved; duplicate base names receive numbered suffixes. */
    public List<File> saveSeparately(List<File> images, File directory, Options options) throws IOException {
        validate(images, options);
        return saveImagesSeparately(images.stream().map(file -> new ImageSource(file, 0)).toList(), directory, options);
    }

    public List<File> saveImagesSeparately(List<ImageSource> images, File directory, Options options) throws IOException {
        validateSources(images, options);
        if (!directory.isDirectory()) throw new IOException("저장 폴더를 찾을 수 없습니다.");
        List<File> created = new ArrayList<>();
        try {
            for (ImageSource image : images) {
                String name = image.file().getName();
                String base = name.substring(0, name.lastIndexOf('.'));
                Path target = directory.toPath().resolve(base + ".pdf");
                int suffix = 2;
                while (Files.exists(target)) {
                    target = directory.toPath().resolve(base + "_" + suffix++ + ".pdf");
                }
                write(List.of(image), target, options, false);
                created.add(target.toFile());
            }
            return List.copyOf(created);
        } catch (IOException | RuntimeException error) {
            for (File file : created) {
                try { Files.deleteIfExists(file.toPath()); }
                catch (IOException cleanup) { error.addSuppressed(cleanup); }
            }
            throw error;
        }
    }

    private void validate(List<File> images, Options options) {
        if (images == null || images.isEmpty()) throw new IllegalArgumentException("변환할 이미지를 추가해주세요.");
        Objects.requireNonNull(options, "변환 옵션이 필요합니다.");
        for (File image : images) {
            if (image == null || !isSupported(image)) {
                throw new IllegalArgumentException("JPG 또는 PNG 파일만 변환할 수 있습니다: " + image);
            }
        }
    }

    private void validateSources(List<ImageSource> images, Options options) {
        if (images == null || images.isEmpty()) throw new IllegalArgumentException("변환할 이미지를 추가해주세요.");
        if (images.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        validate(images.stream().map(ImageSource::file).toList(), options);
    }

    private void write(List<ImageSource> images, Path destination, Options options, boolean replace) throws IOException {
        Path target = destination.toAbsolutePath();
        // Decode and save completely before replacing an existing destination.
        Path temporary = Files.createTempFile(target.getParent(), ".mypdf-", ".pdf");
        try {
            try (PDDocument document = new PDDocument()) {
                for (ImageSource image : images) addPage(document, image, options);
                document.save(temporary.toFile());
            }
            if (replace) Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            else Files.move(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void addPage(PDDocument document, ImageSource source, Options options) throws IOException {
        File file = source.file();
        try {
            PDRectangle size = options.orientation() == Orientation.PORTRAIT
                    ? PDRectangle.A4 : new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            PDPage page = new PDPage(size);
            PDImageXObject image = PDImageXObject.createFromFileByContent(file, document);
            float margin = options.margin().points();
            boolean swapped = source.clockwiseDegrees() % 180 != 0;
            float effectiveWidth = swapped ? image.getHeight() : image.getWidth();
            float effectiveHeight = swapped ? image.getWidth() : image.getHeight();
            float scale = Math.min((size.getWidth() - margin * 2) / effectiveWidth,
                    (size.getHeight() - margin * 2) / effectiveHeight);
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                AffineTransform transform = new AffineTransform();
                transform.translate(size.getWidth() / 2, size.getHeight() / 2);
                // PDF coordinates grow upward, so clockwise is a negative angle.
                transform.rotate(Math.toRadians(-source.clockwiseDegrees()));
                transform.scale(width, height);
                transform.translate(-0.5, -0.5);
                content.drawImage(image, new Matrix(transform));
            }
        } catch (IOException | RuntimeException error) {
            throw new IOException("이미지를 변환할 수 없습니다: " + file.getName(), error);
        }
    }
}
