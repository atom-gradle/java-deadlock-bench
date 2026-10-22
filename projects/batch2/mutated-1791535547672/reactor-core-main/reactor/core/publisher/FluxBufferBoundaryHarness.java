package reactor.core.publisher;

public class FluxBufferBoundaryHarness {
    public static void main(String[] args) throws Exception {
        FluxBufferBoundary instance = new FluxBufferBoundary((Flux) null, (Publisher) null, (Supplier) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_6948(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}