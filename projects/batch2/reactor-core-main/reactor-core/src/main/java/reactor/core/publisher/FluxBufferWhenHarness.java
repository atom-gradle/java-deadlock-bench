package reactor.core.publisher;

public class FluxBufferWhenHarness {
    public static void main(String[] args) throws Exception {
        FluxBufferWhen instance = new FluxBufferWhen(null, null, null, null, null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_1390(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}