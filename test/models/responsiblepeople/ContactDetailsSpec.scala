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

package models.responsiblepeople

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class ContactDetailsSpec extends PlaySpec with MockitoSugar {

  "JSON Read/Write " must {

    "Read the json and return the InKnownByOtherNamesYes domain object successfully" in {

      val json = Json.obj(
        "phoneNumber"  -> "07000000000",
        "emailAddress" -> "myname@example.com"
      )

      ContactDetails.formats.reads(json) must
        be(JsSuccess(ContactDetails("07000000000", "myname@example.com")))
    }

    "Write the json successfully from the InKnownByOtherNamesYes domain object created" in {

      val contactDetails = ContactDetails("07000000000", "myname@example.com")

      val json = Json.obj(
        "phoneNumber"  -> "07000000000",
        "emailAddress" -> "myname@example.com"
      )

      ContactDetails.formats.writes(contactDetails) must be(json)
    }
  }

}
