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

package controllers.tradingpremises

import connectors.DataCacheConnector
import models.tradingpremises.{Address, TradingPremises, YourTradingPremises}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper, StatusConstants}

class PremisesRegisteredControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    lazy val defaultBuilder = new GuiceApplicationBuilder()
      .configure("microservice.services.feature-toggle.show-fees" -> true)
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val controller = app.injector.instanceOf[PremisesRegisteredController]

  }

  "PremisesRegisteredController" must {

    "Get Option:" must {

      "load the Premises Registered page" in new Fixture {

        mockCacheFetch[Seq[TradingPremises]](None, Some(TradingPremises.key))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))

        val title = s"${Messages("tradingpremises.premises.registered.title")} - ${Messages("summary.tradingpremises")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

        htmlValue.title mustBe title
      }

      "load the Premises Registered page1" in new Fixture {
        val ytp = YourTradingPremises("foo", Address("1", "2", None, None, "asdfasdf"),
          Some(true), Some(new LocalDate(1990, 2, 24)))

        mockCacheFetch[Seq[TradingPremises]](Some(Seq(TradingPremises(
          None,
          Some(ytp)),
          TradingPremises(
            registeringAgentPremises = None,
            yourTradingPremises = Some(ytp),
            status = Some(StatusConstants.Deleted))
        )), Some(TradingPremises.key))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("tradingpremises.have.registered.premises.text", 1))
      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes'" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("registerAnotherPremises" -> "true")

        mockCacheFetch[TradingPremises](None, Some(TradingPremises.key))
        mockCacheSave[TradingPremises]

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.tradingpremises.routes.TradingPremisesAddController.get(false).url))
      }

      "successfully redirect to the page on selection of 'no'" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("registerAnotherPremises" -> "false")

        mockCacheFetch[TradingPremises](None, Some(TradingPremises.key))
        mockCacheSave[TradingPremises]

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.get().url))
      }
    }

    "on post invalid data show error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()

      mockCacheFetch[TradingPremises](None, Some(TradingPremises.key))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("tradingpremises.want.to.register.another.premises"))

    }

    "on post with invalid data show error" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "registerAnotherPremises" -> ""
      )

      mockCacheFetch[TradingPremises](None, Some(TradingPremises.key))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("tradingpremises.want.to.register.another.premises"))

    }
  }
}
