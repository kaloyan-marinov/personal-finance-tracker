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



echo "sleeping for $FIVE_SECONDS second(s)"
sleep $FIVE_SECONDS

cmd="
http \\
    --verbose \\
    POST localhost:8080/api/v1/tokens
"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND
# The quotes prevent a password with spaces or "glob characters" from breaking the command.
cmd="
http \\
    --verbose \\
    --auth \"${USER_1_EMAIL}:${USER_1_PASSWORD}\" \\
    POST localhost:8080/api/v1/tokens
"
echo "$cmd"
eval "$cmd"
