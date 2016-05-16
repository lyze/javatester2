package edu.upenn.cis.testing;

import edu.upenn.cis.testing.annotation.Q;
import org.junit.runner.notification.Failure;

import java.io.PrintWriter;

/**
 * Outputs only the first failure.
 *
 * @author davix
 */
public class FirstTestFailureConsumer implements TestFailureConsumer {

    boolean showedFailure;

    @Override
    public synchronized void accept(Q q, Failure failure, PrintWriter out) {
        if (showedFailure) {
            return;
        }
        out.printf("First failure (correct = %f points, incorrect = %f points): %s%n%s%n%s",
                q.points(), q.incorrect(), q.desc(), failure, failure.getTrace());
        showedFailure = true;
    }
}
