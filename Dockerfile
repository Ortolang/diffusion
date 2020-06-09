FROM maven:3.5-jdk-8-slim as builder

ENV JBOSS_HOME=/jboss/wildfly-11.0.0.Final

WORKDIR /app

COPY . .

RUN mvn -q clean package -DskipTests -DjbossHome=/jboss/wildfly-11.0.0.Final -Djboss.home=/jboss/wildfly-11.0.0.Final

FROM jboss/wildfly:11.0.0.Final

ARG VERSION_PGSQL=9.4.1208
ARG VERSION_KEYCLOAK=3.4.3.Final
ARG CUSTOM_UID=1100
ARG CUSTOM_GID=1000

USER root
# Install envsubst
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
    $JBOSS_HOME/bin/jboss-cli.sh --file=bin/adapter-elytron-install-offline.cli

COPY --chown=jboss:jboss src/main/docker/configuration/* /opt/jboss/wildfly/standalone/configuration/    

RUN mkdir -p /opt/jboss/.ortolang/binary-store
COPY --chown=jboss:jboss src/main/docker/config.properties /opt/jboss/.ortolang

RUN curl -O -L 'https://github.com/vishnubob/wait-for-it/raw/master/wait-for-it.sh' && chmod +x wait-for-it.sh

COPY --chown=jboss:jboss --from=builder /app/appli/target/ortolang-diffusion.ear /opt/jboss/wildfly/standalone/deployments/

CMD cp /opt/jboss/.ortolang/config.properties /tmp/ && \
    envsubst < /tmp/config.properties > /opt/jboss/.ortolang/config.properties && \
    /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0
