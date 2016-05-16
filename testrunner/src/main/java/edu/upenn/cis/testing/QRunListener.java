package edu.upenn.cis.testing;

import edu.upenn.cis.testing.annotation.Q;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * A {@code QRunListener} aggregates statistics about test methods annotated with the
 * {@link edu.upenn.cis.testing.annotation.Q Q} class. This class is thread-safe.
 */
@RunListener.ThreadSafe
public class QRunListener extends RunListener {
    private final PrintWriter output;
    private final PrintWriter sql;
    private final QLogFormatter logFormatter;
    private final MissingQHandler missingQHandler;
    private final TestFailureConsumer testFailureConsumer;

    // internal state
    private final Object updatePointsLock = new Object();

    private final ConcurrentMap<Class<?>, Map<Q.Type, Double>> pointsAvailableByTypePerClass;
    private final ConcurrentMap<Class<?>, ConcurrentMap<Q.Type, Double>> pointsEarnedByTypePerClass;

    // TODO: Doesn't work in obscure usage cases. For example, if there are overloaded test methods,
    // like if we used @Theory, or if there are multiple runs of the same test cases.
    private final ConcurrentMap<TestMethodId, QState> runningTests;

    private final ConcurrentLinkedQueue<Failure> otherFailures;

    public QRunListener(PrintWriter output, PrintWriter sql,
                        QLogFormatter logFormatter,
                        MissingQHandler missingQHandler,
                        TestFailureConsumer testFailureConsumer) {
        this.output = output;
        this.sql = sql;
        this.logFormatter = logFormatter;
        this.missingQHandler = missingQHandler;
        this.testFailureConsumer = testFailureConsumer;
        this.otherFailures = new ConcurrentLinkedQueue<>();

        pointsAvailableByTypePerClass = new ConcurrentHashMap<>();
        pointsEarnedByTypePerClass = new ConcurrentHashMap<>();
        runningTests = new ConcurrentHashMap<>();
    }

    private static <T, K> Map<K, Double> aggregateNestedMapOfDoubles(
            Map<T, ? extends Map<K, Double>> mapOfDoublesByType) {
        return mapOfDoublesByType.entrySet().stream()
                .map(Map.Entry::getValue).map(Map::entrySet).flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Double::sum));
    }

    private void updatePoints(Description description) {
        Class<?> testClass = description.getTestClass();
        pointsEarnedByTypePerClass.computeIfAbsent(testClass, c ->
                new ConcurrentHashMap<>(Q.Type.values().length));

        Map<Q.Type, Double> pointsByType = new HashMap<>(Q.Type.values().length);
        for (Method method : testClass.getMethods()) {
            Q q = method.getAnnotation(Q.class);
            if (q == null) {
                continue;
            }
            pointsByType.merge(q.type(), q.points(), Double::sum);
        }
        pointsAvailableByTypePerClass.computeIfAbsent(testClass, (c) -> pointsByType);
    }

    @Override
    public void testStarted(Description d) throws NoSuchMethodException {
        // TODO: JUnit seems to be breaking spec and calling this method multiple times.
        synchronized (updatePointsLock) {
            if (runningTests.containsKey(TestMethodId.create(d))) {
                return;
            }
            updatePoints(d);
            if (!d.isTest()) {
                return;
            }

            Class<?> c = d.getTestClass();
            Method m = c.getMethod(d.getMethodName());

            Q q = m.getAnnotation(Q.class);
            if (q == null) {
                if (missingQHandler != null) {
                    q = missingQHandler.annotationMissing(d);
                }
            }
            QState state = new QState(q);
            runningTests.put(TestMethodId.create(d), state);
        }
    }

    @Override
    public void testFailure(Failure f) {
        // this method is called before testFinished
        QState qState = runningTests.computeIfPresent(TestMethodId.create(f), (id, qState1) -> {
            qState1.failed = true;
            return qState1;
        });
        if (qState == null) {
            // this must be a failure that is NOT related to a test method
            otherFailures.add(f);
        } else {
            testFailureConsumer.accept(qState.q, f, output);
        }
    }

    @Override
    public void testFinished(Description d) throws IllegalStateException {
        QState qState = runningTests.remove(TestMethodId.create(d));
        if (qState == null) {
            return;
            // TODO: JUnit seems to be breaking spec and calling this method multiple times.
//            throw new IllegalStateException("not a running test: " + d);
        }
        Q q = qState.q;
        TestStatus status;
        if (qState.failed) {
            status = TestStatus.FAIL;
        } else {
            status = TestStatus.PASS;
        }
        sql.printf(logFormatter.format(q, status));

        double delta;
        switch (status) {
            case PASS:
                delta = q.points();
                break;
            case FAIL:
                delta = q.incorrect();
                break;
            default:
                throw new IllegalStateException("unhandled enum case");
        }
        ConcurrentMap<Q.Type, Double> earned = pointsEarnedByTypePerClass.get(d.getTestClass());
        earned.merge(q.type(), delta, java.lang.Double::sum);
    }

    @Override
    public void testRunFinished(Result r) {
        if (!otherFailures.isEmpty()) {
            output.println("Other failures:");
            otherFailures.forEach(f -> output.printf("%s%n%s", f, f.getTrace()));
        }

        if (r.wasSuccessful()) {
            output.println("All tests ran successfully!");
        } else {
            int total = r.getRunCount();
            // TODO: what do we do about ignored tests
            output.printf("Ran %d total test(s) with %d failure(s).%n", total, r.getFailureCount());
        }

        printSummary(output);
        output.flush();
    }

    // TODO: Refactor all printing to happen on listeners
    public void printSummary(PrintWriter output) {
        Map<Q.Type, Double> totalEarnedPointsByType = computeTotalPointsEarnedByType();
        Map<Q.Type, Double> totalAvailablePointsByType = computeTotalPointsAvailableByType();

        totalEarnedPointsByType.forEach((type, pointsEarned) ->
                output.printf("Total points (%s): %.1f out of %.1f.%n", type, pointsEarned,
                        totalAvailablePointsByType.get(type)));

        output.println("Points breakdown:");
        pointsEarnedByTypePerClass.forEach((clazz, pointsEarnedByType) ->
                pointsEarnedByType.forEach((type, points) ->
                        output.printf("  %s (%s): %.1f out of %.1f.%n", clazz, type, points,
                                pointsAvailableByTypePerClass.get(clazz).get(type))));
    }

    private Map<Q.Type, Double> computeTotalPointsAvailableByType() {
        return aggregateNestedMapOfDoubles(pointsAvailableByTypePerClass);
    }

    private Map<Q.Type, Double> computeTotalPointsEarnedByType() {
        return aggregateNestedMapOfDoubles(pointsEarnedByTypePerClass);
    }

    public Set<Q> getCurrentQs() {
        return Collections.unmodifiableSet(runningTests.values().stream().map(qState ->
                qState.q).collect(Collectors.<Q>toSet()));
    }

    public enum TestStatus {
        PASS, FAIL
    }

    @FunctionalInterface
    public interface MissingQHandler {
        /**
         * Handles what happens when a {@link Q} annotation is missing for a
         * test method.
         *
         * @param d the description of the test method being run
         * @return a substitute value for {@link Q}
         */
        Q annotationMissing(Description d);
    }

    private final static class TestMethodId {
        final Class<?> clazz;
        final String methodName;

        TestMethodId(Class<?> clazz, String methodName) {
            this.clazz = clazz;
            this.methodName = methodName;
        }

        static TestMethodId create(Description description) {
            return new TestMethodId(description.getTestClass(), description.getMethodName());
        }

        static TestMethodId create(Failure failure) {
            return create(failure.getDescription());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestMethodId that = (TestMethodId) o;
            return Objects.equals(clazz, that.clazz) && Objects.equals(methodName, that.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, methodName);
        }

        @Override
        public String toString() {
            return "TestMethodId{" +
                    "clazz=" + clazz +
                    ", methodName='" + methodName + '\'' +
                    '}';
        }
    }

    private static class QState {
        final Q q;
        boolean failed;

        QState(Q q) {
            this.q = q;
        }
    }
}
