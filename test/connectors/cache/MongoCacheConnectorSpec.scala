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

import connectors.{AuthConnector, Authority}
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
import services.cache.{Cache, MongoCacheClient, MongoCacheClientFactory}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser}

import scala.concurrent.{ExecutionContext, Future}

class MongoCacheConnectorSpec extends FreeSpec
  with MustMatchers
  with PropertyChecks
  with ScalaFutures
  with MockitoSugar
  with IntegrationPatience with Conversions {

  case class Model(tmp: String)

  object Model {
    implicit val formats = Json.format[Model]

    def apply(): Model = Model(arbitrary[String].sample.get)
  }

  trait Fixture {
    implicit val hc = HeaderCarrier()
    implicit val ac = mock[AuthContext]
    implicit val user = mock[LoggedInUser]
    implicit val ec = mock[ExecutionContext]

    val factory = mock[MongoCacheClientFactory]
    val client = mock[MongoCacheClient]
    val cacheId = arbitrary[String].sample.get
    val key = arbitrary[String].sample.get
    val mockAuthConnector = mock[AuthConnector]

    val authority = Authority("", Accounts(), "/user-details", "/ids", "12345678")
    val cacheMap = CacheMap("12345678", Map("id" -> Json.toJson("12345678")))

    when(ac.user) thenReturn user
    when(user.oid) thenReturn cacheId
    when(factory.createClient) thenReturn client

    when {
      mockAuthConnector.getCurrentAuthority(any(), any())
    } thenReturn Future.successful(authority)

    when {
      mockAuthConnector.getCredId(any(), any())
    } thenReturn Future.successful("12345678")

    val connector = new MongoCacheConnector(factory, mockAuthConnector) {
      override lazy val mongoCache: MongoCacheClient = client
    }
  }

  trait OidFixture {
    implicit val hc = HeaderCarrier()
    implicit val ac = mock[AuthContext]
    implicit val user = mock[LoggedInUser]
    implicit val ec = mock[ExecutionContext]

    val factory = mock[MongoCacheClientFactory]
    val client = mock[MongoCacheClient]
    val cacheId = arbitrary[String].sample.get
    val key = arbitrary[String].sample.get
    val mockAuthConnector = mock[AuthConnector]

    val authority = Authority("", Accounts(), "/user-details", "/ids", "12345678")
    val cacheMap = CacheMap("12345678", Map("id" -> Json.toJson("12345678")))

    when(ac.user) thenReturn user
    when(user.oid) thenReturn cacheId
    when(factory.createClient) thenReturn client

    when {
      mockAuthConnector.getCurrentAuthority(any(), any())
    } thenReturn Future.successful(authority)

    when {
      mockAuthConnector.getCredId(any(), any())
    } thenReturn Future.successful("12345678")

    val connector = new MongoCacheConnector(factory, mockAuthConnector) {
      override lazy val mongoCache: MongoCacheClient = client
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

  "fetch" - {
    "should delegate the call to the underlying mongo client for credId" in new Fixture {
      val model = Model("data")

      when(connector.mongoCache.find[Model](any(), any())(any())).thenReturn(Future.successful(Some(model)))

      whenReady(connector.fetch[Model](key)) { _ mustBe Some(model) }
    }

//    "should delegate the call to the underlying mongo client for Oid" in new OidFixture {
//      val model = Model("data")
//
////      when(connector.fetchByCredId[Model](any())).thenReturn(Future.successful(None))
//
//      //when(connector.mongoCache.find[Model](meq("12345678"), any())(any())).thenReturn(Future.successful(None))
//      when(connector.fetchByCredId[Model](meq(key))(ac, hc, any())).thenReturn(Future.successful(None))
//      when(connector.mongoCache.find[Model](meq(cacheId), meq(key))(any())).thenReturn(Future.successful(Some(model)))
//
//
//      whenReady(connector.fetch[Model](key)) { _ mustBe Some(model) }
//    }
  }

  "fetchAll" - {
    "should delegate the call to the underlying mongo client" in new Fixture with Conversions {
      val cache = Cache("12345678", referenceMap())

      when {
        connector.mongoCache.fetchAll("12345678", false)
      } thenReturn Future.successful(Some(cache))

      whenReady(connector.fetchAll) { _ mustBe Some(toCacheMap(cache)) }
    }
  }

  "save" - {
    "should delegate the call to the underlying mongo client" in new Fixture with Conversions {
      val model = Model()
      val cache = Cache("12345678", referenceMap())

      when {
        connector.mongoCache.createOrUpdateWithCacheMiss(cacheId, "12345678", model, key)
      } thenReturn Future.successful(cache)

      whenReady(connector.save(key, model)) { _ mustBe toCacheMap(cache) }
    }
  }

  "saveAll" - {
    "should convert the incoming CacheMap to a Cache before saving the data" in new Fixture {
      when(connector.mongoCache.saveAll(any())) thenReturn Future.successful(true)

      forAll(arbitrary[String], arbitrary[String]) { (str1, str2) =>
        val cacheMap = CacheMap("test", referenceMap(str1, str2))

        whenReady(connector.saveAll(Future.successful(cacheMap))) { cache =>
          cache.data mustBe referenceMap(str1, str2)
        }
      }
    }
  }

  "remove" - {
    "should delegate the call to the underlying mongo client" in new Fixture {
      forAll(arbitrary[Boolean]) { v =>
        reset(connector.mongoCache)
        when(connector.mongoCache.removeById("12345678", false)) thenReturn Future.successful(v)

        whenReady(connector.remove) { _ mustBe v }
      }
    }
  }

  "update" - {
    "should fetch and then save into the underlying mongo client" in new Fixture {
      val model = Model()
      val updatedModel = model.copy(tmp = "this has been updated")
      val f: Option[Model] => Model = { _ => updatedModel }

      when(connector.mongoCache.findWithCacheMiss[Model](cacheId, "12345678", key)) thenReturn Future.successful(Some(model))
      when(connector.mongoCache.createOrUpdateWithCacheMiss(cacheId, "12345678", updatedModel, key)) thenReturn Future.successful(Cache.empty)

      whenReady(connector.update[Model](key)(f)) { _ mustBe Some(updatedModel) }
    }
  }
}