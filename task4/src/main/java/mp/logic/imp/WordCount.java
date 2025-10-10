package mp.logic.imp;

import mp.logic.WorkerLogic;
import mp.model.KeyValue;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WordCount implements WorkerLogic {
    private static final Logger logger = Logger.getLogger(WordCount.class.getName());

    @Override
    public List<KeyValue> map(String content) {
        logger.log(Level.FINE, "processing input text of {0} characters", content.length());

        return Arrays.stream(content.trim().split("\\s+"))
                .map(word -> word.replaceAll("[^\\p{L}\\p{N}]", ""))
                .filter(word -> !word.isEmpty())
                .map(word -> new KeyValue(word, "1"))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> reduce(List<KeyValue> keyValues) {
        logger.log(Level.FINE, "reducing {0} key-value pairs", keyValues.size());

        Map<String, List<String>> groups = new TreeMap<>((a, b) -> {
            int cmp = a.compareToIgnoreCase(b);
            return (cmp != 0) ? cmp : a.compareTo(b);
        });

        for (KeyValue kv : keyValues) {
            groups.computeIfAbsent(kv.key(), k -> new ArrayList<>()).add(kv.value());
        }

        logger.log(Level.FINER, "grouped into {0} unique keys", groups.size());

        List<String> result = new ArrayList<>();
        for (var entry : groups.entrySet()) {
            String key = entry.getKey();
            int count = entry.getValue().size();
            result.add(key + " " + count);
        }
        return result;
    }
}
