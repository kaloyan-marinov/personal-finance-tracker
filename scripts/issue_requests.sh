retrieve_transactions() {
    http \
        localhost:8080/transactions
}

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

delete_transaction() {
    # Guard against a missing ID.
    local id="${1:?usage: delete_transaction <id> [http args...]}"

    http DELETE \
        "localhost:8080/transactions/$id"
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



N_SECONDS=1



# Retrieve all transactions.
# (If you execute this script immediately after a fresh restart of the web application,
# the HTTP response should consist of zero transactions.)
cmd="retrieve_transactions"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd



# Create several transactions.
cmd="create_transaction year=2000 amount=0"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd

cmd="create_transaction year=2010 amount=10"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd

cmd="create_transaction year=2029 amount=29"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd



# Create a transaction that is identical to the most recently created one.
cmd="create_transaction year=2029 amount=29"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd



# Retrieve all transactions.
cmd="retrieve_transactions"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd



# Retrieve a single transaction.
nonexistent_id=17

cmd="retrieve_one_transaction $nonexistent_id"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd



existent_id=3

cmd="retrieve_one_transaction $existent_id"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd



# Delete a transaction.
cmd="delete_transaction $nonexistent_id"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd



cmd="delete_transaction $existent_id"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd



# Update a transaction.
cmd="update_transaction $nonexistent_id year=2020 amount=20"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd


existent_id=4

cmd="update_transaction $existent_id year=2020 amount=20"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd



# Retrieve all transactions.
cmd="retrieve_transactions"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS
echo $cmd
$cmd
