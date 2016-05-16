package edu.upenn.cis.testing;

import edu.upenn.cis.testing.annotation.Q;
import org.junit.runner.notification.Failure;

import java.io.PrintWriter;

/**
 * Accepts a {@link Q} and a {@link Failure} for further processing. Subclasses must be thread-safe.
 *
 * @author davix
 */
public interface TestFailureConsumer {
    void accept(Q q, Failure failure, PrintWriter out);
}
