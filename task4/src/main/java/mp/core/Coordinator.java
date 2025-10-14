package mp.core;

import mp.logic.WorkerLogic;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Coordinator {
    private static final Logger logger = Logger.getLogger(Coordinator.class.getName());
    private final int numBuckets;
    private final int numWorkers;
    private final WorkerLogic logic;
    private final Queue<Path> mapQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ReduceTask> reduceQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger remainingMaps;
    private final AtomicInteger remainingReduces;
    private final ReentrantLock reduceLock = new ReentrantLock();
    private final ExecutorService executor;
    private final Path bucketsDir;
    private final Path mergedDir;
    private final Path outDir;
    private volatile boolean reducePhaseStarted = false;

    public Coordinator(WorkerLogic logic, Path inputDir, Path bucketsDir, Path mergedDir, Path outDir, int numWorkers,
                       int numBuckets) throws IOException {
        this.logic = logic;
        this.numBuckets = numBuckets;
        this.numWorkers = numWorkers;
        this.bucketsDir = bucketsDir;
        this.mergedDir = mergedDir;
        this.outDir = outDir;

        try (Stream<Path> files = Files.list(inputDir)) {
            files.filter(Files::isRegularFile).forEach(mapQueue::add);
        }

        this.remainingMaps = new AtomicInteger(mapQueue.size());
        this.remainingReduces = new AtomicInteger(numBuckets);
        this.executor = Executors.newFixedThreadPool(numWorkers);
    }

    public Path getBucketsDir() {
        return bucketsDir;
    }

    public Path getOutDir() {
        return outDir;
    }

    private void mergeBuckets() throws IOException {
        logger.log(Level.FINE, "starting bucket merging phase");

        Files.createDirectories(mergedDir);

        for (int b = 0; b < numBuckets; b++) {
            final int bucketId = b;
            Path mergedFile = mergedDir.resolve("merged-" + bucketId + ".txt");
            List<String> allLines = new ArrayList<>();

            try (Stream<Path> files = Files.list(bucketsDir)) {
                List<Path> bucketFiles = files
                        .filter(p -> {
                            String name = p.getFileName().toString();
                            return name.endsWith("-" + bucketId + ".txt");
                        })
                        .toList();

                logger.log(Level.FINER, "found {0} files for bucket {1}",
                        new Object[]{bucketFiles.size(), bucketId});

                for (Path file : bucketFiles) {
                    allLines.addAll(Files.readAllLines(file));
                }
            }

            logger.log(Level.FINER, "writing merged file for bucket {0} with {1} lines",
                    new Object[]{bucketId, allLines.size()});

            Files.write(mergedFile, allLines);
            reduceQueue.add(new ReduceTask(bucketId, mergedFile));
        }
        logger.log(Level.FINE, "bucket merging completed for {0} buckets", numBuckets);
    }

    public Task takeTask() {
        logger.log(Level.FINEST, "worker requesting a new task");

        Path inputFile = mapQueue.poll();
        if (inputFile != null) {
            logger.log(Level.FINER, "assigned map task for file: {0}", inputFile.getFileName());
            return new MapTask(inputFile, numBuckets);
        }

        if (reducePhaseStarted) {
            ReduceTask rt = reduceQueue.poll();
            if (rt != null) {
                logger.log(Level.FINER, "assigned reduce task for bucket {0}", rt.id());
                return rt;
            }
        }

        if (remainingMaps.get() == 0 && !reducePhaseStarted) {
            reduceLock.lock();
            try {
                if (!reducePhaseStarted) {
                    logger.log(Level.FINE, "all map tasks done, starting reduce phase");
                    mergeBuckets();
                    reducePhaseStarted = true;
                    logger.log(Level.FINE, "reduce phase initialized");
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "failed to merge buckets", e);
                throw new UncheckedIOException(e);
            } finally {
                reduceLock.unlock();
            }
            return reduceQueue.poll();
        }

        if (remainingMaps.get() == 0 && reducePhaseStarted && reduceQueue.isEmpty()) {
            logger.log(Level.FINE, "all work completed, issuing exit signal");
            return ExitTask.getInstance();
        }

        logger.log(Level.FINEST, "no task available at the moment");
        return null;
    }

    public void onMapDone() {
        remainingMaps.decrementAndGet();
    }

    public void onReduceDone() {
        remainingReduces.decrementAndGet();
    }

    public boolean isFinished() {
        return remainingMaps.get() == 0 && remainingReduces.get() == 0 && reduceQueue.isEmpty();
    }

    public void run() throws InterruptedException {
        logger.log(Level.INFO, "starting coordinator with {0} workers and {1} buckets",
                new Object[]{numWorkers, numBuckets});

        for (int i = 0; i < numWorkers; i++) {
            executor.submit(new Worker(this, logic));
        }

        logger.log(Level.INFO, "all workers submitted, awaiting completion");

        while (!isFinished()) {
            Thread.sleep(50);
        }

        logger.log(Level.INFO, "all tasks completed, shutting down worker pool");

        executor.shutdown();
        if (!executor.awaitTermination(50, TimeUnit.SECONDS)) {
            logger.log(Level.WARNING, "worker pool did not terminate gracefully, forcing shutdown");
            executor.shutdownNow();
        } else {
            logger.log(Level.INFO, "worker pool shut down successfully");
        }
    }
}