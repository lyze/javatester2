package edu.upenn.cis.testing.annotation.processing;

import edu.upenn.cis.testing.annotation.Q;
import org.junit.Test;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This processor performs compile-time checking of {@link Q} annotations.
 * <p>
 * <pre>
 * <code>
 * query = |describe tblScores|
 * Field         Type        Null Key Default           Extra
 * ------------- ----------- ---- --- ----------------- ------------------------
 * SubId         int(11)     NO   PRI
 * Problem       varchar(50) NO   PRI
 * Score         double      NO
 * ExtraCredit   tinyint(1)  NO       0
 * AutoScore     tinyint(1)  NO       0
 * ScoresModTime timestamp   NO       CURRENT_TIMESTAMP on update CURR_TIMESTAMP
 * </code>
 * </pre>
 *
 * @author davix
 */
@SupportedAnnotationTypes({"edu.upenn.cis.testing.annotation.Q"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class QProcessor extends AbstractProcessor {
    private static final int MAX_DESC_LENGTH = 50;

    private final Map<String, Element> descriptionsToElement;
    private final Map<TypeMirror, Map<Q.Type, Double>>
            totalPointsByTypePerClass;

    public QProcessor() {
        descriptionsToElement = new HashMap<>();
        totalPointsByTypePerClass = new HashMap<>();
    }

    private static String truncate(String desc) {
        return desc.substring(0, Math.min(MAX_DESC_LENGTH, desc.length()));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();
        processQAnnotations(roundEnv, messager);
        if (roundEnv.processingOver()) {
            printSummary(messager);
        }
        return false;
    }

    private void printSummary(Messager messager) {
        totalPointsByTypePerClass.forEach((typeMirror, pointsByType) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Q points summary for class %s:%n", typeMirror));
            pointsByType.forEach((type, total) ->
                    sb.append(String.format("  %s: %s%n", type, total)));
            messager.printMessage(Diagnostic.Kind.NOTE, sb);
        });
    }

    private void processQAnnotations(RoundEnvironment roundEnv,
                                     Messager messager) {
        // Note: A symmetric difference on two objects of type Set<? extends Element> from
        // getElementsAnnotatedWith gives a lint warning, because the compiler cannot prove that
        // the wildcard types are the same. Thus, we'll just do a for-loop.
        for (Element e : roundEnv.getElementsAnnotatedWith(Test.class)) {
            Q q = e.getAnnotation(Q.class);
            if (q == null) {
                // TODO: maybe we need an annotation on the class to suppress this warning?
                messager.printMessage(Diagnostic.Kind.WARNING, "Encountered an element annotated "
                        + "with @Test that was not also annotated with @Q.", e);
            }
        }

        for (Element e : roundEnv.getElementsAnnotatedWith(Q.class)) {
            Q q = e.getAnnotation(Q.class);
            Test test = e.getAnnotation(Test.class);
            if (test == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Encountered an element annotated "
                        + "with @Q that was not also annotated with @Test.", e);
            }

            if (q.points() < 0) {
                messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                        "Encountered a @Q with a negative value for 'points'.", e);
            }
            if (q.incorrect() > 0) {
                messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                        "Encountered a @Q with a positive value for 'incorrect'.", e);
            }

            String desc = q.desc();
            boolean currentTruncated;
            if (desc.length() > MAX_DESC_LENGTH) {
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "Encountered a @Q description longer than " + MAX_DESC_LENGTH
                                + " characters. Descriptions longer than that will be truncated "
                                + "in the database.", e);
                desc = truncate(desc);
                currentTruncated = true;
            } else {
                currentTruncated = false;
            }

            Element existing = descriptionsToElement.get(desc);
            if (existing == null) {
                descriptionsToElement.put(desc, e);
            } else { // found a description that was not unique
                Q existingQ = existing.getAnnotation(Q.class);
                boolean existingTruncated = !truncate(existingQ.desc()).equals(existingQ.desc());
                messager.printMessage(Diagnostic.Kind.ERROR,
                        currentTruncated && existingTruncated ?
                        "Encountered a duplicate Q description because both the current and "
                                + "existing descriptions will be truncated in the database." :
                        currentTruncated ?
                        "Encountered a duplicate Q description because the current description "
                                + "will be truncated in the database." :
                        existingTruncated ?
                        "Encountered a duplicate Q description because the existing description "
                                + "will be truncated in the database." :
                        "Encountered a duplicate Q description.",
                        e);
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Previous duplicate description was here.", existing);
            }

            totalPointsByTypePerClass.compute(findEnclosingClass(e), (typeMirror, pointsByType) -> {
                if (pointsByType == null) {
                    pointsByType = new HashMap<>(Q.Type.values().length);
                }
                pointsByType.merge(q.type(), q.points(), (old, v) -> old + v);
                return pointsByType;
            });
        }
    }

    private TypeMirror findEnclosingClass(Element e) {
        Element enc = e.getEnclosingElement();
        while (enc != null && !enc.getKind().isClass()) {
            enc = enc.getEnclosingElement();
        }
        if (enc == null) {
            return null;
        }
        return enc.asType();
    }
}