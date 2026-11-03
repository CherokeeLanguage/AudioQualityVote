#!/bin/bash

# Deploy AudioQualityVote to the serving stack's Docker host
# (vhost-58.cherokeedictionary.net). The application runs as a container on
# the stack's host-local backend network; the compose service delivers the
# DB_* environment keys and the secret files by path (stack convention — no
# configuration values ship from this repository).
#
# Sequencing note: the container image packages the gretty application-export
# product (AudioQualityVote#26's deliverable); the compose service itself is
# devops#13's deliverable. Until #26's export lands, this script's build step
# produces the war artifact and its compose service consumes the packaged
# image — the end-to-end run is verified by devops#13's deployment
# composition, not here.

set -e
set -o pipefail

cd "$(dirname "$(readlink -f "$0")")"

trap "echo ERROR" ERR

STACK_HOST="mconrad@vhost-58.cherokeedictionary.net"
STACK_DIR="/srv/stack/audio-quality-vote"
SERVICE="audio-quality-vote"

# build the deployable product through the project's own wrapper
(cd .. && ./gradlew :audio-quality-vote-war:clean :audio-quality-vote-war:build :audio-quality-vote-war:war)

# ship the built product into the stack's build context (paths only — no
# configuration values ride this script)
ssh "$STACK_HOST" "mkdir -p $STACK_DIR/build"
scp build/libs/AudioQualityVote.war "$STACK_HOST:$STACK_DIR/build/AudioQualityVote.war"

# (re)build and (re)start the application container on the serving stack;
# the compose service carries the DB_* env/secret delivery
ssh "$STACK_HOST" "cd $STACK_DIR && docker compose build $SERVICE && docker compose up -d $SERVICE"

ssh "$STACK_HOST" "docker compose -f $STACK_DIR/docker-compose.yml ps $SERVICE"
