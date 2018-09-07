/*
 *  Copyright (c) IBM Corporation 2018. All Rights Reserved.
 *  Project name: reactive-components
 *  This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.reactive.jpa;

import com.ibm.reactive.jpa.resources.Person;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class TestUtil {

  private static final ArrayList<Person> persons = new ArrayList<>(5);
  private static final AtomicReference<Database> sharedDatabase = new AtomicReference<>();

  static {
    persons.add(new Person("Ailed"));
    persons.add(new Person("Alex"));
    persons.add(new Person("Efrain"));
    persons.add(new Person("Javier"));
    persons.add(new Person("Luis"));
    persons.add(new Person("Marie"));
    persons.add(new Person("Tomas"));
    persons.add(new Person("Ellen"));
    persons.add(new Person("Sarah"));
  }

  public static Database getDatabase() {
    Database database = sharedDatabase.get();
    if (database == null) {
      synchronized (sharedDatabase) {
        database = sharedDatabase.get();
        if (database != null) {
          return database;
        }

        Map<String, String> settings = new HashMap<>();
        settings.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        settings.put("hibernate.connection.url", "jdbc:hsqldb:mem:test");
        settings.put("hibernate.connection.username", "sa");
        settings.put("hibernate.show_sql", "true");
        settings.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        settings.put("hibernate.hbm2ddl.auto", "update");

        List<String> packages = new ArrayList<>(1);
        packages.add(Person.class.getPackage().getName());
        database = new Database(settings, packages);
        initializeDatabase(database);
        sharedDatabase.compareAndSet(null, database);
      }
    }

    return database;
  }

  private static void initializeDatabase(Database database) {
    Session session = database.getSessionFactory().openSession();
    Transaction tx = session.beginTransaction();
    getPersons().forEach(session::persist);
    tx.commit();
    session.close();
  }

  public static List<Person> getPersons() {
    return Collections.unmodifiableList(persons);
  }

}
