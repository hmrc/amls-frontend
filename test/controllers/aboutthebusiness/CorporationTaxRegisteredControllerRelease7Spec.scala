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

package controllers.aboutthebusiness

import connectors.{BusinessMatchingConnector, BusinessMatchingReviewDetails, DataCacheConnector}
import models.aboutthebusiness.{AboutTheBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import utils.GenericTestHelper
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future
class CorporationTaxRegisteredControllerRelease7Spec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new CorporationTaxRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val businessMatchingConnector = mock[BusinessMatchingConnector]
    }
  }


  implicit override lazy val app = new GuiceApplicationBuilder().
    configure(Map("microservice.services.feature-toggle.business-matching-details-lookup" -> true)).build()


  val emptyCache = CacheMap("", Map.empty)

  "CorporationTaxRegisteredControllerRelease7Spec" must {

    "on get retrieve the corporation tax reference from business customer api if no previous entry and feature flag is high" in new Fixture {

      val reviewDetailsModel = mock[BusinessMatchingReviewDetails]
      when(reviewDetailsModel.utr) thenReturn Some("1111111111")

      when(controller.businessMatchingConnector.getReviewDetails(any())) thenReturn Future.successful(Some(reviewDetailsModel))

      val data = AboutTheBusiness(corporationTaxRegistered = None)

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(data)))

      val result = controller.get()(request)

      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("registeredForCorporationTax-true").hasAttr("checked") must be(true)
      document.getElementById("corporationTaxReference").`val` must be("1111111111")
    }

  }
}
