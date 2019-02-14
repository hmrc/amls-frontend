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

import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
import services.cache.{Cache, MongoCacheClient, MongoCacheClientFactory}
import uk.gov.hmrc.play.frontend.auth.LoggedInUser
import utils.AmlsSpec

import scala.concurrent.Future

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
    val cacheId = arbitrary[String].sample.get
    val cache = Cache(cacheId, referenceMap())

    when(authContext.user) thenReturn user
    when(user.oid) thenReturn cacheId
  }

  val factory = mock[MongoCacheClientFactory]
  val client = mock[MongoCacheClient]
  when(factory.createClient) thenReturn client

  def referenceMap(str1: String = "", str2: String = ""): Map[String, JsValue] = Map(
    "dataKey" -> JsBoolean(true),
    "name" -> JsString(str1),
    "obj" -> Json.obj(
      "prop1" -> str2,
      "prop2" -> 12
    )
  )

  object DataCacheConnector extends MongoCacheConnector(factory) {
    override lazy val mongoCache: MongoCacheClient = mock[MongoCacheClient]
  }

  "DataCacheConnector" must {

    "save data to Mongo" in new Fixture {

      val model = Model("data")

      when {
        DataCacheConnector.mongoCache.createOrUpdate(cacheId, model, key)
      } thenReturn Future.successful(cache)

      whenReady(DataCacheConnector.save(key, model)) { _ mustBe toCacheMap(cache) }
    }

    "fetch saved data from Mongo" in new Fixture {

      val model = Model("data")

      when {
        DataCacheConnector.mongoCache.find[Model](cacheId, key)
      } thenReturn Future.successful(Some(model))

      whenReady(DataCacheConnector.fetch[Model](key)) { _ mustBe Some(model) }
    }

    "fetch all data from Mongo" in new Fixture {

      val model = Model("data")

      when {
        DataCacheConnector.mongoCache.fetchAll(cacheId)
      } thenReturn Future.successful(Some(cache))

      whenReady(DataCacheConnector.fetchAll) { _ mustBe Some(toCacheMap(cache)) }
    }

    "remove data from Mongo" in new Fixture {

      forAll(arbitrary[Boolean]) { v =>
        when {
          DataCacheConnector.mongoCache.removeById(cacheId)
        } thenReturn Future.successful(v)

        whenReady(DataCacheConnector.remove) {
          _ mustBe (v)
        }
      }
    }
  }
}