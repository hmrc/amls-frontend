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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.add.WhichFitAndProperController
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
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class WhichFitAndProperControllerSpec extends GenericTestHelper with MockitoSugar with ResponsiblePersonGenerator with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    implicit val authContext: AuthContext = mockAuthContext
    implicit val ec: ExecutionContext = mockExecutionContext

    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper = mock[UpdateServiceHelper]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[BusinessMatchingService].to(mockBusinessMatchingService))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[WhichFitAndProperController]

    when {
      controller.statusService.isPreSubmission(any(), any(), any())
    } thenReturn Future.successful(false)

    when {
      controller.businessMatchingService.getModel(any(), any(), any())
    } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
      activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
    ))

    val responsiblePeople = (responsiblePeopleGen(2).sample.get :+
      responsiblePersonGen.sample.get.copy(hasAlreadyPassedFitAndProper = Some(true))) ++ responsiblePeopleGen(2).sample.get

    mockCacheFetch[Seq[ResponsiblePeople]](Some(responsiblePeople), Some(ResponsiblePeople.key))
    mockCacheSave[Seq[ResponsiblePeople]]
  }

  "WhichFitAndProperController" when {

    "get is called" must {
      "return OK with which_fit_and_proper view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.whichfitandproper.title"))

      }

      "return NOT_FOUND" when {
        "pre-submission" in new Fixture {

          when {
            controller.statusService.isPreSubmission(any(), any(), any())
          } thenReturn Future.successful(true)

          val result = controller.get()(request)
          status(result) must be(NOT_FOUND)

        }
        "without msb or tcsp" in new Fixture {

          when {
            controller.businessMatchingService.getModel(any(), any(), any())
          } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          ))

          val result = controller.get()(request)
          status(result) must be(NOT_FOUND)

        }
      }
    }

    "post is called" must {

      "on valid request" must {

        "redirect to NewServiceInformationController" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody("responsiblePeople[]" -> "1"))

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(add.routes.NewServiceInformationController.get().url))

        }

      }

      "on invalid request" must {

        "return BAD_REQUEST" in new Fixture {

          val result = controller.post()(request)

          status(result) must be(BAD_REQUEST)

        }

      }

      "return NOT_FOUND" when {
        "pre-submission" in new Fixture {

          when {
            controller.statusService.isPreSubmission(any(), any(), any())
          } thenReturn Future.successful(true)

          val result = controller.post()(request.withFormUrlEncodedBody("responsiblePeople[]" -> "1"))
          status(result) must be(NOT_FOUND)

        }
        "without msb or tcsp" in new Fixture {

          when {
            controller.businessMatchingService.getModel(any(), any(), any())
          } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          ))

          val result = controller.post()(request.withFormUrlEncodedBody("responsiblePeople[]" -> "1"))
          status(result) must be(NOT_FOUND)

        }
      }

    }

  }

  it must {
    "save fit and proper as true to responsible people to those matched by index" which {
      "will save fit and proper as false to responsible people to those not matched by index" when {
        "a single selection is made" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody("responsiblePeople[]" -> "1"))

          status(result) must be(SEE_OTHER)

          verify(
            controller.dataCacheConnector
          ).save[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key), eqTo(Seq(
            responsiblePeople.head,
            responsiblePeople(1).copy(hasAlreadyPassedFitAndProper = Some(true), hasAccepted = true, hasChanged = true),
            responsiblePeople(2).copy(hasAlreadyPassedFitAndProper = Some(false), hasAccepted = true, hasChanged = true),
            responsiblePeople(3),
            responsiblePeople.last
          )))(any(), any(), any())

        }
        "multiple selections are made" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody(
            "responsiblePeople[]" -> "0",
            "responsiblePeople[]" -> "3",
            "responsiblePeople[]" -> "4"
          ))

          status(result) must be(SEE_OTHER)

          verify(
            controller.dataCacheConnector
          ).save[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key), eqTo(Seq(
            responsiblePeople.head.copy(hasAlreadyPassedFitAndProper = Some(true), hasAccepted = true, hasChanged = true),
            responsiblePeople(1),
            responsiblePeople(2).copy(hasAlreadyPassedFitAndProper = Some(false), hasAccepted = true, hasChanged = true),
            responsiblePeople(3).copy(hasAlreadyPassedFitAndProper = Some(true), hasAccepted = true, hasChanged = true),
            responsiblePeople.last.copy(hasAlreadyPassedFitAndProper = Some(true), hasAccepted = true, hasChanged = true)
          )))(any(), any(), any())

        }
      }
    }
  }

}