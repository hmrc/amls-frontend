/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails._
import models.businessmatching.{BusinessMatching, BusinessType}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.businessdetails.previously_registered

import scala.concurrent.Future

class PreviouslyRegisteredControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {
  trait Fixture {
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[previously_registered]
    val controller = new PreviouslyRegisteredController (
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      previously_registered = view)
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessRegisteredWithHMRCBeforeController" must {

    "on get display the previously registered with HMRC page" in new Fixture {
      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())
        (any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("businessdetails.registeredformlr.title"))
    }

    "on get display the previously registered with HMRC with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(BusinessDetails(Some(PreviouslyRegisteredYes(Some("12345678")))))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=true]").hasAttr("checked") must be(true)
    }

    "on post with valid data and businesstype is corporate" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "previouslyRegistered" -> "true",
        "prevMLRRegNo" -> "12345678"
      )

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
      when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
        .thenReturn(Some(BusinessDetails(Some(PreviouslyRegisteredNo))))
      when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessdetails.routes.ActivityStartDateController.get(false).url))
    }

    "on post with valid data and load confirm address page when businessType is SoleProprietor" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "previouslyRegistered" -> "false",
        "prevMLRRegNo" -> "12345678"
      )
      val reviewDtls = ReviewDetails("BusinessName", None,
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
      when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
        .thenReturn(Some(BusinessDetails(Some(PreviouslyRegisteredNo))))

      when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessdetails.routes.ConfirmRegisteredOfficeController.get().url))
    }

    "on post with valid data" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "previouslyRegistered" -> "true",
        "prevMLRRegNo" -> "12345678"
      )
      val reviewDtls = ReviewDetails("BusinessName", None,
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"),Country("United Kingdom", "GB")), "ghghg")

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
      when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
        .thenReturn(None)

      when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessdetails.routes.ConfirmRegisteredOfficeController.get().url))
    }

    "on post with valid data in edit mode and load summary page" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "previouslyRegistered" -> "true",
        "prevMLRRegNo" -> "12345678"
      )
      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls))))
      when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
        .thenReturn(Some(BusinessDetails(Some(PreviouslyRegisteredNo))))

      when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessdetails.routes.SummaryController.get.url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "prevMLRRegNo" -> "12345678"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include(Messages("err.summary"))
    }
  }
}
