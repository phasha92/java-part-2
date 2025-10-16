import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EchoServer implements AutoCloseable, Runnable {
    private static final Logger logger = Logger.getLogger(EchoServer.class.getName());
    private final ServerSocket serverSocket = new ServerSocket(8080);
    private final String localName = serverSocket.getLocalSocketAddress().toString();

    public EchoServer() throws IOException {
        logger.log(Level.INFO, "created EchoServer {0} with duration 30 sec", localName);
    }

    @Override
    public void close() throws IOException {
        if (!serverSocket.isClosed()) serverSocket.close();
        logger.log(Level.INFO, "EchoServer {0} is closed", localName);
    }

    @Override
    public void run() {
        Thread shutdown = new Thread(() -> {
            logger.log(Level.INFO,"the shutdown-thread has been launched");
            try {
                Thread.sleep(30_000);
                logger.log(Level.INFO,"server shutdown");
                serverSocket.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "shutdown error", e);
            }
        });
        shutdown.start();

        while (!serverSocket.isClosed()) {
            try {
                logger.log(Level.INFO, "server waiting for client");
                Socket client = serverSocket.accept();
                logger.log(Level.INFO, "client connected: {0}", client.getRemoteSocketAddress());
                handle(client);
            } catch (IOException e) {
                if (serverSocket.isClosed()) break;
                logger.log(Level.WARNING,"echo-server error", e);
            }
        }
    }

    private static void handle(Socket client){
        try (BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter output = new PrintWriter(client.getOutputStream(), true)) {
            String line;

            while ((line = input.readLine()) != null ) {
                System.out.printf("Echo to client %s: %s \n", client.getRemoteSocketAddress(), line);
                output.println(line + "\r\n");
            }

        } catch (IOException e) {
            logger.log(Level.WARNING, "client handling error", e);
        }
    }
}
