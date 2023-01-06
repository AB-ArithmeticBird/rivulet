This application is shows that how to use akka to stream data from a kafka topic to clickhouse cluster
Setup
================

1. To start, you will need to have a working installation of docker and docker-compose.
2. Clone this repository.
3. Go to docker directory and run `make config up` to start the containers.
4. It will start a clickhouse cluster, a kafka cluster and a zookeeper cluster.
5. You can access the clickhouse cluster at http://localhost:8123
6. You can access the kafka cluster at http://localhost:9092
7. You can access the zookeeper cluster at http://localhost:2181
8. You can access the kafka ui at http://localhost:8080
9. create a topic named `pizza-orders` in kafka ui.
10. create the database and table in clickhouse

```
CREATE DATABASE rivulet_db ON CLUSTER 'company_cluster';

CREATE TABLE IF NOT EXISTS rivulet_db.events
(
    timestamp Int64,
    message String
) ENGINE = Memory()


```

11. Run the producer to start producing messages to kafka. I
    used https://github.com/aiven/python-fake-data-producer-for-apache-kafka
    to produce fake data.

```
    python main.py \                                                                                                                                                           ─╯
  --security-protocol plaintext \
  --host localhost \
  --port 9092 \
  --topic-name pizza-orders \
  --nr-messages 8 \
  --max-waiting-time 0 \
  --subject pizza
  ```

13. You can see the messages in kafka ui.
14. Run the application using

> sbt
> clean compile
> run_client

15. You can see the messages in clickhouse ui.

```
select count(*) from rivulet_db.events;
```

Details
================

1. The application is built using akka streams and alpakka kafka client. It is using at least once delivery semantics.
2. It is commiting the offset to kafka in batches. As of now, We are reading batch-size =4 message
   at a time and waiting for 1000 such batches before committing the offset to kafka. The flush interval is 5 seconds.
   All of this is configurable.
3. The application is using the `KafkaConsumer.committableSource` to read the messages from kafka.
4. The application is using the `KafkaConsumer.committableOffsetBatch` to commit the offset to kafka in batches.
5. The application has abstracted the source and sink logic into a separate component. This can be used to create a
   source other than kafka and sink to other data stores.
6. Clickhouse http api is used to create a reactive sink.Under the hood, it is using akka http client.

Limitation
================

1. The application can lose messages at the time of re-balancing. We need to create balancing Listener to mitigate this.
2. We are using just few query settings from clickhouse. We can add more query settings to make it more robust. One
   important addon can
   to show the query progression using HTTP headers. see the TODO section in QuerySettings.scala
3. For test purpose it is using memory engine. I think other engine can be better
   suited for this use case.
4. The table has two columns. The whole event is stored in one column. A view can be created on top of this table to
   make it more readable.
5. It can be turned into a microservice. We can use akka http to expose the api to create the table and view and start
   the streaming
6. once it is turned into a microservice we can add metrics and health check.
7. Scalability - This app is quite simple and can be scaled horizontally. We can use akka cluster to make it more
   robust. topics can be used as sharding key.
8. We can use akka persistence to store the offset in case of failure. We can use akka persistence query to read the
   offset from the store.
