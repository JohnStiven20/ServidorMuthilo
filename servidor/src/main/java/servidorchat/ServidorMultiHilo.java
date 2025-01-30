package servidorchat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServidorMultiHilo {
    
    public static final int PUERTO = 4444;
    public static ArrayList<Worker> clientes = new ArrayList<>();
    public static void main(String[] args) {

        ServerSocket socketServidor = null;
       
        try {

            socketServidor = new ServerSocket(PUERTO);

        } catch (IOException e) {
            System.out.println("No puede escuchar en el puerto: " + PUERTO);
            System.exit(-1);
        }

        Socket socketCliente = null;

        System.out.println("Escuchando: " + socketServidor);

        try {

            while (true) {
                socketCliente = socketServidor.accept();                
                Worker worker = new Worker(socketCliente);
                new Thread(worker).start();
            }

        } catch (Exception e) {
            System.out.println("IOException: " + e.getMessage());
        }

    }
    
}
