package com.gymapi.auth.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StreamUtils;

/**
 * The idempotency filter hashes the body before the handler runs, which drains the stream. This
 * wrapper is what lets the handler still read it.
 */
class CachedBodyHttpServletRequestTest {

  private static final byte[] BODY = "{\"name\":\"FRONT_DESK\"}".getBytes(StandardCharsets.UTF_8);

  @Test
  void theBodyCanBeReadAgainFromTheStream() throws Exception {
    CachedBodyHttpServletRequest request = wrap(BODY, null);

    assertThat(StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8))
        .isEqualTo("{\"name\":\"FRONT_DESK\"}");
    // And again: each call hands out a fresh stream over the same bytes.
    assertThat(StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8))
        .isEqualTo("{\"name\":\"FRONT_DESK\"}");
  }

  @Test
  void theBodyCanBeReadAgainFromTheReader() throws Exception {
    assertThat(wrap(BODY, null).getReader().readLine()).isEqualTo("{\"name\":\"FRONT_DESK\"}");
  }

  @Test
  void theReaderHonoursTheDeclaredEncoding() throws Exception {
    byte[] latin1 = "café".getBytes(StandardCharsets.ISO_8859_1);

    assertThat(wrap(latin1, "ISO-8859-1").getReader().readLine()).isEqualTo("café");
  }

  @Test
  void theStreamReportsWhenItIsDrained() throws Exception {
    ServletInputStream stream = wrap("ab".getBytes(StandardCharsets.UTF_8), null).getInputStream();

    assertThat(stream.isReady()).isTrue();
    assertThat(stream.isFinished()).isFalse();
    assertThat(stream.read()).isEqualTo('a');
    assertThat(stream.read()).isEqualTo('b');
    assertThat(stream.isFinished()).isTrue();
    assertThat(stream.read()).isEqualTo(-1);
  }

  @Test
  void anEmptyBodyIsHandled() throws Exception {
    assertThat(
            StreamUtils.copyToString(
                wrap(new byte[0], null).getInputStream(), StandardCharsets.UTF_8))
        .isEmpty();
  }

  @Test
  void asyncReadsAreRefusedRatherThanSilentlyDoingNothing() throws Exception {
    ServletInputStream stream = wrap(BODY, null).getInputStream();

    assertThatThrownBy(() -> stream.setReadListener(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static CachedBodyHttpServletRequest wrap(byte[] body, String encoding) {
    MockHttpServletRequest delegate = new MockHttpServletRequest("POST", "/auth/roles");
    if (encoding != null) {
      delegate.setCharacterEncoding(encoding);
    }
    return new CachedBodyHttpServletRequest(delegate, body);
  }
}
