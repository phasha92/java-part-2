import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        Path path = Path.of("src/main/resources/MapReduce.txt");
        Worker worker = new Worker( path);
        worker.executeMap(0,8).forEach(System.out::println);
    }
}
