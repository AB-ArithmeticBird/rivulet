package org.ab.repo

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Committer
import akka.kafka.{CommitterSettings, ConsumerMessage}
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.typesafe.config.Config
import org.ab.repo.http.{ClickhouseRequest, QuerySettings}
import org.ab.{InsertOp, KafkaRec}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}


trait ClickHouseStreamingDataRepo extends ClickhouseRequest with StreamingDataRepo[InsertOp[KafkaRec], Future, Done] {

  protected implicit val system: ActorSystem
  protected implicit val config: Config

  protected implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  /**
   * This method returns a Sink that can be used to insert data into ClickHouse
   *
   * @param ec
   * @param settings
   * @return
   */
  override def toSink()(implicit ec: ExecutionContext, settings: QuerySettings): Sink[InsertOp[KafkaRec], Future[Done]] = {
    val committerDefaults = CommitterSettings(system)
    val batchSize = config.getString("clickhouse.client.db-batch-size").toInt
    val flushInterval = config.getInt("clickhouse.client.db-flush-interval").seconds
    flow(batchSize, flushInterval)
      .toMat(Committer.sink(committerDefaults))(Keep.right)

  }


  def flow(batchSize: Int, flushInterval: FiniteDuration)(implicit ec: ExecutionContext, settings: QuerySettings) = {
    Flow[InsertOp[KafkaRec]]
      .groupBy(Int.MaxValue, _.tableName)
      .groupedWithin(batchSize, flushInterval)
      .mapAsync(/*"concurrent-requests"*/ 1)(operations => {
        val tableName = operations.head.tableName
        val offsets = operations.flatMap(a => a.records.map(_.offset))
        insertTable(tableName, operations.flatMap(a => a.records.map(_.data))).map(_ =>
          ConsumerMessage.CommittableOffsetBatch(offsets)
        )
      }).mergeSubstreams
  }

  /**
   * Inserts a row into ClickHouse Table
   *
   * @param table    The table name where the record will be inserted
   * @param payload  The record/s to be inserted
   * @param settings The settings for the query
   * @return
   */
  protected def insertTable(table: String, payload: Seq[String])(
    implicit settings: QuerySettings
  ): Future[String] = {
    logger.debug(s"Inserting ${payload.size} entries in table: $table.")
    val host = s"http://${config.getString("clickhouse.client.host")}:${config.getInt("clickhouse.client.port")}/${config.getString("clickhouse.client.database")}"
    executeRequest(host, s"INSERT INTO $table FORMAT JSONEachRow", settings, payload.mkString("\n"))
      .recover {
        case ex => throw new RuntimeException("failed to Insert", ex)
      }
  }
}


