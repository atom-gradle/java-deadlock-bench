package reactor.core.publisher;

public class DirectProcessorHarness {
    public static void main(String[] args) throws Exception {
        DirectProcessor instance = new DirectProcessor();
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_5582(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}