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

package controllers.businessmatching

import controllers.actions.SuccessfulAuthAction
import forms.businessmatching.BusinessTypeFormProvider
import models.businessmatching.BusinessType
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.test
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.businessmatching.BusinessTypeService
import utils.AmlsSpec
import views.html.businessmatching.BusinessTypeView

import scala.concurrent.Future

class BusinessTypeControllerSpec extends AmlsSpec with ScalaFutures with Injecting with BeforeAndAfterEach {

  val mockService = mock[BusinessTypeService]

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[BusinessTypeView]
    val controller = new BusinessTypeController(
      SuccessfulAuthAction,
      commonDependencies,
      mockMcc,
      mockService,
      inject[BusinessTypeFormProvider],
      view
    )
  }

  override def beforeEach(): Unit = reset(mockService)

  "BusinessTypeController" must {

    "display business Types Page" in new Fixture {

      when(mockService.getBusinessType(any())(any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "display Registration Number page for CORPORATE_BODY" in new Fixture {

      when(mockService.getBusinessType(any())(any()))
        .thenReturn(Future.successful(Some(BusinessType.LimitedCompany)))

      val result = controller.get()(request)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.CompanyRegistrationNumberController.get().url))
    }

    "display Registration Number page for LLP" in new Fixture {

      when(mockService.getBusinessType(any())(any()))
        .thenReturn(Future.successful(Some(BusinessType.LPrLLP)))

      val result = controller.get()(request)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.CompanyRegistrationNumberController.get().url))
    }

    "display Type of Business Page" in new Fixture {

      when(mockService.getBusinessType(any())(any()))
        .thenReturn(Future.successful(Some(BusinessType.UnincorporatedBody)))

      val result = controller.get()(request)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.TypeOfBusinessController.get().url))
    }

    "display Register Services Page" in new Fixture {

      when(mockService.getBusinessType(any())(any()))
        .thenReturn(Future.successful(Some(BusinessType.LPrLLP)))

      val result = controller.get()(request)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.CompanyRegistrationNumberController.get().url))
    }

    "redirect to register services controller when sole proprietor" in new Fixture {

      when(mockService.getBusinessType(any())(any()))
        .thenReturn(Future.successful(Some(BusinessType.SoleProprietor)))

      val result = controller.get()(request)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisterServicesController.get().url))
    }

    "post with updated business matching data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BusinessTypeController.post().url)
        .withFormUrlEncodedBody("businessType" -> BusinessType.LimitedCompany.toString)

      when(mockService.updateBusinessType(any(), eqTo(BusinessType.LimitedCompany))(any()))
        .thenReturn(Future.successful(Some(BusinessType.LimitedCompany)))

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.CompanyRegistrationNumberController.get().url))
    }

    "post with valid data" in new Fixture {

      val newRequest = test
        .FakeRequest(POST, routes.BusinessTypeController.post().url)
        .withFormUrlEncodedBody("businessType" -> BusinessType.LimitedCompany.toString)

      when(mockService.updateBusinessType(any(), eqTo(BusinessType.LimitedCompany))(any()))
        .thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisterServicesController.get().url))
    }

    "post with invalid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.BusinessTypeController.post().url)
        .withFormUrlEncodedBody("businessType" -> "foo")

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
    }

    "post with missing mandatory field" in new Fixture {
      val newRequest = FakeRequest(POST, routes.BusinessTypeController.post().url)
        .withFormUrlEncodedBody()

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
    }
  }
}
