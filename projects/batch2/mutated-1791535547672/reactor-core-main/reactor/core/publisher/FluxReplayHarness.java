package reactor.core.publisher;

public class FluxReplayHarness {
    public static void main(String[] args) throws Exception {
        FluxReplay instance = new FluxReplay((CorePublisher) null, 0, 0L, null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_5629(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}