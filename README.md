# reactive-components
Java reactive components

## reactive-jpa
Library that provide a wrapper for JDBC (Blocking API) with reactive components (Project Reactor) using Java Persistent API (Hibernate).

### How to use

#### Initialize
```java
Map<String, String> settings = new HashMap<>();
settings.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
settings.put("hibernate.connection.url", "jdbc:hsqldb:mem:test");
settings.put("hibernate.connection.username", "sa");
settings.put("hibernate.show_sql", "true");
settings.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
settings.put("hibernate.hbm2ddl.auto", "update");

List<String> packages = new ArrayList<>(1);
packages.add(Person.class.getPackage().getName()); // list of java packages with Entities
Database database = new Database(settings, packages);
```

#### Transaction

```java
Person p1 = new Person("Person 1");
Person p2 = new Person("Person 2");
Mono<Person> mono = database
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
    .mono();
```

#### Stream - Hibernate ScrollableResults
```java
Flux<Person> result = database
        .stream("from PERSON person", Person.class)
        .fetchSize(10)
        .maxResults(100)
        .firstResult(1)
        .isolationLevel(IsolationLevel.READ_COMMITTED)
        .flux();
```
[![Build Status](https://travis-ci.com/IBM/reactive-components.svg?branch=master)](https://travis-ci.com/IBM/reactive-components)
[![codecov](https://codecov.io/gh/IBM/reactive-components/branch/master/graph/badge.svg)](https://codecov.io/gh/IBM/reactive-components)
