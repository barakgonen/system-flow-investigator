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
