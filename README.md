# configure

```shell
$ cp \
    .env.template \
    .env

# Edit the newly-created file according to the instructions therein.
```



# create an empty database:

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
Run:                          1
Previously run:               0
Filtered out:                 0
-------------------------------
Total change sets:            1
```

```sql
mysql> SHOW TABLES;
+-----------------------+
| Tables_in_pft-db      |
+-----------------------+
| DATABASECHANGELOG     |
| DATABASECHANGELOGLOCK |
| transactions          |
+-----------------------+
3 rows in set (0.00 sec)

mysql> select ID, FILENAME, DATEEXECUTED from DATABASECHANGELOG;
+-----------------+--------------------------------------------+---------------------+
| ID              | FILENAME                                   | DATEEXECUTED        |
+-----------------+--------------------------------------------+---------------------+
| 1790190381140-1 | db/changelog/db.changelog-master.mysql.sql | 2026-09-23 19:13:03 |
+-----------------+--------------------------------------------+---------------------+
1 row in set (0.00 sec)
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
$ (
    set -a ;

    source ./.env ;

    ./mvnw spring-boot:run ;
)

# ...

# Running `show tables;` in the `mysql` shell now will return:
```

```sql
mysql> SHOW TABLES;
+-----------------------+
| Tables_in_pft-db      |
+-----------------------+
| DATABASECHANGELOG     |
| DATABASECHANGELOGLOCK |
| transactions          |
+-----------------------+
3 rows in set (0.00 sec)

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

$ chmod u+x scripts/issue_requests.sh

$ scripts/issue_requests.sh
```



# cleanup

```shell
$ podman container rm -f container-pft-mysql \
    && \
    podman volume prune
```
