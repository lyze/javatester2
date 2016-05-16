package edu.upenn.cis.testing;

import edu.upenn.cis.testing.annotation.Q;

/**
 * @author davix
 */
@FunctionalInterface
public interface QLogFormatter {
    String format(Q q, QRunListener.TestStatus status);
}
