package tradeproceeds;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Loads events from every *.json file in a directory. File order is irrelevant to the result. */
public final class EventLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EventLoader() {}

    public static List<TradeEvent> loadDirectory(Path dir) throws IOException {
        List<TradeEvent> all = new ArrayList<>();
        if (!Files.isDirectory(dir)) return all;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
            for (Path f : files) all.addAll(loadFile(f));
        }
        return all;
    }

    public static List<TradeEvent> loadFile(Path file) throws IOException {
        return Arrays.asList(MAPPER.readValue(file.toFile(), TradeEvent[].class));
    }
}
