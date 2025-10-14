package mp.core;

import mp.logic.WorkerLogic;
import mp.model.KeyValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Worker implements Runnable {
    private static final Logger logger = Logger.getLogger(Worker.class.getName());
    private final Coordinator coordinator;
    private final WorkerLogic logic;

    public Worker(Coordinator coordinator, WorkerLogic logic) {
        this.coordinator = coordinator;
        this.logic = logic;
    }


    public void executeMap(Path inputFile, int bucketsCount) throws IOException {
        logger.log(Level.FINE, "starting map task for file: {0}", inputFile.getFileName());

        String content = Files.readString(inputFile).replace('\n', ' ');
        List<KeyValue> listKVs = logic.map(content);

        logger.log(Level.FINER,
                "distributing {0} key-value pairs into {1} buckets",
                new Object[]{listKVs.size(), bucketsCount});

        List<KeyValue>[] buckets = new List[bucketsCount];
        for (int i = 0; i < bucketsCount; i++) buckets[i] = new ArrayList<>();

        for (KeyValue kv : listKVs) {
            int bucket = Math.floorMod(kv.key().hashCode(), bucketsCount);
            buckets[bucket].add(kv);
        }

        Path bucketsDir = coordinator.getBucketsDir();
        Files.createDirectories(bucketsDir);
        String baseName = inputFile.getFileName().toString().replace(".txt", "");

        logger.log(Level.FINER, "writing bucket files for {0}", baseName);

        for (int b = 0; b < bucketsCount; b++) {
            Path outFile = bucketsDir.resolve("mr-" + baseName + "-" + b + ".txt");
            List<String> lines = buckets[b].stream()
                    .map(kv -> kv.key() + "\t" + kv.value())
                    .collect(Collectors.toList());

            Files.write(outFile, lines,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            logger.log(Level.FINE, "map task completed for file: {0}", inputFile.getFileName());
        }
    }

    public void executeReduce(int reduceId, Path bucketFile) throws IOException {
        logger.log(Level.FINE, "starting reduce task for bucket {0}", reduceId);

        List<KeyValue> keyValues = new ArrayList<>();
        for (String line : Files.readAllLines(bucketFile)) {
            String[] parts = line.split("\t", 2);
            if (parts.length >= 2) {
                keyValues.add(new KeyValue(parts[0], parts[1]));
            }
        }

        logger.log(Level.FINER, "parsed {0} key-value pairs from bucket file", keyValues.size());

        List<String> outLines = logic.reduce(keyValues);


        Path outDir = coordinator.getOutDir();
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("mr-out-" + reduceId + ".txt");
        Files.write(outFile, outLines,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        logger.log(Level.FINE, "reduce task completed for bucket {0}, wrote {1} lines",
                new Object[]{reduceId, outLines.size()});
    }

    @Override
    public void run() {
        logger.log(Level.INFO, "the worker started off in the {0}", Thread.currentThread().getName());

        try {
            Task task;
            do {
                task = coordinator.takeTask();

                if (task == null) {
                    logger.log(Level.FINE, "no task available, waiting");
                    if (coordinator.isFinished()) break;
                    Thread.sleep(10);
                    continue;
                }

                if (task instanceof MapTask mt) {
                    logger.log(Level.FINE, "processing map task for file: {0}", mt.inputFile().getFileName());
                    executeMap(mt.inputFile(), mt.bucketCount());
                    coordinator.onMapDone();
                    logger.log(Level.FINE, "map task completed for file: {0}", mt.inputFile().getFileName());

                } else if (task instanceof ReduceTask rt) {
                    logger.log(Level.FINE, "processing reduce task for bucket {0}", rt.id());
                    executeReduce(rt.id(), rt.bucketFile());
                    coordinator.onReduceDone();
                    logger.log(Level.FINE, "reduce task completed for bucket {0}", rt.id());
                }

            } while (task != ExitTask.getInstance());

            logger.log(Level.INFO, "received exit signal, shutting down {0}", Thread.currentThread().getName());

        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "worker interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            coordinator.reportWorkerError(e);
        }
    }
}