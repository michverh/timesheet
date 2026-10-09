package eu.openvalue.timesheet.adapter.out.reading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class PdfPagesTest {

  @Test
  void rendersEveryPageAsAPngImageAtTheGivenResolution() throws IOException {
    // Given: an A4 page in landscape, 11.69 by 8.27 inches
    var pdf = pdfWithPages(1);

    // When
    var pages = PdfPages.asPngImages(pdf, 100);

    // Then
    var image = ImageIO.read(new ByteArrayInputStream(pages.getFirst()));
    assertThat(pages).hasSize(1);
    assertThat(image.getWidth()).isEqualTo(1169);
    assertThat(image.getHeight()).isEqualTo(826);
  }

  @Test
  void rendersNoMoreThanTheFirstFourPages() throws IOException {
    assertThat(PdfPages.asPngImages(pdfWithPages(6), 50)).hasSize(4);
  }

  @Test
  void refusesSomethingThatIsNotAPdfDocument() {
    assertThatThrownBy(() -> PdfPages.asPngImages(new byte[] {1, 2, 3}, 100)).isInstanceOf(IOException.class);
  }

  static byte[] pdfWithPages(int numberOfPages) throws IOException {
    try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
      for (int page = 0; page < numberOfPages; page++) {
        var a4 = PDRectangle.A4;
        document.addPage(new PDPage(new PDRectangle(a4.getHeight(), a4.getWidth())));
      }
      document.save(output);
      return output.toByteArray();
    }
  }
}
