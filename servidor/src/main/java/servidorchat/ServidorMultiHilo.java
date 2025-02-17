package servidorchat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServidorMultiHilo {
    
    public static final int PUERTO = 4444;
    public static ArrayList<Worker> clientes = new ArrayList<>();
    public static final Logger LOG = Logger.getLogger(ServidorMultiHilo.class.getName());

    
    public static void main(String[] args) {

        ServerSocket socketServidor = null;
       
        try {
            configurarLogger();
            LOG.info("Servidor iniciado en el puerto " + PUERTO);

            socketServidor = new ServerSocket(PUERTO);
            LOG.info("Socket de servidor creado en el puerto " + PUERTO);


        } catch (IOException e) {
            LOG.severe("Error en el servidor al intentar abrir el socket: " + e.getMessage());
            System.exit(-1);
        }

        Socket socketCliente = null;

        LOG.info("Servidor en espera de conexiones...");

        try {

            while (true) {

                socketCliente = socketServidor.accept();
                LOG.info("Nuevo cliente conectado desde " + socketCliente.getInetAddress().getHostAddress());
                
                Worker worker = new Worker(socketCliente);
                new Thread(worker).start();
                LOG.fine("Nuevo Worker creado para gestionar cliente: " + socketCliente.getInetAddress());

            }

        } catch (Exception e) {
            LOG.severe("Error en la ejecución del servidor: " + e.getMessage());
        }

    }

    private static void configurarLogger() {
        try {
            FileHandler ficheroTXT = new FileHandler("servidor.txt", true);
            SimpleFormatter formatoTXT = new SimpleFormatter();
            ficheroTXT.setFormatter(formatoTXT);
            LOG.addHandler(ficheroTXT);

            LOG.setLevel(Level.ALL);
            LOG.info("Logger configurado correctamente.");
        } catch (SecurityException | IOException e) {
            LOG.severe("Error en la creacion del fichero de logs: " + e.getMessage());
        }
    }
}
