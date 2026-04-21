package util;

import model.Major;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class PdfReportUtil {
    private PdfReportUtil() {
    }

    public static void writeMajorSummary(File file, String appName, Map<Major, Integer> majorCounts) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                stream.newLineAtOffset(50, 760);
                stream.showText(appName + " - Major Enrollment Report");
                stream.endText();

                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 735);
                stream.showText("Generated: " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                stream.endText();

                int y = 700;
                for (Major major : Major.values()) {
                    stream.beginText();
                    stream.setFont(PDType1Font.HELVETICA, 14);
                    stream.newLineAtOffset(50, y);
                    int count = majorCounts.getOrDefault(major, 0);
                    stream.showText(major.getDisplayName() + ": " + count);
                    stream.endText();
                    y -= 25;
                }
            }

            document.save(file);
        }
    }
}
