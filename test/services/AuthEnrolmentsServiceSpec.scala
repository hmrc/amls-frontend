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

package services

import config.AppConfig
import generators.AmlsReferenceNumberGenerator
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito.{verify, when}
import org.mockito.Matchers.{any, eq => eqTo}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.AppConfigSetup

class AuthEnrolmentsServiceSpec extends PlaySpec
  with MustMatchers
  with MockitoSugar
  with AmlsReferenceNumberGenerator
  with AppConfigSetup {

  trait Fixture {
    implicit val headerCarrier = HeaderCarrier()
    implicit val authContext = mock[AuthContext]

    val legacyService = mock[LegacyAuthEnrolmentService]
    val esService = mock[EnrolmentStoreService]

    val service = new AuthEnrolmentsService(legacyService, esService, appConfig)

    when {
      legacyService.amlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(Some(amlsRegistrationNumber))

    when {
      esService.getAmlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(Some(amlsRegistrationNumber))
  }

  "amlsRegistationNumber" must {
    "return the registration number from the legacy service" when {
      "the feature is toggled off" in new Fixture {
        when(appConfig.enrolmentStoreToggle) thenReturn false

        await(service.amlsRegistrationNumber) mustBe Some(amlsRegistrationNumber)

        verify(legacyService).amlsRegistrationNumber(any(), any(), any())
      }
    }

    "return the registration number from the Enrolment Store service" when {
      "the feature is turned on" in new Fixture {
        when(appConfig.enrolmentStoreToggle) thenReturn true

        await(service.amlsRegistrationNumber) mustBe Some(amlsRegistrationNumber)

        verify(esService).getAmlsRegistrationNumber(any(), any(), any())
      }
    }
  }

}
