import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements AutoCloseable, Runnable {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private final String name;
    private Socket server;

    public Client(String name){
        this.name = name;
        logger.log(Level.INFO, "Client {0} is created", name);
    }

    public boolean connect(String host, int port) {
        try {
            if (!isConnected()) {
                server = new Socket(host, port);
                logger.log(Level.INFO, "client {0} is connected", name);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.log(Level.WARNING, "connection error", e);
            return false;
        }
    }

    public boolean isConnected() {
        return server != null && server.isConnected() && !server.isClosed();
    }

    @Override
    public void close() throws IOException {
        if (isConnected()) server.close();
        logger.log(Level.INFO, "Client {0} is closed", name);
    }

    @Override
    public void run() {
        if (!isConnected()) {
            logger.log(Level.WARNING, "{0} not connected, run() aborted", name);
            return;
        }

        try (BufferedReader input = new BufferedReader(new InputStreamReader(server.getInputStream()));
             PrintWriter output = new PrintWriter(new OutputStreamWriter(server.getOutputStream()), true)) {
            logger.log(Level.INFO, "{0} started conversation", name);
            output.println(name);
            System.out.printf("Echo from server %s: %s\n", server.getRemoteSocketAddress(), input.readLine());

        } catch (IOException e) {
            logger.log(Level.WARNING, "I/O error in " + name, e);
        }
    }
}
