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
import connectors.DataCacheConnector
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching.updateservice._
import models.businessmatching._
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

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
        bind[DataCacheConnector].to(mockCacheConnector),
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

    mockActivities(Some(Set(MoneyServiceBusiness, AccountancyServices)))
  }

  "get" when {
    "called" must {
      "return the page with correct service being edited" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK

        val expectedMessage = Messages("businessmatching.updateservice.currenttradingpremises.header", MoneyServiceBusiness.getMessage)
        contentAsString(result) must include(expectedMessage)
      }
    }
  }

  "post" when {
    "called" when {
      "there was a validation error" must {
        "return to the page with BAD_REQUEST response" in new Fixture {
          val result = controller.post()(request.withFormUrlEncodedBody())

          status(result) mustBe BAD_REQUEST

          val expectedError = Messages("error.businessmatching.updateservice.tradingpremisessubmittedactivities")
          contentAsString(result) must include(expectedError)
        }
      }

      "the user chooses 'yes'" must {

        "redirect to CurrentTradingPremisesController" when {
          "there are additional existing services to iterate" in new Fixture {
            val result = controller.post()(request.withFormUrlEncodedBody("submittedActivities" -> "true"))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.CurrentTradingPremisesController.get(1).url)

          }
        }

        "progress to the 'registration progress' page" when {
          "fit and proper is not required" in new Fixture {

            when {
              controller.businessMatchingService.fitAndProperRequired(any(),any(),any())
            } thenReturn OptionT.some[Future, Boolean](false)

            val result = controller.post(1)(request.withFormUrlEncodedBody("submittedActivities" -> "true"))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.NewServiceInformationController.get().url)

          }
        }

        "progress to the 'fit and proper' page" when {
          "fit and proper requirement is introduced" in new Fixture {

            when {
              controller.businessMatchingService.fitAndProperRequired(any(),any(),any())
            } thenReturn OptionT.some[Future, Boolean](true)

            val result = controller.post(1)(request.withFormUrlEncodedBody("submittedActivities" -> "true"))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.FitAndProperController.get().url)

          }
        }

      }

      "the user chooses 'no'" must {
        "progress to the 'which trading premises'" in new Fixture {
          val result = controller.post()(request.withFormUrlEncodedBody("submittedActivities" -> "false"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.WhichCurrentTradingPremisesController.get(0).url)

        }
      }

    }
  }

}
