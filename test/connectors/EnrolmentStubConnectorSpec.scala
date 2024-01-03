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

package connectors

import config.ApplicationConfig
import generators.BaseGenerator
import models.enrolment.{EnrolmentIdentifier, GovernmentGatewayEnrolment}
import org.mockito.Matchers.{any}
import org.mockito.Mockito.{verify, when}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpClient
import utils.AmlsSpec

import scala.concurrent.Future

class EnrolmentStubConnectorSpec extends AmlsSpec with BaseGenerator {

  // scalastyle:off magic.number
  trait TestFixture {
    val enrolments = Seq(GovernmentGatewayEnrolment("HMRC-MLR-ORG",
      List(EnrolmentIdentifier("MLRRefNumber", "AV23456789")), ""))

    val http = mock[HttpClient]
    val config = mock[ApplicationConfig]
    val connector = new EnrolmentStubConnector(http, config)
    val groupId = stringOfLengthGen(10).sample.get

    when(config.enrolmentStubsUrl) thenReturn "http://stubs"
  }

  "The Enrolment Stub Connector" must {
    "get the enrolments from the stubs service" in new TestFixture {
      when {
        http.GET[Seq[GovernmentGatewayEnrolment]](any(), any(), any())(any(), any(), any())
      } thenReturn Future.successful(enrolments)

      val result = await(connector.enrolments(groupId))

      result mustBe enrolments

      verify(http).GET[Seq[GovernmentGatewayEnrolment]](any(), any(), any())(any(), any(), any())
    }

    "get the enrolments from the stubs service for new auth" in new TestFixture {
      when {
        http.GET[Seq[GovernmentGatewayEnrolment]](any(), any(), any())(any(), any(), any())
      } thenReturn Future.successful(enrolments)

      val result = await(connector.enrolments(groupId))

      result mustBe enrolments

      verify(http).GET[Seq[GovernmentGatewayEnrolment]](any(), any(), any())(any(), any(), any())
    }
  }
}
