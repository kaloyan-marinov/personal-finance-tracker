create_transaction() {
    http POST \
        localhost:8080/transactions \
        "$@"
}

retrieve_one_transaction() {
    # Guard against a missing ID.
    local id="${1:?usage: retrieve_one_transaction <id> [http args...]}"

    http \
        "localhost:8080/transactions/$id"
}

retrieve_transactions() {
    http \
        localhost:8080/transactions
}

update_transaction() {
    # Guard against a missing ID.
    # (
    # `shift` drops the first argument;
    # `$@` passes any remaining arguments through;
    # )
    local id="${1:?usage: update_transaction <id> [http args...]}"; shift

    http PUT \
        "localhost:8080/transactions/$id" \
        "$@"
}

delete_transaction() {
    # Guard against a missing ID.
    local id="${1:?usage: delete_transaction <id> [http args...]}"

    http DELETE \
        "localhost:8080/transactions/$id"
}



N_SECONDS=1



# Retrieve all transactions.
# (If you execute this script immediately after a fresh restart of the web application,
# the HTTP response should consist of zero transactions.)
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_transactions"
echo $cmd
$cmd



# Attempt to create transactions that fail validation.
# (Neither of the following requests should actually create a new transaction.)
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_transaction year=2000"
echo $cmd
$cmd



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_transaction year=2000 amount:=4.789"
echo $cmd
$cmd



# Create several transactions.
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_transaction year=2000 amount=0"
echo $cmd
$cmd



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_transaction year=2010 amount=10"
echo $cmd
$cmd



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_transaction year=2020 amount=20"
echo $cmd
$cmd



# Create a transaction that is identical to the most recently created one.
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_transaction year=2020 amount=20"
echo $cmd
$cmd



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_transactions"
echo $cmd
$cmd



# Retrieve a single transaction.
nonexistent_id=17

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_one_transaction $nonexistent_id"
echo $cmd
$cmd



existent_id=3

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_one_transaction $existent_id"
echo $cmd
$cmd



# Update a transaction.
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="update_transaction $nonexistent_id year=2020 amount=20"
echo $cmd
$cmd


# The following request should leave the (targeted) transaction unchanged.
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="update_transaction $existent_id year:=null amount=30"
echo $cmd
$cmd



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="update_transaction $existent_id year=3030 amount=30"
echo $cmd
$cmd



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_transactions"
echo $cmd
$cmd



# Delete a transaction.
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="delete_transaction $nonexistent_id"
echo $cmd
$cmd



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="delete_transaction $existent_id"
echo $cmd
$cmd



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_transactions"
echo $cmd
$cmd
