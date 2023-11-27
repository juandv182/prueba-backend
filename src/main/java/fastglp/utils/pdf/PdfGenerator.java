package fastglp.utils.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import fastglp.model.Camion;
import fastglp.model.Ciudad;
import fastglp.utils.Estadisticas;
import fastglp.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class PdfGenerator {

    private static DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static final Map<String,Integer> indexMap = new LinkedHashMap<>();
    private static final String logoPath = "src/main/resources/static/images/logo.png";
    private static final String fondoPath = "src/main/resources/static/images/fondo.png";
    private static final String calibriPath = "src/main/resources/static/fonts/Calibri.ttf";
    private static final String montserratPath = "src/main/resources/static/fonts/Montserrat-Light.otf";
    private static int tituloIndex = 0;
    private static int subtituloIndex = 0;
    private static PdfDocument pdf;

    public static byte[] generatePdf(Estadisticas estadisticas, String dest) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Validar rutas
        validateFilePath(dest);

        // Inicializa el PDF
        PdfWriter writer = new PdfWriter(baos);
        pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        HeaderFooterEventHandler handler = new HeaderFooterEventHandler();
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, handler);

        // Portada
        createCoverPage(pdf, document, estadisticas);
        dateFormat = new SimpleDateFormat("HH:mm dd/MM");

        // Guarda las páginas de inicio de cada sección
        createCityInfoSection(document, estadisticas);

        createGlobalStatisticsSection(document, estadisticas);

        createTruckStatisticsSection(document, estadisticas);

        // Generar paginas de grafico de estadisticas por camion
        createGraphicsSection(document, estadisticas);

        // Generar la ultima planificacion
        createLastSimulationSection(document, estadisticas);

        indexMap.put("Referencias", pdf.getNumberOfPages() + 1);
        createReferencesSection(document);

        // Crear la página del índice al principio
        createIndexPage(pdf);

        // Cierra el documento
        document.close();
        pdf.close();
        return baos.toByteArray();
    }

    private static void createLastSimulationSection(Document document, Estadisticas estadisticas) {
        String ultimaPlanificacion = estadisticas.getLastSimulation();
        if(ultimaPlanificacion==null||ultimaPlanificacion.isEmpty()){
            return;
        }
        // convertir a json
        JSONObject json = new JSONObject(ultimaPlanificacion);
        JSONObject ciudad= json.getJSONObject("ciudad");

        insertTitle(document, "Última Planificación");
        document.add(new Paragraph("Se muestra la última planificación realizada en la simulación."));
        insertSubtitle(document, "Pedidos");
        document.add(new Paragraph("Se muestran la ultima organización de pedidos en la ciudad."));
        Table table = new Table(new float[]{1, 2, 3, 4, 1, 4, 3, 1, 2, 4});
        table.addCell(new Cell(2,1).add(new Paragraph("ID")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell(2,1).add(new Paragraph("Cliente")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell(2,1).add(new Paragraph("Coordenada")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell(2,1).add(new Paragraph("Fecha Solicitud")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell(2,1).add(new Paragraph("Plazo")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell(2,1).add(new Paragraph("Fecha Limite")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell(2,1).add(new Paragraph("GLP")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell(1,3).add(new Paragraph("Porciones")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell().add(new Paragraph("ID")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell().add(new Paragraph("GLP")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell().add(new Paragraph("Fecha Entrega")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.setTextAlignment(TextAlignment.RIGHT);
        JSONArray pedidos = ciudad.getJSONArray("pedidos");
        for (int i = 0; i < pedidos.length(); i++) {
            JSONObject pedido = (JSONObject) pedidos.get(i);
            JSONArray porciones = pedido.getJSONArray("porciones");
            int size=porciones.length();
            table.addCell(new Cell(size,1).add(new Paragraph(pedido.getString("id"))));
            table.addCell(new Cell(size,1).add(new Paragraph(pedido.getString("cliente"))
                    .setTextAlignment(TextAlignment.LEFT)));
            JSONObject coordenada = pedido.getJSONObject("coordenada");
            table.addCell(new Cell(size,1).add(new Paragraph(coordenada.getString("x")+", "+coordenada.getString("y"))));
            table.addCell(new Cell(size,1).add(new Paragraph(dateFormat.format(pedido.getLong("fechaSolicitud")))));
            table.addCell(new Cell(size,1).add(new Paragraph(pedido.getString("plazo"))));
            table.addCell(new Cell(size,1).add(new Paragraph(dateFormat.format(pedido.getLong("fechaLimite")))));
            table.addCell(new Cell(size,1).add(new Paragraph(pedido.getString("glp"))));
            for (int j = 0; j < size; j++) {
                JSONObject porcion = (JSONObject) porciones.get(j);
                table.addCell(new Cell().add(new Paragraph(porcion.getString("id"))));
                table.addCell(new Cell().add(new Paragraph(porcion.getString("glp"))));
                table.addCell(new Cell().add(new Paragraph(dateFormat.format(porcion.getLong("fechaEntrega")))));
            }
        }
        document.add(table);
        insertSubtitle(document, "Camiones");
        document.add(new Paragraph("Se muestran la ultima organización de camiones en la ciudad."));
        table = new Table(new float[]{3, 3, 3, 3, 3},true);
        table.addCell(new Cell(2,1).add(new Paragraph("Código")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell(2,1).add(new Paragraph("Capacidad GLP")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell(1,3).add(new Paragraph("Destino")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell().add(new Paragraph("Tipo")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell().add(new Paragraph("ID")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell().add(new Paragraph("Fecha Llegada")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.setTextAlignment(TextAlignment.RIGHT);
        JSONArray camiones = ciudad.getJSONArray("camiones");
        for (int i = 0; i < camiones.length(); i++) {
            JSONObject camion = (JSONObject) camiones.get(i);
            JSONArray ruta = camion.getJSONArray("ruta");
            int size=ruta.length();
            table.addCell(new Cell(size,1).add(new Paragraph(camion.getString("codigo"))));
            table.addCell(new Cell(size,1).add(new Paragraph(camion.getString("capacidadGLP"))));
            for (int j = 0; j < size; j++) {
                JSONObject destino = (JSONObject) ruta.get(j);
                String tipo=destino.getString("tipo");
                table.addCell(new Cell().add(new Paragraph(tipo)));
                if(tipo.equals("almacen")) {
                    table.addCell(new Cell().add(new Paragraph(destino.getString("almacenId"))));
                } else if (tipo.equals("pedido")) {
                    JSONObject pedido = destino.getJSONObject("pedido");
                    table.addCell(new Cell().add(new Paragraph(pedido.getString("id"))));
                }
                JSONObject camino = destino.getJSONObject("camino");
                table.addCell(new Cell().add(new Paragraph(dateFormat.format(camino.getLong("fechaFin")))));
            }
        }
        document.add(table);
    }

    private static void validateFilePath(String path) throws Exception {
        // Crear objeto File a partir de la ruta de destino
        File file = new File(path);

        // Obtener el directorio padre de la ruta del archivo
        File parentDirectory = file.getParentFile();

        // Verificar si el directorio padre no existe y crearlo
        if (parentDirectory != null && !parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

    }

    private static void createCityInfoSection(Document document, Estadisticas estadisticas) {
        insertTitle(document, "Introducción");
        Ciudad ciudad = estadisticas.getCiudad();
        document.add(new Paragraph(
                "Este informe presenta los resultados detallados de la simulación semanal realizada " +
                        "en la ciudad de "+ ciudad.getNombre() + ", un escenario dinámico y complejo que" +
                        " involucra logística urbana y gestión de flotas. La simulación se centró en el " +
                        "funcionamiento y la eficiencia de la distribución a través de "+
                        ciudad.getAlmacenes().size()+" almacenes estratégicamente ubicados, una flota de " +
                        ciudad.getCamiones().size()+" camiones y la gestión de "+estadisticas.getTotalPedidos()+
                        " pedidos en un periodo de una semana.\n\n" +
                        "El objetivo principal de esta simulación fue evaluar la capacidad de respuesta y eficiencia" +
                        " de la red logística bajo un esquema de reprogramación dinámica, donde la asignación de" +
                        " pedidos a los camiones se revisaba y actualizaba cada 5 minutos. Este enfoque permitió" +
                        " adaptar las rutas y asignaciones de los camiones en tiempo real, respondiendo a la" +
                        " entrada continua de nuevos pedidos."
        ).setTextAlignment(TextAlignment.JUSTIFIED));

        insertSubtitle(document, "Ciudad");
        document.add(new Paragraph(
                "La ciudad de "+ciudad.getNombre()+" es una ciudad rectangular de "+ciudad.getMaxX()+
                        " km de ancho y "+ciudad.getMaxY()+" km de alto, cuenta con "+ciudad.getAlmacenes().size()+
                        " almacenes, 1 almacen central ubicado en "+ciudad.getAlmacenPrincipal().getCoordenada()+
                        " y "+(ciudad.getAlmacenes().size()-1)+" almacenes intermedios ubicados en las coordenadas: "+
                        ciudad.getAlmacenes().stream().filter(a->!a.equals(ciudad.getAlmacenPrincipal()))
                                .map(a->a.getCoordenada().toString()).reduce((a,b)->a+", "+b).orElse("")+
                        " que cuentan con una capacidad de "+ciudad.getAlmacenes().stream().filter(a->!a.isPrincipal())
                        .findFirst().get().getCapacidadTotal()+" m3 de GLP cada uno."
        ).setTextAlignment(TextAlignment.JUSTIFIED));
        insertSubtitle(document, "Camiones");
        document.add(new Paragraph(
                "La ciudad cuenta con "+ciudad.getCamiones().size()+" camiones, cada uno con un consumo en galones " +
                        " de Distancia[Km] x Peso [Ton] / "+Camion.getConsumo()+" de petroleo, capacidad de "+
                        Camion.getCapacidadPetroleo()+" galones de petroleo y una velocidad de "+
                        Camion.getVelocidad()+" km/h."
        ));
        // aqui se detalla en una tabla
        Table table = new Table(new float[]{3, 3, 3});
        table.addCell(new Cell().add(new Paragraph("Código")).setBold());
        table.addCell(new Cell().add(new Paragraph("Capacidad GLP")).setBold());
        table.addCell(new Cell().add(new Paragraph("Peso base")).setBold());
        table.setTextAlignment(TextAlignment.RIGHT);
        for (Camion c : ciudad.getCamiones()) {
            table.addCell(new Cell().add(new Paragraph(c.getCodigo())));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(c.getCapacidadGLP()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(c.getPesoBase()))));
        }
        document.add(table);
    }

    private static void createGraphicsSection(Document document, Estadisticas estadisticas) throws Exception {
        CamionStatisticsChart.graficarInteger("src/generated/CamionPorcionPedidos.png",
                estadisticas.getCollectionCamionTotalPorcionPedidos(),
                 "Porciones Entregadas por Camión");
        CamionStatisticsChart.graficarDouble("src/generated/CamionGLP.png",
                estadisticas.getCollectionCamionTotalGLP(),
                "GLP");
        CamionStatisticsChart.graficarDouble("src/generated/CamionKmRecorridos.png",
                estadisticas.getCollectionCamionTotalKmRecorridos(),
                 "Km Recorridos");
        CamionStatisticsChart.graficarDouble("src/generated/CamionPetroleo.png",
                estadisticas.getCollectionCamionTotalPetroleo(),
                 "Petróleo");
        Image img1 = new Image(ImageDataFactory.create("src/generated/CamionPorcionPedidos.png"))
                .scaleToFit(520,341);
        Image img2 = new Image(ImageDataFactory.create("src/generated/CamionGLP.png"))
                .scaleToFit(520,341);
        Image img3 = new Image(ImageDataFactory.create("src/generated/CamionKmRecorridos.png"))
                .scaleToFit(520,341);
        Image img4 = new Image(ImageDataFactory.create("src/generated/CamionPetroleo.png"))
                .scaleToFit(520,341);
        insertTitle(document, "Gráficos de Estadísticas por Camión");
        document.add(new Paragraph("Se muestra la variación de las estadísticas por camión a lo largo de la simulación." +
                " Cada punto representa la suma de las estadísticas generadas anteriores a ese punto en el tiempo y " +
                "las estadísticas futuras proyectadas por la ultima planificación realizada en ese punto. " +
                "En el caso de las porciones entregadas un punto representa la suma de las porciones entregadas por " +
                "ese camión hasta ese punto en el tiempo y las porciones que se espera que entregue en el futuro usando " +
                "la planeación valida en ese punto del tiempo. " )
                        .setTextAlignment(TextAlignment.JUSTIFIED)
                .setKeepWithNext(true));
        insertSubtitle(document, "Porciones Entregadas");
        document.add(img1);
        insertSubtitle(document, "GLP Entregado por Camión");
        document.add(img2);
        insertSubtitle(document, "Kilometros Recorridos por Camión");
        document.add(img3);
        insertSubtitle(document, "Petróleo Consumido por Camión");
        document.add(img4);
    }

    private static void createCoverPage(PdfDocument pdf,Document document,
                                        Estadisticas estadisticas) throws IOException {
        PageSize pageSize = pdf.getDefaultPageSize();

        // Añadir imagen de fondo

        Image fondo= new Image(ImageDataFactory.create(fondoPath))
                .scaleAbsolute(pageSize.getWidth(),pageSize.getHeight())
                .setFixedPosition(0,0);
        document.add(fondo);

        // Añadir logo
        Image logo = new Image(ImageDataFactory.create(logoPath))
                .setWidth(UnitValue.createPointValue(5.39f * 28.64f))
                .setHeight(UnitValue.createPointValue(3.36f * 28.64f))
                .setFixedPosition(
                    pageSize.getWidth() - (5.39f * 28.64f) - (0.5f * 28.64f),
                    pageSize.getHeight() - (3.36f * 28.64f) - (0.5f * 28.64f)
                );
        document.add(logo);

        // Crear división para texto central
        Div textDiv = new Div()
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setWidth(pageSize.getWidth())
                .setHeight(pageSize.getHeight());

        // Añadir texto
        textDiv.add(new Paragraph("Fast GLP")
                .setFont(PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN))
                .setFontSize(72)
                .setFontColor(new DeviceRgb(46, 116, 181))
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
        textDiv.add(new Paragraph("Reporte de\nEjecución")
                .setFont(PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN))
                .setFontSize(50)
                .setFontColor(new DeviceRgb(0, 32, 96))
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
        textDiv.add(new Paragraph("Simulación Semanal")
                .setFont(PdfFontFactory.createFont(calibriPath))
                .setFontSize(26)
                .setFontColor(ColorConstants.BLACK)
                .setTextAlignment(TextAlignment.CENTER));
        textDiv.add(new Paragraph(dateFormat.format(estadisticas.getFechaInicio())+
                " - "+dateFormat.format(estadisticas.getFechaFin()))
                .setFont(PdfFontFactory.createFont(montserratPath))
                .setFontSize(18)
                .setFontColor(ColorConstants.BLACK)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));
        document.add(textDiv);
        document.add(new AreaBreak());
    }

    private static void createIndexPage(PdfDocument pdf) {
        pdf.removePage(2);
        pdf.addNewPage(2);
        Document document = new Document(pdf, PageSize.A4);
        document.add(new Paragraph("Índice")
                .setBold()
                .setFontSize(14));
        for(Map.Entry<String,Integer> entry : indexMap.entrySet()){
            Paragraph p = new Paragraph();
            p.addTabStops(new TabStop(1000, TabAlignment.RIGHT, new DottedLine()));
            p.add(entry.getKey()).add(new Tab()).add("Página "+entry.getValue().toString());
            document.add(p);
        }
        document.add(new AreaBreak());
    }

    private static void insertTitle(Document document, String title) {
        tituloIndex++;
        String titulo = tituloIndex+". "+title;
        document.add(new Paragraph(titulo)
                .setBold()
                .setFontSize(14)
                .setKeepWithNext(true)); // Para evitar orfandad de título
        indexMap.put( titulo, pdf.getNumberOfPages());
        subtituloIndex = 0;
    }

    private static void insertSubtitle(Document document, String subtitle) {
        subtituloIndex++;
        String subTitulo = tituloIndex+"."+subtituloIndex+". "+subtitle;
        document.add(new Paragraph(subTitulo)
                .setBold()
                .setFontSize(12)
                .setKeepWithNext(true)); // Para evitar orfandad de título
        indexMap.put( subTitulo, pdf.getNumberOfPages());
    }

    private static void createGlobalStatisticsSection(Document document, Estadisticas estadisticas) {
        insertTitle(document, "Estadísticas Globales");

        document.add(new Paragraph("Fecha Inicio: " + dateFormat.format(estadisticas.getFechaInicio())));
        document.add(new Paragraph("Fecha Fin: " + dateFormat.format(estadisticas.getFechaFin())));
        document.add(new Paragraph("Tiempo Total: " + estadisticas.getTiempoTotal() + " ms"));
        document.add(new Paragraph("Total de Porciones Entregadas: " + estadisticas.getTotalPorcionPedidos()));
        document.add(new Paragraph("Total de Replanificaciones: " + estadisticas.getTotalReplanificaciones()));
        document.add(new Paragraph("Total Km Recorridos: " + Utils.formatDouble(estadisticas.getTotalKmRecorridos())));
        document.add(new Paragraph("Total de GLP entregado: " + Utils.formatDouble(estadisticas.getTotalGLP())));
        document.add(new Paragraph("Total de Petróleo Consumido: " + Utils.formatDouble(estadisticas.getTotalPetroleo())));
        document.add(new Paragraph("\n")); // Espacio antes de la próxima sección
    }

    private static void createTruckStatisticsSection(Document document, Estadisticas estadisticas) {
        insertTitle(document, "Estadísticas por Camión");

        Table table = new Table(new float[]{3, 3, 3, 3, 3},true);
        table.addCell(new Cell().add(new Paragraph("Camión Código")).setBold());
        table.addCell(new Cell().add(new Paragraph("Porciones Entregadas")).setBold());
        table.addCell(new Cell().add(new Paragraph("GLP Entregado")).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell().add(new Paragraph("Km Recorridos")).setBold());
        table.addCell(new Cell().add(new Paragraph("Petróleo Consumido")).setBold());
        table.setTextAlignment(TextAlignment.RIGHT);
        for (Map.Entry<String, Estadisticas.EstadisticasPorCamion> entry : estadisticas.getEstadisticasPorCamion().entrySet()) {
            Estadisticas.EstadisticasPorCamion e = entry.getValue();
            table.addCell(new Cell().add(new Paragraph(e.getCamion().getCodigo())));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(e.getCamionTotalPorcionPedidos()))));
            table.addCell(new Cell().add(new Paragraph(Utils.formatDouble(e.getCamionTotalGLP()))));
            table.addCell(new Cell().add(new Paragraph(Utils.formatDouble(e.getCamionTotalKmRecorridos()))));
            table.addCell(new Cell().add(new Paragraph(Utils.formatDouble(e.getCamionTotalPetroleo()))));
        }
        document.add(table);
    }

    private static class HeaderFooterEventHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdf.getPageNumber(page);

            if (pageNumber > 1) {
                Rectangle pageSize = page.getPageSize();
                PdfCanvas pdfCanvas = new PdfCanvas(page);

                try {
                    // Encabezado
                    pdfCanvas.beginText()
                            .setFontAndSize(PdfFontFactory.createFont(StandardFonts.HELVETICA), 11)
                            .moveText(30, pageSize.getTop() - 30)
                            .showText("Reporte de Ejecución Semanal")
                            .endText();

                    // Pie de página
                    String text = "Página " + pageNumber;
                    pdfCanvas.beginText()
                            .setFontAndSize(PdfFontFactory.createFont(StandardFonts.HELVETICA), 12)
                            .moveText(pageSize.getWidth() / 2 - 20, pageSize.getBottom() + 20)
                            .showText(text)
                            .endText();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void createReferencesSection(Document document) {
        document.add(new AreaBreak());//salto de pagina
        document.add(new Paragraph("Referencias")
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(14)
                .setKeepWithNext(true));
        document.add(new Paragraph("FastGLPBackend. (2023). Recuperado de https://github.com/Javier843/FastGLP-BackEnd")
                .setFirstLineIndent(30)
                .setFontSize(12));
    }
}
