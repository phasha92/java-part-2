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

        Path resources = Path.of("src/main/resources");
        Path tmp = Path.of("tmp");
        Path bucketsDir = tmp.resolve("buckets");
        Path mergedDir = tmp.resolve("merged");
        Path outDir = tmp.resolve("out");

        Coordinator coordinator = new Coordinator(new WordCount(), resources, bucketsDir, mergedDir, outDir, 2, 4);
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
