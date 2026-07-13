package cloud.poesis.itip.web.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

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
 */
class PrivilegeCodeParityTest {

  private static final Path MAIN_JAVA = Path.of("src", "main", "java");

  private static final String SEED_CSV = "db/changelog/data/structural-privileges.csv";

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
      return codes;
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
    Matcher calls = AUTHORITY_CALL.matcher(source);
    while (calls.find()) {
      Matcher literals = STRING_LITERAL.matcher(calls.group(1));
      while (literals.find()) {
        String value = literals.group(1) != null ? literals.group(1) : literals.group(2);
        codes.add(value.trim());
      }
    }
  }
}
