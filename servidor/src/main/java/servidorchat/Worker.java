package servidorchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.stream.Collectors;

public class Worker implements Runnable {

    private final Socket socketCliente;
    private DataInputStream entrada = null;
    private DataOutputStream salida = null;
    private String nombre = "";

    public Worker(Socket socketCliente) {
        this.socketCliente = socketCliente;

    }

    @Override
    public void run() {

        boolean conectado = true;

        try { 

            entrada = new DataInputStream(socketCliente.getInputStream());
            salida = new DataOutputStream(socketCliente.getOutputStream());

            System.out.println("Conexión aceptada: " + socketCliente.getInetAddress());


            while (conectado) {

                String mensajeRecibido = entrada.readUTF();
                String comando = "";
                String enviarMensaje = "";

                if (!mensajeRecibido.isEmpty()) {
                    comando = mensajeRecibido.substring(0, 3);
                }

                if (comando.equals("MSG")) {
                    enviarMensaje = "CHT " + nombre + "," + mensajeRecibido.substring(4, mensajeRecibido.length());
                    mensajeTodos(enviarMensaje);
                } else if (comando.equals("CON")) {

                    String nombreUsuario = mensajeRecibido.substring(mensajeRecibido.indexOf(" ") + 1,mensajeRecibido.length()).trim();

                    boolean encontrado = ServidorMultiHilo.clientes.stream().anyMatch(cliente -> cliente.nombre.equals(nombreUsuario));

                    if (!encontrado) {
                        
                        agregarCliente();
                        this.nombre = nombreUsuario;
                        enviarMensaje = "OK";
                        salida.writeUTF(enviarMensaje);
                        salida.flush();
                        mensajeTodos("CHT " + this.nombre + ", El usuario " + this.nombre + " se ha conectado");


                    } else {
                        enviarMensaje = "NOK";
                        salida.writeUTF(enviarMensaje);
                        salida.flush();
                    }
                } else if (comando.equals("LUS")) {

                    String clientesNombres = "LST "+ ServidorMultiHilo.clientes
                    .stream()
                    .map(x -> x.nombre).
                    collect(Collectors.joining(","));
                    
                    mensajeTodos(clientesNombres);

                } else if (comando.equals("PRV")) {

                    String usuarioDestinatario = mensajeRecibido
                    .substring(mensajeRecibido.indexOf(" ") + 1, mensajeRecibido.lastIndexOf(","));
                    String mensaje = mensajeRecibido
                    .substring(mensajeRecibido.lastIndexOf(",") + 1, mensajeRecibido.length());

                    mensajePrivado(mensaje, usuarioDestinatario, this.nombre);
                
                } else if (comando.equals("EXI")) {
                    String mensaje = "EXI " + this.nombre;
                    mensajeTodos(mensaje);;
                    conectado = false;
                    eliminarCliente();
                }

                System.out.println("<-- Servidor: " + mensajeRecibido);
                System.out.println("--> Cliente " + nombre + " : " + enviarMensaje);
                System.out.println( "--------------------------------------------------------------------------------------");

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            eliminarCliente();
            try {
                if (entrada != null) entrada.close();
                if (salida != null) salida.close();
                if (socketCliente != null) socketCliente.close();
                System.out.println("Conexión cerrada para el cliente: " + nombre);
            } catch (IOException e) {
                System.out.println("Error al cerrar recursos: " + e.getMessage());
            }
        }
    }

    public void mensajeTodos(String mensaje) {
        synchronized (ServidorMultiHilo.clientes) {
            for (Worker cliente : ServidorMultiHilo.clientes) {
                try {
                    cliente.salida.writeUTF(mensaje);
                    cliente.salida.flush();
                } catch (IOException e) {
                    System.out.println("Error enviando mensaje a " + cliente.nombre + ": " + e.getMessage());
                }
            }
        }
    }
    

    private boolean mensajePrivado(String mensaje, String usuarioDestinario, String usuarioLocal) {
        for (Worker cliente : ServidorMultiHilo.clientes) {
            try {
                if (cliente.nombre.equals(usuarioDestinario)) {
                    String mensajeEnviarCliente = "PRV " + usuarioLocal + "," + mensaje;
                    cliente.salida.writeUTF(mensajeEnviarCliente);
                    cliente.salida.flush();
                    return true;
                }

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        return false;
    }

    private void agregarCliente() {
        synchronized (ServidorMultiHilo.clientes) {
            ServidorMultiHilo.clientes.add(this);
        }
    }

    private void eliminarCliente() {
        synchronized (ServidorMultiHilo.clientes) {
            ServidorMultiHilo.clientes.remove(this);
        }
    }
    
    
}
