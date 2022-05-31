#!/usr/bin/env bash
set -x
USER=anon
PASSWORD=password
BASIC_TOKEN=$(echo -n "$USER:$PASSWORD" | base64)

http POST http://localhost:4567/users username=$USER password=$PASSWORD

http POST http://localhost:4567/spaces name=newspa owner=$USER "Authorization:Basic $BASIC_TOKEN"

http GET http://localhost:4567/logs "Authorization:Basic $BASIC_TOKEN"
