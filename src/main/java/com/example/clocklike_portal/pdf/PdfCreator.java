package com.example.clocklike_portal.pdf;

import com.example.clocklike_portal.timeoff.PtoEntity;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Component
@RequiredArgsConstructor
public class PdfCreator {
    private final TemplateGenerator templateGenerator;
    public final static String PDF_TEMPLATES = "src/main/resources/pdf_temp/";

    public String generateTimeOffRequestPdf(PtoEntity request) {
        try {
            return generatePdfFromHtml(templateGenerator.parseTimeOffRequestTemplate(request));
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    String generatePdfFromHtml(String htmlTemplate) throws DocumentException, IOException {
        String generatedPdfName = "pdf_" + System.currentTimeMillis() + ".pdf";
        String outputFolder = PDF_TEMPLATES + generatedPdfName;
        try (OutputStream outputStream = new FileOutputStream(outputFolder)) {
            ITextRenderer renderer = new ITextRenderer();
            SharedContext sharedContext = renderer.getSharedContext();
            sharedContext.setInteractive(false);
            renderer.getFontResolver().addFont("fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            renderer.setDocumentFromString(htmlTemplate);
            renderer.layout();
            renderer.createPDF(outputStream);
        }
        return generatedPdfName;
    }


}
