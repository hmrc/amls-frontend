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

package connectors

import config.{AppConfig, WSHttp}
import generators.auth.UserDetailsGenerator
import generators.{AmlsReferenceNumberGenerator, BaseGenerator}
import models.enrolment.{AmlsEnrolmentKey, EnrolmentStoreEnrolment}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreConnectorSpec extends PlaySpec
  with MustMatchers
  with ScalaFutures
  with MockitoSugar
  with AmlsReferenceNumberGenerator
  with UserDetailsGenerator
  with BaseGenerator {

  trait Fixture {

    implicit val headerCarrier = HeaderCarrier()
    implicit val authContext = mock[AuthContext]

    val http = mock[WSHttp]
    val appConfig = mock[AppConfig]
    val authConnector = mock[AuthConnector]

    val connector = new EnrolmentStoreConnector(http, appConfig, authConnector)
    val baseUrl = "http://tax-enrolments:3001"
    val userDetails = userDetailsGen.sample.get
    val enrolKey = AmlsEnrolmentKey(amlsRegistrationNumber)

    when {
      appConfig.enrolmentStoreUrl
    } thenReturn baseUrl

    when {
      authConnector.userDetails(any(), any(), any())
    } thenReturn Future.successful(userDetails)
  }

  "enrol" when {
    "called" must {
      "call the ES8 enrolment store endpoint to enrol the user" in new Fixture {
        val enrolment = EnrolmentStoreEnrolment("123456789", postcodeGen.sample.get)
        val endpointUrl = s"$baseUrl/tax-enrolments/groups/${userDetails.groupIdentifier.get}/enrolments/${enrolKey.key}"

        when {
          http.POST[EnrolmentStoreEnrolment, HttpResponse](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future.successful(HttpResponse(OK))

        whenReady(connector.enrol(enrolKey, enrolment)) { _ =>
          verify(authConnector).userDetails(any(), any(), any())
          verify(http).POST[EnrolmentStoreEnrolment, HttpResponse](eqTo(endpointUrl), eqTo(enrolment), any())(any(), any(), any(), any())
        }
      }

      "throw an exception when no group identifier is available" in new Fixture {
        val enrolment = EnrolmentStoreEnrolment("123456789", postcodeGen.sample.get)

        when {
          authConnector.userDetails(any(), any(), any())
        } thenReturn Future.successful(userDetails.copy(groupIdentifier = None))

        intercept[Exception] {
          await(connector.enrol(enrolKey, enrolment))
        }
      }
    }
  }

}
