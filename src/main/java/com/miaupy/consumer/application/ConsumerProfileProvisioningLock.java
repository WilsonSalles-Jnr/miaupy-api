package com.miaupy.consumer.application;

public interface ConsumerProfileProvisioningLock {
  void lock(String authSubject);
}
