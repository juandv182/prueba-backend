package fastglp.utils;

import fastglp.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Utils {

    public static int compareDate(Date d1, Date d2) {
        // Tolerancia definida en milisegundos (1000 ms = 1 segundo)
        long TOLERANCE = 1000;

        long time1 = d1.getTime();
        long time2 = d2.getTime();

        // Calcula la diferencia absoluta entre las dos fechas
        long difference = Math.abs(time1 - time2);

        // Compara con la tolerancia
        if (difference <= TOLERANCE) {
            return 0;  // Las fechas se consideran iguales
        } else if (time1 < time2) {
            return -1;  // d1 es anterior a d2
        } else {
            return 1;   // d1 es posterior a d2
        }
    }
    public static byte[] compress(String data) {
        long init = System.currentTimeMillis();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress the data", e);
        }finally {
            System.out.println("Se ha comprimido en "+(System.currentTimeMillis()-init)+" ms");
        }
        return byteArrayOutputStream.toByteArray();
    }
    public static String decompress(byte[] data) {
        long init = System.currentTimeMillis();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to decompress the data", e);
        } finally {
            System.out.println("Se ha descomprimido en "+(System.currentTimeMillis()-init)+" ms");
        }
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    }

    public static Ciudad createCiudadOnly(){
        Ciudad mockCiudad = new Ciudad("SkyLines", 70, 50);
        for (long i = 1; i <= 2; i++) {
            Camion c = new Camion(25, new Coordenada(12, 8), 2.5);
            c.setCodigo(c.getCodigo() + String.format("%02d", i));
            c.setCiudad(mockCiudad);
            mockCiudad.addCamion(c);
        }
        for (long i = 1; i <= 4; i++) {
            Camion c = new Camion( 15, new Coordenada(12, 8), 2.0);
            c.setCodigo(c.getCodigo() + String.format("%02d", i));
            c.setCiudad(mockCiudad);
            mockCiudad.addCamion(c);
        }
        for (long i = 1; i <= 4; i++) {
            Camion c = new Camion( 10, new Coordenada(12, 8), 1.5);
            c.setCodigo(c.getCodigo() + String.format("%02d", i));
            c.setCiudad(mockCiudad);
            mockCiudad.addCamion(c);
        }
        for (long i = 1; i <= 10; i++) {
            Camion c = new Camion( 5, new Coordenada(12, 8), 1.0);
            c.setCodigo(c.getCodigo() + String.format("%02d", i));
            c.setCiudad(mockCiudad);
            mockCiudad.addCamion(c);
        }
        mockCiudad.addAlmacen(new Almacen(new Coordenada(12, 8), 0, 0, true));
        mockCiudad.addAlmacen(new Almacen(new Coordenada(42, 42), 160, 160, false));
        mockCiudad.addAlmacen(new Almacen(new Coordenada(63, 3), 160, 160, false));

        mockCiudad.getAlmacenes().forEach(a->a.setCiudad(mockCiudad));
        return mockCiudad;
    }

    public static void llenarBloqueosPedidosFromFiles(Ciudad ciudad, int mes, int anio){
        Date fechaInicio = Utils.parseFecha(String.format("01/%02d/%04d 00", mes, anio));
        String nombreArchivo = String.format("%04d%02d", anio, mes);
        ciudad.addPedido(Utils.leerPedidos(nombreArchivo, fechaInicio));
        ciudad.addBloqueo(Utils.leerBloqueos(nombreArchivo, fechaInicio));
        ciudad.getPedidos().forEach(p->p.setCiudad(ciudad));
        ciudad.getBloqueos().forEach(b->b.setCiudad(ciudad));
    }

    public static Ciudad createMockCiudad(int mes, int anio) {
        Ciudad mockCiudad = createCiudadOnly();
        llenarBloqueosPedidosFromFiles(mockCiudad,mes,anio);
        //colocarles id a los camiones
        IntStream.range(0, mockCiudad.getCamiones().size()).forEach(i -> mockCiudad.getCamiones().get(i).setId((long) i));
        //colocarles id a los pedidos
        IntStream.range(0, mockCiudad.getPedidos().size()).forEach(i -> mockCiudad.getPedidos().get(i).setId((long) i));
        //colocarles id a los almacenes
        IntStream.range(0, mockCiudad.getAlmacenes().size()).forEach(i -> mockCiudad.getAlmacenes().get(i).setId((long) i));
        //colocarles id a los bloqueos
        IntStream.range(0, mockCiudad.getBloqueos().size()).forEach(i -> mockCiudad.getBloqueos().get(i).setId((long) i));
        return mockCiudad;
    }

    private static ArrayList<Pedido> leerPedidos(String nombreArchivo, Date fechaInicioMes) {
        ArrayList<Pedido> pedidos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(getPath("pedidos", "ventas"+nombreArchivo, ".txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":")[1].split(",");
                Coordenada coord = new Coordenada(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                double cantidad = Double.parseDouble(parts[3].substring(0, parts[3].length() - 2));
                double duracion = Double.parseDouble(parts[4].substring(0, parts[4].length() - 1));
                pedidos.add(new Pedido(coord, getAdjustedDate(fechaInicioMes, line.split(":")[0]), duracion, cantidad, parts[2]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.printf("Se han leido %d pedidos%n", pedidos.size());
        return pedidos;
    }

    public static Date parseFecha(String fecha) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH");
            return sdf.parse(fecha);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al parsear la fecha: " + fecha, e);
        }
    }

    private static ArrayList<Bloqueo> leerBloqueos(String nombreArchivo, Date fechaInicioMes) {
        ArrayList<Bloqueo> bloqueos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(getPath("bloqueos", nombreArchivo, ".bloqueos.txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                String[] fechas = parts[0].split("-");
                Bloqueo bloqueo = new Bloqueo(getAdjustedDate(fechaInicioMes, fechas[0]), getAdjustedDate(fechaInicioMes, fechas[1]));
                String[] coords = parts[1].split(",");
                for (int j = 0; j < coords.length - 1; j += 2) {
                    bloqueo.addCoordenada(new Coordenada(Double.parseDouble(coords[j]), Double.parseDouble(coords[j+1])));
                }
                bloqueos.add(bloqueo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.printf("Se han leido %d bloqueos%n", bloqueos.size());
        return bloqueos;
    }

    private static String getPath(String base, String nombreArchivo, String ext) {
        return "src/main/java/fastglp/com/example/mtsp/" + base + "/" + nombreArchivo + ext;
    }

    private static Date getAdjustedDate(Date fechaInicioMes, String date) {
        return new Date(fechaInicioMes.getTime() + toMillis(date));
    }

    private static long toMillis(String date) {
        String[] parts = date.split("[dhm]");
        return (Long.parseLong(parts[0]) - 1) * 86400000 + Long.parseLong(parts[1]) * 3600000 + Long.parseLong(parts[2]) * 60000;
    }
}
