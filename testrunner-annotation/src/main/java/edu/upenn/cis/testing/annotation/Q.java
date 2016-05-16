package edu.upenn.cis.testing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that provides information about a question that is graded on the passing or failing of
 * a JUnit test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Q {

    /**
     * The description of the test question.
     *
     * @return the description of the test question.
     */
    String desc();

    /**
     * The points to be gained for a correct answer.
     *
     * @return the points to be gained for a passing test
     */
    double points();

    /**
     * The type of question, indicated by the enum {@link Type}.
     *
     * @return the type of test question
     */
    Type type() default Type.REGULAR;

    /**
     * The score (usually a negative value) for an incorrect question.
     *
     * @return the score for an incorrect question
     */
    double incorrect() default 0;

    enum Type {
        /**
         * Defines a regular-credit question.
         */
        REGULAR,

        /**
         * Defines an extra-credit question.
         */
        EXTRA_CREDIT
    }
}
