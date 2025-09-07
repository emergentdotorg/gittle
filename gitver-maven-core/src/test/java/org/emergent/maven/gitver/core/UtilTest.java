package org.emergent.maven.gitver.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.emergent.maven.gitver.core.Util.VERSION_REGEX;

class UtilTest {

  @ParameterizedTest
  @CsvSource({"refs/tags/v0.1.0,true", "refs/tags/v0.15.0,true", "v0.15.0,true", "0.15.0,true", "hoopla,false"})
  void matches(String tagName, boolean expected) {
    assertThat(VERSION_REGEX.matcher(tagName).matches()).isEqualTo(expected);
    assertThat(VERSION_REGEX.asMatchPredicate().test(tagName)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"3.1.2,3.1.2", "v0.15.0,v0.15.0", "refs/tags/v3.1.2,v3.1.2"})
  void extractTagName(String refName, String expectedTag) {
    Pattern pattern = VERSION_REGEX;
    assertThat(pattern.asMatchPredicate().test(refName)).isTrue();
    Matcher matcher = pattern.matcher(refName);
    assertThat(matcher.matches()).isTrue();
    String tag = matcher.group("tag");
    assertThat(tag).isEqualTo(expectedTag);
    String version = matcher.group("version");
    assertThat(version).isEqualTo(StringUtils.stripStart(expectedTag, "v"));
  }

  @ParameterizedTest
  @MethodSource("emptyStringProvider")
  void isNotBlank(String value, boolean result) {
    assertThat(Util.isNotBlank(value)).isEqualTo(result);
  }

  private static Stream<Arguments> emptyStringProvider() {
    return Stream.of(
      Arguments.of(null, false),
      Arguments.of("", false),
      Arguments.of("  ", false),
      Arguments.of("something", true));
  }

  @ParameterizedTest
  @CsvSource({"1,true", "0,true", "-1, false"})
  void assertNotNegative(Integer value, boolean valid) {
    Throwable ex = catchThrowable(() -> Util.assertNotNegative(value));
    if (valid) {
      assertThat(ex).isNull();
    } else {
      assertThat(ex)
        .isNotNull()
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(String.format("Number %s must be a non-negative integer", value));
    }
  }
}
