package com.gymapi.auth.adapter.in.web.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Replays an already-consumed request body.
 *
 * <p>{@link IdempotencyFilter} has to hash the body before the handler runs, which drains the
 * stream. Spring's {@code ContentCachingRequestWrapper} caches on the way past and so is no help
 * here — it only has the bytes once something downstream has read them.
 */
final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

  private final byte[] body;

  CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
    super(request);
    this.body = body;
  }

  @Override
  public ServletInputStream getInputStream() {
    ByteArrayInputStream source = new ByteArrayInputStream(body);
    return new ServletInputStream() {

      @Override
      public boolean isFinished() {
        return source.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("Async reads are not supported on a cached body");
      }

      @Override
      public int read() {
        return source.read();
      }
    };
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(getInputStream(), charset()));
  }

  private Charset charset() {
    String encoding = getCharacterEncoding();
    return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
  }
}
