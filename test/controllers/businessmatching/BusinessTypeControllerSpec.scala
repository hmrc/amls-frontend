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

package controllers.businessmatching

import connectors.DataCacheConnector
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class BusinessTypeControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new BusinessTypeController {
      override private[controllers] val dataCache: DataCacheConnector = mock[DataCacheConnector]
      override protected val authConnector: AuthConnector = self.authConnector
    }
  }

  "BusinessTypeController" must {

    val emptyCache = CacheMap("", Map.empty)

    "display business Types Page" in new Fixture {

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)

    }

    "display Registration Number page for CORPORATE_BODY" in new Fixture {

     val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
       Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(Some(reviewDtls), None))))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be (Some(routes.CompanyRegistrationNumberController.get().url))
    }

    "display Registration Number page for LLP" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LPrLLP),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(Some(reviewDtls), None))))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be (Some(routes.CompanyRegistrationNumberController.get().url))
    }

    "display Type of Business Page" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.UnincorporatedBody),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(Some(reviewDtls), None))))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be (Some(routes.TypeOfBusinessController.get().url))
    }

    "display Register Services Page" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LPrLLP),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(Some(reviewDtls), None))))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be (Some(routes.CompanyRegistrationNumberController.get().url))
    }


    "post with updated business matching data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "businessType" -> "01"
      )

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.SoleProprietor),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

      when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(Some(reviewDtls), None))))

      when(controller.dataCache.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.CompanyRegistrationNumberController.get().url))

    }

    "post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "businessType" -> "01"
      )

      when(controller.dataCache.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisterServicesController.get().url))

    }

    "post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "businessType" -> "11"
      )

      when(controller.dataCache.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("err.summary"))

    }

    "post with missing mandatory field" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
      )

      when(controller.dataCache.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.save[BusinessMatching](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required"))
    }
  }
}
