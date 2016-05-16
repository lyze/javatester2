package edu.upenn.cis.testing;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A parent-last classloader that will try the child classloader first and then the parent.
 * This takes a fair bit of doing because java really prefers parent-first.
 * <p>
 * For those not familiar with class loading trickery, be wary
 *
 * @author davix
 * @see <a href="http://stackoverflow.com/questions/5445511/how-do-i-create-a-parent-last-child-first-classloader-in-java-or-how-to-overr">http://stackoverflow.com/questions/5445511/how-do-i-create-a-parent-last-child-first-classloader-in-java-or-how-to-overr</a>
 */
public class ParentLastUrlClassLoader extends ClassLoader {
    private ChildURLClassLoader childClassLoader;

    public ParentLastUrlClassLoader(ClassLoader classLoader) {
        this(new URL[0], classLoader);
    }

    public ParentLastUrlClassLoader(URL[] urls) {
        this(urls, null);
    }

    public ParentLastUrlClassLoader(URL[] urls, ClassLoader classLoader) {
        super(classLoader);

        childClassLoader = new ChildURLClassLoader(urls, new FindClassClassLoader(getParent()));
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws
            ClassNotFoundException {
        Class<?> loaded = findLoadedClass(name);
        if (loaded == null) {
            try {
                // first we try to find a class inside the child classloader
                return childClassLoader.findClass(name);
            } catch (ClassNotFoundException e) {
                // didn't find it, try the parent
                return super.loadClass(name, resolve);
            }
        }
        if (resolve) {
            resolveClass(loaded);
        }
        return loaded;
    }

    /**
     * This class allows me to call findClass on a classloader
     */
    private static class FindClassClassLoader extends ClassLoader {

        public FindClassClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }
    }

    /**
     * This class delegates (child then parent) for the findClass method for a URLClassLoader.
     * We need this because findClass is protected in URLClassLoader
     */
    private static class ChildURLClassLoader extends URLClassLoader {
        private FindClassClassLoader realParent;

        public ChildURLClassLoader(URL[] urls, FindClassClassLoader realParent) {
            super(urls, null);
            this.realParent = realParent;
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                // first try to use the URLClassLoader findClass
                return super.findClass(name);
            } catch (ClassNotFoundException e) {
                // if that fails, we ask our real parent classloader to load the class (we give up)
                return realParent.findClass(name);
            }
        }

    }
}