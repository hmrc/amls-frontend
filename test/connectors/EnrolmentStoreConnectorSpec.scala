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
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.inject.ServicesConfig

class EnrolmentStoreConnectorSpec extends PlaySpec with MustMatchers with ScalaFutures with MockitoSugar {

  trait Fixture {

    implicit val headerCarrier = HeaderCarrier()

    val http = mock[WSHttp]
    val appConfig = mock[AppConfig]
    val servicesConfig = mock[ServicesConfig]
    val connector = new EnrolmentStoreConnector(http, appConfig)
    val baseUrl = "http://tax-enrolments:3001"

    when {
      appConfig.config
    } thenReturn servicesConfig

    when {
      servicesConfig.baseUrl(eqTo("tax-enrolments"))
    } thenReturn baseUrl

  }

  "enrol" when {
    "called" must {
      "call the UserDetails service to get the group id" in new Fixture {



      }
    }
  }

}
