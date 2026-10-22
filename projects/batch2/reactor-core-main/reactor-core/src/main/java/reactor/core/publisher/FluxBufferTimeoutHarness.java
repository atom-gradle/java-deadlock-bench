package reactor.core.publisher;

public class FluxBufferTimeoutHarness {
    public static void main(String[] args) throws Exception {
        FluxBufferTimeout instance = new FluxBufferTimeout(null, 0, 0L, null, null, null, false);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_7429(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}