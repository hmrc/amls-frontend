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
import models.flowmanagement.{AddServiceFlowModel, WhichFitAndProperPageId}
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.ResponsiblePeopleService
import services.businessmatching.BusinessMatchingService
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

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
      responsiblePersonGen.sample.get.copy(hasAlreadyPassedFitAndProper = Some(true))) ++ responsiblePeopleGen(2).sample.get

    mockCacheFetch[AddServiceFlowModel](Some(AddServiceFlowModel(Some(TrustAndCompanyServices), Some(true))), Some(AddServiceFlowModel.key))
    mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key), AddServiceFlowModel(Some(BillPaymentServices)))

    mockCacheFetch[Seq[ResponsiblePeople]](Some(responsiblePeople), Some(ResponsiblePeople.key))
    mockCacheSave[Seq[ResponsiblePeople]]

    when {
      controller.statusService.isPreSubmission(any(), any(), any())
    } thenReturn Future.successful(false)

    when {
      controller.businessMatchingService.getModel(any(), any(), any())
    } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
      activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
    ))

    when {
      mockRPService.getActiveWithIndex(any(), any(), any())
    } thenReturn Future.successful(responsiblePeople.zipWithIndex)

    when {
      mockRPService.updateResponsiblePeople(any())(any(), any(), any())
    } thenReturn Future.successful(mockCacheMap)
  }

  "When the WhichFitAndProperController get is called it" must {

    "return OK with which_fit_and_proper view" in new Fixture {

      val result = controller.get()(request)

      status(result) must be(OK)
      Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.whichfitandproper.title"))

    }
    "return INTERNAL_SERVER_ERROR" when {
      "activities cannot be retrieved" in new Fixture {
        mockCacheFetch[AddServiceFlowModel](Some(AddServiceFlowModel()), Some(AddServiceFlowModel.key))
        mockCacheFetch[Seq[ResponsiblePeople]](Some(responsiblePeople), Some(ResponsiblePeople.key))

        when {
          mockRPService.getActiveWithIndex(any(), any(), any())
        } thenReturn Future.failed(new Exception("Failed to get responsible people"))


        val result = controller.get()(request)
        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }
  }

  "When the WhichFitAndProperController post is called it" must {

    "redirect to the Trading Premises page" when {

      "a valid call is made and not editing" in new Fixture {

        val result = controller.post()(request.withFormUrlEncodedBody("responsiblePeople[]" -> "1"))

        status(result) must be(SEE_OTHER)
        controller.router.verify(WhichFitAndProperPageId,
          AddServiceFlowModel(Some(TrustAndCompanyServices), fitAndProper = Some(false), hasChanged = true))

      }
    }

    "return a BAD_REQUEST" when {

      "an invalid request is made" in new Fixture {

        val result = controller.post()(request)

        status(result) must be(BAD_REQUEST)

      }

    }
  }
//Cannot reach this page so tests redundant
//      "return NOT_FOUND" when {
//        "pre-submission" in new Fixture {
//
//          when {
//            controller.statusService.isPreSubmission(any(), any(), any())
//          } thenReturn Future.successful(true)
//
//          val result = controller.post()(request.withFormUrlEncodedBody("responsiblePeople[]" -> "1"))
//          status(result) must be(NOT_FOUND)
//
//        }
//        "without msb or tcsp" in new Fixture {
//
//          when {
//            controller.businessMatchingService.getModel(any(), any(), any())
//          } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
//            activities = Some(BusinessActivities(Set(HighValueDealing)))
//          ))
//
//          val result = controller.post()(request.withFormUrlEncodedBody("responsiblePeople[]" -> "1"))
//          status(result) must be(NOT_FOUND)
//
//        }
//      }
//
//    }
//  }
}
