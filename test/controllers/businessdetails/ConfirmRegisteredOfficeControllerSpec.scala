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

package controllers.businessdetails

import controllers.actions.SuccessfulAuthAction
import forms.businessdetails.ConfirmRegisteredOfficeFormProvider
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails._
import models.businessmatching.{BusinessMatching, BusinessType}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.businessdetails.ConfirmRegisteredOfficeService
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessdetails.ConfirmRegisteredOfficeOrMainPlaceView

import scala.concurrent.Future

class ConfirmRegisteredOfficeControllerSpec extends AmlsSpec with MockitoSugar with BeforeAndAfterEach {

  val mockService = mock[ConfirmRegisteredOfficeService]

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = app.injector.instanceOf[ConfirmRegisteredOfficeOrMainPlaceView]
    val controller = new ConfirmRegisteredOfficeController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      service = mockService,
      formProvider = app.injector.instanceOf[ConfirmRegisteredOfficeFormProvider],
      view = view
    )
  }

  val address           =
    Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB"))
  private val ukAddress = RegisteredOfficeUK("line1", Some("line2"), Some("line3"), Some("line4"), "AA1 1AA")
  val reviewDtls        = ReviewDetails(
    "BusinessName",
    Some(BusinessType.LimitedCompany),
    Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")),
    "ghghg"
  )
  val reviewDtlsNonUk   = reviewDtls.copy(
    businessAddress =
      Address("line1", Some("line2"), Some("line3"), Some("line4"), None, Country("United States of America", "US"))
  )
  val bm                = BusinessMatching(Some(reviewDtls))
  val emptyCache        = Cache.empty

  override def beforeEach(): Unit = reset(mockService)

  "ConfirmRegisteredOfficeController" must {
    "Get Option:" must {
      "load register Office" in new Fixture {

        when(mockService.hasRegisteredAddress(any()))
          .thenReturn(Future.successful(Some(false)))

        when(mockService.getAddress(any()))
          .thenReturn(Future.successful(Some(address)))

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("businessdetails.confirmingyouraddress.title"))
      }

      "load Registered office or main place of business when has registered address returns None" in new Fixture {

        when(mockService.hasRegisteredAddress(any()))
          .thenReturn(Future.successful(None))

        when(mockService.getAddress(any()))
          .thenReturn(Future.successful(Some(address)))

        val result = controller.get()(request)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.businessdetails.routes.RegisteredOfficeIsUKController.get().url)
        )
      }

      "load Registered office or main place of business when Business Address from mongoCache returns None" in new Fixture {

        when(mockService.hasRegisteredAddress(any()))
          .thenReturn(Future.successful(Some(false)))

        when(mockService.getAddress(any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.businessdetails.routes.RegisteredOfficeIsUKController.get().url)
        )
      }

      "load Registered office or main place of business when there is already a registered address in BusinessDetails" in new Fixture {

        when(mockService.hasRegisteredAddress(any()))
          .thenReturn(Future.successful(Some(true)))

        when(mockService.getAddress(any()))
          .thenReturn(Future.successful(Some(address)))

        val result = controller.get()(request)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.businessdetails.routes.RegisteredOfficeIsUKController.get().url)
        )
      }
    }

    "Post" must {
      "successfully redirect to the page on selection of 'Yes' [this is registered address]" in new Fixture {
        val newRequest = FakeRequest(POST, routes.ConfirmRegisteredOfficeController.post().url).withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "true"
        )

        when(mockService.updateRegisteredOfficeAddress(any(), meq(ConfirmRegisteredOffice(true))))
          .thenReturn(Future.successful(Some(ukAddress)))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.businessdetails.routes.BusinessEmailAddressController.get().url)
        )

        verify(mockService).updateRegisteredOfficeAddress(any(), meq(ConfirmRegisteredOffice(true)))
      }

      "successfully redirect to the page on selection of Option 'No' [this is not registered address]" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ConfirmRegisteredOfficeController.post().url).withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "false"
        )

        when(mockService.updateRegisteredOfficeAddress(any(), meq(ConfirmRegisteredOffice(false))))
          .thenReturn(Future.successful(Some(ukAddress)))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.businessdetails.routes.RegisteredOfficeIsUKController.get().url)
        )

        verify(mockService).updateRegisteredOfficeAddress(any(), meq(ConfirmRegisteredOffice(false)))
      }

      "successfully redirect to the page on selection of Option 'Yes' [this is registered address] update returns None" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ConfirmRegisteredOfficeController.post().url).withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "true"
        )

        when(mockService.updateRegisteredOfficeAddress(any(), meq(ConfirmRegisteredOffice(true))))
          .thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.businessdetails.routes.RegisteredOfficeIsUKController.get().url)
        )

        verify(mockService).updateRegisteredOfficeAddress(any(), meq(ConfirmRegisteredOffice(true)))
      }

      "on post invalid data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ConfirmRegisteredOfficeController.post().url).withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "foo"
        )

        when(mockService.getAddress(any()))
          .thenReturn(Future.successful(Some(address)))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("error.required.atb.confirm.office"))

        verify(mockService, times(0)).updateRegisteredOfficeAddress(any(), any())
      }

      "on post with invalid data show error" in new Fixture {
        val newRequest = FakeRequest(POST, routes.ConfirmRegisteredOfficeController.post().url).withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> ""
        )

        when(mockService.getAddress(any()))
          .thenReturn(Future.successful(Some(address)))

        val result = controller.post()(newRequest)
        status(result)          must be(BAD_REQUEST)
        contentAsString(result) must include(messages("err.summary"))

        verify(mockService, times(0)).updateRegisteredOfficeAddress(any(), any())
      }

      "on post with invalid data must redirect if no address is found" in new Fixture {
        val newRequest = FakeRequest(POST, routes.ConfirmRegisteredOfficeController.post().url).withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> ""
        )

        when(mockService.getAddress(any()))
          .thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        redirectLocation(result) mustBe Some(routes.RegisteredOfficeIsUKController.get().url)

        verify(mockService, times(0)).updateRegisteredOfficeAddress(any(), any())
      }
    }
  }
}
