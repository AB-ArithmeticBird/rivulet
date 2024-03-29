package org.ab.service

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage
import akka.kafka.javadsl.Consumer
import akka.stream.scaladsl.{Flow, Keep}
import akka.{Done, NotUsed}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import org.ab.repo.ClickHouseStreamingDataRepo
import org.ab.{InsertOp, KafkaRec}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

trait PipeLine extends StrictLogging {
  implicit val conf: Config
  implicit val sys: ActorSystem

  private implicit lazy val ec: ExecutionContextExecutor = sys.dispatcher

  private val accumulateInterval = conf.getString("clickhouse.client.accumulate-interval").toInt.seconds
  private val BatchSize = conf.getString("batch-size").toInt

  private val topic: String = conf.getString("topic")
  private val groupId: String = conf.getString("consumer-group-id")
  /**
   * KafkaSource is akka stream source for akka-kafka consumer
   */
  val kafkaSource: KafkaSource = KafkaSource(topic, groupId)

  /**
   * ClickHouseStreamingDataRepo is a trait that provides a method toSink() which returns a Sink
   * that can be used to insert data into ClickHouse
   */
  object Inserter extends ClickHouseStreamingDataRepo {
    override protected implicit lazy val system: ActorSystem = sys
    override protected implicit lazy val config: Config = conf
  }

  /**
   * Wiring all the components together to create the pipeline from kafka to clickhouse
   */
  private val p = kafkaSource
    .source()
    .via(flow)
    .toMat(Inserter.toSink())(Keep.both)

  def run(): (Consumer.Control, Future[Done]) = {
    logger.warn("Starting pipeline")
    p.run()
  }

  /**
   * This flow is responsible for converting the kafka records to the format that can be inserted
   */

  private def flow: Flow[ConsumerMessage.CommittableMessage[String, String], InsertOp[KafkaRec], NotUsed] = {
    Flow[ConsumerMessage.CommittableMessage[String, String]]
      .groupedWithin(BatchSize, accumulateInterval)
      .map { rows =>
        val records = rows.map { data =>
          val rec = data.record.value()
          KafkaRec(data.committableOffset,
            s"""
               |{"timestamp":"${data.record.timestamp()}","message":"${rec.replace("\"", "\\\"")}"}
               |""".stripMargin)

        }
        InsertOp("rivulet_db.events", records.toList)
      }

  }
}

object PipeLine extends StrictLogging {
  def apply(system: ActorSystem, config: Config): PipeLine = new PipeLine {
    logger.warn("creating pipeline instance ...")
    override implicit lazy val conf: Config = config
    override implicit lazy val sys: ActorSystem = system
  }
}
