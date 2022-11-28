package org.ab.service

import akka.stream.scaladsl.Source

trait SourceLike[T, U] {
  def source(): Source[T, U]
}
