/*
 * Copyright 2024 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.cache.Cache
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class DataCacheServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object DataCacheService extends DataCacheService {
    override private[services] val cacheConnector = mock[DataCacheConnector]
  }

  val cacheMap: Cache = Cache.empty
  val credID          = "12345678"

  "getCache" must {

    "Return a successful future when a cache exists" in {

      when {
        DataCacheService.cacheConnector.fetchAll(any())
      } thenReturn Future.successful(Some(cacheMap))

      whenReady(DataCacheService.getCache(credID)) { result =>
        result mustEqual cacheMap
      }
    }

    "Return a failed future when no cache exists" in {

      when {
        DataCacheService.cacheConnector.fetchAll(any())
      } thenReturn Future.successful(None)

      whenReady(DataCacheService.getCache(credID).failed) { exception =>
        exception mustBe a[NotFoundException]
      }
    }
  }
}
