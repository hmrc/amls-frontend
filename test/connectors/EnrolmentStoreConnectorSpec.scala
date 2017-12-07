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
import models.enrolment.ESEnrolment
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpResponse}
import org.mockito.Mockito.{verify, when}
import org.mockito.Matchers.{any, eq => eqTo}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreConnectorSpec extends PlaySpec with MustMatchers with ScalaFutures with MockitoSugar {

  trait Fixture {

    implicit val headerCarrier = HeaderCarrier()

    val http = mock[CoreGet]
    val appConfig = mock[AppConfig]
    val servicesConfig = mock[ServicesConfig]
    val userId = "00000038746"
    val connector = new EnrolmentStoreConnector(http, appConfig)
    val baseUrl = "/enrolment-store"

    when {
      appConfig.config
    } thenReturn servicesConfig

    when {
      servicesConfig.baseUrl(eqTo("enrolment-store"))
    } thenReturn baseUrl

  }

  "userEnrolments" must {
    "call the enrolments store to get the user's enrolments" in new Fixture {
      when {
        http.GET[ESEnrolment](any())(any(), any(), any())
      } thenReturn Future.successful(mock[ESEnrolment])

      val result = await(connector.userEnrolments(userId))

      verify(http).GET[Seq[Enrolment]](eqTo(s"$baseUrl/users/$userId/enrolments"))(any(), any(), any())
    }
  }

}
