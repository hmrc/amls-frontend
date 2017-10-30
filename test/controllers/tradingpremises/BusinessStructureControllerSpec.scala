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

package controllers.tradingpremises

import connectors.DataCacheConnector
import models.TradingPremisesSection
import models.tradingpremises._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeApplication
import utils.GenericTestHelper
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class BusinessStructureControllerSpec extends GenericTestHelper with ScalaFutures with MockitoSugar {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> false))

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new BusinessStructureController (self.cache, self.authConnector, messagesApi)
  }

  "BusinessStructureController" must {

    val mockCacheMap = mock[CacheMap]

    "Load Business Structure page" in new Fixture {
      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

    }

    "Load Business Structure page with pre-populatd data" in new Fixture {

      val model = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))

      val result = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      document.select("input[value=01]").hasAttr("checked") mustBe true
      document.select(".amls-error-summary").size mustBe 0
    }

    "return a Bad Request with errors on invalid submission" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsBusinessStructure" -> "invalid"
      )

      val result = controller.post(1)(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST
    }

    "successfully submit and navigate to next page when user selects the option SoleProprietor" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsBusinessStructure" -> "01"
      )
      val model = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post(1, false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.AgentNameController.get(1, false).url)
    }

    "successfully submit and navigate to next page when user selects the option LimitedLiabilityPartnership" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsBusinessStructure" -> "02"
      )

      val model = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1, false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.AgentCompanyNameController.get(1, false).url)
    }

    "successfully submit and navigate to next page when user selects the option Partnership" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsBusinessStructure" -> "03"
      )
      val model = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post(1, false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.AgentPartnershipController.get(1).url)
    }

    "successfully submit and navigate to next page when user selects the option IncorporatedBody" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsBusinessStructure" -> "04"
      )
      val model = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post(1,false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.AgentCompanyNameController.get(1).url)
    }

    "successfully submit and navigate to next page when user selects the option" +
      " UnincorporatedBody without edit and is the first Trading premises" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsBusinessStructure" -> "05"
      )
      val model = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post(1, false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ConfirmAddressController.get(1).url)
    }

    "successfully submit and navigate to next page when user selects the option UnincorporatedBody without edit" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsBusinessStructure" -> "05"
      )
      val model = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model, model)))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post(1, false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.WhereAreTradingPremisesController.get(1, false).url)
    }

    "successfully submit and navigate to next page when user selects the option UnincorporatedBody with edit" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsBusinessStructure" -> "05"
      )
      val model = TradingPremises(
        businessStructure = Some(SoleProprietor)
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post(1,edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
    }

    "set the hasChanged flag to true" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsBusinessStructure" -> "02"
      )
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse)))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post(1)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.AgentCompanyNameController.get(1, false).url))

      verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
        any(),
        meq(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
          hasChanged = true,
          businessStructure = Some(LimitedLiabilityPartnership)
        ))))(any(), any(), any())
    }

  }
}
