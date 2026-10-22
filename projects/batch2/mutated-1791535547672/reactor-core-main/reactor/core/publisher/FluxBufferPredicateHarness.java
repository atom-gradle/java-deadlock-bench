package reactor.core.publisher;

public class FluxBufferPredicateHarness {
    public static void main(String[] args) throws Exception {
        FluxBufferPredicate instance = new FluxBufferPredicate((Flux) null, (Predicate) null, (Supplier) null, (Mode) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_7211(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}