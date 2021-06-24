#!/bin/bash

set -e

# Define help message
show_help() {
    echo """
Usage: docker run <imagename> COMMAND
Commands
dev     : Start a normal Wildfly application for development purpuse (management & debug ports)
run     : Start a normal Wildfly application
bash    : Start a bash shell
help    : Show this message
"""
}

# Define configuration function
edit_config() {
    # Removes warning
    rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history/current/*
    # Sets var in config by env var
    cp /opt/jboss/.ortolang/config.properties /tmp/
    envsubst < /tmp/config.properties > /opt/jboss/.ortolang/config.properties
    rm /tmp/config.properties
}

# Run
case "$1" in
    dev)
        echo "Migrating database..."
        echo "Migrating on ${ORTOLANG_DB_URL} from Flyway ..."
        unzip -o /opt/jboss/wildfly/standalone/deployments/ortolang-diffusion.ear components.jar -d /opt/jboss/flyway/jars/
        unzip -o -j /opt/jboss/flyway/jars/components.jar *.sql -d /opt/jboss/flyway/sql
        /opt/jboss/flyway/flyway -url=${ORTOLANG_DB_URL} -user=${ORTOLANG_DB_USER} -password=${ORTOLANG_DB_PASS} migrate

        # if [ $KEYCLOAK_USER ] && [ $KEYCLOAK_PASSWORD ]; then
        #     echo "Adding keycloak admin user"
        #     /opt/jboss/keycloak/bin/add-user-keycloak.sh --user $KEYCLOAK_USER --password $KEYCLOAK_PASSWORD
        # fi
        edit_config
        echo "Running Development Server..."
        # exec /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement=0.0.0.0 --debug -Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=/opt/jboss/wildfly/standalone/configuration/ortolang-realm.json -Dkeycloak.migration.strategy=IGNORE_EXISTING
        exec /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement=0.0.0.0 --debug
        exit $?
    ;;
    run)
        # echo "Migrating database..."
        edit_config
        echo "Running Production Server..."
        exec /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0
        exit $?
    ;;
    bash)
        /bin/bash "${@:2}"
    ;;
    *)
        show_help
    ;;
esac

