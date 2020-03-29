#!/usr/bin/bash

set -e

/usr/bin/env java -Xmx$MAX_HEAP_SIZE \
                  -Dconfig.edn=/app/config.edn \
                  -Dnndb.graph-dir=/app/graph-dir \
                  -Dnndb.port=$PORT \
                  -jar /app/standalone.jar

