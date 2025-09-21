/*
  ServidorHTTP.java
  Carlos Pineda G. 2025
*/

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

class ServidorHTTP
{
  static class Worker extends Thread
  {
    Socket conexion;
    Worker(Socket conexion)
    {
      this.conexion = conexion;
    }
    int valor(String parametros, String variable) throws Exception
    {
      String[] p = parametros.split("&");
      for (int i = 0; i < p.length; i++)
      {
          String[] s = p[i].split("=");
          if (s[0].equals(variable))
            return Integer.parseInt(s[1]);
      }
      throw new Exception("Se espera la variable: " + variable);
    }
    public void run()
    {
      try
      {
        BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        PrintWriter salida = new PrintWriter(conexion.getOutputStream());

  	    String req= entrada.readLine();
        System.out.println("Petición: " + req);

        for (;;)
        {
          String encabezado= entrada.readLine();
          System.out.println("Encabezado: " + encabezado);
          if (encabezado.equals("")) break;
        }

        if (req.startsWith("GET /suma?"))
        {
          String parametros = req.split(" ")[1].split("\\?")[1];
          String respuesta = String.valueOf(valor(parametros,"a") + valor(parametros,"b") + valor(parametros,"c"));
          salida.println("HTTP/1.1 200 OK");
          salida.println("Access-Control-Allow-Origin: *"); // permite todos los orígenes
          salida.println("Content-type: text/plain; charset=utf-8");
          salida.println("Content-length: "+respuesta.length());
	        salida.println("Connection: close");
          salida.println();
          salida.println(respuesta);
          salida.flush();
        }
        else
        {
          String respuesta = "<html><body><h1>404 File Not Found</h1></body></html>";
          salida.println("HTTP/1.1 404 Not Found");
          salida.println("Content-Type: text/html; charset=utf-8");
          salida.println("Content-Length: " + respuesta.length());
          salida.println("Connection: close");
          salida.println();
          salida.println(respuesta);
          salida.flush();
        }
      }
      catch (Exception e)
      {
        System.err.println("Error en la conexión: " + e.getMessage());
      }
      finally
      {
        try
        {
          conexion.close();
        }
        catch (Exception e)
        {
          System.err.println("Error en close: " + e.getMessage());
        }
      }
    }
  }

  public static void main(String[] args) throws Exception
  {
    ServerSocket servidor = new ServerSocket(80);

    for(;;)
    {
      Socket conexion = servidor.accept();
      new Worker(conexion).start();
    }
  }
}