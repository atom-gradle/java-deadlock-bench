package reactor.core.publisher;

public class FluxBufferWhenHarness {
    public static void main(String[] args) throws Exception {
        FluxBufferWhen instance = new FluxBufferWhen((Flux) null, (Publisher) null, (Function) null, null, (Supplier) null, (Supplier) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_1390(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}