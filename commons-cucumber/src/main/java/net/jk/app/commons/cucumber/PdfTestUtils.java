package net.jk.app.commons.cucumber;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.tools.PDFText2HTML;

public class PdfTestUtils {
  private PdfTestUtils() {}

  /**
   * Returns basic HTML of the PDF document downloaded from the given URL This html will not be the
   * actual representation of the document. Can only be used to verify that important text fields
   * exist in the document.
   *
   * @param path URL for the pdf document download
   */
  public static String getPdfAsHTML(String path) throws IOException {
    URL url = new URL(path);
    BufferedInputStream file = new BufferedInputStream(url.openStream());
    PDDocument pdf = PDDocument.load(file);
    PDFText2HTML pdfText2Html = new PDFText2HTML();
    pdfText2Html.setWordSeparator(""); // Workaround to avoid spacing issue for custom fonts
    return pdfText2Html.getText(pdf).replaceAll("(\r\n|\n)", "\r\n");
  }
}
