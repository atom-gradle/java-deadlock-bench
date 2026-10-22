package reactor.core.publisher;

public class FluxRefCountGraceHarness {
    public static void main(String[] args) throws Exception {
        FluxRefCountGrace instance = new FluxRefCountGrace(null, 0, null, null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_1347(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}