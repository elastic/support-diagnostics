#!/usr/bin/env bash

docker run --network host -it -v ${PWD}/diagnostic-output:/diagnostic-output support-diagnostics-app  bash
