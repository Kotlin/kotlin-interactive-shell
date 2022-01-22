#!/usr/bin/env sh

VERSION="$0"

xh PUT https://vendors.sdkman.io/default Consumer-Key:"$JRELEASER_SDKMAN_CONSUMER_KEY" Consumer-Token:"$JRELEASER_SDKMAN_CONSUMER_TOKEN" candidate=ki version="$VERSION"

