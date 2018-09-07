# reactive-components
Java reactive components

## How to use

Initialize
```java
Map<String, String> settings = new HashMap<>();
settings.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
settings.put("hibernate.connection.url", "jdbc:hsqldb:mem:test");
settings.put("hibernate.connection.username", "sa");
settings.put("hibernate.show_sql", "true");
settings.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
settings.put("hibernate.hbm2ddl.auto", "update");

List<String> packages = new ArrayList<>(1);
packages.add(Person.class.getPackage().getName()); // list of java packes with Entities
Database database = new Database(settings, packages);
```

Transaction

```java
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
```

Stream - Hibernate ScrollableResults 
```java
Flux<Person> result = simpleDatabase
    .stream("from PERSON where name = :name", Person.class)
    .fetchSize(10)
    .isolationLevel(IsolationLevel.READ_COMMITTED)
    .addParameter("name", person.getName())
    .flux();
```
