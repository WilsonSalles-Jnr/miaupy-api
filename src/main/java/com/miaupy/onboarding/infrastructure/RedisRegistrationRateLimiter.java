package com.miaupy.onboarding.infrastructure;

import com.miaupy.onboarding.application.RegistrationRateLimiter;
import com.miaupy.onboarding.domain.RegistrationRateLimitExceededException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
class RedisRegistrationRateLimiter implements RegistrationRateLimiter {
  private static final long WINDOW_SECONDS = 3600;
  private static final DefaultRedisScript<Long> SCRIPT =
      new DefaultRedisScript<>(
          "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[2]) end; if n>tonumber(ARGV[1]) then return 0 else return 1 end",
          Long.class);

  private final StringRedisTemplate redis;
  private final int ipLimit;
  private final int emailLimit;
  private final byte[] keySecret;

  RedisRegistrationRateLimiter(
      StringRedisTemplate redis,
      @Value("${miaupy.onboarding.registration.ip-limit-per-hour}") int ipLimit,
      @Value("${miaupy.onboarding.registration.email-limit-per-hour}") int emailLimit,
      @Value("${miaupy.onboarding.registration.key-secret}") String keySecret) {
    this.redis = redis;
    this.ipLimit = ipLimit;
    this.emailLimit = emailLimit;
    this.keySecret = keySecret.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public void check(String remoteAddress, String normalizedEmail) {
    boolean ipAllowed = allowed("ip", remoteAddress, ipLimit);
    boolean emailAllowed = allowed("email", normalizedEmail, emailLimit);
    if (!ipAllowed || !emailAllowed) {
      throw new RegistrationRateLimitExceededException();
    }
  }

  private boolean allowed(String dimension, String value, int limit) {
    Long result =
        redis.execute(
            SCRIPT,
            List.of("onboarding:registration:" + dimension + ":" + hmac(value)),
            Integer.toString(limit),
            Long.toString(WINDOW_SECONDS));
    return Long.valueOf(1).equals(result);
  }

  private String hmac(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(keySecret, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to protect rate-limit key", exception);
    }
  }
}
