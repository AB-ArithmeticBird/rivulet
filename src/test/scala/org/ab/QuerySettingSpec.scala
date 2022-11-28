package org.ab

import org.ab.repo.http.QuerySettings.AllQueries
import org.ab.repo.http.{Authentication, QuerySettings}
import org.scalatest.funsuite.AnyFunSuite

class QuerySettingSpec extends AnyFunSuite {
  test("An default QuerySettings should have size 1") {
    val x = QuerySettings()
    assert(x.toQueryParams.toList.size == 1)
  }

  test("An custom QuerySettings should have size 6") {
    val x = QuerySettings(
      readOnly = AllQueries,
      authentication = Option(Authentication("admin", "123")),
      httpCompression = Option(true),
      settings = Map("a" -> "b", "c" -> "d"),
      retries = Option(1)
    )
    assert(x.toQueryParams.toList.size == 6)
  }
}
