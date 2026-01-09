#!/usr/bin/env sh
# Script to add ssl trust certs into the current trust keystore before we start our spring boot app
# We use self signed certificates in our dev and test environments so we need to add these to our chain of trust
# The kubernetes startup will load any self signed certificates into /etc/certs
# We load any certs found in the /etc/certs into the default truststore
# The spring boot java app will load the certificates from the default truststore
function logmsg {
    SCRIPTNAME=$(basename $0)
    echo "$SCRIPTNAME : $1"
}

logmsg "startup.sh running and loading certificates ..."
if [ -z "$CERTS_DIR" ]; then
    logmsg "Warning - expects \$CERTS_DIR to be set. i.e. export CERTS_DIR="/etc/certs
    logmsg "Defaulting to /etc/certs"
    export CERTS_DIR="/etc/certs"
fi

export KEYSTORE="$JAVA_HOME/lib/security/cacerts"
if [ ! -f "$KEYSTORE" ]; then
    logmsg "Error - expects keystore to already exist"
    exit 1
fi

export count=1
logmsg "Loading certificates from $CERTS_DIR"
for FILE in $(ls $CERTS_DIR)
do
    alias="mojcert$count"
    logmsg "Adding $CERTS_DIR/$FILE to truststore with alias $alias"
    keytool -importcert -file $CERTS_DIR/$FILE -keystore $KEYSTORE -storepass changeit -alias $alias -noprompt
    count=$((count+1))
done

keytool -list -keystore $KEYSTORE -storepass changeit | grep "Your keystore contains"

export JARFILE=$(ls /app/*.jar | grep -v 'plain' | head -n1)
if [ -f "$JARFILE" ]; then
    logmsg "Running java jarfile $JARFILE"
    java -jar $JARFILE
else
    logmsg "No jarfile found in /app"
fi
