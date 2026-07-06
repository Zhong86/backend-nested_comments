package com.example.NestedComments.util;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

  private static final int MAX_REQUESTS = 5; 
  private static final long WINDOW_SECONDS = 60;

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  public boolean allow(String key) {
    Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());

    synchronized (bucket) {
      Instant now = Instant.now(); 
      if (now.isAfter(bucket.windowStart.plusSeconds(WINDOW_SECONDS))) {
        bucket.windowStart = now; 
        bucket.count = 0;
      }
      if (bucket.count >= MAX_REQUESTS) {
        return false;
      }
      bucket.count++; 
      return true;
    }
  }

  private static class Bucket {
    Instant windowStart = Instant.now();
    int count = 0;
  }
}
