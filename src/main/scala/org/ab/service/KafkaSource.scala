package org.ab.service

import akka.actor.ActorSystem
import akka.kafka.javadsl.Consumer
import akka.kafka.{ConsumerMessage, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

class KafkaSource(topic: String, groupId: String)(implicit system: ActorSystem, config: Config)
  extends SourceLike[ConsumerMessage.CommittableMessage[String, String], Consumer.Control] {

  private val bootstrapServer = config.getString("bootstrap.servers")

  private val kafkaConfig = system.settings.config.getConfig("akka.kafka.consumer")

  override def source(): Source[ConsumerMessage.CommittableMessage[String, String], Consumer.Control] = {
    val consumerSettings = ConsumerSettings(kafkaConfig, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServer)
      .withGroupId(groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    Consumer.committableSource(consumerSettings, Subscriptions.topics(topic)).asScala
  }

}

object KafkaSource {
  def apply(topic: String, groupId: String)(implicit system: ActorSystem, config: Config): KafkaSource = new KafkaSource(topic, groupId)
}
