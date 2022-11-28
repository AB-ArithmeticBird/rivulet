package org.ab.repo.http

import akka.http.scaladsl.model.Uri.Query
import com.typesafe.config.Config
import org.ab.repo.http.QuerySettings.{AllQueries, ReadOnlySetting}

import scala.util.Try

case class Authentication(
                           user: String,
                           password: String,
                         )

/**
 * Query settings for Clickhouse
 * The optional ‘readonly’ parameter can be passed as 1 or 0. If it is 1, the query is executed in read-only mode.
 * The optional ‘enable_http_compression’ parameter can be passed as 1 or 0.
 * The optional ‘user’ parameter can be passed as the user name. The ‘user’ parameter is used to identify the user in the system tables.
 * The optional ‘password’ parameter can be passed as the password. The ‘password’ parameter is used to identify the user in the system tables.
 * The optional ‘settings’ parameter can be passed as a list of settings in the form of ‘name=value’ separated by ampersands.
 * The optional ‘retries’ parameter can be passed as the number of retries. If it is 0, the query is executed without retries.
 *
 * $TODO  The optional ‘query_id’ parameter can be passed as the query ID (any string). For more information,
 * see the section “Settings, replace_running_query”. The ‘query_id’ parameter is used to identify the query in the system tables.
 * $TODO The optional ‘profile’ parameter can be passed as the name of the query processing profile.
 * For more information,https://clickhouse.com/docs/en/operations/settings/settings_profiles/
 * $TODO progressHeaders The optional ‘send_progress_in_http_headers’ parameter can be passed as 1 or 0.
 * $TODO The optional ‘idempotent’ parameter can be passed as 1 or 0. If it is 1, the query is executed in idempotent mode.
 * $TODO The optional ‘max_execution_time’ parameter can be passed as the maximum execution time in seconds.
 * $TODO The optional ‘max_memory_usage’ parameter can be passed as the maximum memory usage in bytes.
 * $TODO The optional ‘max_rows_to_read’ parameter can be passed as the maximum number of rows to read.
 * $TODO The optional ‘max_bytes_to_read’ parameter can be passed as the maximum number of bytes to read.
 * $TODO The optional ‘max_rows_to_group_by’ parameter can be passed as the maximum number of rows to group by.
 * $TODO The optional ‘max_bytes_to_group_by’ parameter can be passed as the maximum number of bytes to group by.
 * $TODO The optional ‘max_rows_to_sort’ parameter can be passed as the maximum number of rows to sort.
 * $TODO The optional ‘max_bytes_to_sort’ parameter can be passed as the maximum number of bytes to sort.
 * $TODO The optional ‘max_result_rows’ parameter can be passed as the maximum number of rows in the result.
 * $TODO The optional ‘max_result_bytes’ parameter can be passed as the maximum number of bytes in the result.
 * $TODO The optional ‘max_result_rows_to_transfer’ parameter can be passed as the maximum number of rows to transfer.
 * $TODO The optional ‘max_result_bytes_to_transfer’ parameter can be passed as the maximum number of bytes to transfer.
 * $TODO The optional ‘max_execution_time’ parameter can be passed as the maximum execution time in seconds.
 *
 */
case class QuerySettings(readOnly: ReadOnlySetting = AllQueries,
                         authentication: Option[Authentication] = None,
                         httpCompression: Option[Boolean] = None,
                         settings: Map[String, String] = Map.empty,
                         retries: Option[Int] = None) {

  def toQueryParams: Query =
    Query(
      settings ++ (Seq("readonly" -> readOnly.value.toString) ++
        authentication.map(
          auth => "user" -> auth.user
        ) ++
        authentication.map(auth => "password" -> auth.password) ++
        httpCompression
          .map(compression => "enable_http_compression" -> (if (compression) "1" else "0"))).toMap
    )

  def fromConfig(config: Config): QuerySettings = {

    this.copy(
      authentication = authentication.orElse(Try {
        val authConfig = config.getConfig("authentication")
        Authentication(authConfig.getString("user"), authConfig.getString("password"))
      }.toOption),
      httpCompression = httpCompression.orElse(Try {
        config.getBoolean("http-compression")
      }.toOption),
    )
  }
}

object QuerySettings {
  /**
   * https://clickhouse.tech/docs/en/interfaces/http/#http-headers
   * readonly - 0 - all queries, 1 - only SELECT queries, 2 - no queries
   * 0 — Read, Write, and Change settings queries are allowed.
   * 1 — Only Read data queries are allowed.
   * 2 — Read data and Change settings queries are allowed.
   */
  sealed trait ReadOnlySetting extends Product with Serializable {
    val value: Int
  }

  case object AllQueries extends ReadOnlySetting {
    override val value: Int = 0
  }

  case object ReadQueries extends ReadOnlySetting {
    override val value: Int = 1
  }

  case object ReadAndChangeQueries extends ReadOnlySetting {
    override val value: Int = 2
  }
}
