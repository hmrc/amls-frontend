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

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.Country
import models.businessmatching.{BusinessMatching, BusinessType}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import models.businesscustomer.{Address => BusinessCustomerAddress, ReviewDetails}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.cache.Cache
import utils.{AmlsSpec, AuthAction}

import scala.concurrent.Future

class ConfirmPostCodeControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val dataCacheConnector = mock[DataCacheConnector]

    lazy val app = new GuiceApplicationBuilder()
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .build()

    val controller = app.injector.instanceOf[ConfirmPostCodeController]
  }

  val emptyCache      = Cache.empty
  val businessAddress = BusinessCustomerAddress(
    "line1",
    Some("line2"),
    Some("line3"),
    Some("line4"),
    Some("AA1 1AA"),
    Country("United Kingdom", "GB")
  )
  val reviewDtls      = ReviewDetails(
    "BusinessName",
    Some(BusinessType.LimitedCompany),
    BusinessCustomerAddress(
      "line1",
      Some("line2"),
      Some("line3"),
      Some("line4"),
      Some("AA1 1AA"),
      Country("United Kingdom", "GB")
    ),
    "ghghg"
  )

  val businessMatching = BusinessMatching(reviewDetails = Some(reviewDtls))

  "ConfirmPostCodeController" must {

    "display confirm post code of your business page successfully" in new Fixture {
      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "update ReviewDetails with valid input post code" in new Fixture {
      val postRequest = FakeRequest(POST, routes.ConfirmPostCodeController.post().url)
        .withFormUrlEncodedBody(
          "postCode" -> "BB1 1BB"
        )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(Some(businessMatching)))

      val updatedModel = businessMatching.copy(reviewDetails =
        Some(reviewDtls.copy(businessAddress = businessAddress.copy(postcode = Some("BB1 1BB"))))
      )
      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), meq(updatedModel))(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(postRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BusinessTypeController.get().url))

      verify(controller.dataCacheConnector)
        .save[BusinessMatching](any(), meq(BusinessMatching.key), meq(updatedModel))(any())
    }

    "update ReviewDetails with valid input post code and UK as country" in new Fixture {
      val postRequest = FakeRequest(POST, routes.ConfirmPostCodeController.post().url)
        .withFormUrlEncodedBody(
          "postCode" -> "BB1 1BB"
        )

      val businessMatchingWithEmptyCountry = businessMatching.copy(
        reviewDetails = Some(reviewDtls.copy(businessAddress = businessAddress.copy(country = Country("", ""))))
      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(Some(businessMatchingWithEmptyCountry)))

      val updatedModel = businessMatching.copy(reviewDetails =
        Some(reviewDtls.copy(businessAddress = businessAddress.copy(postcode = Some("BB1 1BB"))))
      )
      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), meq(updatedModel))(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(postRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BusinessTypeController.get().url))

      verify(controller.dataCacheConnector)
        .save[BusinessMatching](any(), meq(BusinessMatching.key), meq(updatedModel))(any())
    }

    "update ReviewDetails as none when business matching -> reviewDetails is empty" in new Fixture {
      val postRequest = FakeRequest(POST, routes.ConfirmPostCodeController.post().url)
        .withFormUrlEncodedBody(
          "postCode" -> "BB1 1BB"
        )
      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(Some(businessMatching.copy(reviewDetails = None))))

      val updatedModel = businessMatching.copy(reviewDetails = None)
      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), meq(updatedModel))(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(postRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BusinessTypeController.get().url))
    }

    "throw validation error on invalid field" in new Fixture {

      val postRequest = FakeRequest(POST, routes.ConfirmPostCodeController.post().url)
        .withFormUrlEncodedBody(
          "postCode" -> "AA1111AA"
        )
      val result      = controller.post()(postRequest)
      status(result) must be(BAD_REQUEST)
    }
  }
}
