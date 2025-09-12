import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    private static final int MAX = 25;
    private static final Lock LOCK = new ReentrantLock();
    private static final Condition ODD = LOCK.newCondition();
    private static final Condition EVEN = LOCK.newCondition();

    private static volatile int counter = 0;

    public static void main(String[] args) {
        Thread odd = new Thread(printer("Odd Thread", true));
        Thread even = new Thread(printer("Even Thread", false));

        odd.start();
        even.start();
    }

    private static Runnable printer(String name, boolean isOdd) {
        return () -> {
            while (true) {
                LOCK.lock();
                try {
                    Condition current = isOdd ? ODD : EVEN;
                    Condition other = isOdd ? EVEN : ODD;

                    while (counter <= MAX && isOdd() != isOdd) {
                        current.await();
                    }
                    if (counter > MAX) {
                        other.signal();
                        return;
                    }

                    print(name);
                    counter++;

                    other.signal();

                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    LOCK.unlock();
                }
            }
        };
    }

    private static boolean isOdd() {
        return counter % 2 != 0;
    }

    private static void print(String name) {
        System.out.println(name + " writes the number " + counter);
    }
}