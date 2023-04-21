/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.businessactivities


import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.businessactivities.ExpectedAMLSTurnoverFormProvider
import models.businessactivities.ExpectedAMLSTurnover.First
import models.businessactivities._
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessMatching, BusinessActivities => Activities}
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.businessactivities.ExpectedAMLSTurnoverView

import scala.concurrent.{ExecutionContext, Future}

class ExpectedAMLSTurnoverControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture {
    self =>
    val request = addToken(authRequest)
    implicit val ec = inject[ExecutionContext]

    lazy val view = inject[ExpectedAMLSTurnoverView]
    val controller = new ExpectedAMLSTurnoverController (
      dataCacheConnector = mock[DataCacheConnector],
      SuccessfulAuthAction, ds = commonDependencies,
      statusService = mock[StatusService],
      cc = mockMcc,
      formProvider = inject[ExpectedAMLSTurnoverFormProvider],
      view = view
    )

    val mockCache = mock[CacheMap]

    def model: Option[BusinessActivities] = None
  }

  val emptyCache = CacheMap("", Map.empty)

  "ExpectedAMLSTurnoverController" when {

    "get is called" must {
      "respond with OK" when {
        "there is no existing data, and show the services on the page" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(Activities(Set(
              AccountancyServices,
              ArtMarketParticipant,
              BillPaymentServices,
              EstateAgentBusinessService,
              HighValueDealing,
              MoneyServiceBusiness,
              TrustAndCompanyServices,
              TelephonePaymentService
            )))
          )

          when(controller.statusService.getStatus(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(mockCache.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(None)

          when(mockCache.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(controller.dataCacheConnector.fetchAll(any())(any()))
            .thenReturn(Future.successful(Some(mockCache)))

          val result = controller.get()(request)
          status(result) must be(OK)

          val html = contentAsString(result)
          val document = Jsoup.parse(html)

          ExpectedAMLSTurnover.all.foreach { value =>
            document.select(s"input[value=${value.toString}]").hasAttr("checked") must be(false)
            html must include(messages(s"businessactivities.registerservices.servicename.lbl.${value.value}"))
          }

          html must include(messages("businessactivities.registerservices.servicename.lbl.08"))
        }

        "there is existing data" in new Fixture {

          override def model = Some(BusinessActivities(expectedAMLSTurnover = Some(First)))

          val businessMatching = BusinessMatching(
            activities = Some(Activities(Set(
              AccountancyServices,
              BillPaymentServices,
              EstateAgentBusinessService,
              HighValueDealing,
              MoneyServiceBusiness,
              TrustAndCompanyServices,
              TelephonePaymentService
            )))
          )

          when(controller.statusService.getStatus(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(controller.dataCacheConnector.fetchAll(any())(any()))
            .thenReturn(Future.successful(Some(mockCache)))

          when(mockCache.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockCache.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
            .thenReturn(model)

          val result = controller.get()(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[value=zeroPlus]").hasAttr("checked") must be(true)
        }

        "there is no cache data" in new Fixture {

          override def model = Some(BusinessActivities(expectedAMLSTurnover = Some(First)))

          when(controller.statusService.getStatus(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(controller.dataCacheConnector.fetchAll(any())(any()))
            .thenReturn(Future.successful(None))

          val result = controller.get()(request)
          status(result) must be(OK)
        }
      }
    }


    "post is called" must {
      "on post with valid data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ExpectedAMLSTurnoverController.post().url).withFormUrlEncodedBody(
          "expectedAMLSTurnover" -> "zeroPlus"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.BusinessFranchiseController.get().url))
      }

      "on post with valid data in edit mode" in new Fixture {

        val newRequest = test.FakeRequest(POST, routes.ExpectedAMLSTurnoverController.post().url).withFormUrlEncodedBody(
          "expectedAMLSTurnover" -> "zeroPlus"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get.url))
      }

      "on post with invalid data" in new Fixture {

        val businessMatching = BusinessMatching(
          activities = Some(Activities(Set(
            AccountancyServices,
            BillPaymentServices,
            EstateAgentBusinessService,
            HighValueDealing,
            MoneyServiceBusiness,
            TrustAndCompanyServices,
            TelephonePaymentService
          )))
        )

        when(controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        val result = controller.post(true)(request)

        status(result) mustBe BAD_REQUEST
      }
    }
  }
}
