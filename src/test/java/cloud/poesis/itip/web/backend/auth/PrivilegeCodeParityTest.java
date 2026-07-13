package cloud.poesis.itip.web.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * G3 anti-drift gate: enforces that every privilege-code string literal referenced by a Spring
 * method-security annotation ({@code @PreAuthorize} / {@code @PostAuthorize}) in {@code
 * src/main/java} is a member of the seed catalog's {@code code} column.
 *
 * <p>The seed CSV ({@code db/changelog/data/structural-privileges.csv}) is the single source of
 * truth; this test reads it at runtime rather than hardcoding a list, so the gate stays correct as
 * the catalog grows.
 *
 * <p><b>String-literal-only convention.</b> The gate only understands privilege codes written as
 * plain string literals inside {@code hasAuthority(...)} / {@code hasAnyAuthority(...)} SpEL
 * expressions (single- or double-quoted). Codes assembled dynamically (concatenation, constants,
 * method calls) are invisible to this deterministic source scan and are therefore disallowed by
 * convention — always write the literal code directly in the annotation.
 *
 * <p>There are currently no method-security annotations, so the "used" set is empty and the gate
 * passes trivially. It is armed for the first future gated endpoint: the moment someone gates on a
 * code that is not seeded, this test fails and lists the offending codes.
 *
 * <p><b>Fail-loud, never vacuous.</b> The gate is only meaningful if it can actually read the
 * inputs it scans. If the {@code src/main/java} source root cannot be located, or the seed CSV
 * cannot be found on the classpath, the test fails with a clear message instead of silently
 * treating "no sources found" as "no violations" — otherwise G3 would be silently disabled in any
 * environment where sources or seeds are unavailable at runtime.
 */
class PrivilegeCodeParityTest {

  private static final Path MAIN_JAVA = Path.of("src", "main", "java");

  private static final String SEED_CSV = "db/changelog/data/structural-privileges.csv";

  /**
   * Captures the string-expression argument passed to {@code @PreAuthorize(...)} /
   * {@code @PostAuthorize(...)} (optionally via {@code value = ...}). Extraction is intentionally
   * scoped to annotation payloads to avoid false positives from comments, constants, or unrelated
   * code.
   */
  private static final Pattern SECURITY_ANNOTATION_EXPRESSION =
      Pattern.compile(
          "@(?:PreAuthorize|PostAuthorize)\\s*\\(\\s*(?:value\\s*=\\s*)?"
              + "(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')\\s*\\)",
          Pattern.DOTALL);

  /**
   * Matches a {@code hasAuthority(...)} or {@code hasAnyAuthority(...)} call and captures its raw
   * argument list (everything up to the closing parenthesis).
   */
  private static final Pattern AUTHORITY_CALL =
      Pattern.compile("has(?:Any)?Authority\\s*\\(([^)]*)\\)");

  /** Matches a single- or double-quoted string literal and captures its content. */
  private static final Pattern STRING_LITERAL = Pattern.compile("'([^']*)'|\"([^\"]*)\"");

  @Test
  void everyAnnotatedPrivilegeCodeIsSeeded() throws IOException {
    Set<String> seededCodes = loadSeededCodes();
    Set<String> usedCodes = collectAnnotatedPrivilegeCodes();

    assertThat(usedCodes)
        .as(
            "privilege codes used in @PreAuthorize/@PostAuthorize must all be seeded in %s"
                + " (add the missing codes to the CSV seed)",
            SEED_CSV)
        .isSubsetOf(seededCodes);
  }

  @Test
  void extractionIgnoresHasAuthorityOutsideSecurityAnnotations() {
    String source =
        """
        package demo;

        class Demo {
          // hasAuthority('comment:only')
          static final String S = "hasAuthority('const:only')";

          @PreAuthorize(\"hasAuthority('from:annotation')\")
          void secured() {}
        }
        """;

    Set<String> codes = new LinkedHashSet<>();
    extractCodesFrom(source, codes);

    assertThat(codes).containsExactly("from:annotation");
  }

  private Set<String> loadSeededCodes() throws IOException {
    Set<String> codes = new LinkedHashSet<>();
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(SEED_CSV)) {
      assertThat(in).as("seed CSV %s must be on the classpath", SEED_CSV).isNotNull();
      String[] lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R", -1);
      boolean header = true;
      for (String line : lines) {
        String code = line.trim();
        if (code.isEmpty()) {
          continue;
        }
        if (header) {
          header = false;
          continue;
        }
        codes.add(code);
      }
    }
    return codes;
  }

  private Set<String> collectAnnotatedPrivilegeCodes() throws IOException {
    Set<String> codes = new LinkedHashSet<>();
    if (!Files.isDirectory(MAIN_JAVA)) {
      fail(
          "Cannot locate %s to scan for @PreAuthorize codes — G3 parity gate would be silently"
              + " disabled",
          MAIN_JAVA);
    }
    try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        if (!source.contains("PreAuthorize") && !source.contains("PostAuthorize")) {
          continue;
        }
        extractCodesFrom(source, codes);
      }
    }
    return codes;
  }

  private void extractCodesFrom(String source, Set<String> codes) {
    Matcher annotations = SECURITY_ANNOTATION_EXPRESSION.matcher(source);
    while (annotations.find()) {
      String expression = unquote(annotations.group(1));
      Matcher calls = AUTHORITY_CALL.matcher(expression);
      while (calls.find()) {
        Matcher literals = STRING_LITERAL.matcher(calls.group(1));
        while (literals.find()) {
          String value = literals.group(1) != null ? literals.group(1) : literals.group(2);
          codes.add(value.trim());
        }
      }
    }
  }

  private static String unquote(String quoted) {
    if (quoted.length() < 2) {
      return quoted;
    }
    return quoted.substring(1, quoted.length() - 1);
  }
}
