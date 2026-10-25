#!/usr/bin/env bash

# A FEW WORDS OF CAUTION
#
# Each request below is stored as a string in `cmd` and executed via `eval "$cmd"`,
# so that quoted arguments containing spaces (e.g. 'description=train ticket') stay intact.
# That is safe here,
# because every `cmd` is built from literals within this script.
#
# `eval` is only dangerous
# when the string it runs contains data you don't control (script arguments, env vars, HTTP responses);
# here, every cmd="..." is a fixed string written into the script.

ONE_SECOND=1
FIVE_SECONDS=5

USER_1_USERNAME=jd
USER_1_EMAIL=john.doe@protonmail.com
USER_1_PASSWORD=123
USER_1_ID=1

USER_2_USERNAME=ms
USER_2_EMAIL=mary.smith@protonmail.com
USER_2_PASSWORD=456
USER_2_ID=2



# Please note the different responses to these two requests:

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    --verbose \\
    POST \\
    localhost:8080/api/v1/users
"
echo "$cmd"
eval "$cmd"

# Be advised that
# `--raw` needs `$ http --version` to be at least 3.0.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    --verbose \\
    --raw '{}' \\
    POST \\
    localhost:8080/api/v1/users
"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/users \\
    username=${USER_1_USERNAME} \\
    email=${USER_1_EMAIL}
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/users \\
    username=${USER_1_USERNAME} \\
    email=${USER_1_EMAIL} \\
    password=${USER_1_PASSWORD}
"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/users \\
    username=${USER_2_USERNAME} \\
    email=${USER_1_EMAIL} \\
    password=${USER_2_PASSWORD}
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/users \\
    username=${USER_1_USERNAME} \\
    email=${USER_2_EMAIL} \\
    password=${USER_2_PASSWORD}
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/users \\
    username=${USER_2_USERNAME} \\
    email=${USER_2_EMAIL} \\
    password=${USER_2_PASSWORD}
"
echo "$cmd"
eval "$cmd"



echo "sleeping for $FIVE_SECONDS second(s)"
sleep $FIVE_SECONDS

cmd="
http \\
    DELETE \\
    localhost:8080/api/v1/users/2
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    --auth ${USER_1_EMAIL}:${USER_1_PASSWORD} \\
    DELETE \\
    localhost:8080/api/v1/users/${USER_2_ID}
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    --auth ${USER_1_EMAIL}:${USER_1_PASSWORD} \\
    DELETE \\
    localhost:8080/api/v1/users/${USER_1_ID}
"
echo "$cmd"
eval "$cmd"
