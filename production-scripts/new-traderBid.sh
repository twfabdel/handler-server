#!/bin/bash

if [[ -z $1 ]]; then
  curl \
    --header "Content-type: application/json" \
    --header "X-ADMIN-TOKEN: development-use-only" \
    --request POST \
    --data '{ "trader_id" : 1, "handlerSeller_ids" : [1], "almond_variety" : "NP", "almond_size" : "23/25", "almond_pounds" :  100000,  "price_per_pound" : "2.23", "management_type" : { "type" : "FCFSService", "delay" : 10}, "comment" : "This is an awesome test comment", "email_address" : "ryscot@gmail.com" }' \
    server.agrity.net/admin/traderBids
  echo # Insert Blank Line
else
  echo "ERROR: do not use any arguements."
fi
