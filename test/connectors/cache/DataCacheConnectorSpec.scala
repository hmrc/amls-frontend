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

import config.ApplicationConfig
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json, OFormat}
import services.cache.{Cache, MongoCacheClient, MongoCacheClientFactory}
import utils.AmlsSpec

import scala.concurrent.Future

class DataCacheConnectorSpec
    extends AmlsSpec
    with Conversions
    with ScalaFutures
    with ScalaCheckPropertyChecks
    with IntegrationPatience {

  case class Model(value: String)
  object Model {
    implicit val format: OFormat[Model] = Json.format[Model]
  }

  trait Fixture {

    val key             = "key"
    val oId             = "oldId"
    val credId          = "12345678"
    val cache: Cache    = Cache(oId, referenceMap())
    val newCache: Cache = cache.copy(id = credId)

    val factory: MongoCacheClientFactory = mock[MongoCacheClientFactory]
    val client: MongoCacheClient         = mock[MongoCacheClient]

    when(factory.createClient) thenReturn client

    val appConfig: ApplicationConfig = mock[ApplicationConfig]

    val dataCacheConnector: MongoCacheConnector = new MongoCacheConnector(factory) {
      override lazy val mongoCache: MongoCacheClient = mock[MongoCacheClient]
    }
  }

  def referenceMap(str1: String = "", str2: String = ""): Map[String, JsValue] = Map(
    "dataKey" -> JsBoolean(true),
    "name"    -> JsString(str1),
    "obj"     -> Json.obj(
      "prop1" -> str2,
      "prop2" -> 12
    )
  )

  "DataCacheConnector" must {
    "save data to Mongo" in new Fixture {
      val model: Model = Model("data")

      when {
        dataCacheConnector.mongoCache.createOrUpdate(credId, model, key)
      } thenReturn Future.successful(newCache)

      whenReady(dataCacheConnector.save(credId, key, model)) { result =>
        result mustBe newCache
        result.id mustBe credId
      }
    }

    "fetch saved data from Mongo" in new Fixture {
      val model: Model = Model("data")

      when {
        dataCacheConnector.mongoCache.find[Model](credId, key)
      } thenReturn Future.successful(Some(model))

      whenReady(dataCacheConnector.fetch[Model](credId, key))(_ mustBe Some(model))
    }

    "fetch all data from Mongo" in new Fixture {

      when {
        dataCacheConnector.mongoCache.fetchAll(Some(credId))
      } thenReturn Future.successful(Some(newCache))

      whenReady(dataCacheConnector.fetchAll(credId))(_ mustBe Some(newCache))
    }

    "remove data from Mongo for CredId" in new Fixture {

      when {
        dataCacheConnector.mongoCache.removeById(credId)
      } thenReturn Future.successful(true)

      whenReady(dataCacheConnector.remove(credId)) {
        _ mustBe true
      }
    }
  }
}
