import mp.core.Coordinator;
import mp.logic.imp.WordCount;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        tmpClean();

        Path path = Path.of("src/main/resources");
        Coordinator coordinator = new Coordinator(new WordCount(), path, 2, 4);
        coordinator.run();
    }

    static void tmpClean() throws IOException {
        Path tmpDir = Path.of("tmp");
        if (Files.exists(tmpDir)) {
            try (var paths = Files.walk(tmpDir)) {
                paths
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        }
    }
}
