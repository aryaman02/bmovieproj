#!/bin/bash

docker build -t aggregator-svc -f Dockerfile_aggregator .
docker build -t processor-svc -f Dockerfile_processor .
docker build -t grpc-svc -f Dockerfile_grpc .
docker build -t rest-svc -f Dockerfile_rest .
