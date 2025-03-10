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

package models.businessactivities

import models.businessactivities.TransactionTypes.{DigitalSoftware, DigitalSpreadsheet, Paper}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.libs.json.{JsError, JsPath, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text

class TransactionTypeSpec extends PlaySpec with Matchers {

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  "TransactionType" must {

    "write values to JSON" in {
      val model = TransactionTypes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("test")))

      Json.toJson(model) mustBe Json.obj(
        "types"    -> Seq("01", "02", "03"),
        "software" -> "test"
      )
    }

    "read values from JSON" in {
      val json = Json.obj(
        "types"    -> Seq("01", "02", "03"),
        "software" -> "example software"
      )

      json.asOpt[TransactionTypes] mustBe Some(
        TransactionTypes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("example software")))
      )
    }

    "read values from JSON without software" in {
      val json = Json.obj(
        "types" -> Seq("01", "02")
      )

      json.asOpt[TransactionTypes] mustBe Some(TransactionTypes(Set(Paper, DigitalSpreadsheet)))
    }

    "fail when the name not supplied in the Json" in {
      val json = Json.obj(
        "types" -> Seq("01", "02", "03")
      )

      Json.fromJson[TransactionTypes](json) mustBe JsError(
        JsPath \ "software" -> play.api.libs.json.JsonValidationError("error.missing")
      )
    }

    "fail when an invalid value was given" in {
      val json = Json.obj(
        "types" -> Seq("01", "10")
      )

      Json.fromJson[TransactionTypes](json) mustBe JsError(
        JsPath \ "types" -> play.api.libs.json.JsonValidationError("error.invalid")
      )
    }

    "fail when no values are given" in {
      val json = Json.obj()

      Json.fromJson[TransactionTypes](json) mustBe JsError(
        JsPath \ "types" -> play.api.libs.json.JsonValidationError("error.missing")
      )
    }
  }

  "formValues" must {

    "provider the correct checkboxes" when {

      "given conditionalHtml" in {

        val html = Html("foo")

        val result = TransactionTypes.formValues(html)

        TransactionTypes.all.foreach { x =>
          val index    = TransactionTypes.all.indexOf(x)
          val checkbox = result(index)

          checkbox.content mustBe Text(messages(s"businessactivities.transactiontype.lbl.${x.value}"))
          checkbox.value mustBe x.toString
          checkbox.id mustBe Some(s"types_${index + 1}")
          checkbox.name mustBe Some(s"types[${index + 1}]")
          if (checkbox == result.last) {
            checkbox.conditionalHtml mustBe Some(html)
          } else {
            checkbox.conditionalHtml mustBe None
          }
        }
      }
    }
  }
}
