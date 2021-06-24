FROM maven:3.6.1-jdk-8-alpine as builder

ENV VERSION_WILDFLY=12.0.0.Final
ENV JBOSS_HOME=/jboss/wildfly-${VERSION_WILDFLY}

WORKDIR /app
COPY . .
RUN mvn -q clean package -DskipTests -DjbossHome=/jboss/wildfly-${VERSION_WILDFLY} -Djboss.home=/jboss/wildfly-${VERSION_WILDFLY}

FROM jboss/wildfly:12.0.0.Final

ARG VERSION_PGSQL=9.4.1208
ARG VERSION_KEYCLOAK=3.4.3.Final
ARG VERSION_WILDFLY=12.0.0.Final
ARG VERSION_FLYWAY=4.0.3
ARG CUSTOM_UID=1100
ARG CUSTOM_GID=1000

USER root
# Install envsubst
# Sets a custom UID and GID for jboss user
# Sets language to fr
RUN yum install -y gettext && \
    sed -i -E "s/^jboss:x:[[:digit:]]+:[[:digit:]]+:(.*)$/jboss:x:${CUSTOM_UID}:${CUSTOM_GID}:\1/" /etc/passwd && \
    sed -i -E "s/^jboss:x:[[:digit:]]+:(.*)$/jboss:x:${CUSTOM_GID}:\1/" /etc/group && \
    chown -R ${CUSTOM_UID}:${CUSTOM_GID} /opt/jboss && \
    localedef -i fr_FR -f UTF-8 fr_FR.UTF-8 && \
    echo "LANG=\"fr_FR.UTF-8\"" > /etc/locale.conf

USER jboss

ENV LANG fr_FR.UTF-8
ENV LANGUAGE fr_FR.UTF-8
ENV LC_ALL fr_FR.UTF-8

WORKDIR /opt/jboss/wildfly/
# Downloading custom PostgreSQL module for wildlfy and Keycloak Adapter
RUN curl -q -O "https://maven.ortolang.fr/service/local/repositories/releases/content/fr/ortolang/ortolang-pgsql-wf-module/${VERSION_PGSQL}/ortolang-pgsql-wf-module-${VERSION_PGSQL}.zip" && \
    unzip -q ortolang-pgsql-wf-module-${VERSION_PGSQL}.zip -d /opt/jboss/wildfly/ && \
    curl -L -q -O "https://downloads.jboss.org/keycloak/${VERSION_KEYCLOAK}/adapters/keycloak-oidc/keycloak-wildfly-adapter-dist-${VERSION_KEYCLOAK}.tar.gz" && \
    tar zxvf keycloak-wildfly-adapter-dist-${VERSION_KEYCLOAK}.tar.gz && \
    $JBOSS_HOME/bin/jboss-cli.sh --file=bin/adapter-elytron-install-offline.cli && \
    curl -q -O "https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/${VERSION_FLYWAY}/flyway-commandline-${VERSION_FLYWAY}-linux-x64.tar.gz" && \
    tar zxvf "flyway-commandline-${VERSION_FLYWAY}-linux-x64.tar.gz" && \ 
    mv "/opt/jboss/wildfly/flyway-${VERSION_FLYWAY}" /opt/jboss/flyway

# Copies Wildfly configuration files
COPY --chown=jboss:jboss src/main/docker/configuration/server.keystore /opt/jboss/wildfly/standalone/configuration/
# Overrides Wildfly configuration 
COPY --chown=jboss:jboss src/main/docker/configuration/wildfly-${VERSION_WILDFLY}/standalone.xml /opt/jboss/wildfly/standalone/configuration/

RUN mkdir -p /opt/jboss/.ortolang/binary-store
COPY --chown=jboss:jboss src/main/docker/config.properties /opt/jboss/.ortolang
COPY --chown=jboss:jboss src/main/docker/docker-entrypoint.sh /opt/jboss
# Copies EAR from builder stage
COPY --chown=jboss:jboss --from=builder /app/appli/target/ortolang-diffusion.ear /opt/jboss/wildfly/standalone/deployments/

ENTRYPOINT [ "/opt/jboss/docker-entrypoint.sh" ]
CMD [ "run" ]

EXPOSE 8080
EXPOSE 8443
EXPOSE 8787
EXPOSE 9990
