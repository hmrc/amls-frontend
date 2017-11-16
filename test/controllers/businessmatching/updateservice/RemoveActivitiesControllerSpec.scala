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
import models.businessmatching._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveActivitiesControllerSpec extends GenericTestHelper with MockitoSugar with MustMatchers {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[BusinessMatchingService].to(mock[BusinessMatchingService]))
      .build()

    val controller = app.injector.instanceOf[RemoveActivitiesController]

    when {
      controller.businessMatchingService.getModel(any(),any(),any())
    } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
      activities = Some(BusinessActivities(Set(MoneyServiceBusiness, HighValueDealing)))
    ))

  }

  "RemoveActivitiesController" when {

    "get is called" must {

      "display the view" in new Fixture {

        when {
          controller.statusService.isPreSubmission(any(),any(),any())
        } thenReturn Future.successful(false)

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("updateservice.removeactivities.title"))

      }

      "return NOT_FOUND" when {
        "status is pre-submission" in new Fixture {

          when {
            controller.statusService.isPreSubmission(any(),any(),any())
          } thenReturn Future.successful(true)

          val result = controller.get()(request)

          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" must {

      "redirect to UpdateServiceDateOfChangeController" when {
        "service is removed" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody("businessActivities[]" -> "03"))

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.UpdateServiceDateOfChangeController.get("03").url))

        }
        "mutliple services are removed" in new Fixture {

          when {
            controller.businessMatchingService.getModel(any(),any(),any())
          } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
            activities = Some(BusinessActivities(Set(MoneyServiceBusiness, HighValueDealing, TrustAndCompanyServices)))
          ))

          val result = controller.post()(request.withFormUrlEncodedBody(
            "businessActivities[]" -> "03",
            "businessActivities[]" -> "04"
          ))

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.UpdateServiceDateOfChangeController.get("03/04").url))

        }
      }

      "redirect to RemoveActivitiesInformationController" when {
        "all services are selected to be removed" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody(
            "businessActivities[]" -> "03",
            "businessActivities[]" -> "04"
          ))

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.RemoveActivitiesInformationController.get().url))

        }
      }

      "respond with BAD_REQUEST" when {
        "request is invalid" in new Fixture {

          val result = controller.post()(request)

          status(result) must be(BAD_REQUEST)

        }
      }

    }

  }

}