package fastglp;

import fastglp.model.Ciudad;
import fastglp.utils.Estadisticas;
import fastglp.utils.Utils;
import fastglp.utils.pdf.PdfGenerator;
import org.junit.jupiter.api.Test;

public class PdfGeneratorTest {
    @Test
    public void testGeneratePdf() {
        Ciudad ciudad = Utils.createMockCiudad(4,2023);
        Estadisticas estadisticas = new Estadisticas(ciudad, Utils.parseFecha("01/04/2023 00"),
                "Simulación semanal");
        estadisticas.setFechaFin(Utils.parseFecha("08/04/2023 00"));
        String dest = "src/generated/test.pdf";
        try {
            PdfGenerator.generatePdf(estadisticas, dest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
