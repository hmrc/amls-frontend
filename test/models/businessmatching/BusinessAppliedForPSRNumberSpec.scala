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

package models.businessmatching

import org.scalatestplus.play.PlaySpec
import play.api.i18n.{Messages, MessagesImplicits}
import play.api.libs.json.{JsPath, JsSuccess}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import play.twirl.api.Html

class BusinessAppliedForPSRNumberSpec extends PlaySpec with MessagesImplicits {

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  "BusinessAppliedForPSRNumber" should {

    "Successfully read and write data:option yes" in {
      BusinessAppliedForPSRNumber.jsonReads.reads(
        BusinessAppliedForPSRNumber.jsonWrites.writes(BusinessAppliedForPSRNumberYes("123456"))
      ) must
        be(JsSuccess(BusinessAppliedForPSRNumberYes("123456"), JsPath \ "regNumber"))
    }

    "Successfully read and write data:option No" in {
      BusinessAppliedForPSRNumber.jsonReads.reads(
        BusinessAppliedForPSRNumber.jsonWrites.writes(BusinessAppliedForPSRNumberNo)
      ) must
        be(JsSuccess(BusinessAppliedForPSRNumberNo, JsPath))
    }

    "return the correct radio button items" in {

      val htmlText = "foo"

      val radioButtons = BusinessAppliedForPSRNumber.formValues(Html(s"<p>$htmlText</p>"))

      radioButtons.head.id mustBe Some("appliedFor-true")
      radioButtons.head.value mustBe Some("true")
      radioButtons.head.conditionalHtml.isDefined mustBe true
      radioButtons.head.conditionalHtml.get.toString() must include(htmlText)

      radioButtons.last.id mustBe Some("appliedFor-false")
      radioButtons.last.value mustBe Some("false")
      radioButtons.last.conditionalHtml.isDefined mustBe false
    }
  }
}
