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

package connectors

import config.AppConfig
import generators.enrolment.ESEnrolmentGenerator
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import models.enrolment.Formatters._
import org.scalacheck.Gen

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreConnectorSpec extends PlaySpec with MustMatchers with ScalaFutures with MockitoSugar with ESEnrolmentGenerator {

  trait Fixture {

    implicit val headerCarrier = HeaderCarrier()

    val http = mock[CoreGet]
    val appConfig = mock[AppConfig]
    val servicesConfig = mock[ServicesConfig]
    val userId = numSequence(10).sample.get
    val connector = new EnrolmentStoreConnector(http, appConfig)
    val baseUrl = "http://enrolment-store:3001"

    when {
      appConfig.config
    } thenReturn servicesConfig

    when {
      servicesConfig.baseUrl(eqTo("enrolment-store"))
    } thenReturn baseUrl

  }

  "userEnrolments" must {
    "call the enrolments store to get the user's enrolments" in new Fixture {
      val enrolment = esEnrolmentGen.sample.get

      when {
        http.GET[HttpResponse](any())(any(), any(), any())
      } thenReturn Future.successful(HttpResponse(OK, Some(Json.toJson(enrolment))))

      val result = await(connector.userEnrolments(userId))

      result must contain(enrolment)
      verify(http).GET[HttpResponse](eqTo(EnrolmentStoreConnector.enrolmentsUrl(userId, baseUrl)))(any(), any(), any())
    }

    "return None if the user was not found" in new Fixture {
      when {
        http.GET[HttpResponse](any)(any(), any(), any())
      } thenReturn Future.successful(HttpResponse(NO_CONTENT))

      val result = await(connector.userEnrolments(userId))

      result must not be defined
    }
  }

  "EnrolmentStoreConnector" must {
    "generate the correct url" in new Fixture {
      EnrolmentStoreConnector.enrolmentsUrl(userId, baseUrl) mustBe
        s"$baseUrl/users/$userId/enrolments?service=HMRC-MLR-ORG&type=principal&start-record=1&max-records=1000"
    }
  }

}
