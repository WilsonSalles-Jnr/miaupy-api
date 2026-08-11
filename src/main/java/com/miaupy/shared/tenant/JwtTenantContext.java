package com.miaupy.shared.tenant;

import com.miaupy.shared.exception.TenantAccessDeniedException;
import com.miaupy.shared.security.ActorContext;
import com.miaupy.shared.security.ActorType;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtTenantContext implements TenantContext {

    private final ActorContext actorContext;

    public JwtTenantContext(ActorContext actorContext) {
        this.actorContext = actorContext;
    }

    @Override
    public Long getRequiredTenantId() {
        if (actorContext.getRequiredActor().type() != ActorType.B2B) {
            throw new TenantAccessDeniedException("A B2B actor is required");
        }

        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Object claim = jwt.getClaim("tenant_id");
        long tenantId = parseTenantId(claim);
        if (tenantId <= 0) {
            throw new TenantAccessDeniedException("JWT tenant_id must be a positive number");
        }
        MDC.put("tenantId", Long.toString(tenantId));
        return tenantId;
    }

    private long parseTenantId(Object claim) {
        try {
            if (claim instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(String.valueOf(claim));
        } catch (RuntimeException exception) {
            throw new TenantAccessDeniedException("JWT tenant_id must be a positive number");
        }
    }
}
