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

import connectors.DataCacheConnector
import models.Country
import models.aboutthebusiness._
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import utils.AuthorisedFixture
import play.api.test.Helpers._

import scala.concurrent.Future

class ConfirmRegisteredOfficeControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ConfirmRegisteredOfficeController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  private val ukAddress = RegisteredOfficeUK("line_1", "line_2", Some(""), Some(""), "AA1 1AA")
  private val aboutTheBusiness = AboutTheBusiness(None, None, None, None, None, Some(ukAddress), None)
  val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
    Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "ghghg")
  val bm = BusinessMatching(Some(reviewDtls))

  "ConfirmRegisteredOfficeController" must {

    "Get Option:" must {

      "load register Office" in new Fixture {

        when(controller.dataCache.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(Future.successful(Some(bm)))
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.confirmingyouraddress.title"))
      }

      "load Registered office or main place of business when Business Address from save4later returns None" in new Fixture {

        val registeredAddress = ConfirmRegisteredOffice(isRegOfficeOrMainPlaceOfBusiness = true)

        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.RegisteredOfficeController.get().url))

      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes' [this is registered address]" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "true"
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.ContactingYouController.get().url))
      }

      "successfully redirect to the page on selection of Option 'No' [this is not registered address]" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "false"
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.RegisteredOfficeController.get().url))
      }

      "on post invalid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("error.required.atb.confirm.office"))

      }

      "on post with invalid data show error" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> ""
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("err.summary"))

      }
    }
  }
}
