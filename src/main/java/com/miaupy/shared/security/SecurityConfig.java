package com.miaupy.shared.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final SecurityProblemWriter problemWriter;

  public SecurityConfig(SecurityProblemWriter problemWriter) {
    this.problemWriter = problemWriter;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        "/api/v1/public/**",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            problemWriter.write(
                                response,
                                org.springframework.http.HttpStatus.UNAUTHORIZED,
                                "UNAUTHORIZED",
                                "Authentication required",
                                "A valid access token is required"))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            problemWriter.write(
                                response,
                                org.springframework.http.HttpStatus.FORBIDDEN,
                                "ACCESS_DENIED",
                                "Access denied",
                                "The authenticated actor is not allowed to perform this operation")))
        .oauth2ResourceServer(
            oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new ClaimsAuthoritiesConverter());
    converter.setPrincipalClaimName("sub");
    return converter;
  }

  private static final class ClaimsAuthoritiesConverter
      implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      List<GrantedAuthority> authorities = new ArrayList<>();
      addAuthorities(jwt.getClaimAsStringList("authorities"), "", authorities);
      addAuthorities(jwt.getClaimAsStringList("roles"), "ROLE_", authorities);
      return authorities;
    }

    private void addAuthorities(List<String> values, String prefix, List<GrantedAuthority> target) {
      if (values != null) {
        values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(value -> new SimpleGrantedAuthority(prefix + value))
            .forEach(target::add);
      }
    }
  }
}
