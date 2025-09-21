import java.io.*;
import java.net.*;
import javax.net.ssl.*;

public class AdministradorTraficoSSL {
    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Uso: java AdministradorTraficoSSL <puerto_proxy> <ip_servidor1> <puerto1> <ip_servidor2> <puerto2>");
            return;
        }

        int puertoProxy = Integer.parseInt(args[0]);
        String ipServidor1 = args[1];
        int puertoServidor1 = Integer.parseInt(args[2]);
        String ipServidor2 = args[3];
        int puertoServidor2 = Integer.parseInt(args[4]);

        // Configurar SSL
        System.setProperty("javax.net.ssl.keyStore", "keystore_servidor.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password123");

        try {
            SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(puertoProxy);
            
            System.out.println("Proxy inverso SSL escuchando en puerto " + puertoProxy);

            while (true) {
                SSLSocket navegador = (SSLSocket) serverSocket.accept();
                new Thread(() -> manejarConexion(navegador, ipServidor1, puertoServidor1, ipServidor2, puertoServidor2)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void manejarConexion(SSLSocket navegador, String ipServidor1, int puertoServidor1,
                                        String ipServidor2, int puertoServidor2) {
        try (
            BufferedReader inNavegador = new BufferedReader(new InputStreamReader(navegador.getInputStream()));
            OutputStream outNavegador = navegador.getOutputStream()
        ) {
            // Leer petición completa del navegador
            StringBuilder peticion = new StringBuilder();
            String linea;
            while ((linea = inNavegador.readLine()) != null && !linea.isEmpty()) {
                peticion.append(linea).append("\r\n");
            }
            peticion.append("\r\n"); // fin de cabecera

            String request = peticion.toString();
            System.out.println("Petición SSL recibida:\n" + request);

            // Cliente 1 -> Servidor 1 (HTTP normal, no SSL)
            Socket cliente1 = new Socket(ipServidor1, puertoServidor1);
            PrintWriter outCliente1 = new PrintWriter(cliente1.getOutputStream(), true);
            BufferedReader inCliente1 = new BufferedReader(new InputStreamReader(cliente1.getInputStream()));

            outCliente1.print(request);
            outCliente1.flush();

            // Cliente 2 -> Servidor 2 (HTTP normal, no SSL)
            Socket cliente2 = new Socket(ipServidor2, puertoServidor2);
            PrintWriter outCliente2 = new PrintWriter(cliente2.getOutputStream(), true);
            BufferedReader inCliente2 = new BufferedReader(new InputStreamReader(cliente2.getInputStream()));

            outCliente2.print(request);
            outCliente2.flush();

            // Leer respuesta del Servidor 1 y reenviar al navegador
            String lineaResp1;
            while ((lineaResp1 = inCliente1.readLine()) != null) {
                outNavegador.write((lineaResp1 + "\r\n").getBytes());
            }
            outNavegador.flush();

            // Leer respuesta del Servidor 2 pero NO enviarla al navegador
            while (inCliente2.readLine() != null) {
                // simplemente consumir la respuesta
            }

            cliente1.close();
            cliente2.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { navegador.close(); } catch (IOException ignored) {}
        }
    }
}