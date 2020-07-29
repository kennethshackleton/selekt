#!/usr/bin/env bash -ex

./gradlew \
  :AndroidLib:testDebugUnitTest \
  :AndroidSupportLib:testDebugUnitTest \
  :Commons:test \
  :Lib:test \
  :Pools:test

