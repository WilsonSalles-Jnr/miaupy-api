package com.miaupy.business.domain;

import java.time.Instant;

public record BusinessSettings(Long tenantId,AppointmentApprovalMode appointmentApprovalMode,String timezone,
                               String currency,boolean allowOnlineBooking,boolean allowOnlineSales,Instant createdAt,
                               Instant updatedAt){
    public static BusinessSettings create(Long tenant,AppointmentApprovalMode mode,String timezone,String currency,boolean booking,boolean sales){Instant now=Instant.now();return new BusinessSettings(tenant,mode,timezone,currency,booking,sales,now,now);}
    public BusinessSettings update(AppointmentApprovalMode mode,String timezone,String currency,boolean booking,boolean sales){return new BusinessSettings(tenantId,mode,timezone,currency,booking,sales,createdAt,Instant.now());}
}
