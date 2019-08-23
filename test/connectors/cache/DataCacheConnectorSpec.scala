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
import connectors.Authority
import org.mockito.Matchers.any
import org.mockito.Mockito._
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
    val oId = "oldId"
    val credId = "12345678"
    val cache = Cache(oId, referenceMap())
    val newCache = cache.copy(id = credId)
    implicit val ec = mock[ExecutionContext]

//    when(authContext.user) thenReturn user
//    when(user.oid) thenReturn oId

//    val mockAuthConnector = mock[AuthConnector]

    val factory = mock[MongoCacheClientFactory]
    val client = mock[MongoCacheClient]
    val authority = Authority("", Accounts(), "/user-details", "/ids", credId)

    when(factory.createClient) thenReturn client

//    when {
//      mockAuthConnector.getCurrentAuthority(any(), any())
//    } thenReturn Future.successful(authority)

//    when {
//      mockAuthConnector.getgetCredId(any(), any())
//    } thenReturn Future.successful(credId)

    val appConfig = mock[AppConfig]

    val dataCacheConnector = new MongoCacheConnector(factory) {
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

  "DataCacheConnector" must {
    "save data to Mongo" in new Fixture {
      val model = Model("data")

      when {
        dataCacheConnector.mongoCache.createOrUpdate(credId, model, key)
      } thenReturn Future.successful(newCache)

      whenReady(dataCacheConnector.save(credId, key, model)) { result =>
        result mustBe toCacheMap(newCache)
        result.id mustBe credId
      }
    }

    "fetch saved data from Mongo" in new Fixture {
      val model = Model("data")

      when {
        dataCacheConnector.mongoCache.find[Model](credId, key)
      } thenReturn Future.successful(Some(model))

      whenReady(dataCacheConnector.fetch[Model](credId, key)) { _ mustBe Some(model) }
    }

    "fetch all data from Mongo" in new Fixture {
      val model = Model("data")

      when {
        dataCacheConnector.mongoCache.fetchAll(Some(credId))
      } thenReturn Future.successful(Some(newCache))

      whenReady(dataCacheConnector.fetchAll(credId)) { _ mustBe Some(toCacheMap(newCache)) }
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