package com.miaupy.business.api;

import com.miaupy.business.application.BusinessCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessRequest(
        @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "must contain lowercase letters, numbers and single hyphens only")
        String slug,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 160) String tradeName,
        @Size(max = 32) String document,
        @Size(max = 2000) String description,
        @Size(max = 32) String phone,
        @Email @Size(max = 254) String email,
        @Size(max = 500) String website,
        boolean publicVisible
) {
    BusinessCommand toCommand() {
        return new BusinessCommand(slug, name, tradeName, document, description, phone, email, website, publicVisible);
    }
}
