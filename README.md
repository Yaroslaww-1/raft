## Overview

Raft consensus algorithm implementation using Kotlin programming language.

## How to run

1. Start a cluster - `docker-compose up --build`
2. Send a command - `curl -X POST -H "Content-Type: application/json" -d '{"command": "YOUR_COMMAND", "depth": "0"}' http://0.0.0.0:8001/appendCommand`
3. Get saved commands - `curl http://0.0.0.0:8001/appendCommand -H "Accept: application/json"`

## Tested scenarios

You can find tested scenarios under the `src/test/kotlin/ucu/edu` folder.