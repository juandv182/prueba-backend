package fastglp.utils.pdf;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CamionStatisticsChart {

    public static void graficarInteger(String fileName, Map<String, ArrayList<Integer>> data, String estadistica) throws Exception {
        // Crear el dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, ArrayList<Integer>> entry : data.entrySet()) {
            String camion = entry.getKey();
            int index = 1;
            for (Integer value : entry.getValue()) {
                dataset.addValue(value, camion, String.valueOf(index++));
            }
        }
        generarGrafico(fileName, estadistica, dataset);
    }
    public static void graficarDouble(String fileName, Map<String, ArrayList<Double>> data, String estadistica) throws Exception {
        // Crear el dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, ArrayList<Double>> entry : data.entrySet()) {
            String camion = entry.getKey();
            int index = 1;
            for (Double value : entry.getValue()) {
                dataset.addValue(value, camion, String.valueOf(index++));
            }
        }
        generarGrafico(fileName,estadistica, dataset);
    }


    private static void generarGrafico(String fileName, String estadistica, DefaultCategoryDataset dataset) throws IOException {
        // Crear el gráfico
        JFreeChart chart = ChartFactory.createLineChart(
                null, // Título del gráfico
                "Iteración", // Eje X
                estadistica, // Eje Y
                dataset,
                PlotOrientation.VERTICAL,
                true, // Incluir leyendas
                true,
                false
        );
        CategoryPlot plot = chart.getCategoryPlot();
        plot.getDomainAxis().setTickLabelsVisible(false);

        // Exportar como imagen
        ChartUtils.saveChartAsPNG(new File(fileName), chart, 842, 595,
                null,true,10); // Tamaño A4 en horizontal
    }
}
