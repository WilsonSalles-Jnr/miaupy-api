package com.miaupy.business.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BusinessAddress(UUID id,Long tenantId,UUID businessId,String street,String number,String district,
                              String city,String state,String postalCode,BigDecimal latitude,BigDecimal longitude,
                              Instant createdAt,Instant updatedAt){
    public static BusinessAddress create(Long tenant,UUID business,String street,String number,String district,String city,String state,String postal,BigDecimal lat,BigDecimal lon){Instant now=Instant.now();return new BusinessAddress(UUID.randomUUID(),tenant,business,street,number,district,city,state,postal,lat,lon,now,now);}
    public BusinessAddress update(String street,String number,String district,String city,String state,String postal,BigDecimal lat,BigDecimal lon){return new BusinessAddress(id,tenantId,businessId,street,number,district,city,state,postal,lat,lon,createdAt,Instant.now());}
}
