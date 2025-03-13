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

package connectors.cache

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json._
import services.cache.{Cache, MongoCacheClient, MongoCacheClientFactory}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoCacheConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with ScalaFutures
    with MockitoSugar
    with IntegrationPatience
    with Conversions {

  case class Model(tmp: String)

  object Model {
    implicit val formats: OFormat[Model] = Json.format[Model]

    def apply(): Model = Model(arbitrary[String].sample.get)
  }

  trait Fixture {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val factory: MongoCacheClientFactory = mock[MongoCacheClientFactory]
    val client: MongoCacheClient         = mock[MongoCacheClient]
    val credId                           = "12345678"
    val key: String                      = arbitrary[String].sample.get

    val cacheMap: Cache = Cache("12345678", Map("id" -> Json.toJson("12345678")))

    when(factory.createClient) thenReturn client

    val connector = new MongoCacheConnector(factory)
  }

  def referenceMap(str1: String = "", str2: String = ""): Map[String, JsValue] = Map(
    "dataKey" -> JsBoolean(true),
    "name"    -> JsString(str1),
    "obj"     -> Json.obj(
      "prop1" -> str2,
      "prop2" -> 12
    )
  )

  "fetch" - {
    "should delegate the call to the underlying mongo client for credId" in new Fixture {
      val model: Model = Model("data")

      when(client.find[Model](any(), any())(any())).thenReturn(Future.successful(Some(model)))

      whenReady(connector.fetch[Model](credId, key))(_ mustBe Some(model))
    }
  }

  "fetchAll" - {
    "should delegate the call to the underlying mongo client for CredId" in new Fixture with Conversions {
      val cache: Cache = Cache(credId, referenceMap())

      when {
        client.fetchAll(Some(credId))
      } thenReturn Future.successful(Some(cache))

      whenReady(connector.fetchAll(credId))(_ mustBe Some(cache))
    }

  }

  "save" - {
    "should delegate the call to the underlying mongo client for CredId" in new Fixture with Conversions {
      val model: Model = Model()
      val cache: Cache = Cache(credId, referenceMap())

      when(client.createOrUpdate(any(), any(), meq(key))(any())) thenReturn Future.successful(cache)

      whenReady(connector.save(credId, key, model)) { result =>
        result mustBe cache
        result.id mustBe credId
      }
    }

  }

  "saveAll" - {
    "should convert the incoming CacheMap to a Cache before saving the data" in new Fixture {
      when(client.saveAll(any(), any())) thenReturn Future.successful(true)

      forAll(arbitrary[String], arbitrary[String]) { (str1, str2) =>
        val cacheMap = Cache("test", referenceMap(str1, str2))

        whenReady(connector.saveAll(credId, Future.successful(cacheMap))) { cache =>
          cache.data mustBe referenceMap(str1, str2)
        }
      }
    }
  }

  "remove" - {
    "should delegate the call to the underlying mongo client" in new Fixture {
      reset(client)
      when(client.removeById(credId)) thenReturn Future.successful(true)

      whenReady(connector.remove(credId))(_ mustBe true)
    }

    "should delegate the call to the underlying mongo client and return true if removed for Cred ID" in new Fixture {
      reset(client)
      when(client.removeById(credId)) thenReturn Future.successful(true)

      whenReady(connector.remove(credId))(_ mustBe true)
    }

    "should delegate the call to the underlying mongo client and return false if neither removed" in new Fixture {
      reset(client)
      when(client.removeById(credId)) thenReturn Future.successful(false)

      whenReady(connector.remove(credId))(_ mustBe false)
    }
  }

  "update" - {
    "should fetch and then save into the underlying mongo client" in new Fixture {
      val model: Model              = Model()
      val updatedModel: Model       = model.copy(tmp = "this has been updated")
      val f: Option[Model] => Model = { _ => updatedModel }

      when(client.find[Model](any(), meq(key))(any())) thenReturn Future.successful(Some(model))
      when(client.createOrUpdate(any(), meq(updatedModel), meq(key))(any())) thenReturn Future.successful(Cache.empty)

      whenReady(connector.update[Model](credId, key)(f))(_ mustBe Some(updatedModel))
    }
  }
}
