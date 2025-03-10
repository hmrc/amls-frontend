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

package models.businessdetails

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import play.twirl.api.Html

class VATRegisteredSpec extends PlaySpec with MockitoSugar {

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  "VAT Registered" must {

    "successfully validate given an enum value" in {

      Json.fromJson[VATRegistered](Json.obj("registeredForVAT" -> false)) must
        be(JsSuccess(VATRegisteredNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("registeredForVAT" -> true, "vrnNumber" -> "12345678")

      Json.fromJson[VATRegistered](json) must
        be(JsSuccess(VATRegisteredYes("12345678"), JsPath \ "vrnNumber"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("registeredForVAT" -> true)

      Json.fromJson[VATRegistered](json) must
        be(JsError((JsPath \ "vrnNumber") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(VATRegisteredNo.asInstanceOf[VATRegistered]) must
        be(Json.obj("registeredForVAT" -> false))

      Json.toJson(VATRegisteredYes("12345678").asInstanceOf[VATRegistered]) must
        be(
          Json.obj(
            "registeredForVAT" -> true,
            "vrnNumber"        -> "12345678"
          )
        )
    }

    "return the correct radio button items" in {

      val htmlText = "foo"

      val radioButtons = VATRegistered.formValues(Html(s"<p>$htmlText</p>"))

      radioButtons.head.id mustBe Some("registeredForVAT-true")
      radioButtons.head.value mustBe Some("true")
      radioButtons.head.conditionalHtml.isDefined mustBe true
      radioButtons.head.conditionalHtml.get.toString() must include(htmlText)

      radioButtons.last.id mustBe Some("registeredForVAT-false")
      radioButtons.last.value mustBe Some("false")
      radioButtons.last.conditionalHtml.isDefined mustBe false
    }
  }

}
