#!/bin/bash
set -x

# $1 - appName
# $2 - namespace
# $3 - chartPath
# $4 - helmData

echo "Helm Initializing"

helm init
helm init --upgrade

helm upgrade --install $1 $3 \
--namespace $2 \
$4