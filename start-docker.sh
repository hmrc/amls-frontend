#!/bin/sh

SCRIPT=$(find . -type f -name amls-frontend)
exec $SCRIPT \
  $HMRC_CONFIG
