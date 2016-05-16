package edu.upenn.cis.testing;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@code QClassLoader} attempts to load all class in the {@code edu.upenn.cis121.hw*} package first, and delegates all other classes to the parent classloader.
 * to the parent classloader, but attempts to load all other classes by looking for the class
 * file using {@link Class#getResourceAsStream(String)}.
 *
 * @author davix
 */
public class QClassLoader extends ClassLoader {

    public QClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!name.startsWith("edu.upenn.cis121.hw")) {
            return super.loadClass(name, resolve);
        }

        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }

            InputStream r = getResourceAsStream(name.replace('.', '/').concat(".class"));
            if (r == null) {
                throw new ClassNotFoundException(name);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (BufferedInputStream in = r instanceof BufferedInputStream ?
                                          (BufferedInputStream) r : new BufferedInputStream(r)) {
                int b;
                while ((b = in.read()) != -1) {
                    baos.write(b);
                }
            } catch (IOException e) {
                throw new LoadClassError(name, e);
            }

            byte[] data = baos.toByteArray();
            return defineClass(name, data, 0, data.length);
        }
    }
}
