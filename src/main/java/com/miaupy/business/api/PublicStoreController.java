package com.miaupy.business.api;

import com.miaupy.business.application.GetPublicBusinessUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/stores")
public class PublicStoreController {

  private final GetPublicBusinessUseCase getPublicBusiness;

  public PublicStoreController(GetPublicBusinessUseCase getPublicBusiness) {
    this.getPublicBusiness = getPublicBusiness;
  }

  @GetMapping("/{slug}")
  public PublicStoreResponse getBySlug(@PathVariable String slug) {
    return PublicStoreResponse.from(getPublicBusiness.execute(slug));
  }
}
