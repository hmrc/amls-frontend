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

class CorrespondenceAddressNonUkSpec extends PlaySpec with MockitoSugar {

  val defaultYourName     = "Default Your Name"
  val defaultBusinessName = "Default Business Name"
  val defaultAddressLine1 = "Default Line 1"
  val defaultAddressLine2 = Some("Default Line 2")
  val defaultAddressLine3 = Some("Default Line 3")
  val defaultAddressLine4 = Some("Default Line 4")
  val defaultPostcode     = "AA1 1AA"
  val defaultCountry      = Country("Albania", "AL")

  val NewYourName     = "New Your Name"
  val NewBusinessName = "New Business Name"
  val NewAddressLine1 = "New Line 1"
  val NewAddressLine2 = Some("New Line 2")
  val NewAddressLine3 = Some("New Line 3")
  val NewAddressLine4 = Some("New Line 4")
  val NewPostcode     = "AA1 1AA"
  val NewCountry      = "AB"

  val defaultNonUKAddress = CorrespondenceAddressNonUk(
    defaultYourName,
    defaultBusinessName,
    defaultAddressLine1,
    defaultAddressLine2,
    defaultAddressLine3,
    defaultAddressLine4,
    defaultCountry
  )

  val defaultNonUKModel = Map(
    "yourName"          -> Seq(defaultYourName),
    "businessName"      -> Seq(defaultBusinessName),
    "addressLineNonUK1" -> Seq(defaultAddressLine1),
    "addressLineNonUK2" -> Seq("Default Line 2"),
    "addressLineNonUK3" -> Seq("Default Line 3"),
    "addressLineNonUK4" -> Seq("Default Line 4"),
    "country"           -> Seq(defaultCountry.code)
  )

  val defaultNonUKJson = Json.obj(
    "yourName"                   -> defaultYourName,
    "businessName"               -> defaultBusinessName,
    "correspondenceAddressLine1" -> defaultAddressLine1,
    "correspondenceAddressLine2" -> defaultAddressLine2,
    "correspondenceAddressLine3" -> defaultAddressLine3,
    "correspondenceAddressLine4" -> defaultAddressLine4,
    "correspondenceCountry"      -> defaultCountry
  )

  "JSON validation" must {

    val nonUkAddress = CorrespondenceAddress(None, Some(defaultNonUKAddress))

    "Round trip a Non UK Address correctly through serialisation" in {

      CorrespondenceAddress.jsonReads.reads(
        CorrespondenceAddress.jsonWrites.writes(nonUkAddress)
      ) must be(JsSuccess(nonUkAddress))
    }

    "Serialise non-UK address as expected" in {
      Json.toJson(nonUkAddress) must be(defaultNonUKJson)
    }

    "Deserialise non-UK address as expected" in {
      defaultNonUKJson.as[CorrespondenceAddress] must be(nonUkAddress)
    }
  }
}
