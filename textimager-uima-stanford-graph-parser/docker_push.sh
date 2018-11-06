#!/bin/bash
IMAGE_VER=1
bash ./docker_build.sh
sudo docker tag textimager-stanford-dp texttechnologylab/textimager-stanford-dp:${IMAGE_VER}
sudo docker push texttechnologylab/textimager-stanford-dp:${IMAGE_VER}
