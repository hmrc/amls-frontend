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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.microservice.filters.MicroserviceFilterSupport

sealed trait FilterSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with Results with MicroserviceFilterSupport {

  val featureOn: Boolean

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.change-officer" -> featureOn)
    .build()

  lazy val filter = app.injector.instanceOf[ChangeOfficerFeatureFilter]
  val nextAction = Action(Ok("ok"))

  "The filter" must {
    "allow all pages that are not within the 'change officer' flow" in {
      val request = FakeRequest("GET", controllers.routes.LandingController.get().url)
      val result = filter(nextAction)(request).run()

      status(result) mustBe OK
    }
  }
}


class ChangeOfficerFeatureOnFilterSpec extends FilterSpec {

  override val featureOn = true

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

class ChangeOfficerFeatureOffFilterSpec extends FilterSpec {

  override val featureOn = false

  "ChangeOfficerFeatureOffFilter" must {
    "prevent access to the change officer journey" when {
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
