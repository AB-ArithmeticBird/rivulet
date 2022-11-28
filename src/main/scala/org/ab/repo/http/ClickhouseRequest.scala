package org.ab.repo.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.Coders
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpEncodingRange.apply
import akka.http.scaladsl.model.headers.HttpEncodings.{deflate, gzip}
import akka.http.scaladsl.model.headers.{HttpEncoding, HttpEncodings, `Accept-Encoding`}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.QueueOfferResult
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import org.ab.repo.ClickHouseStreamingDataRepo

import scala.collection.immutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

trait ClickhouseRequest extends StrictLogging with Retry {
  this: ClickHouseStreamingDataRepo =>

  private val Headers = immutable.Seq(`Accept-Encoding`(gzip, deflate))


  private lazy val poolClientFlow = Http().cachedHostConnectionPool[Promise[HttpResponse]]("localhost", 8123)

  // see https://doc.akka.io/docs/akka-http/current/client-side/host-level.html?language=scala#examples
  private val BufferSize = 10
  private lazy val queue =
    Source.queue[(HttpRequest, Promise[HttpResponse])](BufferSize)
      .via(poolClientFlow)
      .to(Sink.foreach({
        case ((Success(resp), p)) => p.success(resp)
        case ((Failure(e), p)) => p.failure(e)
      }))
      .run()

  protected def executeRequest(host: Uri,
                               query: String,
                               settings: QuerySettings,
                               entity: RequestEntity): Future[String] =
    executeWithRetries(10, settings) { () =>

      val request = createRequest(host,
        query,
        settings,
        entity
      )

      queueRequest(request).flatMap {
        processResponse(_, query)
      }
    }

  // see https://doc.akka.io/docs/akka-http/current/client-side/host-level.html?language=scala#examples
  def queueRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise) match {
      case QueueOfferResult.Enqueued => responsePromise.future
      case QueueOfferResult.Dropped => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }

  protected def createRequest(uri: Uri,
                              query: String,
                              settings: QuerySettings,
                              entity: RequestEntity
                             ): HttpRequest = {
    val urlQuery = uri.withQuery(Query(Query("query" -> query) ++ settings.fromConfig(config).toQueryParams: _*))
    HttpRequest(
      method = HttpMethods.POST,
      uri = urlQuery,
      entity = entity,
      headers = Headers
    )
  }

  protected def processResponse(response: HttpResponse,
                                query: String): Future[String] =

    decodeResponse(response) match {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        handleSuccess(query: String, entity)
      case HttpResponse(code, _, entity, _) =>
        handleFailure(query, code, entity)
    }

  private def handleFailure(query: String, code: StatusCode, entity: ResponseEntity) = {
    Unmarshaller
      .stringUnmarshaller(entity)
      .flatMap(
        response =>
          Future.failed(
            new RuntimeException(s"Error occurred during execution $query. Server has returned code $code; $response")
          )
      )
  }

  private def handleSuccess(query: String, entity: ResponseEntity) = {
    Unmarshaller
      .stringUnmarshaller(entity)
      .map(content => {
        if (content.contains("DB::Exception")) {
          throw new RuntimeException("Exception at server during query execution")
        }
        content
      })
      .andThen {
        case Success(_) =>
          logger.debug(s"Successfully executed query $query")
        case Failure(exception) =>
          logger.error(s"Failed to execute query $query", exception)
      }
  }

  protected def decodeResponse(response: HttpResponse): HttpResponse = {

    val decoder = response.encoding match {
      case HttpEncodings.gzip => Coders.Gzip
      case HttpEncodings.deflate => Coders.Deflate
      case HttpEncodings.identity => Coders.NoCoding
      case enc@HttpEncodings.`x-compress` => throw new IllegalArgumentException(s"Unsupported response encoding: $enc")
      case enc@HttpEncodings.`x-zip` => throw new IllegalArgumentException(s"Unsupported response encoding: $enc")
      case enc@HttpEncodings.compress => throw new IllegalArgumentException(s"Unsupported response encoding: $enc")
      case enc@HttpEncodings.chunked => throw new IllegalArgumentException(s"Unsupported response encoding: $enc")
      case HttpEncoding(enc) => throw new IllegalArgumentException(s"Unknown response encoding: $enc")
    }
    decoder.decodeMessage(response)
  }


}
