package edu.upenn.cis.testing;

import org.apache.commons.cli.*;
import org.junit.runner.JUnitCore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author davix
 */
public class QRunner {

    private static final String DEFAULT_TIMEOUT_OPTION_VALUE = "30";
    private static final TimeUnit DEFAULT_TIMEOUT_TIMEUNIT = TimeUnit.SECONDS;

    private static void logError(String msg, Object... format) {
        System.err.println(">>> [" + Instant.now().toString() + "] "
                + String.format(msg, format));
    }

    private static void logWarning(String msg, Object... format) {
        logError(msg, format);
    }

    private static void log(String msg, Object... format) {
        System.out.println(">>> [" + Instant.now().toString() + "] "
                + String.format(msg, format));
    }

    public static void main(String[] args) throws AlreadySelectedException {
        Options options = new Options();
//        Option classpathOption = Option.builder()
//                .longOpt("classpath")
//                .hasArg()
//                .valueSeparator(':')
//                .desc("The classpath to search first.")
//                .build();

        Option timeoutOption = Option.builder()
                .longOpt("timeout")
                .hasArg()
                .type(Number.class)
                .argName("n")
                .desc("The maximum number of seconds for which the runner is allowed to execute")
                .build();

        Option allFailuresOption = Option.builder()
                .longOpt("all-failures")
                .desc("Shows information about all failures.")
                .build();
        Option firstFailureOption = Option.builder()
                .longOpt("first-failure")
                .desc("Shows information about only the first failure.")
                .build();
        OptionGroup failureDisplayOptionGroup = new OptionGroup();
        failureDisplayOptionGroup.addOption(firstFailureOption);
        failureDisplayOptionGroup.addOption(allFailuresOption);
        failureDisplayOptionGroup.setSelected(firstFailureOption);

//        options.addOption(classpathOption);
        options.addOption(timeoutOption);
        options.addOptionGroup(failureDisplayOptionGroup);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(QRunner.class.getCanonicalName() + ": " + e);
            System.exit(1);
            return;
        }

        List<String> argList = cmd.getArgList();
        if (argList.size() < 2) {
            System.err.printf("Usage: %s [scoresfile] [test1] [tests]...%n",
                    QRunner.class.getCanonicalName());
            System.exit(1);
        }

        String scoresFilename = argList.get(0);
        List<String> testClassNameList = argList.subList(1, argList.size());

//        String[] classpathStrings = classpathOption.getValues();
//        URL[] classpathUrls;
//        if (classpathStrings == null) {
//            classpathUrls = null;
//        } else {
//            classpathUrls = new URL[classpathStrings.length];
//            for (int i = 0; i < classpathUrls.length; i++) {
//                try {
//                    classpathUrls[i] = new URL(classpathStrings[i]);
//                } catch (MalformedURLException e) {
//                    e.printStackTrace();
//                    System.exit(1);
//                }
//            }
//        }

        long timeout;
        try {
            timeout = Long.parseLong(timeoutOption.getValue(DEFAULT_TIMEOUT_OPTION_VALUE));
        } catch (NumberFormatException e) {
            System.err.println("Invalid timeout: " + e);
            System.exit(1);
            return;
        }

        TestFailureConsumer testFailureConsumer;
        Option selectedFailureOption = options.getOption(failureDisplayOptionGroup.getSelected());
        if (selectedFailureOption == firstFailureOption) {
            testFailureConsumer = new FirstTestFailureConsumer();
        } else if (selectedFailureOption == allFailuresOption) {
            testFailureConsumer = new AllTestFailuresConsumer();
        } else {
            logWarning("Unmatched test failure printing strategy; "
                    + "defaulting to printing only the first test failure.");
            testFailureConsumer = new FirstTestFailureConsumer();
        }


        try (PrintWriter scoresWriter = new PrintWriter(Files.newBufferedWriter(
                Paths.get(scoresFilename)))) {

            QRunListener qrl = new QRunListener(
                    new PrintWriter(System.out),
                    scoresWriter,
                    new SqlFormatter(),
                    d -> {
                        System.err.printf("Missing annotation for %s in class %s%n",
                                d.getMethodName(), d.getClassName());
                        System.exit(1);
                        return null;
                    },
                    testFailureConsumer);

            ArrayList<Class<?>> testClasses = new ArrayList<>();
            List<String> notFound = new ArrayList<>();
            for (String testClassName : testClassNameList) {
                QClassLoader qClassLoader = new QClassLoader(// classpathUrls,
                        Thread.currentThread().getContextClassLoader());
                try {
                    Class<?> testClass = qClassLoader.loadClass(testClassName);
                    testClasses.add(testClass);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    notFound.add(testClassName);
                }
            }

            if (!notFound.isEmpty()) {
                logError("Cannot find tests: %s", notFound.stream()
                        .collect(Collectors.joining(" ")));
            }
            if (testClasses.isEmpty()) {
                log("No tests to run!");
            } else {
                log("Starting tests: %s", testClasses.stream()
                        .map(Class::getCanonicalName).collect(Collectors.joining(" ")));
            }

            JUnitCore core = new JUnitCore();
            core.addListener(qrl);
            ExecutorService executorService =
                    Executors.newCachedThreadPool(r -> {
                        Thread t = new Thread(r);
                        t.setDaemon(true);
                        t.setUncaughtExceptionHandler((t1, e) -> e.printStackTrace());
                        return t;
                    });
            GlobalTimeoutParallelComputer computer = GlobalTimeoutParallelComputer.classes(
                    executorService, timeout, DEFAULT_TIMEOUT_TIMEUNIT);
            core.run(computer, testClasses.toArray(new Class<?>[testClasses.size()]));

            log("Cleaning up...");
            executorService.shutdown();
            if (!executorService.isTerminated()) {  // sanity
                log("Cleaning up harder...");
                executorService.shutdownNow();
            }

            if (computer.hasTimedOut()) {
                logError("Exceeded timeout of %s %s. Running test(s): %s",
                        timeout, DEFAULT_TIMEOUT_TIMEUNIT, qrl.getCurrentQs());
                PrintWriter pw = new PrintWriter(System.out);
                qrl.printSummary(pw);
                pw.flush();
            }
            log("Executed of %s out of %s total test class files.",
                    computer.getNumCompletedClasses(),
                    computer.getNumTotalClasses());

        } catch (FileNotFoundException e) {
            logError("Cannot create output file: " + e);
            System.exit(1);
        } catch (IOException e) {
            logError(e.toString());
            System.exit(1);
        }

    }

}
