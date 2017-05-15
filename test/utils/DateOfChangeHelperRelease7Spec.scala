/*
 * Copyright 2017 HM Revenue & Customs
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

package utils

import models.aboutthebusiness.{RegisteredOffice, RegisteredOfficeUK}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication
import uk.gov.hmrc.play.test.UnitSpec

class DateOfChangeHelperRelease7Spec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true) )

  "DateOfChangeHelper" should {

    object DateOfChangeHelperTest extends DateOfChangeHelper{
    }

    val originalModel =RegisteredOfficeUK(
      "addressLine1",
      "addressLine2",
      None,
      None,
      "postCode",
      None
    )


    val changeModel = RegisteredOfficeUK("","",None, None, "", None)

    "return true" when {
      "a change has been made to a model" in {
        DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](Some(originalModel), changeModel) should be(true)
      }
    }

    "return false" when {
      "no change has been made to a model" in {
        DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](Some(originalModel), originalModel) should be(false)
      }
    }

  }

}
