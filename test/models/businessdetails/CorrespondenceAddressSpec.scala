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

import models.Country
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class CorrespondenceAddressSpec extends PlaySpec with MockitoSugar {

  "CorrespondenceAddress" must {

    val ukAddress    = CorrespondenceAddressUk(
      "YourName",
      "BusinessName",
      "Line1",
      Some("Line2"),
      Some("Line3"),
      Some("Line4"),
      "NE1 1NE"
    )
    val nonUkAddress = CorrespondenceAddressNonUk(
      "YourName",
      "BusinessName",
      "Line1",
      Some("Line2"),
      Some("Line3"),
      Some("Line4"),
      Country("Albania", "AL")
    )

    "return true for isUk" when {
      "it contains a UK address" in {
        val correspondenceAddress = CorrespondenceAddress(Some(ukAddress), None)
        correspondenceAddress.isUk must be(Some(true))
      }
    }

    "return false for isUk" when {
      "it contains a Non UK address" in {
        val correspondenceAddress = CorrespondenceAddress(None, Some(nonUkAddress))
        correspondenceAddress.isUk must be(Some(false))
      }
    }

    "return None for isUk" when {
      "it contains neither UK nor non-UK address" in {
        val correspondenceAddress = CorrespondenceAddress(None, None)
        correspondenceAddress.isUk must be(None)
      }
    }
  }
}
