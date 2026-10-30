#!/bin/bash

set -e
set -o pipefail

trap "echo ERROR" ERR

gradle clean build war

scp build/libs/AudioQualityVote.war mconrad@vhost.cherokeedictionary.net:/opt/tomcat/webapps/AudioQualityVote.tmp

ssh mconrad@vhost.cherokeedictionary.net 'cd /opt/tomcat/webapps && mv -v AudioQualityVote.tmp AudioQualityVote.war'

