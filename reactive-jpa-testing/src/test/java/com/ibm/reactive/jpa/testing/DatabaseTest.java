/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.ibm.reactive.jpa.Database;
import com.ibm.reactive.jpa.IsolationLevel;
import com.ibm.reactive.jpa.TransactionDefinition;
import org.junit.jupiter.api.Test;

public class DatabaseTest {

  private static final String PACKAGE = DatabaseTest.class.getPackage().getName();

  @Test
  public void testInstances() {
    Database database = TestUtil.getDatabase(PACKAGE);
    assertNotNull(database);
    assertSame(database, TestUtil.getDatabase(PACKAGE));
  }

  @Test
  public void testConcurrentInstances() throws InterruptedException {
    TestUtil.removeDatabase(PACKAGE);
    Runnable r = () -> TestUtil.getDatabase(PACKAGE);
    Thread thread1 = new Thread(r);
    Thread thread2 = new Thread(r);

    thread1.start();
    thread2.start();

    thread1.join();
    thread2.join();

    assertNotNull(TestUtil.getDatabase(PACKAGE));
  }


  @Test
  public void removeTestInstances() {
    Database database = TestUtil.getDatabase(PACKAGE);
    assertNotNull(database);
    assertSame(database, TestUtil.getDatabase(PACKAGE));
    TestUtil.removeDatabase(PACKAGE);
    Database newDatabase = TestUtil.getDatabase(PACKAGE);
    assertNotNull(database);
    assertNotSame(database, newDatabase);
  }

  @Test
  public void testExecution() {
    Database database = TestUtil.getDatabase(PACKAGE);
    Event event = new Event();
    event.setType("tag");

    database.execute(entityManager -> {
      entityManager.persist(event);
      return event;
    })
        .transaction(TransactionDefinition.builder()
            .isolation(IsolationLevel.READ_COMMITTED).build())
        .mono()
        .block();

    Event result = database.stream("SELECT e from Event e", Event.class)
        .flux().blockFirst();
    assertEquals(event.getId(), result.getId());
  }
}
