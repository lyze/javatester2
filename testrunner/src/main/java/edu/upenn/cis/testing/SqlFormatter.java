package edu.upenn.cis.testing;

import edu.upenn.cis.testing.annotation.Q;

/**
 * @author davix
 */
public class SqlFormatter implements QLogFormatter {

    @Override
    public String format(Q q, QRunListener.TestStatus status) {
        double points;
        switch (status) {
            case PASS:
                points = q.points();
                break;
            case FAIL:
                points = q.incorrect();
                break;
            default:
                throw new IllegalStateException("Missing enum case");
        }
        return String.format("%f|%d|%s%n",
                points, q.type() == Q.Type.EXTRA_CREDIT ? 1 : 0, q.desc());
    }
}
