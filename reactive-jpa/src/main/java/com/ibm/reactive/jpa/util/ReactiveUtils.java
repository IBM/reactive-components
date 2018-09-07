/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.util;

import com.ibm.reactive.jpa.Execution;
import com.ibm.reactive.jpa.implementation.DefaultStreamer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import javax.persistence.EntityManager;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveUtils {

  private ReactiveUtils() {
  }

  public static <T> Mono<T> execute(@NonNull ExecutorService service,
      @NonNull Execution<T> execution,
      @NonNull Function<EntityManager, T> function) {
    return Mono.defer(() -> Mono.fromFuture(executeJpa(service, execution, function)));
  }

  private static <T> CompletableFuture<T> executeJpa(ExecutorService service,
      Execution<T> execution,
      Function<EntityManager, T> function) {
    CompletableFuture<T> result = new CompletableFuture<>();
    service.submit(() -> {
      try {
        T response = execution.execute(function);
        result.complete(response);
      } catch (Exception e) {
        result.completeExceptionally(e);
      }
    });
    return result;
  }

  public static <T> Flux<T> stream(@NonNull ExecutorService service,
      @NonNull DefaultStreamer<T> defaultStreamer) {
    return Flux.create(fluxSink -> service.execute(() -> defaultStreamer.stream(fluxSink)));
  }

}
