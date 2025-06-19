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

package controllers.businessdetails

import controllers.actions.SuccessfulAuthAction
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import utils.AmlsSpec
import views.html.businessdetails.YourResponsibilitiesView

class YourResponsibilitiesControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = app.injector.instanceOf[YourResponsibilitiesView]
    val controller = new YourResponsibilitiesController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      yourResponsibilitiesView = view
    )
  }

  "YourResponsibilitiesController" must {
    "get" must {

      "load the page" in new Fixture {

        val pageTitle = messages("title.yr") + " - " +
          messages("summary.businessdetails") + " - " +
          messages("title.amls") + " - " + messages("title.gov")

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(pageTitle)
      }
    }
  }
}