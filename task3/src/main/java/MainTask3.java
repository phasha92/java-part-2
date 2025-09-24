public class MainTask3 {
    public static void main(String[] args) {
        RingBuffer<TestObject> ring = new RingBuffer<>(10);
        System.out.println(ring.getSize());
        ring.rotateNext();

        ring.append(new TestObject("first"));
        ring.append(new TestObject("second"));
        ring.append(new TestObject("third"));
        ring.append(new TestObject("four"));

        System.out.println(ring.getAbsolute(0));
        System.out.println(ring.getAbsolute(1));
        System.out.println(ring.getAbsolute(2));
        System.out.println(ring.getSize());

        System.out.println(ring.getRelative(0));
        System.out.println(ring.getRelative(1));
        System.out.println(ring.getRelative(2));
        System.out.println("-------111--------");
        System.out.println(ring.getAbsolute(0));
        System.out.println(ring.getAbsolute(1));
        System.out.println(ring.getAbsolute(2));
        System.out.println("-------2-----------");
        ring.rotatePrev(4);
        System.out.println();
        System.out.println(ring.getRelative(0));
        System.out.println(ring.getRelative(1));
        System.out.println(ring.getRelative(2));
        System.out.println("-------3-----------");
        System.out.println(ring.getAbsolute(0));
        System.out.println(ring.getAbsolute(1));
        System.out.println(ring.getAbsolute(2));
        System.out.println("-------4-----------");
        System.out.println(ring.getCurrent());
        System.out.println(ring.remove());
        System.out.println(ring.remove());
        System.out.println(ring.remove());
        System.out.println(ring.remove());
        System.out.println(ring.getSize());
        System.out.println(ring.remove());
        System.out.println(ring.getCurrent());
        System.out.println(ring.getSize());
        System.out.println("---------5---------");
        ring.append(new TestObject("first"));
        ring.append(new TestObject("second"));
        ring.append(new TestObject("third"));
        ring.append(new TestObject("four"));
        System.out.println(ring.getSize());
        System.out.println(ring.getAbsolute(0));
        System.out.println(ring.getAbsolute(1));
        System.out.println(ring.getAbsolute(2));
        ring.rotateNext();
        System.out.println(ring.getRelative(0));
        System.out.println(ring.getRelative(1));

        System.out.println(ring.getRelative(2));

        for (TestObject t : ring) {
            System.out.println(t + " <----");
        }
    }
}
