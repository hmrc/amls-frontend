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

package controllers

import connectors.{AmlsConnector, DataCacheConnector, FeeConnector}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.status._
import models.{Country, FeeResponse}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class StatusControllerWithoutNotificationsSpec extends GenericTestHelper with MockitoSugar {

  val cacheMap = mock[CacheMap]

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new StatusController {
      override private[controllers] val landingService: LandingService = mock[LandingService]
      override val authConnector = self.authConnector
      override private[controllers] val enrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
      override private[controllers] val statusService: StatusService = mock[StatusService]
      override private[controllers] val progressService: ProgressService = mock[ProgressService]
      override private[controllers] val feeConnector: FeeConnector = mock[FeeConnector]
      override private[controllers] val renewalService: RenewalService = mock[RenewalService]
      override protected[controllers] val dataCache: DataCacheConnector = mock[DataCacheConnector]
      override private[controllers] val amlsConnector = mock[AmlsConnector]
    }
  }

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.notifications" -> false) )

  "StatusControllerWithoutNotificationsSpec" must {
    "hide notifications when notifications toggle is off" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

      when(controller.landingService.cacheMap(any(), any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](contains(BusinessMatching.key))(any()))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

      when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(Some("XAML00000000000")))

      when(controller.statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReadyForReview, None))

      when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mock[FeeResponse]))

      when(controller.amlsConnector.getPaymentByAmlsReference(any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when {
        controller.dataCache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(BusinessMatching(Some(reviewDtls), None)))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))

      document.getElementsByClass("messaging").size() mustBe 0

    }
  }
}
