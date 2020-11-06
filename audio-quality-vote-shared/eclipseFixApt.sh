#!/bin/bash

set -e
set -o pipefail
 
cd "$(dirname "$0")"
cwd="$(pwd)"
gradle="$cwd/../gradlew"

rm .settings/org.eclipse.jdt.apt.core.prefs 2> /dev/null || true
rm .settings/org.eclipse.jdt.core.prefs 2> /dev/null || true
rm .factorypath 2> /dev/null || true
$gradle eclipseJdtApt > /dev/null
$gradle eclipseFactorypath > /dev/null

	