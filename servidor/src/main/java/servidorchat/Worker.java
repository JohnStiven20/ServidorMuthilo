package servidorchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.stream.Collectors;

/**
 * Clase que maneja la conexión de un cliente en el chat.
 */
public class Worker implements Runnable {

    private final Socket socketCliente;
    private DataInputStream entrada = null;
    private DataOutputStream salida = null;
    String nombre = "";

    /**
     * Constructor de la clase Worker.
     * @param socketCliente Socket del cliente conectado.
     */
    public Worker(Socket socketCliente) {
        this.socketCliente = socketCliente;
    }

    /**
     * Método que gestiona la comunicación con el cliente.
     */
    @Override
    public void run() {

        boolean conectado = true;

        try {
            entrada = new DataInputStream(socketCliente.getInputStream());
            salida = new DataOutputStream(socketCliente.getOutputStream());


            while (conectado) {

                String mensajeRecibido = entrada.readUTF();
                String comando = "";
                String enviarMensaje = "";

                if (!mensajeRecibido.isEmpty()) {
                    comando = mensajeRecibido.substring(0, 3);
                }

                ServidorMultiHilo.LOG.info("Mensaje recibido del cliente " + nombre + ": " + mensajeRecibido);

                if (comando.equals("MSG")) {
                    enviarMensaje = "CHT " + nombre + "," + mensajeRecibido.substring(4, mensajeRecibido.length());
                    mensajeTodos(enviarMensaje);
                } else if (comando.equals("CON")) {
                    String nombreUsuario = mensajeRecibido.substring(mensajeRecibido.indexOf(" ") + 1, mensajeRecibido.length()).trim();
                    boolean encontrado = ServidorMultiHilo.clientes.stream().anyMatch(cliente -> cliente.nombre.equals(nombreUsuario));

                    if (!encontrado) {
                        agregarCliente();
                        this.nombre = nombreUsuario;
                        enviarMensaje = "OK";
                        salida.writeUTF(enviarMensaje);
                        salida.flush();
                    } else {
                        enviarMensaje = "NOK";
                        salida.writeUTF(enviarMensaje);
                        salida.flush();
                    }

                } else if (comando.equals("LUS")) {
                    
                    String clientesNombres = "LST " + ServidorMultiHilo.clientes
                            .stream()
                            .map(x -> x.nombre)
                            .collect(Collectors.joining(","));
                    mensajeTodos(clientesNombres);

                } else if (comando.equals("PRV")) {

                    String usuarioDestinatario = mensajeRecibido
                        .substring(mensajeRecibido.indexOf(" ") + 1, mensajeRecibido.lastIndexOf(","));
                    String mensaje = mensajeRecibido
                        .substring(mensajeRecibido.lastIndexOf(",") + 1, mensajeRecibido.length());

                    mensajePrivado(mensaje, usuarioDestinatario, this.nombre);

                } else if (comando.equals("EXI")) {
                    
                    mensajeTodos("CHT " + this.nombre + ",El usuario " + this.nombre + " ha salido del chat");
                    String mensaje = "EXI " + this.nombre;
                    mensajeTodos(mensaje);
                    conectado = false;
                }

                ServidorMultiHilo.LOG.info("Respuesta enviada al cliente " + nombre + ": " + enviarMensaje);

            }

        } catch (IOException e) {
            ServidorMultiHilo.LOG.severe("Error en la comunicacion con el cliente " + nombre + ": " + e.getMessage());
        } finally {
            eliminarCliente();
            try {

                if (entrada != null) entrada.close();
                if (salida != null) salida.close();
                if (socketCliente != null) socketCliente.close();

                ServidorMultiHilo.LOG.info("Conexion cerrada para el cliente: " + nombre);
            } catch (IOException e) {
                ServidorMultiHilo.LOG.severe("Error al cerrar recursos para el cliente " + nombre + ": " + e.getMessage());
            }
        }
    }

    /**
     * Envía un mensaje a todos los clientes conectados.
     * @param mensaje Mensaje a enviar.
     */
    public void mensajeTodos(String mensaje) {
        synchronized (ServidorMultiHilo.clientes) {
            for (Worker cliente : ServidorMultiHilo.clientes) {
                try {
                    cliente.salida.writeUTF(mensaje);
                    cliente.salida.flush();
                    ServidorMultiHilo.LOG.info("Mensaje enviado a " + cliente.nombre + ": " + mensaje);
                } catch (IOException e) {
                    ServidorMultiHilo.LOG.severe("Error enviando mensaje a " + cliente.nombre + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Envía un mensaje privado a un usuario específico.
     * @param mensaje Mensaje a enviar.
     * @param usuarioDestinatario Nombre del usuario destinatario.
     * @param usuarioLocal Nombre del usuario que envía el mensaje.
     * @return true si el mensaje fue enviado con éxito, false en caso contrario.
     */
    private boolean mensajePrivado(String mensaje, String usuarioDestinatario, String usuarioLocal) {
        for (Worker cliente : ServidorMultiHilo.clientes) {
            try {
                if (cliente.nombre.equals(usuarioDestinatario)) {
                    String mensajeEnviarCliente = "PRV " + usuarioLocal + "," + mensaje;
                    cliente.salida.writeUTF(mensajeEnviarCliente);
                    cliente.salida.flush();
                    ServidorMultiHilo.LOG.info("Mensaje privado enviado a " + usuarioDestinatario + " desde " + usuarioLocal + ": " + mensaje);
                    return true;
                }
            } catch (IOException e) {
                ServidorMultiHilo.LOG.severe("Error enviando mensaje privado a " + usuarioDestinatario + ": " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Agrega un cliente a la lista de clientes conectados.
     */
    private void agregarCliente() {
        synchronized (ServidorMultiHilo.clientes) {
            ServidorMultiHilo.clientes.add(this);
            ServidorMultiHilo.LOG.info("Cliente agregado: " + nombre);
        }
    }

    /**
     * Elimina un cliente de la lista de clientes conectados.
     */
    private void eliminarCliente() {
        synchronized (ServidorMultiHilo.clientes) {
            ServidorMultiHilo.clientes.remove(this);
            ServidorMultiHilo.LOG.info("Cliente eliminado: " + nombre);
        }
    }
}
