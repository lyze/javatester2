package edu.upenn.cis.testing;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import edu.upenn.cis.testing.annotation.GuiceModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * This test class should pass
 *
 * @author davix
 */
@RunWith(Guiced.class)
@GuiceModule(GuiceModuleTest.FooModule.class)
public class GuiceModuleTest {
    @Inject
    List<Integer> list;

    @Test
    public void testInjection() throws Exception {
        assertNotNull(list);
    }

    static class FooModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(List.class).to(ArrayList.class);
            bind(new TypeLiteral<List<Integer>>() {
            }).to(new TypeLiteral<ArrayList<Integer>>() {
            });
        }
    }
}