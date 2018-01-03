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

package controllers.renewal

import connectors.DataCacheConnector
import models.businessmatching.{BusinessActivities => BMActivities, _}
import models.renewal.{InvolvedInOtherYes, Renewal}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class InvolvedInOtherControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures with PrivateMethodTester {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[CacheMap]

    val emptyCache = CacheMap("", Map.empty)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockStatusService = mock[StatusService]
    lazy val mockRenewalService = mock[RenewalService]

    val controller = new InvolvedInOtherController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )
  }

  "InvolvedInOtherController" must {

    "when get is called" must {
      "display the is your business involved in other activities page" in new Fixture {

        when(mockCacheMap.getEntry[Renewal](Renewal.key))
          .thenReturn(None)
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(None)

        when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("renewal.involvedinother.title"))
      }


      "display the involved in other with pre populated data" in new Fixture {

        when(mockCacheMap.getEntry[Renewal](Renewal.key))
          .thenReturn(Some(Renewal(involvedInOtherActivities = Some(InvolvedInOtherYes("test")))))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching()))

        when(mockDataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include("test")

      }

      "display the correct business type" when {
        "the business type is AccountancyServices" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(AccountancyServices)))
          )

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[Renewal](Renewal.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.01"))

        }

        "the business type is BillPaymentServices" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(BillPaymentServices)))
          )

          when(mockCacheMap.getEntry[Renewal](Renewal.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.02"))

        }

        "the business type is EstateAgentBusinessService" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(EstateAgentBusinessService)))
          )

          when(mockCacheMap.getEntry[Renewal](Renewal.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.03"))

        }

        "the business type is HighValueDealing" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(HighValueDealing)))
          )

          when(mockCacheMap.getEntry[Renewal](Renewal.key))
            .thenReturn(None)

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.04"))

        }

        "the business type is MoneyServiceBusiness" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(MoneyServiceBusiness)))
          )

          when(mockCacheMap.getEntry[Renewal](Renewal.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.05"))

        }

        "the business type is TrustAndCompanyServices" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(TrustAndCompanyServices)))
          )

          when(mockCacheMap.getEntry[Renewal](Renewal.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.06"))

        }

        "the business type is TelephonePaymentService" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(TelephonePaymentService)))
          )

          when(mockCacheMap.getEntry[Renewal](Renewal.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.07"))

        }
      }

    }

    "when post is called" must {
      "when there is pre-existing Renewal Data" must {
        "redirect to BusinessTurnoverController with valid data with option yes" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "true",
            "details" -> "test"
          )

          when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(mockRenewalService.getRenewal(any(),any(),any()))
            .thenReturn(Future.successful(Some(Renewal())))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BusinessTurnoverController.get().url))
        }

        "redirect to AMLSTurnoverController with valid data with option no" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "false"
          )

          when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(mockRenewalService.getRenewal(any(),any(),any()))
            .thenReturn(Future.successful(Some(Renewal())))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AMLSTurnoverController.get().url))
        }
      }
      "when there is no Renewal data at all yet" must {
        "redirect to BusinessTurnoverController with valid data with option yes" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "true",
            "details" -> "test"
          )

          when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(mockRenewalService.getRenewal(any(),any(),any()))
            .thenReturn(Future.successful(Some(Renewal())))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BusinessTurnoverController.get().url))
        }

        "redirect to AMLSTurnoverController with valid data with option no" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "false"
          )

          when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(mockRenewalService.getRenewal(any(),any(),any()))
            .thenReturn(Future.successful(Some(Renewal())))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AMLSTurnoverController.get().url))
        }

        "redirect to SummaryController with valid data with option no in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "false"
          )

          when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(mockRenewalService.getRenewal(any(),any(),any()))
            .thenReturn(Future.successful(Some(Renewal())))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }

        "redirect to BusinessTurnoverController with valid data in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "true",
            "details" -> "test"
          )

          when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(mockRenewalService.getRenewal(any(),any(),any()))
            .thenReturn(Future.successful(Some(Renewal())))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BusinessTurnoverController.get(true).url))
        }
      }

      "respond with BAD_REQUEST" when {

        "with invalid data with business activities" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(AccountancyServices)))
          )

          when(mockDataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(businessMatching)))

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "test"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.01"))

          val document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#involvedInOther]").html() must include(Messages("error.required.renewal.ba.involved.in.other"))
        }

        "with invalid data without business activities" in new Fixture {

          when(mockDataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "test"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#involvedInOther]").html() must include(Messages("error.required.renewal.ba.involved.in.other"))
        }

        "with required field not filled with business activities" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(AccountancyServices)))
          )

          when(mockDataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(businessMatching)))

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "true",
            "details" -> ""
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.01"))

          val document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#details]").html() must include(Messages("error.required.renewal.ba.involved.in.other.text"))
        }
      }
    }
  }
}
