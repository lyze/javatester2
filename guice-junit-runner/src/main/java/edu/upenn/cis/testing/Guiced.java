package edu.upenn.cis.testing;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import edu.upenn.cis.testing.annotation.GuiceModule;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * @author davix
 */
public class Guiced extends BlockJUnit4ClassRunner {
    private final Injector injector;

    public Guiced(Class<?> klass) throws InitializationError, IllegalAccessException,
            InstantiationException {
        super(klass);
        Class<?>[] guiceModuleClasses = getModuleClasses(klass);
        Module[] guiceModules = new Module[guiceModuleClasses.length];
        for (int i = 0; i < guiceModules.length; i++) {
            guiceModules[i] = (Module) guiceModuleClasses[i].newInstance();
        }
        injector = Guice.createInjector(guiceModules);
    }

    private static Class<?>[] getModuleClasses(Class<?> klass) {
        GuiceModule[] moduleAnnotations = klass.getAnnotationsByType(GuiceModule.class);
        Class<?>[] moduleClasses = new Class<?>[moduleAnnotations.length];
        for (int i = 0; i < moduleClasses.length; i++) {
            moduleClasses[i] = moduleAnnotations[i].value();
        }
        return moduleClasses;
    }

    @Override
    protected Object createTest() throws Exception {
        Object obj = super.createTest();
        injector.injectMembers(obj);
        return obj;
    }
}
