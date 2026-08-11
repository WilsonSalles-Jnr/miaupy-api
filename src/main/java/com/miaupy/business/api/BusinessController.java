package com.miaupy.business.api;

import com.miaupy.business.application.CreateBusinessUseCase;
import com.miaupy.business.application.GetBusinessProfileUseCase;
import com.miaupy.business.application.UpdateBusinessUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business/profile")
public class BusinessController {

    private final CreateBusinessUseCase createBusiness;
    private final GetBusinessProfileUseCase getBusinessProfile;
    private final UpdateBusinessUseCase updateBusiness;

    public BusinessController(CreateBusinessUseCase createBusiness, GetBusinessProfileUseCase getBusinessProfile,
                              UpdateBusinessUseCase updateBusiness) {
        this.createBusiness = createBusiness;
        this.getBusinessProfile = getBusinessProfile;
        this.updateBusiness = updateBusiness;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<BusinessResponse> create(@Valid @RequestBody BusinessRequest request) {
        BusinessResponse response = BusinessResponse.from(createBusiness.execute(request.toCommand()));
        return ResponseEntity.created(URI.create("/api/v1/business/profile")).body(response);
    }

    @GetMapping
    public BusinessResponse get() {
        return BusinessResponse.from(getBusinessProfile.execute());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public BusinessResponse update(@Valid @RequestBody BusinessRequest request) {
        return BusinessResponse.from(updateBusiness.execute(request.toCommand()));
    }
}
