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

compute_totals() {
    # Guard against a missing year.
    local year="${1:?usage: compute_totals <year> [currency]}"
    local currency="$2"

    if [ -z "$currency" ]; then
        http \
            "localhost:8080/api/v1/totals?year=$year" \
            Authorization:"Bearer ${TKN}"
    else
        http \
            "localhost:8080/api/v1/totals?year=$year&currency=$currency" \
            Authorization:"Bearer ${TKN}"
    fi
}

create_exchange_rate() {
    http POST \
        localhost:8080/api/v1/exchange-rates \
        Authorization:"Bearer ${TKN}" \
        "$@"
}



echo "sleeping for $FIVE_SECONDS second(s)"
sleep $FIVE_SECONDS



# Create transactions,
# and compute a given year's totals for each currency.
# (
# Run the current script against an empty database,
# because the script below creates the transactions anew on every run.
# )
"$(dirname "$0")/2_issue_requests_for_computing_totals.sh"



# Compute the totals for a year without any transactions.
echo "sleeping for $FIVE_SECONDS second(s)"
sleep $FIVE_SECONDS

cmd="compute_totals 1999 USD"
echo "$cmd"
eval "$cmd"



# Attempt to compute the totals in a currency that fails validation.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="compute_totals 2025 eur"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="compute_totals 2025 EURO"
echo "$cmd"
eval "$cmd"



# Attempt to compute the totals in EUR, before any exchange rate exists.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="compute_totals 2025 EUR"
echo "$cmd"
eval "$cmd"



# Create an exchange rate,
# which is required to be able to compute the totals in EUR.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="create_exchange_rate year:=2025 sourceCurrency=CHF targetCurrency=EUR sourceToTargetFactor:=1.07"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="compute_totals 2025 EUR"
echo "$cmd"
eval "$cmd"



# Attempt to compute the totals in CHF.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="compute_totals 2025 CHF"
echo "$cmd"
eval "$cmd"



# Attempt to compute the totals in a currency,
# which none of the transactions are in
# and which is not the `target_currency` of any record/row within the `exchange_rates` table.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="compute_totals 2025 USD"
echo "$cmd"
eval "$cmd"



# Create only 1 of the missing records/rows within the `exchange_rates` table.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="create_exchange_rate year:=2025 sourceCurrency=CHF targetCurrency=USD sourceToTargetFactor:=1.20"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="compute_totals 2025 USD"
echo "$cmd"
eval "$cmd"



# Create all the still-missing records/rows within the `exchange_rates` table,
# and compute the totals in USD again.
echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="create_exchange_rate year:=2025 sourceCurrency=EUR targetCurrency=USD sourceToTargetFactor:=1.13"
echo "$cmd"
eval "$cmd"



echo "sleeping for $ONE_SECOND second(s)"
sleep $ONE_SECOND

cmd="compute_totals 2025 USD"
echo "$cmd"
eval "$cmd"
