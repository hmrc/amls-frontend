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

package controllers.tradingpremises

import connectors.{AmlsConnector, DataCacheConnector}
import controllers.actions.SuccessfulAuthAction
import forms.tradingpremises.ConfirmAddressFormProvider
import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.registrationdetails.RegistrationDetails
import models.tradingpremises.{TradingPremises, YourTradingPremises}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.StatusService
import utils.{AmlsSpec, DependencyMocks}
import views.html.tradingpremises.ConfirmAddressView

import scala.concurrent.Future

class ConfirmAddressControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with TradingPremisesGenerator
    with BusinessMatchingGenerator {

  trait Fixture extends DependencyMocks {
    self =>
    val applicationReference = "SUIYD3274890384"

    val request                       = addToken(authRequest)
    val dataCache: DataCacheConnector = mockCacheConnector
    val reviewDetails                 = mock[ReviewDetails]
    val statusService                 = mock[StatusService]
    val amls                          = mock[AmlsConnector]
    lazy val view                     = app.injector.instanceOf[ConfirmAddressView]
    val controller                    = new ConfirmAddressController(
      messagesApi,
      self.dataCache,
      SuccessfulAuthAction,
      ds = commonDependencies,
      statusService,
      amls,
      cc = mockMcc,
      app.injector.instanceOf[ConfirmAddressFormProvider],
      view = view
    )

    mockCacheFetchAll
  }

  "ConfirmTradingPremisesAddress" must {

    val bm = businessMatchingGen.sample.get

    "Get Option:" must {

      "Load Confirm trading premises address page successfully" when {
        "YourTradingPremises is not set" in new Fixture {
          val tp = tradingPremisesGen.sample.get.copy(yourTradingPremises = None)

          mockCacheGetEntry(Some(bm), BusinessMatching.key)
          mockCacheGetEntry(Some(Seq(tp)), TradingPremises.key)

          val result = controller.get(1)(request)

          status(result)          must be(OK)
          contentAsString(result) must include(Messages(bm.reviewDetails.get.businessAddress.line_1))
        }
      }

      "redirect to where is your trading premises page" when {
        "business matching model does not exist" in new Fixture {
          mockCacheGetEntry(None, BusinessMatching.key)

          val result = controller.get(1)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }

        "business matching -> review details is empty" in new Fixture {
          mockCacheGetEntry(Some(bm.copy(reviewDetails = None)), BusinessMatching.key)

          val result = controller.get(1)(request)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }

        "the trading premises already has a 'your trading premises' model set" in new Fixture {
          mockCacheGetEntry(Some(bm), BusinessMatching.key)
          mockCacheGetEntry(Some(Seq(tradingPremisesGen.sample.get)), TradingPremises.key)

          val result = controller.get(1)(request)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }
      }
    }

    "Post" must {

      val safeId = "X87FUDIKJJKJH87364"

      val businessAddress        =
        Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB"))
      val reviewDetailsModel     =
        ReviewDetails("Business name from review details", Some(BusinessType.SoleProprietor), businessAddress, safeId)
      val bmWithNewReviewDetails = bm.copy(reviewDetails = Some(reviewDetailsModel))

      val ytp = (bmWithNewReviewDetails.reviewDetails map { rd =>
        YourTradingPremises(
          "Business Name from registration",
          models.tradingpremises.Address(
            rd.businessAddress.line_1,
            rd.businessAddress.line_2,
            rd.businessAddress.line_3,
            rd.businessAddress.line_4,
            rd.businessAddress.postcode.get
          )
        )
      }).get

      "successfully redirect to next page" when {

        "option is 'Yes' is selected confirming the mentioned address is the trading premises address" in new Fixture {

          val model = BusinessMatching(
            reviewDetails = Some(reviewDetailsModel)
          )

          when {
            controller.dataCacheConnector.fetch[BusinessMatching](any(), meq(BusinessMatching.key))(any())
          } thenReturn Future.successful(Some(model))

          when {
            controller.amlsConnector.registrationDetails(any(), meq(safeId))(any(), any())
          } thenReturn Future.successful(RegistrationDetails("Business Name from registration", isIndividual = false))

          when {
            controller.statusService.getSafeIdFromReadStatus(any(), any(), any())(any(), any())
          } thenReturn Future.successful(Some(safeId))

          val newRequest = FakeRequest(POST, routes.ConfirmAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "confirmAddress" -> "true"
            )

          mockCacheGetEntry(Some(Seq(TradingPremises())), TradingPremises.key)
          mockCacheGetEntry(Some(model), BusinessMatching.key)

          when(controller.dataCacheConnector.save(any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1)(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.ActivityStartDateController.get(1).url))

          verify(controller.dataCacheConnector)
            .save[Seq[TradingPremises]](any(), any(), meq(Seq(TradingPremises(yourTradingPremises = Some(ytp)))))(any())

        }

        "option is 'No' is selected confirming the mentioned address is the trading premises address" in new Fixture {
          val newRequest = FakeRequest(POST, routes.ConfirmAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "confirmAddress" -> "false"
            )

          mockCacheGetEntry(
            Some(Seq(TradingPremises(yourTradingPremises = Some(mock[YourTradingPremises])))),
            TradingPremises.key
          )
          mockCacheGetEntry(Some(bm), BusinessMatching.key)

          when(controller.dataCacheConnector.save(any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }

      }

      "throw error message on not selecting the option" in new Fixture {
        val newRequest = FakeRequest(POST, routes.ConfirmAddressController.post(1).url)
          .withFormUrlEncodedBody("" -> "")

        mockCacheFetch[BusinessMatching](Some(bm))

        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)
      }

    }
  }
}
