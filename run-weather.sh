#!/usr/bin/env bash
set -e

export DB_URL='jdbc:postgresql://127.0.0.1:5434/weather_db'
export DB_USERNAME='weather_app'

read -s -p 'weather_app のDBパスワード: ' DB_PASSWORD
printf '\n'
export DB_PASSWORD

./gradlew bootRun
