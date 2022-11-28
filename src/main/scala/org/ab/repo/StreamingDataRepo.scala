package org.ab.repo

import akka.stream.scaladsl.Sink
import org.ab.repo.http.QuerySettings

import scala.concurrent.ExecutionContext

trait StreamingDataRepo[A, F[_], R] {
  def toSink()(
    implicit ec: ExecutionContext,
    settings: QuerySettings = QuerySettings()
  ): Sink[A, F[R]]
}
