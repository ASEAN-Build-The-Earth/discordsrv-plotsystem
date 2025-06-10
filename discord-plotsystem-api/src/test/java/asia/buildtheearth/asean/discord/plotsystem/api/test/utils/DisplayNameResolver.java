package asia.buildtheearth.asean.discord.plotsystem.api.test.utils;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Resolve the test context's {@link DisplayName} annotation as the constructor's {@link String} parameter.
 *
 * <p>Test classes using this resolver may assume the testing parameter will result as the annotated value.
 * Nested test outside an annotated {@link ExtendWith} will not have the display name forwarded.</p>
 * <blockquote>{@snippet :
 * @Nested
 * @ExtendWith(DisplayNameResolver.class)
 * class TestCase {
 *     // Result in the test runner having display name
 *     // "Test Suite" forwarded to the name field
 *     static final String TEST_SUITE = "Test Suite";
 *
 *     @Nested @DisplayName(TEST_SUITE)
 *     class TestSuite extends TestSuite {
 *         protected String name;
 *         TestSuite(String name) { this.name = name; }
 *         @Test void testRunner() { Assertions.assertEquals(this.name, TEST_SUITE); }
 *     }
 * }}</blockquote>
 */
public class DisplayNameResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(@NotNull ParameterContext parameterContext,
                                     @NotNull ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext.getDisplayName() != null;
    }

    @Override
    public Object resolveParameter(@NotNull ParameterContext parameterContext,
                                   @NotNull ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext.getDisplayName();
    }
}
