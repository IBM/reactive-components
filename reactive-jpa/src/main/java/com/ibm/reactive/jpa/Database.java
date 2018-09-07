/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa;

import com.github.fluent.hibernate.cfg.scanner.EntityScanner;
import com.ibm.reactive.jpa.implementation.DefaultExecution;
import com.ibm.reactive.jpa.implementation.DefaultStreamer;
import com.ibm.reactive.jpa.util.HibernateUtils;
import com.ibm.reactive.jpa.util.ReactiveUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javax.persistence.EntityManager;
import lombok.Getter;
import lombok.NonNull;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.service.ServiceRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Getter
public class Database {

  private SessionFactory sessionFactory;
  private ExecutorService service;


  public Database(Map<String, String> settings, List<String> resourcePackages) {
    initSessionFactory(settings, resourcePackages, PoolConfiguration.defaultConfiguration());
  }

  public Database(Map<String, String> settings, List<String> resourcePackages,
      PoolConfiguration configuration) {
    initSessionFactory(settings, resourcePackages, configuration);
  }

  public Database(SessionFactory sessionFactory, int maxPoolSize) {
    this.sessionFactory = sessionFactory;
    initializeExecutorService(maxPoolSize);
  }

  private void initSessionFactory(Map<String, String> settings, List<String> resourcePackages,
      PoolConfiguration configuration) {
    ServiceRegistry registry = HibernateUtils.createServiceRegistry(settings, configuration);
    MetadataSources sources = new MetadataSources(registry);
    List<Class<?>> classes = EntityScanner
        .scanPackages(resourcePackages.toArray(new String[resourcePackages.size()]))
        .result();
    classes.forEach(sources::addAnnotatedClass);
    sessionFactory = HibernateUtils.getSessionFactory(sources);
    initializeExecutorService(configuration.getMaxPoolSize());
  }

  private void initializeExecutorService(int maxPoolSize) {
    service = Executors.newFixedThreadPool(maxPoolSize);
  }

  public <T> ReactiveExecutionBuilder<T> execute(Function<EntityManager, T> function) {
    return new ReactiveExecutionBuilder<>(function, this);
  }

  public <T> StreamerBuilder<T> stream(String query, Class<T> type) {
    return new StreamerBuilder<>(this, query, type);
  }


  public static class StreamerBuilder<T> {

    private final Database database;
    private final String query;
    private IsolationLevel isolationLevel;
    private final Class<T> type;
    private int maxResults = -1;
    private int firstResult = -1;
    private int fetchSize = DefaultStreamer.DEFAULT_FETCH_SIZE;
    private final HashMap<String, Object> parameters = new HashMap<>();
    private List<Object> parameterList;

    private StreamerBuilder(Database database, String query, Class<T> type) {
      this.database = database;
      this.query = query;
      this.type = type;
    }

    public Flux<T> flux() {
      DefaultStreamer<T> streamer = DefaultStreamer.<T>builder()
          .type(type)
          .query(query)
          .sessionFactory(database.sessionFactory)
          .parameters(parameters)
          .parameterList(parameterList)
          .fetchSize(fetchSize)
          .maxResults(maxResults)
          .firstResult(firstResult)
          .isolationLevel(isolationLevel)
          .build();
      return ReactiveUtils.stream(database.service, streamer);
    }

    public StreamerBuilder<T> isolationLevel(IsolationLevel level) {
      this.isolationLevel = level;
      return this;
    }

    public StreamerBuilder<T> addParameter(@NonNull String name, Object value) {
      parameters.put(name, value);
      return this;
    }

    public StreamerBuilder<T> addParameters(@NonNull Map<String, Object> newParameters) {
      parameters.putAll(newParameters);
      return this;
    }

    public StreamerBuilder<T> parameterList(@NonNull List<Object> parameterList) {
      this.parameterList = parameterList;
      return this;
    }

    public StreamerBuilder<T> maxResults(int maxResults) {
      this.maxResults = maxResults;
      return this;
    }

    public StreamerBuilder<T> firstResult(int firstResult) {
      this.firstResult = firstResult;
      return this;
    }

    public StreamerBuilder<T> fetchSize(int fetchSize) {
      this.fetchSize = fetchSize;
      return this;
    }
  }

  public static class ReactiveExecutionBuilder<T> {

    private final Function<EntityManager, T> function;
    private final Database database;
    private TransactionDefinition transaction;

    private ReactiveExecutionBuilder(Function<EntityManager, T> function, Database database) {
      this.function = function;
      this.database = database;
    }

    public ReactiveExecutionBuilder<T> transaction(TransactionDefinition transaction) {
      this.transaction = transaction;
      return this;
    }

    public Mono<T> mono() {
      DefaultExecution<T> execution = new DefaultExecution<>(transaction, database.sessionFactory);
      return ReactiveUtils.execute(database.service, execution, function);
    }

    @SuppressWarnings("unchecked")
    public Flux flux() {
      Mono<T> mono = mono();
      return mono
          .map(t -> {
            if (t instanceof Iterable) {
              return (Iterable) t;
            }
            return Collections.singletonList(t);
          })
          .flatMapMany(Flux::fromIterable);
    }

  }


}
