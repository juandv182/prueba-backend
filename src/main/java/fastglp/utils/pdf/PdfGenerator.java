package fastglp.utils.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import fastglp.utils.Estadisticas;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

public class PdfGenerator {

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static int globalStatsPage;
    private static int truckStatsPage;
    private static int referencesPage;

    public static void generatePdf(Estadisticas estadisticas, String dest) throws IOException {
        // Inicializa el PDF
        PdfWriter writer = new PdfWriter(dest);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        // Encabezado y Pie de Página
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new HeaderFooterEventHandler());

        // Portada
        createCoverPage(document, estadisticas);

        // Crear la página del índice al principio
        createIndexPage(document);

        // Guarda las páginas de inicio de cada sección
        globalStatsPage = pdf.getNumberOfPages();
        createGlobalStatisticsSection(document, estadisticas);

        truckStatsPage = pdf.getNumberOfPages();
        createTruckStatisticsSection(document, estadisticas);

        referencesPage = pdf.getNumberOfPages();
        createReferencesSection(document);

        // Actualiza la página del índice
        //updateIndexPage(document, pdf);

        // Cierra el documento
        document.close();
    }

    private static void createCoverPage(Document document, Estadisticas estadisticas) throws IOException {
        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        document.add(new Paragraph("Reporte de Ejecución")
                .setFont(bold)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(250));
        String fechaInicio = dateFormat.format(estadisticas.getFechaInicio());
        String fechaFin = dateFormat.format(estadisticas.getFechaFin());
        document.add(new Paragraph(fechaInicio + " - " + fechaFin)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16));
        document.add(new AreaBreak());
    }

    private static void createIndexPage(Document document) {
        document.add(new Paragraph("Índice")
                .setBold()
                .setFontSize(14));
        document.add(new Paragraph("1. Estadísticas Globales - Página ..."));
        document.add(new Paragraph("2. Estadísticas detalladas por Camión - Página ..."));
        document.add(new Paragraph("3. Referencias - Página ..."));
        document.add(new AreaBreak());
    }




    private static void createGlobalStatisticsSection(Document document, Estadisticas estadisticas) {
        document.add(new Paragraph("Estadísticas Globales")
                .setBold()
                .setFontSize(14)
                .setKeepWithNext(true)); // Para evitar orfandad de título

        document.add(new Paragraph("Fecha Inicio: " + dateFormat.format(estadisticas.getFechaInicio())));
        document.add(new Paragraph("Fecha Fin: " + dateFormat.format(estadisticas.getFechaFin())));
        document.add(new Paragraph("Tiempo Total: " + estadisticas.getTiempoTotal() + " ms"));
        document.add(new Paragraph("Total Porciones Pedidos: " + estadisticas.getTotalPorcionPedidos()));
        document.add(new Paragraph("Total Replanificaciones: " + estadisticas.getTotalReplanificaciones()));
        document.add(new Paragraph("Total Km Recorridos: " + estadisticas.getTotalKmRecorridos()));
        document.add(new Paragraph("Total GLP: " + estadisticas.getTotalGLP()));
        document.add(new Paragraph("Total Petróleo: " + estadisticas.getTotalPetroleo()));
        document.add(new Paragraph("\n")); // Espacio antes de la próxima sección
    }

    private static void createTruckStatisticsSection(Document document, Estadisticas estadisticas) {
        document.add(new Paragraph("Estadísticas detalladas por Camión")
                .setBold()
                .setFontSize(14)
                .setKeepWithNext(true));

        Table table = new Table(new float[]{1, 2, 2, 2, 2});
        table.addCell(new Cell().add(new Paragraph("Camión ID")).setBold());
        table.addCell(new Cell().add(new Paragraph("Total Porciones")).setBold());
        table.addCell(new Cell().add(new Paragraph("Total GLP")).setBold());
        table.addCell(new Cell().add(new Paragraph("Km Recorridos")).setBold());
        table.addCell(new Cell().add(new Paragraph("Total Petróleo")).setBold());

        for (Map.Entry<String, Estadisticas.EstadisticasPorCamion> entry : estadisticas.getEstadisticasPorCamion().entrySet()) {
            Estadisticas.EstadisticasPorCamion e = entry.getValue();
            table.addCell(new Cell().add(new Paragraph(e.getCamionId().toString())));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(e.getCamionTotalPorcionPedidos()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(e.getCamionTotalGLP()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(e.getCamionTotalKmRecorridos()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(e.getCamionTotalPetroleo()))));
        }

        document.add(table);
        document.add(new Paragraph("\n")); // Espacio antes de la próxima sección
    }

    private static class HeaderFooterEventHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdf.getPageNumber(page);
            Rectangle pageSize = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);

            // Encabezado
            try {
                pdfCanvas.beginText()
                        .setFontAndSize(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD), 11)
                        .moveText(30, pageSize.getTop() - 30)
                        .showText("Reporte de Ejecución Semanal")
                        .endText();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Pie de página
            try {
                pdfCanvas.beginText()
                        .setFontAndSize(PdfFontFactory.createFont(StandardFonts.HELVETICA), 12)
                        .moveText(pageSize.getWidth() / 2, pageSize.getBottom() + 20)
                        .showText("Página " + pageNumber)
                        .endText();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private static void createReferencesSection(Document document) {
        document.add(new AreaBreak());//salto de pagina
        document.add(new Paragraph("Referencias")
                .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(14));
        document.add(new Paragraph("FastGLPBackend. (2023). Recuperado de https://github.com/Javier843/FastGLP-BackEnd")
                .setFirstLineIndent(30)
                .setFontSize(12));
    }
    private static void updateIndexPage(Document document, PdfDocument pdf) {
        // Suponiendo que las páginas se han añadido en el orden correcto,
        // reemplaza los marcadores de posición con números de página reales.
        int globalStatsPage = 3; // Ajusta según sea necesario
        int truckStatsPage = 4; // Ajusta según sea necesario
        int referencesPage = pdf.getNumberOfPages(); // Última página para referencias

        // Reemplaza el contenido de la página del índice
        pdf.getPage(2).flush(true); // Limpia el contenido existente
        PdfCanvas canvas = new PdfCanvas(pdf.getPage(2));
        canvas.beginText();
        // Añade el contenido actualizado del índice
        canvas.endText();
    }
}
