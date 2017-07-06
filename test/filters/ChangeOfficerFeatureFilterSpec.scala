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

package filters

import akka.stream.Materializer
import org.scalatest.TestData
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, OneAppPerTest, PlaySpec}
import play.api.Environment
import uk.gov.hmrc.play.config.inject.ServicesConfig
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, Results}
import play.api.test.FakeRequest
import play.api.test._
import play.api.test.Helpers._
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport

class ChangeOfficerFeatureOnFilterSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with Results with MicroserviceFilterSupport {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.change-officer" -> true)
    .build()

  lazy val filter = app.injector.instanceOf[ChangeOfficerFeatureFilter]
  val nextAction = Action(Ok("ok"))

  "ChangeOfficerFeatureOnFilter" must {
    "allow access to the change officer journey" when {
      "the feature is turned on" in {

        controllers.changeofficer.Flow.journeyUrls foreach { url =>
          val requestHeader = FakeRequest("GET", url)
          val result = filter(nextAction)(requestHeader).run()

          status(result) mustBe OK
        }
      }
    }
  }
}

class ChangeOfficerFeatureOffFilterSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with Results with MicroserviceFilterSupport {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.change-officer" -> false)
    .build()

  lazy val filter = app.injector.instanceOf[ChangeOfficerFeatureFilter]
  val nextAction = Action(Ok("ok"))

  "ChangeOfficerFeatureOffFilter" must {
    "allow access to the change officer journey" when {
      "the feature is turned off" in {

        controllers.changeofficer.Flow.journeyUrls foreach { url =>
          val requestHeader = FakeRequest("GET", url)
          val result = filter(nextAction)(requestHeader).run()
          status(result) mustBe NOT_FOUND
        }
      }
    }
  }
}
