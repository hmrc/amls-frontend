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

package models.businessdetails

import models.Country
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class CorrespondenceAddressUkSpec extends PlaySpec with MockitoSugar {

  val defaultYourName     = "Default Your Name"
  val defaultBusinessName = "Default Business Name"
  val defaultAddressLine1 = "Default Line 1"
  val defaultAddressLine2 = Some("Default Line 2")
  val defaultAddressLine3 = Some("Default Line 3")
  val defaultAddressLine4 = Some("Default Line 4")
  val defaultPostcode     = "AA1 1AA"
  val defaultCountry      = Country("Albania", "AL")

  val newYourName     = "New Your Name"
  val newBusinessName = "New Business Name"
  val newAddressLine1 = "New Line 1"
  val newAddressLine2 = Some("New Line 2")
  val newAddressLine3 = Some("New Line 3")
  val newAddressLine4 = Some("New Line 4")
  val newPostcode     = "AA1 1AA"
  val newCountry      = "AB"

  val defaultUKAddress = CorrespondenceAddressUk(
    defaultYourName,
    defaultBusinessName,
    defaultAddressLine1,
    defaultAddressLine2,
    defaultAddressLine3,
    defaultAddressLine4,
    defaultPostcode
  )

  val defaultUKModel = Map(
    "yourName"     -> Seq(defaultYourName),
    "businessName" -> Seq(defaultBusinessName),
    "addressLine1" -> Seq(defaultAddressLine1),
    "addressLine2" -> Seq("Default Line 2"),
    "addressLine3" -> Seq("Default Line 3"),
    "addressLine4" -> Seq("Default Line 4"),
    "postCode"     -> Seq(defaultPostcode)
  )

  val defaultUKJson = Json.obj(
    "yourName"                   -> defaultYourName,
    "businessName"               -> defaultBusinessName,
    "correspondenceAddressLine1" -> defaultAddressLine1,
    "correspondenceAddressLine2" -> defaultAddressLine2,
    "correspondenceAddressLine3" -> defaultAddressLine3,
    "correspondenceAddressLine4" -> defaultAddressLine4,
    "correspondencePostCode"     -> defaultPostcode
  )

  "JSON validation" must {

    val ukAddress = CorrespondenceAddress(Some(defaultUKAddress), None)

    "Round trip a UK Address correctly through serialisation" in {

      CorrespondenceAddress.jsonReads.reads(
        CorrespondenceAddress.jsonWrites.writes(ukAddress)
      ) must be(JsSuccess(ukAddress))
    }

    "Serialise UK address as expected" in {
      Json.toJson(ukAddress) must be(defaultUKJson)
    }

    "Deserialise UK address as expected" in {
      defaultUKJson.as[CorrespondenceAddress] must be(ukAddress)
    }
  }
}
