#!/usr/bin/env bash

# Run this from the root of the repository. The first argument is the graph
# directory.


if [[ $# -eq 0 ]] ; then
    echo "Error: first argument must be set to graph directory"
    exit 1
fi

# diff-index returns a non-zero exit code if there are uncommited changes in
# the repo.
if `git diff-index --quiet HEAD --`; then
    short_id=`git rev-parse --short HEAD`
else
    echo "Error: uncommited changes in repository"
    exit 2
fi

set -e

lein clean
lein uberjar

image_name="nndb-api:${short_id}"

docker build -t ${image_name} \
             --build-arg jar_path=target/uberjar/standalone.jar \
	     --build-arg graph_dir=${1} \
	     -f deployment/Dockerfile .

echo "Built image: ${image_name}"

