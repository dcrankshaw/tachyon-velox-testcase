#!/usr/bin/env bash

source ~/ec2-variables.sh

java -Xmx10g -Xms10g \
  -Dfs.s3n.awsAccessKeyId=$S3_ACCESS_KEY -Dfs.s3n.awsSecretAccessKey=$S3_SECRET_KEY \
  -cp target/velox-parent-0.0.1-SNAPSHOT.jar \
  edu.berkeley.TachyonKVExercise tachyon://ec2-54-160-185-36.compute-1.amazonaws.com:19998/test-store10 \
  10000 100000
