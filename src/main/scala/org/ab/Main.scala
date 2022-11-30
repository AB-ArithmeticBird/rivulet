package org.ab

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import org.ab.service.PipeLine

import scala.concurrent.ExecutionContextExecutor

object Main extends App with StrictLogging{
  private implicit val sys: ActorSystem = ActorSystem("Pipeline")

  private implicit val ec: ExecutionContextExecutor = sys.dispatcher

  private implicit val conf = ConfigFactory.load()

  val pipeline =  PipeLine (sys, conf)

  /**
   * Main entry point for the application. Starts the pipeline from kafka topic to clickhouse table
   */
  val (consumerControl, done) = pipeline.run()
  logger.warn("pipeline started ...")

  /**
   * This is a blocking call that will wait for the actor system to terminate
   */
  done.andThen { case s =>
    consumerControl.shutdown()
    sys.terminate()
  }

  /**
   * This is a blocking call that will wait for the actor system to terminate
   */
  sys.registerOnTermination {
    logger.warn("I am dying")
    System.exit(0)
  }

}
