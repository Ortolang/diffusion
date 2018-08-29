FROM maven:3.5-jdk-8-slim as builder

ENV JBOSS_HOME=/jboss/wildfly-11.0.0.Final

WORKDIR /jboss

RUN curl -O "http://download.jboss.org/wildfly/11.0.0.Final/wildfly-11.0.0.Final.zip" && \
    unzip -q wildfly-11.0.0.Final.zip

COPY comp/src/test/resources/ortolang-roles.properties /jboss/wildfly-11.0.0.Final/standalone/configuration/
COPY comp/src/test/resources/ortolang-users.properties /jboss/wildfly-11.0.0.Final/standalone/configuration/
COPY comp/src/test/resources/ortolang-test-11.xml /jboss/wildfly-11.0.0.Final/standalone/configuration/

WORKDIR /app

COPY . .

RUN mvn -q clean package -DjbossHome=/jboss/wildfly-11.0.0.Final -Djboss.home=/jboss/wildfly-11.0.0.Final

FROM jboss/wildfly:11.0.0.Final

ARG VERSION_PGSQL=9.4.1208
ARG VERSION_KEYCLOAK=3.4.3.Final

WORKDIR /opt/jboss/wildfly/

USER root

# Install envsubst
RUN yum install -y gettext

USER jboss

# Downloading custom PostgreSQL module for wildlfy
RUN curl -q -O "http://maven.ortolang.fr/service/local/repositories/releases/content/fr/ortolang/ortolang-pgsql-wf-module/${VERSION_PGSQL}/ortolang-pgsql-wf-module-${VERSION_PGSQL}.zip" && \
    unzip -q ortolang-pgsql-wf-module-${VERSION_PGSQL}.zip -d /opt/jboss/wildfly/
# Keycloak Adapter
RUN curl -L "https://downloads.jboss.org/keycloak/${VERSION_KEYCLOAK}/adapters/keycloak-oidc/keycloak-wildfly-adapter-dist-${VERSION_KEYCLOAK}.tar.gz" | tar zx
RUN $JBOSS_HOME/bin/jboss-cli.sh --file=bin/adapter-elytron-install-offline.cli

COPY src/main/docker/configuration/* /opt/jboss/wildfly/standalone/configuration/    

RUN mkdir /opt/jboss/.ortolang
RUN mkdir /opt/jboss/.ortolang/binary-store
COPY src/main/docker/config.properties /opt/jboss/.ortolang

COPY --from=builder /app/appli/target/ortolang-diffusion.ear /opt/jboss/wildfly/standalone/deployments/

CMD cp /opt/jboss/.ortolang/config.properties /tmp/ && \
    envsubst < /tmp/config.properties > /opt/jboss/.ortolang/config.properties && \
    /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0