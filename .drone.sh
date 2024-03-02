#!/bin/bash

export TAG=${DRONE_COMMIT_SHA:0:7}_ci_build_${BUILD_NUMBER:-$DRONE_BUILD_PARENT}

mkdir -p $HOME/.ssh
echo "$SSH_KEY" > $HOME/.ssh/ed25519
chmod 600 $HOME/.ssh/ed25519
ssh-keyscan bitbucket.org > /$HOME/.ssh/known_hosts
cat <<EOT >> $HOME/.ssh/config
    Host bitbucket.org
    HostName bitbucket.org
    User git
    IdentityFile ~/.ssh/ed25519
EOT


if git clone git@bitbucket.org:creativeadvtech/soc-helm-charts.git charts; then
    cd charts
else
    echo "couldn't clone $PORJECT helm charts"
    exit 1
fi

echo "deploying for $ENV ..."

git checkout master && git branch --set-upstream-to=origin master

yq e -i '(.image.tag.backend) = strenv(TAG)' ./soc/values-$ENV.yaml

helm secrets lint ./soc -f ./soc/values-$ENV.yaml -f ./soc/secrets-$ENV.enc.yaml

if git add --all && git commit -m "bump $ENV backend image tag to $TAG" && git push -u origin master;  then
    echo "**SUCCESS** SNP $ENV Backend Services will be deployed soon"
    argocd login --username $ARGOCD_USERNAME --password $ARGOCD_PASSWORD argocd.creativeadvtech.ml
    argocd app get --grpc-web ${PROJECT}-${ENV} --hard-refresh
    argocd app wait --grpc-web ${PROJECT}-${ENV}
else
    echo "**ERROR** nothing has changed"
    exit 1
fi
