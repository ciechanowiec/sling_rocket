FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app
COPY ../2_rocket-instance/maven-project .
RUN mvn clean package -DskipAdvancedDependencyResolution

FROM ciechanowiec/rocket-base:14.0.4

ARG SLING_DIR
ARG RUN_MODES
ARG ENABLE_MULTI_VERSION_SUPPORT
ARG HTTP_PORT
ARG JAVA_DEBUG_PORT
ARG EXTRA_JAVA_OPTS
ARG ROCKET_FEATURE_ARTIFACT_FINAL_NAME

WORKDIR ${SLING_DIR}

COPY --from=build /app/feature/target/${ROCKET_FEATURE_ARTIFACT_FINAL_NAME} ${SLING_DIR}

COPY ../2_rocket-instance/rocket-instance-installer.sh .
COPY ../2_rocket-instance/rocket-instance-starter.sh .

RUN ./rocket-instance-installer.sh
RUN rm rocket-instance-installer.sh
RUN rm LICENSE NOTICE README.md

VOLUME ${SLING_DIR}/launcher/repository/segmentstore

# exec is required in order to set the Java process as PID 1 inside the container, since Docker sends
# termination signals only to PID 1, and we need those signals to be handled by the java process:
ENTRYPOINT [ "sh", "-c", "exec /opt/sling/rocket-instance-starter.sh" ]
