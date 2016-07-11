#!/bin/bash

HANDLER_BID_ID=$1

if ! [[ -z $HANDLER_BID_ID ]]; then
 curl \
    --header "Content-type: application/json" \
    --header "X-ADMIN-TOKEN: development-use-only" \
    --request DELETE \
    localhost:9000/admin/bids/$HANDLER_BID_ID
  echo # Insert Blank Line
else 
  echo "ERROR: Please provide an bid id to delete."
fi
