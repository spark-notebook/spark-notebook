#!/bin/bash

export TEST_GIT_REPO_HTTPS="${SN_NOTEBOOKS_GIT_HTTPS_REPO}"
export TEST_GIT_USER="${SN_NOTEBOOKS_GIT_USER}"
export TEST_GIT_PASS="${SN_NOTEBOOKS_GIT_TOKEN}"

export TEST_GIT_REPO_SSH="$SN_NOTEBOOKS_GIT_SSH_REPO"
export TEST_GIT_KEYFILE="$SN_NOTEBOOKS_GIT_KEYFILE"
export TEST_GIT_KEYFILE_PASS="$SN_NOTEBOOKS_GIT_KEYFILE_PASS"

# TODO: currently one still needs to uncomment the @Ignore inside the tests
sbt "project git-notebook-provider" "~test"
