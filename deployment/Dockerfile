FROM openjdk:10-jre

ARG jar_path
ARG graph_dir

ENV MAX_HEAP_SIZE "512m"
ENV PORT "80"

COPY ./deployment/docker_cmd.sh /app/cmd.sh
COPY $graph_dir /app/graph-dir/
COPY ./resources/config.edn /app/
COPY $jar_path /app/standalone.jar

EXPOSE $PORT

# Can't use array syntax for access to env variables.
CMD ./app/cmd.sh
