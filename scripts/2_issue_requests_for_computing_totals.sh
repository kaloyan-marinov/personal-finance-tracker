#!/usr/bin/env bash

# The data below is meant to look realistic,
# but it does not belong to a real person.

http POST \
    localhost:8080/transactions \
    year:=2025 \
    amount:=90123 \
    currency=CHF \
    description="salary certificate >> 8. gross salary"

http POST \
    localhost:8080/transactions \
    year:=2025 \
    amount:=-5678 \
    currency=CHF \
    description="salary certificate >> 9. contributions to pillar 1 (AHV/IV/EO/ALV/NBUV)"

http POST \
    localhost:8080/transactions \
    year:=2025 \
    amount:=-1700 \
    currency=CHF \
    description="salary certificate >> 10. contributions to pillar 2 (occupational pension insurance)"

http POST \
    localhost:8080/transactions \
    year:=2025 \
    amount:=-12345 \
    currency=CHF \
    description="salary certificate >> 12. tax withheld at source"



http POST \
    localhost:8080/transactions \
    year:=2025 \
    amount:=-400 \
    currency=EUR \
    description="car >> total_interest"

http POST \
    localhost:8080/transactions \
    year:=2025 \
    amount:=-2000 \
    currency=EUR \
    description="car >> total_of_installments"



http POST \
    localhost:8080/transactions \
    year:=2025 \
    amount:=-$((8 * 70)) \
    currency=CHF \
    description="gym membership"

http POST \
    localhost:8080/transactions \
    year:=2025 \
    amount:=-180 \
    currency=EUR \
    description="gym equipment >> sneakers"

http POST \
    localhost:8080/transactions \
    year:=2025 \
    amount:=-50 \
    currency=CHF \
    description="gym equipment >> rope"



http \
    'localhost:8080/totals?year=2025' \
    --verbose
