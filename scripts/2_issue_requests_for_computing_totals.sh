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



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

TKN=$(
    set -a ;
    source .env ;
    http \
        --print=b \
        --auth "${USER_1_EMAIL}:${USER_1_PASSWORD}" \
        POST localhost:8080/api/v1/tokens \
        | jq \
            --raw-output \
            '.accessToken'
)



echo "sleeping for $FIVE_SECONDS second(s)"
sleep $FIVE_SECONDS



# The data below is meant to look realistic,
# but it does not belong to a real person.

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/transactions \\
    Authorization:\"Bearer ${TKN}\" \\
    year:=2025 \\
    amount:=90123 \\
    currency=CHF \\
    description=\"salary certificate >> 8. gross salary\"
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/transactions \\
    Authorization:\"Bearer ${TKN}\" \\
    year:=2025 \\
    amount:=-5678 \\
    currency=CHF \\
    description=\"salary certificate >> 9. contributions to pillar 1 (AHV/IV/EO/ALV/NBUV)\"
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/transactions \\
    Authorization:\"Bearer ${TKN}\" \\
    year:=2025 \\
    amount:=-1700 \\
    currency=CHF \\
    description=\"salary certificate >> 10. contributions to pillar 2 (occupational pension insurance)\"
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/transactions \\
    Authorization:\"Bearer ${TKN}\" \\
    year:=2025 \\
    amount:=-12345 \\
    currency=CHF \\
    description=\"salary certificate >> 12. tax withheld at source\"
"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/transactions \\
    Authorization:\"Bearer ${TKN}\" \\
    year:=2025 \\
    amount:=-400 \\
    currency=EUR \\
    description=\"car >> total_interest\"
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/transactions \\
    Authorization:\"Bearer ${TKN}\" \\
    year:=2025 \\
    amount:=-2000 \\
    currency=EUR \\
    description=\"car >> total_of_installments\"
"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/transactions \\
    Authorization:\"Bearer ${TKN}\" \\
    year:=2025 \\
    amount:=-$((8 * 70)) \\
    currency=CHF \\
    description=\"gym membership\"
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/transactions \\
    Authorization:\"Bearer ${TKN}\" \\
    year:=2025 \\
    amount:=-180 \\
    currency=EUR \\
    description=\"gym equipment >> sneakers\"
"
echo "$cmd"
eval "$cmd"

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    POST \\
    localhost:8080/api/v1/transactions \\
    Authorization:\"Bearer ${TKN}\" \\
    year:=2025 \\
    amount:=-50 \\
    currency=CHF \\
    description=\"gym equipment >> rope\"
"
echo "$cmd"
eval "$cmd"



echo "sleeping for $FIVE_SECONDS second(s)"
sleep $FIVE_SECONDS

cmd="
http \\
    \\
    'localhost:8080/api/v1/totals?year=2025' \\
    Authorization:\"Bearer ${TKN}\" \\
    --verbose
"
echo "$cmd"
eval "$cmd"
