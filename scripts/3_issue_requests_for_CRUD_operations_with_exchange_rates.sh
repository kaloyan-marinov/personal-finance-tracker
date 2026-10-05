# A FEW WORDS OF CAUTION
#
# Each request below is stored as a string in `cmd` and executed via `eval "$cmd"`,
# so that quoted arguments containing spaces stay intact.
# That is safe here,
# because every `cmd` is built from literals within this script.
#
# `eval` is only dangerous
# when the string it runs contains data you don't control (script arguments, env vars, HTTP responses);
# here, every cmd="..." is a fixed string written into the script.

# An exchange rate is addressed by its natural key, `<year> <sourceCurrency> <targetCurrency>`,
# which plays the role that `<id>` plays for transactions.

create_exchange_rate() {
    http POST \
        localhost:8080/api/v1/exchange-rates \
        "$@"
}

retrieve_one_exchange_rate() {
    # Guard against a missing key.
    local year="${1:?usage: retrieve_one_exchange_rate <year> <sourceCurrency> <targetCurrency>}"
    local source_currency="${2:?usage: retrieve_one_exchange_rate <year> <sourceCurrency> <targetCurrency>}"
    local target_currency="${3:?usage: retrieve_one_exchange_rate <year> <sourceCurrency> <targetCurrency>}"

    http \
        "localhost:8080/api/v1/exchange-rates/$year/$source_currency/$target_currency"
}

retrieve_exchange_rates() {
    http \
        localhost:8080/api/v1/exchange-rates
}

update_exchange_rate() {
    # Guard against a missing key.
    # (
    # `shift 3` drops the first three arguments;
    # `$@` passes any remaining arguments through;
    # )
    local year="${1:?usage: update_exchange_rate <year> <sourceCurrency> <targetCurrency> [http args...]}"
    local source_currency="${2:?usage: update_exchange_rate <year> <sourceCurrency> <targetCurrency> [http args...]}"
    local target_currency="${3:?usage: update_exchange_rate <year> <sourceCurrency> <targetCurrency> [http args...]}"
    shift 3

    http PUT \
        "localhost:8080/api/v1/exchange-rates/$year/$source_currency/$target_currency" \
        "$@"
}

delete_exchange_rate() {
    # Guard against a missing key.
    local year="${1:?usage: delete_exchange_rate <year> <sourceCurrency> <targetCurrency>}"
    local source_currency="${2:?usage: delete_exchange_rate <year> <sourceCurrency> <targetCurrency>}"
    local target_currency="${3:?usage: delete_exchange_rate <year> <sourceCurrency> <targetCurrency>}"

    http DELETE \
        "localhost:8080/api/v1/exchange-rates/$year/$source_currency/$target_currency"
}


N_SECONDS=1



# Retrieve all exchange rates.
# (If you execute this script immediately after a fresh restart of the web application
# against an empty database,
# the HTTP response should consist of zero exchange rates.)
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_exchange_rates"
echo "$cmd"
eval "$cmd"



# Attempt to create exchange rates that fail validation.
# (None of the following requests should actually create a new exchange rate.)
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_exchange_rate year:=2000 sourceCurrency=EUR targetCurrency=USD"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_exchange_rate year:=2000 sourceCurrency=EUR targetCurrency=USD sourceToTargetFactor:=-1.08"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_exchange_rate year:=2000 sourceCurrency=EUR targetCurrency=USD sourceToTargetFactor:=1.123456789"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_exchange_rate year:=2000 sourceCurrency=eur targetCurrency=USD sourceToTargetFactor:=1.08"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_exchange_rate year:=2000 sourceCurrency=EUR targetCurrency=EUR sourceToTargetFactor:=1"
echo "$cmd"
eval "$cmd"



# Create several exchange rates.
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_exchange_rate year:=2010 sourceCurrency=EUR targetCurrency=USD sourceToTargetFactor:=1.10"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_exchange_rate year:=2020 sourceCurrency=EUR targetCurrency=USD sourceToTargetFactor:=1.20"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_exchange_rate year:=2030 sourceCurrency=EUR targetCurrency=USD sourceToTargetFactor:=1.30"
echo "$cmd"
eval "$cmd"



# Attempt to create an exchange rate whose key is identical to that of the most recently created one.
# (Unlike for transactions, this should fail with a 409 Conflict.)
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_exchange_rate year:=2030 sourceCurrency=EUR targetCurrency=USD sourceToTargetFactor:=1.33"
echo "$cmd"
eval "$cmd"



# Create an exchange rate in the opposite direction of an existing one.
# (That is a different key, so this should succeed.)
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="create_exchange_rate year:=2030 sourceCurrency=USD targetCurrency=EUR sourceToTargetFactor:=0.76923077"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_exchange_rates"
echo "$cmd"
eval "$cmd"



# Retrieve a single exchange rate.
nonexistent_key="2017 EUR USD"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_one_exchange_rate $nonexistent_key"
echo "$cmd"
eval "$cmd"



existent_key="2030 EUR USD"

echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_one_exchange_rate $existent_key"
echo "$cmd"
eval "$cmd"



# Update an exchange rate.
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="update_exchange_rate $nonexistent_key sourceToTargetFactor:=1.17"
echo "$cmd"
eval "$cmd"


# The following request should leave the (targeted) exchange rate unchanged.
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="update_exchange_rate $existent_key sourceToTargetFactor:=null"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="update_exchange_rate $existent_key sourceToTargetFactor:=1.36"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_exchange_rates"
echo "$cmd"
eval "$cmd"



# Delete an exchange rate.
echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="delete_exchange_rate $nonexistent_key"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="delete_exchange_rate $existent_key"
echo "$cmd"
eval "$cmd"



echo "sleeping for $N_SECONDS second(s)"
sleep $N_SECONDS

cmd="retrieve_exchange_rates"
echo "$cmd"
eval "$cmd"
