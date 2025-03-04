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

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.tradingpremises.BusinessStructureFormProvider
import models.TradingPremisesSection
import models.tradingpremises.BusinessStructure._
import models.tradingpremises._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.AmlsSpec
import views.html.tradingpremises.BusinessStructureView

import scala.concurrent.Future

class BusinessStructureControllerSpec extends AmlsSpec with ScalaFutures with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]
    lazy val view                 = app.injector.instanceOf[BusinessStructureView]
    val controller                = new BusinessStructureController(
      self.cache,
      SuccessfulAuthAction,
      ds = commonDependencies,
      messagesApi,
      cc = mockMcc,
      formProvider = inject[BusinessStructureFormProvider],
      view = view,
      error = errorView
    )
  }

  "BusinessStructureController" must {

    val mockCacheMap = mock[Cache]

    "Load Business Structure page" in new Fixture {
      when(cache.fetch[Seq[TradingPremises]](any(), any())(any())).thenReturn(Future.successful(None))

      val result   = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=radio]").size mustBe 5
      document.select("input[type=radio][checked]").size mustBe 0
      document.select(".govuk-error-summary").size mustBe 0
    }

    "Load Business Structure page with pre-populated data" in new Fixture {

      val model = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(cache.fetch[Seq[TradingPremises]](any(), any())(any())).thenReturn(Future.successful(Some(Seq(model))))

      val result   = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      document.select(s"input[value=${SoleProprietor.toString}]").hasAttr("checked") mustBe true
    }

    "return a Bad Request with errors on invalid submission" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BusinessStructureController.post(1).url)
        .withFormUrlEncodedBody(
          "agentsBusinessStructure" -> "invalid"
        )

      val result   = controller.post(1)(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST

      document.select("input[type=radio]").size mustBe 5
      document.select("input[type=radio][selected]").size mustBe 0
      document.select(".govuk-error-summary").size mustBe 1
    }

    "successfully submit and navigate to next page when user selects the option SoleProprietor" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BusinessStructureController.post(1).url)
        .withFormUrlEncodedBody(
          "agentsBusinessStructure" -> SoleProprietor.toString
        )
      val model      = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save(any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post(1, false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.AgentNameController.get(1, false).url)
    }

    "successfully submit and navigate to next page when user selects the option LimitedLiabilityPartnership" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BusinessStructureController.post(1).url)
        .withFormUrlEncodedBody(
          "agentsBusinessStructure" -> LimitedLiabilityPartnership.toString
        )

      val model = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save(any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post(1, false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.AgentCompanyDetailsController.get(1, false).url)
    }

    "successfully submit and navigate to next page when user selects the option Partnership" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BusinessStructureController.post(1).url)
        .withFormUrlEncodedBody(
          "agentsBusinessStructure" -> Partnership.toString
        )
      val model      = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save(any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post(1, false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.AgentPartnershipController.get(1).url)
    }

    "successfully submit and navigate to next page when user selects the option IncorporatedBody" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BusinessStructureController.post(1).url)
        .withFormUrlEncodedBody(
          "agentsBusinessStructure" -> IncorporatedBody.toString
        )
      val model      = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save(any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post(1, false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.AgentCompanyDetailsController.get(1).url)
    }

    "successfully submit and navigate to next page when user selects the option UnincorporatedBody without edit" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BusinessStructureController.post(1).url)
        .withFormUrlEncodedBody(
          "agentsBusinessStructure" -> UnincorporatedBody.toString
        )
      val model      = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model, model)))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save(any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post(1, false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.WhereAreTradingPremisesController.get(1, false).url)
    }

    "successfully submit and navigate to next page when user selects the option UnincorporatedBody" +
      " without edit and is the First Trading Premises" in new Fixture {

        val newRequest = FakeRequest(POST, routes.BusinessStructureController.post(1).url)
          .withFormUrlEncodedBody(
            "agentsBusinessStructure" -> UnincorporatedBody.toString
          )
        val model      = TradingPremises(
          businessStructure = Some(SoleProprietor)
        )
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save(any(), any(), any())(any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(1, false)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ConfirmAddressController.get(1).url)
      }

    "successfully submit and navigate to next page when user selects the option UnincorporatedBody with edit" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BusinessStructureController.post(1, true).url)
        .withFormUrlEncodedBody(
          "agentsBusinessStructure" -> UnincorporatedBody.toString
        )
      val model      = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save(any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post(1, edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.get(1).url)
    }

    "set the hasChanged flag to true" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BusinessStructureController.post(1).url)
        .withFormUrlEncodedBody(
          "agentsBusinessStructure" -> LimitedLiabilityPartnership.toString
        )

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse)))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save(any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post(1)(newRequest)

      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.AgentCompanyDetailsController.get(1, false).url))

      verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
        any(),
        any(),
        meq(
          Seq(
            TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
              hasChanged = true,
              businessStructure = Some(LimitedLiabilityPartnership)
            )
          )
        )
      )(any())
    }

  }
}
