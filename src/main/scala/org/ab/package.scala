package org

import akka.kafka.ConsumerMessage

package object ab {

  case class InsertOp[T](tableName: String, records: List[T])

  case class KafkaRec(offset: ConsumerMessage.CommittableOffset, data: String)
}
