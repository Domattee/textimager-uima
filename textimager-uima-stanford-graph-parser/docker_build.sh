#!/bin/bash
sudo docker build -t textimager-stanford-dp .
sudo docker run -p 5000:81 -it --rm --name stanford-test textimager-stanford-dp
