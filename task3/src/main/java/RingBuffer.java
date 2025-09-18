import java.util.Iterator;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RingBuffer<T> implements Iterable<T> {

    private final Logger logger = Logger.getLogger(RingBuffer.class.getName());
    private final int capacity;
    private int size = 0;
    private Node<T> current;
    private Node<T> head;

    public RingBuffer(int capacity) {
        if (capacity < 2) {
            logger.log(Level.WARNING, "attempt to create ring buffer with illegal capacity {0}", capacity);
            throw new IllegalArgumentException("the capacity should not be less than 2!");
        }
        this.capacity = capacity;
        logger.log(Level.INFO, "ring buffer created, capacity={0}", capacity);
    }

    public void append(T value) {
        logger.log(Level.FINE, "append value: {0}", value);
        Node<T> node = new Node<>(value);

        if (size == 0) {
            current = node;
            head = current;
            node.next = node.prev = node;
            size = 1;
            logger.log(Level.FINER, "first element appended");
            return;
        }

        Node<T> tail = head.prev;

        node.prev = tail;
        node.next = head;
        tail.next = node;
        head.prev = node;

        if (size < capacity) {
            size++;
            logger.log(Level.FINER, "element appended, new size={0}", size);
        } else {
            head = head.next;
            Node<T> oldHead = head.prev;
            oldHead.prev.next = oldHead.next;
            oldHead.next.prev = oldHead.prev;
            logger.log(Level.FINE, "buffer full, oldest element overwritten");
        }

        current = node;
    }

    public T remove() {
        if (size == 0) {
            logger.log(Level.WARNING, "remove() called on empty buffer");
            return null;
        }

        T removed = current.value;
        logger.log(Level.FINE, "remove element: {0}", removed);

        if (size == 1) {
            current = null;
            size = 0;
            logger.log(Level.FINER, "buffer became empty");
            return removed;
        }

        Node<T> prev = current.prev;
        Node<T> next = current.next;


        prev.next = next;
        next.prev = prev;

        if (current == head) {
            head = next;
        }

        current = next;
        size--;
        logger.log(Level.FINER, "element removed, new size={0}", size);
        return removed;
    }

    public T getCurrent() {
        return (current != null) ? current.value : null;
    }

    public T getRelative(int offset) {
        rotateNext(offset);
        T value = current.value;
        rotatePrev(offset);
        return value;
    }

    public void setRelative(int offset, T value) {
        rotateNext(offset);
        current.value = value;
        rotatePrev(offset);
    }

    public T getAbsolute(int offset) {
        rotateHeadNext(offset);
        T value = head.value;
        rotateHeadPrev(offset);
        return value;
    }

    public void setAbsolute(int offset, T value) {
        rotateHeadNext(offset);
        head.value = value;
        rotateHeadPrev(offset);
    }

    public void rotateNext() {
        rotate(1, false, true);
        logger.log(Level.FINEST, "rotate next");
    }

    public void rotateNext(int count) {
        logger.log(Level.FINE, "rotate next {0} step(s)", count);
        rotate(count, false, true);
    }

    public void rotatePrev() {
        rotate(1, false, false);
        logger.log(Level.FINEST, "rotate prev");
    }

    public void rotatePrev(int count) {
        logger.log(Level.FINE, "rotate prev {0} step(s)", count);
        rotate(count, false, false);
    }

    private void rotateHeadNext(int count) {
        rotate(count, true, true);
    }

    private void rotateHeadPrev(int count) {
        rotate(count, true, false);
    }

    private void rotate(int count, boolean head, boolean next) {
        if (size < 2 || count == 0) return;
        count %= size;
        if (count < 0) count += size;
        for (int i = 0; i < count; i++) {
            if (head) {
                this.head = (next) ? this.head.next : this.head.prev;
            } else {
                current = (next) ? current.next : current.prev;

            }
        }
    }

    public int getSize() {
        return size;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public T next() {
                return null;
            }
        };
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        Iterable.super.forEach(action);
    }

    private static class Node<T> {
        Node<T> prev, next;
        T value;

        Node(T value) {
            this.value = value;
        }
    }
}
