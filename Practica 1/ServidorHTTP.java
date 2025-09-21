import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServidorHTTP {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Servidor HTTP escuchando en puerto 8080...");

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> manejarCliente(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void manejarCliente(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            // Leer primera línea de la petición (ej: "GET /archivo.html HTTP/1.1")
            String requestLine = in.readLine();
            if (requestLine == null) return;

            System.out.println("Petición: " + requestLine);

            StringTokenizer tokens = new StringTokenizer(requestLine);
            String method = tokens.nextToken();
            String resource = tokens.nextToken();

            // Solo manejamos GET
            if (!method.equals("GET")) {
                enviarRespuesta(out, "501 Not Implemented", "text/plain", "Método no soportado".getBytes(), null);
                return;
            }

            // Manejar la ruta /suma
            if (resource.contains("/suma")) {
                String query = resource.contains("?") ? resource.split("\\?")[1] : "";
                manejarSuma(out, query);
                return;
            }

            // Quitar el "/" inicial
            if (resource.startsWith("/")) resource = resource.substring(1);
            if (resource.equals("")) resource = "index.html"; // default

            File file = new File(resource);

            if (!file.exists()) {
                enviarRespuesta(out, "404 Not Found", "text/plain", "Archivo no encontrado".getBytes(), null);
                return;
            }

            // Revisar encabezados adicionales (If-Modified-Since)
            String headerLine;
            String ifModifiedSince = null;
            while (!(headerLine = in.readLine()).equals("")) {
                if (headerLine.startsWith("If-Modified-Since:")) {
                    ifModifiedSince = headerLine.substring(19).trim();
                }
            }

            // Fecha de modificación real del archivo
            long lastModified = file.lastModified();
            String lastModifiedStr = formatoFechaHTTP(new Date(lastModified));

            if (ifModifiedSince != null) {
                Date fechaCliente = parseFechaHTTP(ifModifiedSince);
                if (fechaCliente != null && fechaCliente.getTime() >= lastModified) {
                    // Cliente ya tiene la versión más reciente
                    String header = "HTTP/1.1 304 Not Modified\r\n" +
                                    "Last-Modified: " + lastModifiedStr + "\r\n\r\n";
                    out.write(header.getBytes());
                    return;
                }
            }

            // Leer archivo
            byte[] contenido = leerArchivo(file);

            // Determinar MIME (aquí fijo en text/html para lo que pides)
            enviarRespuesta(out, "200 OK", "text/html", contenido, lastModifiedStr);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void manejarSuma(OutputStream out, String query) throws IOException {
        // Extraer parámetros a, b, c
        Map<String, String> parametros = new HashMap<>();
        try {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    parametros.put(keyValue[0], keyValue[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        int a = Integer.parseInt(parametros.getOrDefault("a", "0"));
        int b = Integer.parseInt(parametros.getOrDefault("b", "0"));
        int c = Integer.parseInt(parametros.getOrDefault("c", "0"));
        
        int resultado = a + b + c;
        String respuesta = "Resultado: " + resultado;
        
        enviarRespuesta(out, "200 OK", "text/plain", respuesta.getBytes(), null);
    }

    private static void enviarRespuesta(OutputStream out, String status, String contentType, byte[] contenido, String lastModified) throws IOException {
        String header = "HTTP/1.1 " + status + "\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + (contenido != null ? contenido.length : 0) + "\r\n";
        if (lastModified != null) {
            header += "Last-Modified: " + lastModified + "\r\n";
        }
        header += "\r\n";

        out.write(header.getBytes());
        if (contenido != null) {
            out.write(contenido);
        }
    }

    private static byte[] leerArchivo(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    private static String formatoFechaHTTP(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }

    private static Date parseFechaHTTP(String fecha) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            return format.parse(fecha);
        } catch (Exception e) {
            return null;
        }
    }
}