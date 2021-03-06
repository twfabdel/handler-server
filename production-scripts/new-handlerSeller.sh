#!/bin/bash

COMPANY_NAME=$1

if ! [[ -z $COMPANY_NAME ]]; then

  curl \
    --header "Content-type: application/json" \
    --header "X-ADMIN-TOKEN: development-use-only" \
    --request POST \
    --data "{ \"trader_id\" : 1, \"first_name\" : \"F_NAME\", \"last_name\" : \"L_NAME\", \"company_name\" : \"$COMPANY_NAME\", \"email_address\" : \"test@test.com\", \"phone_number\" : \"5551231234\"}" \
    server.agrity.net/admin/handlerSellers
else
  echo "ERROR: Please provide a company name to create."
fi


