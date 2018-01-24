#!/bin/bash
#set -x

# $1 - appName

echo "Check Kubernetes status"

deploymentName="$1-app"
# Get current amount of pods
current=$(kubectl get deployment ${deploymentName} | tail -n 1 | awk '{print $3}')
# Get desired amount of pods
desired=$(kubectl get deployments ${deploymentName} | tail -n 1 | awk {'print $2'})

if [ "$current" != "$desired" ]; then
  # Get failed pod
  pod=$(kubectl get pods | grep "^${deploymentName}-[0-9a-zA-Z-]*[ 0-9/]*[^Running]*[0-9a-z ]*\$" | awk {'print $1'})
  if [ ! -z "$pod" ]; then
   kubectl logs ${pod} -c php-fpm | head -n 20
  fi
  echo "Build failed. Check your code and logs."
  exit 1
else
  echo "Kubernetes updated successful."
fi
