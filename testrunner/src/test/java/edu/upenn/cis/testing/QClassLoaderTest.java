package edu.upenn.cis.testing;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * @author davix
 */
public class QClassLoaderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testLoadClassQSame() throws Exception {
        QClassLoader qClassLoader1 = new QClassLoader(getClass().getClassLoader());
        QClassLoader qClassLoader2 = new QClassLoader(getClass().getClassLoader());

        assertEquals(qClassLoader1.loadClass("edu.upenn.cis.testing.annotation.Q"),
                qClassLoader2.loadClass("edu.upenn.cis.testing.annotation.Q"));
    }

    @Test
    public void testLoadClassSame() throws Exception {
        QClassLoader qClassLoader1 = new QClassLoader(getClass().getClassLoader());
        QClassLoader qClassLoader2 = new QClassLoader(getClass().getClassLoader());

        assertEquals(qClassLoader1.loadClass("java.lang.Object"),
                qClassLoader2.loadClass("java.lang.Object"));
    }

    @Test
    public void testLoadClassNotSame() throws Exception {
        QClassLoader qClassLoader1 = new QClassLoader(getClass().getClassLoader());
        QClassLoader qClassLoader2 = new QClassLoader(getClass().getClassLoader());

        assertNotEquals(qClassLoader1.loadClass("edu.upenn.cis121.hw0.FakeClass"),
                qClassLoader2.loadClass("edu.upenn.cis121.hw0.FakeClass"));
    }

    @Test
    public void testLoadClass() throws Exception {
        QClassLoader qClassLoader1 = new QClassLoader(getClass().getClassLoader());

        thrown.expect(ClassNotFoundException.class);

        qClassLoader1.loadClass("edu.upenn.cis.testing.NonExistentClass");
    }
}