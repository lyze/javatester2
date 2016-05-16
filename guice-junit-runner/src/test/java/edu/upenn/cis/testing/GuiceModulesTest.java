package edu.upenn.cis.testing;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import edu.upenn.cis.testing.annotation.GuiceModule;
import org.junit.Test;
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
@GuiceModule(GuiceModulesTest.FooModule.class)
@GuiceModule(GuiceModulesTest.BarModule.class)
public class GuiceModulesTest {
    @Inject
    List<Integer> list;

    @Inject

    @Test
    public void testInjection() throws Exception {
        assertNotNull(list);
    }

    static class FooModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(List.class).to(ArrayList.class);
        }
    }

    static class BarModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(new TypeLiteral<List<Integer>>() {
            }).to(new TypeLiteral<ArrayList<Integer>>() {
            });
        }
    }
}