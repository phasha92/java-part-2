public class MainTask2 {
    public static void main(String[] args) {
        
        try (EchoServer server = new EchoServer();
             Client client1 = new Client("Vasya");
             Client client2 = new Client("Petya")) {
            Thread serverThread = new Thread(server);
            serverThread.start();
            if (client1.connect("", 8080)) new Thread(client1).start();
            if (client2.connect("localhost", 8080)) new Thread(client2).start();
            serverThread.join();
        }
    }
}
