#!/bin/bash
#set -x

# $1 - appName
# $2 - namespace
# $3 - chartPath
# $4 - helmData

echo "Helm Initializing"

helm init
helm init --upgrade

exist=$(kubectl get pods | grep $1)
if [ ! -z "$exist"  ]; then
  # Upgrade
  helm upgrade $1 $3 \
  --namespace $2 \
  $4
else
  # Installation
  helm install "$3" --name-template "$1" \
  --namespace $2 \
  $4
fi
