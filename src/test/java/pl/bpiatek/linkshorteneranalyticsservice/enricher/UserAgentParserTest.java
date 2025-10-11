package pl.bpiatek.linkshorteneranalyticsservice.enricher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SpringJUnitConfig
@ContextConfiguration(classes = {UserAgentConfig.class})
class UserAgentParserTest {

    @Autowired
    private UserAgentParser parser;

    @Test
    void shouldParseChromeOnWindows() {
        // given
        var userAgent = """
        Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15
        (KHTML, like Gecko) Version/26.0 Safari/605.1.15""";

        // when
        var result = parser.parse(userAgent);

        // then
        assertSoftly(s -> {
            s.assertThat(result.deviceType()).isEqualTo("Desktop");
            s.assertThat(result.os()).contains("Mac OS");
            s.assertThat(result.browser()).contains("Safari");
        });
    }

    @Test
    void shouldParseSafariOnIphone() {
        // given
        var userAgent = """
                Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15
                (KHTML, like Gecko) Version/18.6.2 Mobile/15E148 Safari/604.1""";

        // when
        var result = parser.parse(userAgent);

        assertSoftly(s -> {
            s.assertThat(result.deviceType()).isEqualTo("Phone");
            s.assertThat(result.os()).contains("iOS");
            s.assertThat(result.browser()).contains("Safari");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "nonsense", "Hacker" , "null" })
    void shouldReturnNullValuesWhenUserAgentMakesNoSense(String userAgent) {
        // when
        var parsedUserAgent = parser.parse(userAgent);

        // then
        assertSoftly(s -> {
            s.assertThat(parsedUserAgent.deviceType()).isNull();
            s.assertThat(parsedUserAgent.os()).isNull();
            s.assertThat(parsedUserAgent.browser()).isNull();
        });
    }

    @Test
    void shouldNotThrowExceptionWhenUserAgentNull() {
        // then
        assertThatCode(() -> parser.parse(null))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionWhenUserAgentEmpty() {
        // then
        assertThatCode(() -> parser.parse(""))
                .doesNotThrowAnyException();
    }
}