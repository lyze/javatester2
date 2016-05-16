package edu.upenn.cis.testing.annotation.processing;

import com.google.testing.compile.JavaFileObjects;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static org.junit.Assert.*;

/**
 * Schema:
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
public class QProcessorTest {

    // TODO: add more tests when com.google.compile-testing has assertions for non-error messages

    @Test
    public void testHelloWorld() throws Exception {
        JavaFileObject source = JavaFileObjects.forSourceLines("HelloWorld",
                "class HelloWorld {",
                "  public static void main(String[] args) { System.out.println(\"Hello world\"); }",
                "}");
        assert_().about(javaSource()).that(source)
                .processedWith(new QProcessor())
                .compilesWithoutError();
    }

    @Test
    public void testDuplicateDescriptionError() throws Exception {
        JavaFileObject source = JavaFileObjects.forSourceLines("DuplicateDescriptions",
                "import org.junit.Test;",
                "import edu.upenn.cis.testing.annotation.Q;",
                "",
                "class DuplicateDescriptions {",
                "  @Test",
                "  @Q(desc = \"seventeen\", points = 17)",
                "  public void test1() {}",
                "",
                "  @Test",
                "  @Q(desc = \"seventeen\", points = 17)",
                "  public void test2() {}",
                "}");
        assert_().about(javaSource()).that(source)
                .processedWith(new QProcessor())
                .failsToCompile()
                .withErrorContaining("duplicate").in(source).onLine(7).atColumn(15)
                .and()
                .withErrorContaining("duplicate").in(source).onLine(11).atColumn(15);
    }

    @Test
    public void testTestAnnotationMissingButQAnnotationPresent() throws Exception {
        JavaFileObject source = JavaFileObjects.forSourceLines("QTest",
                "import org.junit.Test;",
                "import edu.upenn.cis.testing.annotation.Q;",
                "",
                "class QTest {",
                "  @Q(desc = \"seventeen\", points = 17)",
                "  public void test1() {}",
                "",
                "  @Test",
                "  @Q(desc = \"eighteen\", points = 18)",
                "  public void test2() {}",
                "}");
        assert_().about(javaSource()).that(source)
                .processedWith(new QProcessor())
                .failsToCompile()
                .withErrorContaining("@Test").in(source).onLine(6).atColumn(15);
    }

    @Test
    public void testDuplicateDescriptionAfterTruncation1() throws Exception {
        JavaFileObject source = JavaFileObjects.forSourceLines("QTest",
                "import org.junit.Test;",
                "import edu.upenn.cis.testing.annotation.Q;",
                "",
                "class QTest {",
                "  @Test",
                "  @Q(desc = \"012345678901234567890123456789012345678901234567890\", points = 17)",
                "  public void test1() {}",
                "",
                "  @Test",
                "  @Q(desc = \"01234567890123456789012345678901234567890123456789\", points = 17)",
                "  public void test2() {}",
                "}");
        assert_().about(javaSource()).that(source)
                .processedWith(new QProcessor())
                .failsToCompile()
                .withErrorContaining("duplicate").in(source).onLine(7).atColumn(15)
                .and()
                .withErrorContaining("existing description will be truncated")
                .in(source).onLine(11).atColumn(15);
    }

    @Test
    public void testDuplicateDescriptionAfterTruncation2() throws Exception {
        JavaFileObject source = JavaFileObjects.forSourceLines("QTest",
                "import org.junit.Test;",
                "import edu.upenn.cis.testing.annotation.Q;",
                "",
                "class QTest {",
                "  @Test",
                "  @Q(desc = \"01234567890123456789012345678901234567890123456789\", points = 17)",
                "  public void test1() {}",
                "",
                "  @Test",
                "  @Q(desc = \"012345678901234567890123456789012345678901234567890\", points = 17)",
                "  public void test2() {}",
                "}");
        assert_().about(javaSource()).that(source)
                .processedWith(new QProcessor())
                .failsToCompile()
                .withErrorContaining("duplicate").in(source).onLine(7).atColumn(15)
                .and()
                .withErrorContaining("current description will be truncated")
                .in(source).onLine(11).atColumn(15);
    }

    @Test
    public void testDuplicateDescriptionAfterTruncation3() throws Exception {
        JavaFileObject source = JavaFileObjects.forSourceLines("QTest",
                "import org.junit.Test;",
                "import edu.upenn.cis.testing.annotation.Q;",
                "",
                "class QTest {",
                "  @Test",
                "  @Q(desc = \"012345678901234567890123456789012345678901234567890\", points = 17)",
                "  public void test1() {}",
                "",
                "  @Test",
                "  @Q(desc = \"012345678901234567890123456789012345678901234567890\", points = 17)",
                "  public void test2() {}",
                "}");
        assert_().about(javaSource()).that(source)
                .processedWith(new QProcessor())
                .failsToCompile()
                .withErrorContaining("duplicate").in(source).onLine(7).atColumn(15)
                .and()
                .withErrorContaining("current and existing descriptions will be truncated")
                .in(source).onLine(11).atColumn(15);
    }

}