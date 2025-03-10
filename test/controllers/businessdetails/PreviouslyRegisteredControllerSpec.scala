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
import forms.businessdetails.PreviouslyRegisteredFormProvider
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails._
import models.businessmatching.{BusinessMatching, BusinessType}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.businessdetails.PreviouslyRegisteredService
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessdetails.PreviouslyRegisteredView

import scala.concurrent.Future

class PreviouslyRegisteredControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val mockService  = mock[PreviouslyRegisteredService]
  val mockCacheMap = mock[Cache]

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = app.injector.instanceOf[PreviouslyRegisteredView]
    val controller = new PreviouslyRegisteredController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      service = mockService,
      formProvider = app.injector.instanceOf[PreviouslyRegisteredFormProvider],
      view = view
    )
  }

  val emptyCache = Cache.empty

  override def beforeEach(): Unit = reset(mockService, mockCacheMap)

  "BusinessRegisteredWithHMRCBeforeController" must {

    "on get display the previously registered with HMRC page" in new Fixture {
      when(mockService.getPreviouslyRegistered(any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("businessdetails.registeredformlr.title"))
    }

    "on get display the previously registered with HMRC with pre populated data" in new Fixture {

      when(mockService.getPreviouslyRegistered(any()))
        .thenReturn(Future.successful(Some(PreviouslyRegisteredYes(Some("12345678")))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=true]").hasAttr("checked") must be(true)
    }

    "on post with valid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.post().url).withFormUrlEncodedBody(
        "value"        -> "true",
        "prevMLRRegNo" -> "12345678"
      )
      val reviewDtls = ReviewDetails(
        "BusinessName",
        Some(BusinessType.LimitedCompany),
        Address(
          "line1",
          Some("line2"),
          Some("line3"),
          Some("line4"),
          Some("NE77 0QQ"),
          Country("United Kingdom", "GB")
        ),
        "ghghg"
      )

      val update = PreviouslyRegisteredYes(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))

      when(mockService.updatePreviouslyRegistered(any(), meq(update)))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(
        Some(controllers.businessdetails.routes.ActivityStartDateController.get(false).url)
      )

      verify(mockService).updatePreviouslyRegistered(any(), meq(update))
    }

    "on post with valid data and update returns None" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.post().url).withFormUrlEncodedBody(
        "value" -> "false"
      )
      val reviewDtls = ReviewDetails(
        "BusinessName",
        None,
        Address(
          "line1",
          Some("line2"),
          Some("line3"),
          Some("line4"),
          Some("AA11 1AA"),
          Country("United Kingdom", "GB")
        ),
        "ghghg"
      )

      val update = PreviouslyRegisteredNo

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))

      when(mockService.updatePreviouslyRegistered(any(), meq(update)))
        .thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ConfirmRegisteredOfficeController.get().url))

      verify(mockService).updatePreviouslyRegistered(any(), meq(update))
    }

    "on post with valid data in edit mode and load summary page" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.post().url).withFormUrlEncodedBody(
        "value"        -> "true",
        "prevMLRRegNo" -> "12345678"
      )
      val reviewDtls = ReviewDetails(
        "BusinessName",
        Some(BusinessType.LimitedCompany),
        Address(
          "line1",
          Some("line2"),
          Some("line3"),
          Some("line4"),
          Some("AA11 1AA"),
          Country("United Kingdom", "GB")
        ),
        "ghghg"
      )

      val update = PreviouslyRegisteredYes(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))

      when(mockService.updatePreviouslyRegistered(any(), meq(update)))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessdetails.routes.SummaryController.get.url))

      verify(mockService).updatePreviouslyRegistered(any(), meq(update))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PreviouslyRegisteredController.post().url).withFormUrlEncodedBody(
        "prevMLRRegNo" -> "12345678"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include(messages("err.summary"))

      verify(mockService, times(0)).updatePreviouslyRegistered(any(), any())
    }
  }
}
