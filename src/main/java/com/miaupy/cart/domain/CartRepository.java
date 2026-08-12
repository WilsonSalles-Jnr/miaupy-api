package com.miaupy.cart.domain;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository {
  Cart save(Cart cart);

  Optional<Cart> findActiveByConsumerProfileId(UUID consumerProfileId);

  Optional<Cart> lockActiveByConsumerProfileId(UUID consumerProfileId);
}
