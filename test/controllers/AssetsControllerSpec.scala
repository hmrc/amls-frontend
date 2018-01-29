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

package controllers

import models.autocomplete.LocationGraphTransformer
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class AssetsControllerSpec extends PlaySpec with MockitoSugar {

  trait Fixture {
    val environment = mock[Environment]
    val transformer = mock[LocationGraphTransformer]

    val controller = new AssetsController(mock[HttpErrorHandler], environment, transformer)
  }

  "countries" when {
    "called" must {
      "return an OK 200 status with a Json response" in new Fixture {
        val resultJson = Json.obj(
          "response" -> "OK"
        )

        when {
          transformer.transform(any())
        } thenReturn Some(resultJson)

        val result = controller.countries()(FakeRequest())

        status(result) mustBe OK
        contentAsJson(result) mustBe resultJson
      }

      "return a 500 status when no Json was transformed" in new Fixture {
        when {
          transformer.transform(any())
        } thenReturn None

        status(controller.countries()(FakeRequest())) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
