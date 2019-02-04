#!/usr/bin/env sh

RELATIVE_SCRIPT_DIR=$(dirname $0)
REPO_ROOT_DIR="$(cd "${RELATIVE_SCRIPT_DIR}" && pwd)"

${REPO_ROOT_DIR}/gradlew clean test
