package asia.buildtheearth.asean.discord.plotsystem.api.test.utils;

/**
 * Throwable exception to abort a test that happens to a mocked method that is yet to have implementation.
 *
 * <p>This is a {@link org.opentest4j.TestAbortedException} and causes your Test to be skipped instead of just failing.</p>
 *
 * <p>instead of returning placeholder values or failing your test, throw an {@link UnimplementedOperationException}.</p>
 * {@snippet : throw new UnimplementedOperationException(); }
 */
public class UnimplementedOperationException extends org.opentest4j.TestAbortedException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public UnimplementedOperationException() {
        super("Not implemented");
    }
}
