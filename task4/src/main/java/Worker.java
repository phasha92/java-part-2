import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Worker implements Runnable {

    private final Path workDir;

    public Worker(Path workDir) {
        this.workDir = workDir;
    }

    private static List<KeyValue> map(String content) {


        return
    }

    public List<KeyValue> executeMap(int mapId, int reduceTasks) throws IOException {
        String content = Files.readString(workDir).replace('\n',' ');

        List<String> words = Arrays.stream(content.trim().split(" ")).toList();

        List<KeyValue> listKVs = words.stream()
                .map(word -> word.replaceAll("[^\\p{L}\\p{N}]+", ""))
                .filter(word -> !word.isEmpty())
                .map(word -> new KeyValue(word, "1"))
                .toList();

        List<KeyValue>[] buckets = new List[reduceTasks];
        for (int i = 0; i < reduceTasks; i++) buckets[i] = new ArrayList<>();

        for (KeyValue kv : listKVs) {
            int bucket = Math.floorMod(kv.key().hashCode(), reduceTasks);
            buckets[bucket].add(kv);
        }

        Path tmpDir = Path.of("tmp");
        Files.createDirectories(tmpDir);

        for (int b = 0; b < reduceTasks; b++) {
            Path outFile = tmpDir.resolve("mr-" + mapId + "-" + b);

            List<String> lines = buckets[b].stream()
                    .map(kv -> kv.key() + "\t" + kv.value())
                    .toList();

            Files.write(outFile, lines,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }

        return listKVs;
    }

    public void reduce(String key, List<String> values) {
        String.valueOf(values.size());
    }

    @Override
    public void run() {

    }
}
