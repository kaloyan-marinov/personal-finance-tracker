# introduction

`PersonalFinanceTracker` is a web application for tracking one's income, expenses, and wealth.

Problem:
A typical person lacks an easy way of computing her/his net worth.
That is partly due to the fact that
one's income, expenses, and wealth are scattered across
multiple bank accounts, investment accounts, retirement savings accounts, etc.

`PersonalFinanceTracker` provides a one-stop shop for
recording (the annual values of) one's income, expenses, and assets,
as well as computing one's net worth (as of a given year).



# run the test suite

```shell
$ ./mvnw test
```

To run a single test case (with more logging output):

```shell
# The Surefire plugin passes `-D` properties on to the test JVM
# (and Spring Boot reads them, so the `logging.level.*` overrides apply).
#
# `-q` hides Maven's own output but not your application's log output.
$ ./mvnw \
    -q \
    test \
    -Dtest=TransactionControllerTests#create_1 \
    -Dlogging.level.org.springframework.security=DEBUG \
    -Dlogging.level.org.springframework.web=TRACE \
    -Dlogging.level.org.springframework.test=DEBUG
```

```shell
# Maven reads the whole command line before it runs anything.
# It collects every `-q` and `-D...` into its settings
# and every bare word into the list of things to run.
# A bare word can be a lifecycle phase (`test`, `clean`, `package` and so on)
# or a plugin goal (`dependency:tree` and so on).
#
# So, the order of flags relative to `test` doesn't matter,
# which means that the preceding command and the following command run identically.
#
# (
# As an FYI:
#
# A commonly used pattern is `tool [options] goal`.
# With the goal alone on the last line,
# the thing being run is easy to spot,
# and each line above it is one flag you can add, remove or comment out.
# )
$ ./mvnw \
    -q \
    -Dtest=TransactionControllerTests#create_1 \
    -Dlogging.level.org.springframework.security=DEBUG \
    -Dlogging.level.org.springframework.web=TRACE \
    -Dlogging.level.org.springframework.test=DEBUG \
    test
```



# configure

```shell
$ cp \
    .env.template \
    .env

# Edit the newly-created file according to the instructions therein.
```



# create an empty database

```shell
$ podman run \
    --name container-pft-mysql \
    --mount source=volume-pft-mysql,destination=/var/lib/mysql \
    --env-file .env \
    --publish $(sed -n 's/^MYSQL_PORT=//p' .env):$(sed -n 's/^MYSQL_PORT=//p' .env) \
    mysql:8.4.10 \
    --character-set-server=utf8mb4 \
    --collation-server=utf8mb4_bin \
    --mysql-native-password=ON
```

```shell
$ podman container exec \
    -it container-pft-mysql \
    /bin/bash

bash-5.1# mysql \
    -u pft-u \
    -p \
    pft-db
Enter password: <enter-the-requested-password>

mysql> SHOW TABLES;
Empty set (0.00 sec)
```



# apply the database migrations

```shell
$ (
    set -a ;

    source .env ;

    ./mvnw -q clean compile liquibase:update \
        -Dliquibase.changeLogFile=db/changelog/db.changelog-master.mysql.sql ;
)

UPDATE SUMMARY
Run:                          6
Previously run:               0
Filtered out:                 0
-------------------------------
Total change sets:            6
```

```sql
mysql> SHOW TABLES;
+-----------------------+
| Tables_in_pft-db      |
+-----------------------+
| DATABASECHANGELOG     |
| DATABASECHANGELOGLOCK |
| exchange_rates        |
| transactions          |
+-----------------------+
4 rows in set (0.00 sec)

mysql> select ID, FILENAME, DATEEXECUTED from DATABASECHANGELOG;
+-----------------+--------------------------------------------+---------------------+
| ID              | FILENAME                                   | DATEEXECUTED        |
+-----------------+--------------------------------------------+---------------------+
| 1790882019121-1 | db/changelog/db.changelog-master.mysql.sql | 2026-10-07 20:22:48 |
| 1791063470290-1 | db/changelog/db.changelog-master.mysql.sql | 2026-10-07 20:22:48 |
| 1791224952245-1 | db/changelog/db.changelog-master.mysql.sql | 2026-10-07 20:22:48 |
| 1791224952245-2 | db/changelog/db.changelog-master.mysql.sql | 2026-10-07 20:22:48 |
| 1791224952245-3 | db/changelog/db.changelog-master.mysql.sql | 2026-10-07 20:22:49 |
| 1791224952245-4 | db/changelog/db.changelog-master.mysql.sql | 2026-10-07 20:22:49 |
+-----------------+--------------------------------------------+---------------------+
6 rows in set (0.00 sec)
```



# run the application from the command line

using one of the following:

option 1:
with Maven

```shell
# Since the following line is wrapped in parentheses,
# it runs not in your current shell, but in a child process - in a sub-shell.
# After that sub-shell exits, no environment variables will remain set in the parent shell process.
#
# The first statement = Turn on the shell's "allexport" (auto-export) option.
# The command-line argument is NOT required
# but, when used, can help confirm which filter is rejecting an incoming HTTP request.

$ (
    set -a ;

    source ./.env ;

    ./mvnw spring-boot:run \
        -Dspring-boot.run.arguments=--logging.level.org.springframework.security=TRACE ;
)

# ...

# Running `SHOW TABLES;` in the `mysql` shell now will return:
```

```sql
mysql> SHOW TABLES;
+-----------------------+
| Tables_in_pft-db      |
+-----------------------+
| DATABASECHANGELOG     |
| DATABASECHANGELOGLOCK |
| exchange_rates        |
| transactions          |
+-----------------------+
4 rows in set (0.00 sec)

mysql> select ID, FILENAME, DATEEXECUTED from DATABASECHANGELOG;
-- The output should be identical to
-- the output of the above-noted preceding run of this same statement.
```



option 2:
build a single executable JAR file that contains all the necessary dependencies, classes, and resources
and
run that

build the JAR file with
```shell
$ ./mvnw clean package
```

run the JAR file, as follows:
```shell
$ (
    set -a ;

    source ./.env ;

    java \
        -jar target/personal-finance-tracker-0.0.1-SNAPSHOT.jar ;
)
```

---

Use another terminal window to issue these HTTP requests:

```shell
# The following will work
# only if the HTTPie command line tool is installed on your machine.

# Create an empty database from scratch.

$ chmod u+x scripts/5_issue_requests_for_tokens.sh
$ (
    set -a ;

    source .env ;

    scripts/5_issue_requests_for_tokens.sh ;
)

$ chmod u+x scripts/1_issue_requests_for_CRUD_operations_with_transactions.sh
$ (
    set -a ;

    source .env ;

    scripts/1_issue_requests_for_CRUD_operations_with_transactions.sh ;
)

# Create an empty database from scratch.

$ chmod u+x scripts/2_issue_requests_for_computing_totals.sh
$ (
    set -a ;

    source .env ;

    scripts/2_issue_requests_for_computing_totals.sh ;
)

# Create an empty database from scratch.

$ chmod u+x scripts/3_issue_requests_for_CRUD_operations_with_exchange_rates.sh
$ (
    set -a ;

    source .env ;

    scripts/3_issue_requests_for_CRUD_operations_with_exchange_rates.sh ;
)

# Create an empty database from scratch.

$ chmod u+x scripts/4_issue_requests_for_computing_totals_and_converting.sh
$ (
    set -a ;

    source .env ;

    scripts/4_issue_requests_for_computing_totals_and_converting.sh ;
)
```



# cleanup

```shell
$ podman container rm -f container-pft-mysql \
    && \
    podman volume prune
```
