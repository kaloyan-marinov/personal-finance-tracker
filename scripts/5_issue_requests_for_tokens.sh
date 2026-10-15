#!/usr/bin/env bash

http \
    --verbose \
    POST localhost:8080/api/v1/tokens



# The quotes prevent a password with spaces or "glob characters" from breaking the command.
http \
    --verbose \
    --auth "${PFT_SECURITY_USER_NAME}:${PFT_SECURITY_USER_PASSWORD}" \
    POST localhost:8080/api/v1/tokens
