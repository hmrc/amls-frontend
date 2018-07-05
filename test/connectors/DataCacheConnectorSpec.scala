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

package connectors

import config.AmlsShortLivedCache
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser}

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

class DataCacheConnectorSpec
  extends PlaySpec
    with OneAppPerSuite
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience {

  case class Model(value: String)
  object Model {
    implicit val format = Json.format[Model]
  }

  trait Fixture {

    implicit val headerCarrier = HeaderCarrier()
    implicit val authContext = mock[AuthContext]
    implicit val user = mock[LoggedInUser]
    val oid = "user_oid"
    val key = "key"

    when(authContext.user) thenReturn user
    when(user.oid) thenReturn oid
  }

  val emptyCache = CacheMap("", Map.empty)

  object DataCacheConnector extends S4LCacheConnector {
    override lazy val shortLivedCache: ShortLivedCache = mock[ShortLivedCache]
  }

  "DataCacheConnector" must {

    "save data to save4later" in new Fixture {

      val model = Model("data")

      when {
        DataCacheConnector.shortLivedCache.cache(eqTo(oid), eqTo(key), eqTo(model))(any(), any(), any())
      } thenReturn Future.successful(emptyCache)

      val result = DataCacheConnector.save(key, model)

      whenReady(result) {
        result =>
          result must be (emptyCache)
      }
    }

    "fetch saved data from save4later" in new Fixture {

      val model = Model("data")

      when {
        DataCacheConnector.shortLivedCache.fetchAndGetEntry[Model](eqTo(oid), eqTo(key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))

      val result = DataCacheConnector.fetch[Model](key)

      whenReady (result) {
        result =>
          result must be (Some(model))
      }
    }

    "fetch all data from save4later" in new Fixture {

      val model = Model("data")

      when {
        DataCacheConnector.shortLivedCache.fetch(eqTo(oid))(any(), any())
      } thenReturn Future.successful(Some(emptyCache))

      val result = DataCacheConnector.fetchAll

      whenReady(result) {
        result =>
          result must be (Some(emptyCache))
      }
    }

    "remove data from save4later" in new Fixture {

      val response = mock[HttpResponse]

      when {
        DataCacheConnector.shortLivedCache.remove(any())(any(), any())
      } thenReturn Future.successful(response)

      val result = DataCacheConnector.shortLivedCache.remove(eqTo(key))(any(), any())

      whenReady(result) {
        result =>
          result must be (response)
      }
    }
  }
}
