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
import models.businessactivities._
import models.businessmatching.{BusinessActivities => Activities, _}
import models.renewal.Renewal
import models.renewal.AMLSTurnover.First
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class AMLSTurnoverControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService = mock[RenewalService]

    val mockCacheMap = mock[CacheMap]

    val controller = new AMLSTurnoverController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )

    val businessMatching = BusinessMatching(
      activities = Some(Activities(Set.empty))
    )

    def testRenewal: Option[Renewal] = None

    when(mockDataCacheConnector.fetchAll(any(), any()))
      .thenReturn(Future.successful(Some(mockCacheMap)))

    when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
      .thenReturn(Some(businessMatching))

    when(mockCacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
      .thenReturn(testRenewal)
  }

  val emptyCache = CacheMap("", Map.empty)

  "AMLSTurnoverController" must {

    "on get" must {

      "display AMLS Turnover page" in new Fixture {

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("renewal.turnover.title"))
      }

      "display the Role Within Business page with pre populated data" in new Fixture {

        override def testRenewal = Some(Renewal(turnover = Some(First)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=01]").hasAttr("checked") must be(true)
      }

      "display the business type is AccountancyServices" in new Fixture {

        val bMatching = BusinessMatching(
          activities = Some(Activities(Set(AccountancyServices)))
        )

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(bMatching))

        when(mockDataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.01"))

      }

      "display the business type is BillPaymentServices" in new Fixture {

        val bMatching = BusinessMatching(
          activities = Some(Activities(Set(BillPaymentServices)))
        )

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(bMatching))

        when(mockDataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.02"))

      }

      "display the business type is EstateAgentBusinessService" in new Fixture {

        val bMatching = BusinessMatching(
          activities = Some(Activities(Set(EstateAgentBusinessService)))
        )

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(bMatching))

        when(mockDataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.03"))

      }

      "display the business type is HighValueDealing" in new Fixture {

        val bMatching = BusinessMatching(
          activities = Some(Activities(Set(HighValueDealing)))
        )

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(bMatching))

        when(mockDataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.04"))

      }

      "display the business type is MoneyServiceBusiness" in new Fixture {

        val bMatching = BusinessMatching(
          activities = Some(Activities(Set(MoneyServiceBusiness)))
        )

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(bMatching))

        when(mockDataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.05"))

      }

      "display the business type is TrustAndCompanyServices" in new Fixture {

        val bMatching = BusinessMatching(
          activities = Some(Activities(Set(TrustAndCompanyServices)))
        )

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(bMatching))

        when(mockDataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.06"))

      }

      "display the business type is TelephonePaymentService" in new Fixture {

        val bMatching = BusinessMatching(
          activities = Some(Activities(Set(TelephonePaymentService)))
        )

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(bMatching))

        when(mockDataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.07"))

      }

    }

    "on post" must {

      "valid data go to renewal CustomerOutsideUK" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "turnover" -> "01"
        )

        when(mockDataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        when(mockRenewalService.getRenewal(any(),any(),any()))
          .thenReturn(Future.successful(None))
        when(mockRenewalService.updateRenewal(any())(any(),any(),any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideUKController.get().url))
      }

      "valid data in edit mode go to renewal Summary" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "turnover" -> "01"
        )

        when(mockDataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        when(mockRenewalService.getRenewal(any(),any(),any()))
          .thenReturn(Future.successful(None))
        when(mockRenewalService.updateRenewal(any())(any(),any(),any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
      }

      "invalid data show BAD_REQUEST" in new Fixture {

        when(mockDataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        val result = controller.post(true)(request)
        val document = Jsoup.parse(contentAsString(result))

        status(result) mustBe BAD_REQUEST
      }

    }

  }
}
