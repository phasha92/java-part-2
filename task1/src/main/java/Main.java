import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final int MAX = 25;
    private static final Lock LOCK = new ReentrantLock();
    private static final Condition ODD = LOCK.newCondition();
    private static final Condition EVEN = LOCK.newCondition();
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private static final AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) {
        Thread odd = new Thread(printer("Odd Thread", true));
        Thread even = new Thread(printer("Even Thread", false));

        odd.start();
        even.start();
    }

    private static Runnable printer(String name, boolean isOdd) {
        return () -> {
            logger.log(Level.FINE, "{0} started. isOdd={1}", new Object[]{name, isOdd});
            while (counter.get() < MAX) {

                logger.log(Level.FINEST, "{0} is waiting for lock", name);
                LOCK.lock();
                logger.log(Level.FINER, "{0} captured the lock", name);

                try {
                    Condition current = isOdd ? ODD : EVEN;
                    Condition other = isOdd ? EVEN : ODD;
                    logger.log(Level.FINEST, "{0} is running | isOdd={1}", new Object[]{name, isOdd});

                    while (counter.get() <= MAX && isOdd() != isOdd) {
                        logger.log(Level.FINEST, "{0} going to await", name);
                        current.await();
                        logger.log(Level.FINEST, "{0} returned from await", name);
                    }

                    print(name);
                    counter.incrementAndGet();
                    other.signal();
                    logger.log(Level.FINEST, "{0} thread is running | isOdd={1}", new Object[]{name, isOdd});

                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, name + " interrupted - shutdown", e);
                    Thread.currentThread().interrupt();
                } finally {
                    LOCK.unlock();
                    logger.log(Level.FINER, "{0} released lock", name);
                }
            }
            logger.log(Level.FINE, "{0} finished", name);
        };
    }

    private static boolean isOdd() {
        return counter.get() % 2 != 0;
    }

    private static void print(String name) {
        System.out.println(name + " writes the number " + counter);
    }
}