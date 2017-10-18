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
import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching.{BusinessActivities, BusinessMatching, HighValueDealing, MoneyServiceBusiness}
import models.responsiblepeople.ResponsiblePeople
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class FitAndProperControllerSpec extends GenericTestHelper with MockitoSugar with ResponsiblePersonGenerator with BusinessMatchingGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val authContext: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[BusinessMatchingService].to(mockBusinessMatchingService))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[FitAndProperController]

    when {
      controller.statusService.isPreSubmission(any(),any(),any())
    } thenReturn Future.successful(false)

    when {
      controller.businessMatchingService.getModel(any(),any(),any())
    } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
      activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
    ))

    val responsiblePeople = responsiblePeopleGen(5).sample.get

    mockCacheFetch[Seq[ResponsiblePeople]](Some(responsiblePeople))
    mockCacheSave[Seq[ResponsiblePeople]]
  }

  "FitAndProperController" when {

    "get is called" must {
      "return OK with fit_and_proper view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.fitandproper.title"))

      }
      "return NOT_FOUND" when {
        "pre-submission" in new Fixture {

          when {
            controller.statusService.isPreSubmission(any(),any(),any())
          } thenReturn Future.successful(true)

          val result = controller.get()(request)
          status(result) must be(NOT_FOUND)

        }
        "without msb or tcsp" in new Fixture {

          when {
            controller.businessMatchingService.getModel(any(),any(),any())
          } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          ))

          val result = controller.get()(request)
          status(result) must be(NOT_FOUND)

        }
      }
    }

    "post is called" must {
      "redirect to WhichFitAndProperController" when {
        "request is false" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody("passedFitAndProper" -> "false"))

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhichFitAndProperController.get().url))
        }
      }
      "redirect to NewServiceInformationController" when {
        "request is true" which {
          "will set hasAlreadyPassedFitAndProper to true for each responsible person" in new Fixture {

            val result = controller.post()(request.withFormUrlEncodedBody("passedFitAndProper" -> "true"))

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.NewServiceInformationController.get().url))

            verify(
              mockCacheConnector
            ).save[Seq[ResponsiblePeople]](any(), eqTo(responsiblePeople.map(
              _.copy(
                hasAlreadyPassedFitAndProper = Some(true),
                hasChanged = true,
                hasAccepted = true
              )
            )))(any(),any(),any())

          }
        }
      }
      "return BAD_REQUEST" when {
        "request is invalid" in new Fixture {

          val result = controller.post()(request)

          status(result) must be(BAD_REQUEST)

        }
      }
      "return NOT_FOUND" when {
        "pre-submission" in new Fixture {

          when {
            controller.statusService.isPreSubmission(any(),any(),any())
          } thenReturn Future.successful(true)

          val result = controller.post()(request.withFormUrlEncodedBody("passedFitAndProper" -> "true"))
          status(result) must be(NOT_FOUND)

        }
        "without msb or tcsp" in new Fixture {

          when {
            controller.businessMatchingService.getModel(any(),any(),any())
          } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          ))

          val result = controller.post()(request.withFormUrlEncodedBody("passedFitAndProper" -> "true"))
          status(result) must be(NOT_FOUND)

        }
      }
    }

  }

}
