package org.ab

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.ab.repo.PipeLine

import scala.concurrent.ExecutionContextExecutor

object Main extends App {
  private implicit val sys: ActorSystem = ActorSystem("QuickStart")

  private implicit val ec: ExecutionContextExecutor = sys.dispatcher

  private implicit val conf = ConfigFactory.load()

  val (consumerControl, done) = PipeLine.run()

  done.onComplete(_ => sys.terminate())

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
    System.exit(0)
  }

}
