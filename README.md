# system-flow

This repo contains 4 apps running on the same EMQX broker:

1. `flow-lab-producer`
2. `flow-lab-consumer-producer`
3. `flow-lab-consumer`
4. `system-flow-investigator`

## Flow

- producer publishes to `lab/flow/in`
- consumer-producer subscribes to `lab/flow/in` and republishes to `lab/flow/out`
- consumer subscribes to `lab/flow/out`
- investigator currently subscribes to `lab/flow/#` and prints observed messages

## Run

```bash
mvn clean package
docker compose up --build
```

## Verify

You should see logs like:

- producer: `published topic=lab/flow/in seq=...`
- consumer-producer: `forwarded traceId=... seq=...`
- consumer: `consumed traceId=... seq=... step=consumer-producer`
- investigator: `observed topic=... payload=...`

## Optional failure simulation

Set `DROP_EVERY_N` in `docker-compose.yml` for `flow-lab-consumer-producer`.

Example:

```yaml
DROP_EVERY_N: 10
```

Then every 10th message will be dropped before publishing to `lab/flow/out`.


## Investigator phase 1

After `docker compose up --build`, the investigator auto-connects to EMQX and subscribes to `lab/flow/#`.

Useful endpoints:

- `GET http://localhost:8080/api/events/mqtt/topics`
- `GET http://localhost:8080/api/events/recent`
- `GET http://localhost:8080/api/events/recent?channel=lab/flow/in`
- `GET http://localhost:8080/api/stream/events`
- `POST http://localhost:8080/api/control/mqtt/subscribe`

Example subscribe body:

```json
{"topicFilter":"lab/flow/out","persistToFile":false}
```
