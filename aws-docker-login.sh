#!/bin/bash

DOCKER_REG_ID=073327071517
DOCKER_REG_REGION="us-east-1"

eval $(aws --region ${DOCKER_REG_REGION} ecr get-login --registry-ids ${DOCKER_REG_ID} --no-include-email)
