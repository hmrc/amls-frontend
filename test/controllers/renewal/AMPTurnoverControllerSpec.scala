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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.renewal.AMPTurnoverFormProvider
import models.businessmatching.{BusinessActivities => Activities, _}
import models.businessmatching.BusinessActivity._
import models.renewal.{AMPTurnover, Renewal}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.RenewalService
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.renewal.AMPTurnoverView

import scala.concurrent.Future

class AMPTurnoverControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val emptyCache = Cache.empty

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService     = mock[RenewalService]
    lazy val view                   = app.injector.instanceOf[AMPTurnoverView]
    val controller                  = new AMPTurnoverController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = mockRenewalService,
      cc = mockMcc,
      formProvider = inject[AMPTurnoverFormProvider],
      view = view
    )

    val businessMatching = BusinessMatching(
      activities = Some(Activities(Set(BusinessActivity.ArtMarketParticipant)))
    )

    def testRenewal: Option[Renewal] = None

    when(controller.dataCacheConnector.fetchAll(any()))
      .thenReturn(Future.successful(Some(mockCacheMap)))

    when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
      .thenReturn(Some(businessMatching))

    when(mockCacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
      .thenReturn(testRenewal)
  }

  val emptyCache = Cache.empty

  "AMPTurnoverController" when {

    "get is called" must {

      "display the AMP Turnover page" in new Fixture {

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(
          "How much of your turnover for the last 12 months came from sales of art for €10,000 or more?"
        )
      }

      "display the AMP Turnover page with pre populated data" in new Fixture {

        when(mockCacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
          .thenReturn(Some(Renewal(ampTurnover = Some(AMPTurnover.First))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=zeroToTwenty]").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {

      "respond with BAD_REQUEST when given invalid data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.AMPTurnoverController.post().url).withFormUrlEncodedBody(
        )

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.required.renewal.amp.percentage"))
      }

      "when edit is true"  must {
        "Redirect to the summary page" in new Fixture {
          val newRequest = FakeRequest(POST, routes.AMPTurnoverController.post().url).withFormUrlEncodedBody(
            "percentageExpectedTurnover" -> AMPTurnover.First.toString
          )

          val bMatching = BusinessMatching(
            activities = Some(Activities(Set(ArtMarketParticipant)))
          )

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(None))

          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

          when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(mockCacheMap))

          mockCacheFetch[BusinessMatching](Some(bMatching), Some(BusinessMatching.key))

          val result = controller.post(true)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
        }
      }
      "When edit is false" must {
        "go to summary page if business type is not ASP, HVD or MSB" in new Fixture {
          val newRequest = FakeRequest(POST, routes.AMPTurnoverController.post().url).withFormUrlEncodedBody(
            "percentageExpectedTurnover" -> AMPTurnover.First.toString
          )

          val bMatching = BusinessMatching(
            activities = Some(Activities(Set(ArtMarketParticipant)))
          )

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(bMatching)))

          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))
          when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
        }
        "go to CustomerOutsideIsUKController when HVD (and not ASP or MSB) is selected" in new Fixture {
          val newRequest = FakeRequest(POST, routes.AMPTurnoverController.post().url).withFormUrlEncodedBody(
            "percentageExpectedTurnover" -> AMPTurnover.First.toString
          )

          val bMatching = BusinessMatching(
            activities = Some(Activities(Set(ArtMarketParticipant, HighValueDealing)))
          )

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(bMatching)))

          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))
          when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideIsUKController.get().url))
        }
        "go to the TotalThroughput page when MSB (and not ASP) is selected" in new Fixture {
          val newRequest = FakeRequest(POST, routes.AMPTurnoverController.post().url).withFormUrlEncodedBody(
            "percentageExpectedTurnover" -> AMPTurnover.First.toString
          )

          val bMatching = BusinessMatching(
            activities = Some(Activities(Set(ArtMarketParticipant, MoneyServiceBusiness, HighValueDealing)))
          )

          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(bMatching)))

          when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))
          when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.renewal.routes.TotalThroughputController.get().url))
        }
      }
    }
  }
}
