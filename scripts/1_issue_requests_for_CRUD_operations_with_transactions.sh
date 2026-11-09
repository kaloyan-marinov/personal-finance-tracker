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

create_transaction() {
    http POST \
        localhost:8080/api/v1/transactions \
        Authorization:"Bearer ${TKN}" \
        "$@"
}

retrieve_one_transaction() {
    # Guard against a missing ID.
    local id="${1:?usage: retrieve_one_transaction <id> [http args...]}"

    http \
        "localhost:8080/api/v1/transactions/$id" \
        Authorization:"Bearer ${TKN}"
}

retrieve_transactions() {
    http \
        localhost:8080/api/v1/transactions \
        Authorization:"Bearer ${TKN}"
}

update_transaction() {
    # Guard against a missing ID.
    # (
    # `shift` drops the first argument;
    # `$@` passes any remaining arguments through;
    # )
    local id="${1:?usage: update_transaction <id> [http args...]}"; shift

    http PUT \
        "localhost:8080/api/v1/transactions/$id" \
        Authorization:"Bearer ${TKN}" \
        "$@"
}

delete_transaction() {
    # Guard against a missing ID.
    local id="${1:?usage: delete_transaction <id> [http args...]}"

    http DELETE \
        "localhost:8080/api/v1/transactions/$id" \
        Authorization:"Bearer ${TKN}"
}



echo "sleeping for $FIVE_SECONDS second(s)"
sleep $FIVE_SECONDS



# Retrieve all transactions.
# (If you execute this script immediately after a fresh restart of the web application,
# the HTTP response should consist of zero transactions.)
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="retrieve_transactions"
echo "$cmd"
eval "$cmd"



# Attempt to create transactions that fail validation.
# (Neither of the following requests should actually create a new transaction.)
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="create_transaction year:=2000 currency=EUR 'description=train ticket'"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="create_transaction year:=2000 amount:=-4.789 currency=EUR 'description=train ticket'"
echo "$cmd"
eval "$cmd"



# Create several transactions.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="create_transaction year:=2010 amount:=-10 currency=EUR 'description=train ticket'"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="create_transaction year:=2020 amount:=-20 currency=EUR 'description=train ticket'"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="create_transaction year:=2030 amount:=-30 currency=EUR 'description=train ticket'"
echo "$cmd"
eval "$cmd"



# Create a transaction that is identical to the most recently created one.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="create_transaction year:=2030 amount:=-30 currency=EUR 'description=train ticket'"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="retrieve_transactions"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="
http \\
    'localhost:8080/api/v1/transactions?pageSize=3&pageNumber=1' \\
    Authorization:\"Bearer ${TKN}\"
"
echo "$cmd"
eval "$cmd"



# Retrieve a single transaction.
nonexistent_id=17

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="retrieve_one_transaction $nonexistent_id"
echo "$cmd"
eval "$cmd"



existent_id=3

echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="retrieve_one_transaction $existent_id"
echo "$cmd"
eval "$cmd"



# Update a transaction.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="update_transaction $nonexistent_id year:=2020 amount:=-20 currency=EUR 'description=train ticket'"
echo "$cmd"
eval "$cmd"


# The following request should leave the (targeted) transaction unchanged.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="update_transaction $existent_id year:=null amount:=-30 currency=EUR 'description=train ticket'"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="update_transaction $existent_id year:=3060 amount:=-60 currency=USD 'description=train ticket'"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="retrieve_transactions"
echo "$cmd"
eval "$cmd"



# Delete a transaction.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="delete_transaction $nonexistent_id"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="delete_transaction $existent_id"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="retrieve_transactions"
echo "$cmd"
eval "$cmd"
