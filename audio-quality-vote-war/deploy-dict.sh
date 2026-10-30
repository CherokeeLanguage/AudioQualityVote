#!/bin/bash

set -e
set -o pipefail

cd "$(dirname "$(readlink -f "$0")")"

trap "echo ERROR" ERR

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
PROD_PROPERTIES="$SCRIPT_DIR/../AudioQualityVote-prod.properties"
if [ ! -r "$PROD_PROPERTIES" ]; then
	echo "ERROR: $PROD_PROPERTIES not found or not readable"
	exit 1
fi

(cd "$SCRIPT_DIR/.." && ./gradlew :audio-quality-vote-war:clean :audio-quality-vote-war:build :audio-quality-vote-war:war)

# deployment configuration: scp to temp name, remote rename into place
# BEFORE the war rename so a fresh context init finds it
scp "$PROD_PROPERTIES" mconrad@vhost.cherokeedictionary.net:/opt/tomcat/webapps/AudioQualityVote.properties.tmp

ssh mconrad@vhost.cherokeedictionary.net 'cd /opt/tomcat/webapps && mv -v AudioQualityVote.properties.tmp AudioQualityVote.properties'

scp build/libs/AudioQualityVote.war mconrad@vhost.cherokeedictionary.net:/opt/tomcat/webapps/AudioQualityVote.tmp

ssh mconrad@vhost.cherokeedictionary.net 'cd /opt/tomcat/webapps && mv -v AudioQualityVote.tmp AudioQualityVote.war'

