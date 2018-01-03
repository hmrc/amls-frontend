/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import connectors.DataCacheConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import uk.gov.hmrc.http.{ HeaderCarrier, NotFoundException }

class DataCacheServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object DataCacheService extends DataCacheService {
    override private[services] val cacheConnector = mock[DataCacheConnector]
  }

  val cacheMap = CacheMap("", Map.empty)

  implicit val hc = HeaderCarrier()
  implicit val ac = mock[AuthContext]

  "getCache" must {

    "Return a successful future when a cache exists" in {

      when {
        DataCacheService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cacheMap))

      whenReady (DataCacheService.getCache) {
        result =>
          result mustEqual cacheMap
      }
    }

    "Return a failed future when no cache exists" in {

      when {
        DataCacheService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(None)

      val result = DataCacheService.getCache

      whenReady (DataCacheService.getCache.failed) {
        exception =>
          exception mustBe a[NotFoundException]
      }
    }
  }
}
