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
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.http.cache.client.CacheMap

class AmlsMongoCacheConnectorSpec extends FreeSpec with MustMatchers with PropertyChecks {

  val referenceJson: JsValue = Json.obj(
    "dataKey" -> true,
    "name" -> "Some name",
    "obj" -> Json.obj(
      "prop1" -> "some string",
      "prop2" -> 12
    )
  )

  "toMap" - {

    "should convert from a JsValue to a Map[String, JsValue] properly" in {

      AmlsMongoCacheConnector.toMap(referenceJson) mustBe Map[String, JsValue](
        "dataKey" -> JsBoolean(true),
        "name" -> JsString("Some name"),
        "obj" -> Json.obj(
          "prop1" -> "some string",
          "prop2" -> 12
        )
      )
      
    }

  }

  "toCacheMap" - {

    "should convert from a Cache type to a CacheMap type" in {

      forAll(arbitrary[String]) { id =>
        val cache = Cache(id, Some(referenceJson))

        AmlsMongoCacheConnector.toCacheMap(cache) mustBe CacheMap(cache.id.id, Map[String, JsValue](
          "dataKey" -> JsBoolean(true),
          "name" -> JsString("Some name"),
          "obj" -> Json.obj(
            "prop1" -> "some string",
            "prop2" -> 12
          )
        ))
      }

    }

  }

}
