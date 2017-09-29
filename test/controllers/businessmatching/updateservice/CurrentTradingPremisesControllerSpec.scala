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

import cats.data.OptionT
import cats.implicits._
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching.{AccountancyServices, BusinessActivity, BusinessMatching, MoneyServiceBusiness}
import org.scalatest.mock.MockitoSugar
import play.api.inject.guice.GuiceInjectorBuilder
import services.businessmatching.BusinessMatchingService
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import play.api.test.Helpers._
import play.api.inject.bind
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.MustMatchers
import play.api.i18n.Messages

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CurrentTradingPremisesControllerSpec extends GenericTestHelper with MustMatchers with MockitoSugar with BusinessMatchingGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val businessMatchingService = mock[BusinessMatchingService]

    val injector = new GuiceInjectorBuilder()
      .bindings(
        bind[BusinessMatchingService].to(businessMatchingService),
        bind[AuthConnector].to(self.authConnector)
      )
      .build()

    val controller = injector.instanceOf[CurrentTradingPremisesController]

    def mockActivities(activities: Option[Set[BusinessActivity]]) = when {
      businessMatchingService.getSubmittedBusinessActivities(any(), any(), any())
    } thenReturn (activities match {
      case Some(act) => OptionT.some[Future, Set[BusinessActivity]](act)
      case _ => OptionT.none[Future, Set[BusinessActivity]]
    })
  }

  "get" when {
    "called" must {
      "return the page with correct service being edited" in new Fixture {
        mockActivities(Some(Set(MoneyServiceBusiness, AccountancyServices)))

        val result = controller.get()(request)

        status(result) mustBe OK

        val expectedMessage = Messages("businessmatching.updateservice.currenttradingpremises.header", MoneyServiceBusiness.getMessage)
        contentAsString(result) must include(expectedMessage)
      }
    }
  }

  "post" when {
    "called" must {
      "return the page if there was a validation error" in new Fixture {
        mockActivities(Some(Set(MoneyServiceBusiness, AccountancyServices)))

        val result = controller.post()(request.withFormUrlEncodedBody())

        status(result) mustBe BAD_REQUEST

        val expectedError = Messages("error.businessmatching.updateservice.tradingpremisessubmittedactivities")
        contentAsString(result) must include(expectedError)
      }
    }
  }

}
