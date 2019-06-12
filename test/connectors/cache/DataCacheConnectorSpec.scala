/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors.cache

import config.AppConfig
import connectors.{AuthConnector, Authority}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
import services.cache.{Cache, MongoCacheClient, MongoCacheClientFactory}
import uk.gov.hmrc.play.frontend.auth.LoggedInUser
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import utils.AmlsSpec

import scala.concurrent.{ExecutionContext, Future}

class DataCacheConnectorSpec
  extends AmlsSpec
    with Conversions
    with ScalaFutures
    with PropertyChecks
    with IntegrationPatience {

  case class Model(value: String)
  object Model {
    implicit val format = Json.format[Model]
  }

  trait Fixture {

    implicit val user = mock[LoggedInUser]
    val key = "key"
    val cacheId = "12345678"
    val cache = Cache(cacheId, referenceMap())
    implicit val ec = mock[ExecutionContext]

    when(authContext.user) thenReturn user
    when(user.oid) thenReturn cacheId

    val mockAuthConnector = mock[AuthConnector]

    val factory = mock[MongoCacheClientFactory]
    val client = mock[MongoCacheClient]
    val authority = Authority("", Accounts(), "/user-details", "/ids", "12345678")
    when(factory.createClient) thenReturn client

    when {
      mockAuthConnector.getCurrentAuthority(any(), any())
    } thenReturn Future.successful(authority)

    when {
      mockAuthConnector.getCredId(any(), any())
    } thenReturn Future.successful("12345678")

    val appConfig = mock[AppConfig]

    val dataCacheConnector = new MongoCacheConnector(factory, mockAuthConnector) {
      override lazy val mongoCache: MongoCacheClient =  mock[MongoCacheClient]
    }
  }

  trait OidFixture {

    implicit val user = mock[LoggedInUser]
    val key = "key"
    val cacheId = "oid"
    val cache = Cache(cacheId, referenceMap())
    implicit val ec = mock[ExecutionContext]

    when(authContext.user) thenReturn user
    when(user.oid) thenReturn cacheId

    val mockAuthConnector = mock[AuthConnector]

    val factory = mock[MongoCacheClientFactory]
    val client = mock[MongoCacheClient]
    val authority = Authority("", Accounts(), "/user-details", "/ids", "12345678")
    when(factory.createClient) thenReturn client

    when {
      mockAuthConnector.getCurrentAuthority(any(), any())
    } thenReturn Future.successful(authority)

    when {
      mockAuthConnector.getCredId(any(), any())
    } thenReturn Future.successful("12345678")

    val appConfig = mock[AppConfig]

    val dataCacheConnector = new MongoCacheConnector(factory, mockAuthConnector) {
      override lazy val mongoCache: MongoCacheClient =  mock[MongoCacheClient]
    }
  }

  def referenceMap(str1: String = "", str2: String = ""): Map[String, JsValue] = Map(
    "dataKey" -> JsBoolean(true),
    "name" -> JsString(str1),
    "obj" -> Json.obj(
      "prop1" -> str2,
      "prop2" -> 12
    )
  )

  "DataCacheConnector for CredId" must {
    "save data to Mongo" in new Fixture {

      val model = Model("data")

      when {
        dataCacheConnector.mongoCache.createOrUpdateWithCacheMiss(cacheId, "12345678", model, key)
      } thenReturn Future.successful(cache)

      whenReady(dataCacheConnector.save(key, model)) { _ mustBe toCacheMap(cache) }
    }

    "fetch saved data from Mongo" in new Fixture {

      val model = Model("data")

      when {
        dataCacheConnector.mongoCache.find[Model](cacheId, key)
      } thenReturn Future.successful(Some(model))

      whenReady(dataCacheConnector.fetch[Model](key)) { _ mustBe Some(model) }
    }

    "fetch all data from Mongo" in new Fixture {

      val model = Model("data")

      when {
        dataCacheConnector.mongoCache.fetchAll(cacheId, false)
      } thenReturn Future.successful(Some(cache))

      whenReady(dataCacheConnector.fetchAll) { _ mustBe Some(toCacheMap(cache)) }
    }

    "remove data from Mongo" in new Fixture {
      forAll(arbitrary[Boolean]) { v =>
        when {
          dataCacheConnector.mongoCache.removeById(cacheId, false)
        } thenReturn Future.successful(v)

        whenReady(dataCacheConnector.remove) {
          _ mustBe (v)
        }
      }
    }
  }

  "DataCacheConnector for Oid" must {
    "save data to Mongo" in new OidFixture {

      val model = Model("data")

      when {
        dataCacheConnector.mongoCache.createOrUpdateWithCacheMiss(cacheId, "12345678", model, key)
      } thenReturn Future.successful(cache)

      when(dataCacheConnector.mongoCache.fetchAll("12345678", false)).thenReturn(Future.successful(None))
      when(dataCacheConnector.mongoCache.fetchAll(cacheId, true)).thenReturn(Future.successful(Some(cache)))

      whenReady(dataCacheConnector.save(key, model)) { _ mustBe toCacheMap(cache) }
    }

    "fetch saved data from Mongo" in new OidFixture {

      val model = Model("data")

      when {
        dataCacheConnector.mongoCache.find[Model]("12345678", key)
      } thenReturn Future.successful(None)

      when {
        dataCacheConnector.mongoCache.find[Model](cacheId, key)
      } thenReturn Future.successful(Some(model))

      whenReady(dataCacheConnector.fetch[Model](key)) { _ mustBe Some(model) }
    }

    "fetch all data from Mongo" in new OidFixture {

      val model = Model("data")

      when {
        dataCacheConnector.mongoCache.fetchAll(cacheId, true)
      } thenReturn Future.successful(Some(cache))

      whenReady(dataCacheConnector.fetchAll) { _ mustBe Some(toCacheMap(cache)) }
    }

    "remove data from Mongo" in new OidFixture {
      forAll(arbitrary[Boolean]) { v =>
        when {
          dataCacheConnector.mongoCache.removeById(cacheId, true)
        } thenReturn Future.successful(v)

        whenReady(dataCacheConnector.remove) {
          _ mustBe (v)
        }
      }
    }
  }
}