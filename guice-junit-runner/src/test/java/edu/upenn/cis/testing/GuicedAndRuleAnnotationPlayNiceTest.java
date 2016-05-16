package edu.upenn.cis.testing;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

/**
 * This test class should pass
 *
 * @author davix
 */
@RunWith(Guiced.class)
public class GuicedAndRuleAnnotationPlayNiceTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testGuiceModule() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        throw new IllegalArgumentException();
    }
}