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

package connectors.cache

import org.scalatest.prop.PropertyChecks
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
import services.cache.MongoCacheClient
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import org.mockito.Mockito.when
import org.mockito.Matchers.any

import scala.concurrent.Future

class MongoCacheConnectorSpec extends FreeSpec
  with MustMatchers
  with PropertyChecks
  with ScalaFutures
  with MockitoSugar {

  trait Fixture extends Conversions {
    implicit val hc = HeaderCarrier()
    implicit val ac = mock[AuthContext]
  }

  def referenceJson(str1: String, str2: String): JsValue = Json.obj(
    "dataKey" -> true,
    "name" -> str1,
    "obj" -> Json.obj(
      "prop1" -> str2,
      "prop2" -> 12
    )
  )

  def referenceMap(str1: String, str2: String) = Map[String, JsValue](
    "dataKey" -> JsBoolean(true),
    "name" -> JsString(str1),
    "obj" -> Json.obj(
      "prop1" -> str2,
      "prop2" -> 12
    )
  )

  "toMap" - {

    "should convert from a JsValue to a Map[String, JsValue] properly" in new Fixture {

      forAll(arbitrary[String], arbitrary[String]) { (str1, str2) =>
        toMap(referenceJson(str1, str2)) mustBe referenceMap(str1, str2)
      }
    }

  }

  "toCacheMap" - {

    "should convert from a Cache type to a CacheMap type" in new Fixture {

      forAll(arbitrary[String], arbitrary[String], arbitrary[String]) { (cacheId, str1, str2) =>

        val cache = Cache(cacheId, Some(referenceJson(str1, str2)))

        toCacheMap(cache) mustBe CacheMap(cacheId, referenceMap(str1, str2))
      }

    }

  }

  "saveAll" - {

    "should convert the incoming CacheMap to a Cache before saving the data" in new Fixture {

      val client = mock[MongoCacheClient]
      when(client.saveAll(any())) thenReturn Future.successful(true)

      forAll(arbitrary[String], arbitrary[String]) { (str1, str2) =>
        val connector = new MongoCacheConnector(client)
        val cacheMap = CacheMap("test", referenceMap(str1, str2))

        whenReady(connector.saveAll(cacheMap)) { cache =>
          cache.data mustBe Some(referenceJson(str1, str2))
        }
      }
    }

  }

}
