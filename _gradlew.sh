#!/bin/bash

# Run gradlew with the proper version of Java

# ----
# Figure Java to use

if [[ ! -d "$JAVA_HOME" ]]; then
    AS="C:\Program Files\Android\Android Studio\jbr"
    if [[ -x "$AS/bin/java.exe" ]]; then
        export JAVA_HOME="$AS"
    fi
fi
if [[ ! -d "$JAVA_HOME" ]]; then
    AS="C:\Program Files\Android\Android Studio\jre"
    if [[ -x "$AS/bin/java.exe" ]]; then
        export JAVA_HOME="$AS"
    fi
fi
echo "JAVA_HOME=${JAVA_HOME}"
JV="$JAVA_HOME/bin/java.exe"
V=$( "$JV" -version 2>&1 | sed -n -e '/build /s/.*build \([0-9]\+\.[0-9]\+\).*/\1/p' | head -n 1 )
echo "Java version: $V"

# ----

./gradlew --console=plain $*
