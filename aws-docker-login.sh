#!/bin/bash

DOCKER_REG_REGION="ap-south-1"

$(aws ecr get-login --no-include-email --region ${DOCKER_REG_REGION})
