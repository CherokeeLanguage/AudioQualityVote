#!/bin/bash

gradle clean build war

scp build/libs/AudioQualityVote.war clcom@vhost.cherokeelessons.com:/var/lib/tomcat9/webapps/AudioQualityVote.tmp

ssh clcom@vhost.cherokeelessons.com 'cd /var/lib/tomcat9 && mv -v AudioQualityVote.tmp AudioQualityVote.war'

