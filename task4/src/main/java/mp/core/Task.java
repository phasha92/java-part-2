package mp.core;

import java.nio.file.Path;

public sealed abstract class Task permits MapTask, ReduceTask, ExitTask {
}

final class MapTask extends Task {
    private final Path inputFile;
    private final int bucketCount;

    public MapTask(Path inputFile, int bucketCount) {
        this.inputFile = inputFile;
        this.bucketCount = bucketCount;
    }

    public Path inputFile() {
        return inputFile;
    }

    public int bucketCount() {
        return bucketCount;
    }
}

final class ReduceTask extends Task {
    private final int id;
    private final Path bucketFile;

    public ReduceTask(int id, Path bucketFile) {
        this.id = id;
        this.bucketFile = bucketFile;
    }

    public int id() {
        return id;
    }

    public Path bucketFile() {
        return bucketFile;
    }
}

final class ExitTask extends Task{
    private static ExitTask INSTANCE;
    private ExitTask(){}

    public static ExitTask getInstance(){
        if (INSTANCE == null){
            INSTANCE = new ExitTask();
        }
        return INSTANCE;
    }
}