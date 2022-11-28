package org.ab

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import org.ab.service.PipeLine

import scala.concurrent.ExecutionContextExecutor

object Main extends App with StrictLogging{
  private implicit val sys: ActorSystem = ActorSystem("QuickStart")

  private implicit val ec: ExecutionContextExecutor = sys.dispatcher

  private implicit val conf = ConfigFactory.load()

  val (consumerControl, done) = PipeLine.run()

  done.onComplete{_ =>
    logger.warn("I am here")
    sys.terminate()
  }

  /**
   * This is a blocking call that will wait for the actor system to terminate
   */
  done.andThen { case s =>
    logger.warn("I am here2")
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
