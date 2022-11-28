//package org.ab
//import akka.kafka.{ConsumerMessage, Subscriptions}
//import akka.kafka.scaladsl.Consumer.DrainingControl
//import akka.kafka.scaladsl.{Committer, Consumer}
//import akka.kafka.testkit.internal.TestFrameworkInterface
//import akka.kafka.testkit.internal.TestcontainersKafka.Singleton.kafkaPort
//import akka.kafka.testkit.scaladsl.{KafkaSpec, TestcontainersKafkaLike}
//import akka.stream.scaladsl.{Sink, Source}
//import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
//import org.ab.repo.PipeLine
//import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
//import org.scalatest.flatspec.AnyFlatSpecLike
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
//import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
//
//import scala.concurrent.{Await, Future}
//
//class KafkaConsumerSourceSpec extends KafkaSpec(kafkaPort)
//  with AnyFlatSpecLike
//  with TestFrameworkInterface.Scalatest
//  with Matchers
//  with ScalaFutures
//  with IntegrationPatience
//  with Eventually with TestcontainersKafkaLike   {
//
//  "Consume messages at-least-once" should "work" in assertAllStagesStopped {
//    val source = (1 to 4).map{
//      ConsumerMessage.CommittableMessage[String, String](
//        ConsumerRecordFactory[String, String](topic1, _).create(),
//      )
//    }
//    val flow = PipeLine.flow
//    source.via(flow).runWith(Sink.seq).futureValue should contain theSameElementsAs (1 to 4)
//  }
//}
