import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RingBufferTest {

    private RingBuffer<String> ringBuffer;

    @BeforeEach
    void setUp(){
        ringBuffer = new RingBuffer<>(4);
    }

    @AfterEach
    void tearDown(){
        if(!ringBuffer.isClosed()) ringBuffer.close();
    }

    @Test
    @DisplayName("created empty and with given capacity")
    void createdEmpty() {
        assertTrue(ringBuffer.isEmpty());
        assertFalse(ringBuffer.isFull());
        assertEquals(0, ringBuffer.getSize());
    }

    @Test
    @DisplayName("append increases size and returns current")
    void appendIncreasesSize() {
        ringBuffer.append("a");
        assertEquals(1, ringBuffer.getSize());
        assertEquals("a", ringBuffer.getCurrent());
    }

    @Test
    @DisplayName("FIFO order via absolute index")
    void fifoAbsolute() {
        ringBuffer.append("a");
        ringBuffer.append("b");
        ringBuffer.append("c");
        assertEquals("a", ringBuffer.getAbsolute(0));
        assertEquals("b", ringBuffer.getAbsolute(1));
        assertEquals("c", ringBuffer.getAbsolute(2));
    }

    @Test
    @DisplayName("overwrites oldest when full")
    void overwriteOldest() {
        Stream.of("a", "b", "c", "d").forEach(ringBuffer::append);
        ringBuffer.append("e");
        assertEquals(4, ringBuffer.getSize());
        assertEquals("b", ringBuffer.getAbsolute(0));
        assertEquals("e", ringBuffer.getAbsolute(3));
    }

    @Test
    @DisplayName("iterator returns elements in FIFO order")
    void iteratorFifo() {
        Stream.of("a", "b", "c").forEach(ringBuffer::append);
        List<String> out = new ArrayList<>();
        ringBuffer.forEach(out::add);
        assertEquals(List.of("a", "b", "c"), out);
    }

    @Test
    @DisplayName("remove decreases size and returns current value")
    void removeDecreasesSize() {
        ringBuffer.append("x");
        assertEquals("x", ringBuffer.remove());
        assertEquals(0, ringBuffer.getSize());
        assertNull(ringBuffer.remove());
    }

    @Test
    @DisplayName("close makes ringBufferfer unusable")
    void closeMakesUnusable() {
        ringBuffer.close();
        assertTrue(ringBuffer.isClosed());
        assertThrows(IllegalStateException.class, () -> ringBuffer.append("x"));
        assertThrows(IllegalStateException.class, () -> ringBuffer.remove());
        assertEquals(0, ringBuffer.getSize());
    }

    @Test
    @DisplayName("double close is idempotent")
    void doubleCloseIdempotent() {
        assertDoesNotThrow(() -> {
            ringBuffer.close();
            ringBuffer.close();
        });
        assertTrue(ringBuffer.isClosed());
    }

    @Test
    @DisplayName("isFull/isEmpty work correctly")
    void fullEmptyFlags() {
        assertTrue(ringBuffer.isEmpty());
        assertFalse(ringBuffer.isFull());
        Stream.of("a", "b", "c", "d").forEach(ringBuffer::append);
        assertTrue(ringBuffer.isFull());
        assertFalse(ringBuffer.isEmpty());
    }
}
