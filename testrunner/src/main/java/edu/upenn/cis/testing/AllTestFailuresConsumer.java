package edu.upenn.cis.testing;

import edu.upenn.cis.testing.annotation.Q;
import org.junit.runner.notification.Failure;

import java.io.PrintWriter;

/**
 * Outputs all test failures.
 *
 * @author davix
 */
public class AllTestFailuresConsumer implements TestFailureConsumer {
    @Override
    public void accept(Q q, Failure failure, PrintWriter out) {
        out.printf("Failure (correct = %f points, incorrect = %f points): %s%n%s%n%s",
                q.points(), q.incorrect(), q.desc(), failure, failure.getTrace());
    }
}
