/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ibm.reactive.jpa.annotation.Integration;
import com.ibm.reactive.jpa.resources.Person;
import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@Integration
public class IntegrationTest {

  private Database simpleDatabase = TestUtil.getDatabase();

  @Test
  public void insert() {
    Person p1 = new Person("Person 1");

    Person p2 = new Person("Person 2");

    simpleDatabase
        .execute(entityManager -> {
          entityManager.persist(p1);
          entityManager.persist(p2);
          return p1;
        })
        .transaction(
            TransactionDefinition
                .builder()
                .isolation(IsolationLevel.SERIALIZABLE)
                .timeout(5)
                .build()
        )
        .mono()
        .block();
    int initialSize = TestUtil.getPersons().size();
    assertEquals(++initialSize, p1.getId());
    assertEquals(++initialSize, p2.getId());
  }

  @Test
  public void list() {
    Flux result = simpleDatabase.execute(entityManager -> {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<Person> criteria = builder.createQuery(Person.class);
      Root<Person> root = criteria.from(Person.class);
      criteria.select(root);
      criteria.orderBy(builder.asc(root.get("id")));
      return entityManager
          .createQuery(criteria)
          .setMaxResults(TestUtil.getPersons().size())
          .getResultList();
    }).flux();

    StepVerifier.create(result)
        .expectNextSequence(TestUtil.getPersons())
        .verifyComplete();
  }

  @Test
  public void testConnectionPool() throws Exception {
    int poolSize = ((ThreadPoolExecutor) simpleDatabase.getService()).getMaximumPoolSize();
    int count = poolSize * 4;
    StepVerifier.create(
        Flux.range(1, count)
            .flatMap(index ->
                simpleDatabase.execute(entityManager -> entityManager.find(Person.class, 1L))
                    .mono()
                    .subscribeOn(Schedulers.parallel()), 4))
        .expectNextSequence(Collections.nCopies(count, TestUtil.getPersons().get(0)))
        .verifyComplete();
  }

  @Test
  public void testConnectionPoolWithException() throws Exception {
    int poolSize = ((ThreadPoolExecutor) simpleDatabase.getService()).getMaximumPoolSize();
    int count = poolSize * 4;
    StepVerifier.create(
        Flux.range(1, count)
            .flatMap(index ->
                simpleDatabase.execute(entityManager -> {
                  throw new RuntimeException();
                })
                    .mono()
                    .subscribeOn(Schedulers.parallel()), 4))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  public void stream() {
    System.out.println("Stream");
    Flux<Person> result = simpleDatabase
        .stream("from PERSON person", Person.class)
        .fetchSize(1)
        .maxResults(TestUtil.getPersons().size())
        .isolationLevel(IsolationLevel.READ_COMMITTED)
        .flux();

    StepVerifier.create(result)
        .expectNextSequence(TestUtil.getPersons())
        .verifyComplete();
  }

  @Test
  public void streamSingle() {
    Person first = TestUtil.getPersons().get(0);
    Flux<Person> result = simpleDatabase
        .stream("from PERSON where name = :name and id = :id", Person.class)
        .fetchSize(1)
        .isolationLevel(IsolationLevel.READ_COMMITTED)
        .addParameter("name", first.getName())
        .addParameter("id", first.getId())
        .flux();

    StepVerifier.create(result)
        .expectNext(first)
        .verifyComplete();
  }

}
