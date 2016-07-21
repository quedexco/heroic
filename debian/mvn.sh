#!/bin/bash
MVN=mvn

# quiet
if [[ ${DH_VERBOSE} != "1" ]]; then
    MVN="${MVN} -q"
else
    MVN="${MVN} -X"
fi

# use scoped home directory to avoid contaminating build system
MVN="${MVN} -D user.home=${MAVEN_HOME}"

if [[ -f debian/settings.xml ]]; then
    # use in-project settings.xml
    MVN="${MVN} -s debian/settings.xml"
fi

exec $MVN "$@"
