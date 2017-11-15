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

package controllers.businessmatching.updateservice

import connectors.DataCacheConnector
import org.jsoup.Jsoup
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class UpdateServiceDateOfChangeControllerSpec extends GenericTestHelper with MockitoSugar with MustMatchers{

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[BusinessMatchingService].to(mock[BusinessMatchingService]))
      .build()

    val controller = app.injector.instanceOf[UpdateServiceDateOfChangeController]

  }

  "UpdateServiceDateOfChangeControllerSpec" when {

    "get is called" must {
      "display date_of_change view" in new Fixture {

        val result = controller.get(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("dateofchange.title"))
      }
    }

    "post is called" must {
      "redirect to UpdateAnyInformationController" when {
        "request is valid" in new Fixture {

          val result = controller.post(request.withFormUrlEncodedBody(
            "dateOfChange.day" -> "13",
            "dateOfChange.month" -> "10",
            "dateOfChange.year" -> "2017"
          ))

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.UpdateAnyInformationController.get().url))

        }
      }
      "respond with BAD_REQUEST" when {
        "request is invalid" in new Fixture {

          val result = controller.post(request)

          status(result) must be(BAD_REQUEST)
        }
      }
    }

  }

}
