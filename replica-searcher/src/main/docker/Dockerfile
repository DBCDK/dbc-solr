FROM docker.dbc.dk/docker-dbc.artifacts.dbccloud.dk:latest

USER root
RUN echo 'export JAVA_OPTS="$JAVA_OPTS -DdbcSearchHandler=dk.dbc.solr.module.searcher.ReplicaSearcher"' > /docker-entrypoint-confd.d/50-enable-dbc-replica-searcher.env
USER solr

ADD target/${project.build.finalName}.jar $SOLR_BASE/dbc/lib/
