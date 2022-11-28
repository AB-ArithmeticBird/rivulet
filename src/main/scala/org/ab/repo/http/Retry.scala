package org.ab.repo.http

import akka.stream.StreamTcpException

import scala.concurrent.{ExecutionContext, Future}

trait Retry {
  this: ClickhouseRequest =>

  protected def executeWithRetries(retries: Int,
                                   settings: QuerySettings)(
                                    request: () => Future[String]
                                  )(implicit ec: ExecutionContext): Future[String] =
    request().recoverWith {
      case e: StreamTcpException if retries > 0 =>
        logger.warn(s"Retrying after error, retries left: $retries", e)
        executeWithRetries(retries - 1, settings)(request)
      case e: RuntimeException
        if e.getMessage.contains("The http server closed the connection unexpectedly") && retries > 0 =>
        logger.warn(s"Unexpected connection closure, retries left: $retries", e)
        executeWithRetries(retries - 1, settings)(request)
    }
}
