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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import controllers.businessmatching.updateservice.UpdateServiceHelper
import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.businessmatching.updateservice.ResponsiblePeopleFitAndProper
import models.flowmanagement.{AddServiceFlowModel, WhichFitAndProperPageId}
import models.responsiblepeople.PersonName
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.ResponsiblePeopleService
import services.businessmatching.BusinessMatchingService
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper, StatusConstants}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhichFitAndProperControllerSpec extends GenericTestHelper with MockitoSugar with ResponsiblePersonGenerator with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper = mock[UpdateServiceHelper]
    val mockRPService = mock[ResponsiblePeopleService]

    val controller = new WhichFitAndProperController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector,
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService,
      responsiblePeopleService = mockRPService,
      helper = mockUpdateServiceHelper,
      router = createRouter[AddServiceFlowModel]
    )

    val responsiblePeople = (responsiblePeopleGen(2).sample.get :+
      responsiblePersonGen.sample.get.copy(hasAlreadyPassedFitAndProper = Some(true))) ++
      responsiblePeopleGen(2).sample.get

    var peopleMixedWithInactive = Seq(
      responsiblePersonGen.sample.get.copy(Some(PersonName("Person", None, "1"))),
      responsiblePersonGen.sample.get.copy(Some(PersonName("Person", None, "2")), status = Some(StatusConstants.Deleted)), // Deleted
      responsiblePersonGen.sample.get.copy(Some(PersonName("Person", None, "3")), None) // isComplete = false
    )

    mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key),
      AddServiceFlowModel(activity = Some(TrustAndCompanyServices),
        areNewActivitiesAtTradingPremises = Some(false),
        tradingPremisesActivities = None,
        addMoreActivities = None,
        fitAndProper = Some(true),
        responsiblePeople = None,
        hasChanged = true,
        hasAccepted = false))

    when {
      controller.businessMatchingService.getModel(any(), any(), any())
    } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
      activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
    ))

    when {
      mockRPService.getAll(any(), any(), any())
    } thenReturn Future.successful(responsiblePeople)
  }

  "When the WhichFitAndProperController get is called it" must {

    "return OK with which_fit_and_proper view" in new Fixture {

      val result = controller.get()(request)

      status(result) must be(OK)
      Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.whichfitandproper.title"))

    }
  }

  "When the WhichFitAndProperController post is called it" must {

    "redirect to the Trading Premises page" when {

      "a valid call is made and not editing" in new Fixture {

        val result = controller.post()(request.withFormUrlEncodedBody("responsiblePeople[]" -> "1"))

        status(result) must be(SEE_OTHER)
        controller.router.verify(WhichFitAndProperPageId,
          AddServiceFlowModel(activity = Some(TrustAndCompanyServices),
            areNewActivitiesAtTradingPremises = Some(false),
            tradingPremisesActivities = None,
            addMoreActivities = None,
            fitAndProper = Some(true),
            responsiblePeople = Some(ResponsiblePeopleFitAndProper(Set(1))),
            hasChanged = true,
            hasAccepted = false), false)

      }
    }

    "a valid call is made and editing" in new Fixture {

      val result = controller.post(true)(request.withFormUrlEncodedBody("responsiblePeople[]" -> "1"))
      status(result) must be(SEE_OTHER)
      controller.router.verify(WhichFitAndProperPageId,
        AddServiceFlowModel(activity = Some(TrustAndCompanyServices),
          areNewActivitiesAtTradingPremises = Some(false),
          tradingPremisesActivities = None,
          addMoreActivities = None,
          fitAndProper = Some(true),
          responsiblePeople = Some(ResponsiblePeopleFitAndProper(Set(1))),
          hasChanged = true,
          hasAccepted = false), true)

    }


    "return a BAD_REQUEST" when {

      "an invalid request is made" in new Fixture {

        val result = controller.post()(request)

        status(result) must be(BAD_REQUEST)

      }
    }
  }

  "Inactive people" must {
    "be hidden from the selection list" when {
      "showing the page on a GET request" in new Fixture {
        when {
          mockRPService.getAll(any(), any(), any())
        } thenReturn Future.successful(peopleMixedWithInactive)

        val result = controller.get()(request)

        status(result) must be(OK)

        contentAsString(result) must include("Person 1")
        contentAsString(result) must not include "Person 2"
        contentAsString(result) must not include "Person 3"
      }

      "showing the page having POSTed with validation errors" in new Fixture {
        when {
          mockRPService.getAll(any(), any(), any())
        } thenReturn Future.successful(peopleMixedWithInactive)

        val result = controller.post()(request)

        status(result) must be(BAD_REQUEST)

        contentAsString(result) must include("Person 1")
        contentAsString(result) must not include "Person 2"
        contentAsString(result) must not include "Person 3"
      }
    }
  }
}
