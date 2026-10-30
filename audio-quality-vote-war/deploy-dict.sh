#!/bin/bash

set -e
set -o pipefail

trap "echo ERROR" ERR

gradle clean build war

scp build/libs/AudioQualityVote.war mconrad@vhost.cherokeedictionary.net:/opt/tomcat/webapps/CED-AudioQualityVote.tmp

ssh mconrad@vhost.cherokeedictionary.net 'cd /opt/tomcat/webapps && mv -v CED-AudioQualityVote.tmp CED-AudioQualityVote.war'

