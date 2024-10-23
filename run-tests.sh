#!/bin/bash

IMAGE_NAME="megogo-test"
PROJECT_DIR=$(pwd)
RESULTS_DIR="$PROJECT_DIR/allure-results"
ALLURE_REPORT_DIR="$PROJECT_DIR/allure-report"

docker build -t $IMAGE_NAME .

docker run --rm \
    -v $PROJECT_DIR:/usr/src/app \
    -v $RESULTS_DIR:/usr/src/app/target/allure-results \
    $IMAGE_NAME mvn clean test

docker run --rm \
    -v $RESULTS_DIR:/usr/src/app/target/allure-results \
    -v $ALLURE_REPORT_DIR:/usr/src/app/allure-report \
    $IMAGE_NAME mvn allure:report

docker run --rm -d \
    -p 8080:8080 \
    -v $ALLURE_REPORT_DIR:/app/allure-report \
    -w /app \
    openjdk:17-jdk-slim \
    bash -c "cd allure-report && python3 -m http.server 8080"
