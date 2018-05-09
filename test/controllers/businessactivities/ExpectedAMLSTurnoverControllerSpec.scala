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

package controllers.businessactivities


import connectors.DataCacheConnector
import models.businessactivities.ExpectedAMLSTurnover.First
import models.businessactivities._
import models.businessmatching.{BusinessActivities => Activities, _}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, AmlsSpec}

import scala.concurrent.Future

class ExpectedAMLSTurnoverControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new ExpectedAMLSTurnoverController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService: StatusService = mock[StatusService]
    }

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
              BillPaymentServices,
              EstateAgentBusinessService,
              HighValueDealing,
              MoneyServiceBusiness,
              TrustAndCompanyServices,
              TelephonePaymentService
            )))
          )

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(mockCache.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(None)

          when(mockCache.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCache)))

          val result = controller.get()(request)
          status(result) must be(OK)

          val html = contentAsString(result)
          val document = Jsoup.parse(html)

          document.select("input[value=01]").hasAttr("checked") must be(false)
          document.select("input[value=02]").hasAttr("checked") must be(false)
          document.select("input[value=03]").hasAttr("checked") must be(false)
          document.select("input[value=04]").hasAttr("checked") must be(false)
          document.select("input[value=05]").hasAttr("checked") must be(false)
          document.select("input[value=06]").hasAttr("checked") must be(false)
          document.select("input[value=07]").hasAttr("checked") must be(false)

          html must include(Messages("businessmatching.registerservices.servicename.lbl.01"))
          html must include(Messages("businessmatching.registerservices.servicename.lbl.02"))
          html must include(Messages("businessmatching.registerservices.servicename.lbl.03"))
          html must include(Messages("businessmatching.registerservices.servicename.lbl.04"))
          html must include(Messages("businessmatching.registerservices.servicename.lbl.05"))
          html must include(Messages("businessmatching.registerservices.servicename.lbl.06"))
          html must include(Messages("businessmatching.registerservices.servicename.lbl.07"))

        }

        "there is existing data" in new Fixture {

          override def model = Some(BusinessActivities(expectedAMLSTurnover = Some(First)))

          val businessMatching = BusinessMatching(
            activities = Some(Activities(Set.empty))
          )

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCache)))

          when(mockCache.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockCache.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
            .thenReturn(model)

          val result = controller.get()(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[value=01]").hasAttr("checked") must be(true)
        }

        "there is no cache data" in new Fixture {

          override def model = Some(BusinessActivities(expectedAMLSTurnover = Some(First)))

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.get()(request)
          status(result) must be(OK)
        }
      }

      "respond with NOT_FOUND" when {
        "allowedToEdit is false (status is not SubmissionReady | NotCompleted | SubmissionReadyForReview)" in new Fixture {

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.get()(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }


    "post is called" must {
      "on post with valid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "expectedAMLSTurnover" -> "01"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.BusinessFranchiseController.get().url))
      }

      "on post with valid data in edit mode" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "expectedAMLSTurnover" -> "01"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
      }

      "on post with invalid data" in new Fixture {

        val businessMatching = BusinessMatching(
          activities = Some(Activities(Set.empty))
        )

        when(controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        val result = controller.post(true)(request)

        status(result) mustBe BAD_REQUEST
      }
    }
  }
}
