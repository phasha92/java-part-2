import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RingBuffer<T> implements Iterable<T> {

    private final Logger logger = Logger.getLogger(RingBuffer.class.getName());
    private final int capacity;
    private final AtomicReference<Node<T>> current = new AtomicReference<>();
    private final AtomicReference<Node<T>> head = new AtomicReference<>();
    private final AtomicInteger size = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();

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

        if (size.get() == 0) {
            node.next = node.prev = node;
            head.set(node);
            current.set(node);
            size.set(1);
            logger.log(Level.FINER, "first element appended");
            return;
        }

        Node<T> tail = head.get().prev;
        node.prev = tail;
        node.next = head.get();
        tail.next = node;
        head.get().prev = node;

        if (size.get() < capacity) {
            size.incrementAndGet();
            logger.log(Level.FINER, "element appended, new size={0}", size);
        } else {
            lock.lock();
            try {
            head.set(head.get().next);
            Node<T> oldHead = head.get().prev;
            oldHead.prev.next = oldHead.next;
            oldHead.next.prev = oldHead.prev;
            logger.log(Level.FINE, "buffer full, oldest element overwritten");
            } finally {
                lock.unlock();
            }
        }

        current.set(node);
    }

    public T remove() {
        if (size.get() == 0) {
            logger.log(Level.WARNING, "remove() called on empty buffer");
            return null;
        }

        Node<T> curSnap = current.get();
        T removed = curSnap.value;
        logger.log(Level.FINE, "remove element: {0}", removed);

        if (size.get() == 1) {
            current.set(null);
            head.set(null);
            size.set(0);
            logger.log(Level.FINER, "buffer became empty");
            return removed;
        }

        Node<T> prev = curSnap.prev;
        Node<T> next = curSnap.next;

        prev.next = next;
        next.prev = prev;

        if (curSnap == head.get()) {
            head.set(next);
        }

        current.set(next);
        size.decrementAndGet();
        logger.log(Level.FINER, "element removed, new size={0}", size);
        return removed;
    }

    public T getCurrent() {
        Node<T> current = this.current.get();
        return (current != null) ? current.value : null;
    }

    public T getRelative(int offset) {
        rotateNext(offset);
        T value = current.get().value;
        rotatePrev(offset);
        return value;
    }

    public void setRelative(int offset, T value) {
        rotateNext(offset);
        current.get().value = value;
        rotatePrev(offset);
    }

    public T getAbsolute(int offset) {
        rotateHeadNext(offset);
        T value = head.get().value;
        rotateHeadPrev(offset);
        return value;
    }

    public void setAbsolute(int offset, T value) {
        rotateHeadNext(offset);
        head.get().value = value;
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
        if (size.get() < 2 || count == 0) return;
        count %= size.get();
        if (count < 0) count += size.get();

        for (int i = 0; i < count; i++) {
            if (head) {
                Node<T> h = this.head.get();
                Node<T> nextNode = next ? h.next : h.prev;
                this.head.set(nextNode);
            } else {
                Node<T> c = current.get();
                Node<T> nextNode = next ? c.next : c.prev;
                current.set(nextNode);
            }
        }
    }

    public int getSize() {
        return size.get();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Node<T> last;
            private Node<T> pos = head.get();
            private int steps = 0;

            @Override
            public boolean hasNext() {
                return steps < size.get();
            }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                last = pos;
                T value = pos.value;
                pos = pos.next;
                steps++;
                return value;
            }
        };
    }


    private static class Node<T> {
        Node<T> prev, next;
        T value;

        Node(T value) {
            this.value = value;
        }
    }
}
