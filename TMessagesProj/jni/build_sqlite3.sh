#!/usr/bin/env bash

function checkPreRequisites {
    if ! [[ -d "sqlite" ]] || ! [[ "$(ls -A sqlite)" ]]; then
        echo -e "\033[31mFailed! Submodule 'sqlite' not found!\033[0m"
        echo -e "\033[31mTry to run: 'git submodule init && git submodule update'\033[0m"
        exit
    fi
}

function generateManifest {
    git rev-parse --git-dir >/dev/null
    echo $(git log -1 --format=format:%H) > manifest.uuid
    echo C $(cat manifest.uuid) > manifest
    git log -1 --format=format:%ci%n | sed 's/ [-+].*$//;s/ /T/;s/^/D /' >> manifest
}

function buildBundle {
    ./configure
    make sqlite3.c
}

checkPreRequisites
cd sqlite

generateManifest
buildBundle
cd ..
