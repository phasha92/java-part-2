package mp.logic;

import mp.model.KeyValue;

import java.util.List;

public interface WorkerLogic {
    List<KeyValue> map(String content);
    List<String> reduce(List<KeyValue> keyValues);
}
