/*
 * Copyright 2019 HM Revenue & Customs
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
import models.businessdetails._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, AutoCompleteServiceMocks}

import scala.collection.JavaConversions._
import scala.concurrent.Future

class RegisteredOfficeIsUKControllerSpec extends AmlsSpec with  MockitoSugar{

  trait Fixture extends AutoCompleteServiceMocks {
    self => val request = addToken(authRequest)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    val mockCacheMap = mock[CacheMap]

    val controller = new RegisteredOfficeIsUKController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc)

    when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any()))
      .thenReturn(Future.successful(None))
    val ukAddress = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "AA1 1AA")
    val nonUKAddress = RegisteredOfficeNonUK("305", "address line", Some("address line2"), Some("address line3"), Country("Albania", "AL"))
  }

  "RegisteredOfficeIsUKController" must {
    "load the where is your registered office or main place of business place page" in new Fixture {
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessdetails.registeredoffice.title"))
    }

    "successfully submit form and navigate to RegisteredOfficeUK when true and edit false" in new Fixture {
      when(mockDataCacheConnector.save[BusinessDetails](any(), any(), any())(any(), any())).thenReturn(Future.successful(mockCacheMap))

      val newRequest = requestWithUrlEncodedBody(
        "isUK"-> "true"
      )

      val result = controller.post()(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisteredOfficeUKController.get().url))
    }

    "successfully submit form and navigate to RegisteredOfficeNonUK when false and edit false" in new Fixture {
      when(mockDataCacheConnector.save[BusinessDetails](any(), any(), any())(any(), any())).thenReturn(Future.successful(mockCacheMap))
      val newRequest = requestWithUrlEncodedBody(
        "isUK"-> "false"
      )

      val result = controller.post()(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisteredOfficeNonUKController.get().url))
    }

    "successfully submit form and navigate to RegisteredOfficeUK when true and edit true and UKAddressSaved" in new Fixture {
      when(mockDataCacheConnector.save[BusinessDetails](any(), any(), any())(any(), any())).thenReturn(Future.successful(mockCacheMap))
      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(BusinessDetails(None,None, None, None, None, Some(RegisteredOfficeIsUK(true)), Some(ukAddress), None))))

      val newRequest = requestWithUrlEncodedBody(
        "isUK"-> "true"
      )

      val result = controller.post(edit = true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisteredOfficeUKController.get(edit = true).url))
    }

    "successfully submit form and navigate to RegisteredOfficeNonUK when false and edit true and UKAddressSaved" in new Fixture {
      when(mockDataCacheConnector.save[BusinessDetails](any(), any(), any())(any(), any())).thenReturn(Future.successful(mockCacheMap))
      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(BusinessDetails(None,None, None, None, None, Some(RegisteredOfficeIsUK(false)), Some(ukAddress), None))))

      val newRequest = requestWithUrlEncodedBody(
        "isUK"-> "false"
      )

      val result = controller.post(edit = true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisteredOfficeNonUKController.get(edit = true).url))
    }

    "successfully submit form and navigate to RegisteredOfficeUK when true and edit true and NonUKAddressSaved" in new Fixture {
      when(mockDataCacheConnector.save[BusinessDetails](any(), any(), any())(any(), any())).thenReturn(Future.successful(mockCacheMap))
      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(BusinessDetails(None,None, None, None, None, Some(RegisteredOfficeIsUK(true)), Some(nonUKAddress), None))))

      val newRequest = requestWithUrlEncodedBody(
        "isUK"-> "true"
      )

      val result = controller.post(edit = true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisteredOfficeUKController.get(edit = true).url))
    }

    "successfully submit form and navigate to RegisteredOfficeNonUK when false and edit true and NonUKAddressSaved" in new Fixture {
      when(mockDataCacheConnector.save[BusinessDetails](any(), any(), any())(any(), any())).thenReturn(Future.successful(mockCacheMap))
      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(BusinessDetails(None,None, None, None, None, Some(RegisteredOfficeIsUK(false)), Some(nonUKAddress), None))))

      val newRequest = requestWithUrlEncodedBody(
        "isUK"-> "false"
      )

      val result = controller.post(edit = true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisteredOfficeNonUKController.get(edit = true).url))
    }

    "fail submission on invalid isUK" in new Fixture {
      val newRequest = requestWithUrlEncodedBody(
        "isUK"-> ""
      )

      val result = controller.post()(newRequest)
      val document: Document  = Jsoup.parse(contentAsString(result))
      val elementsWithError : Elements = document.getElementsByClass("error-notification")
      elementsWithError.size() must be(1)
      for (ele: Element <- elementsWithError) {
        ele.html() must include(Messages("error.required.atb.registered.office.uk.or.overseas"))
      }
    }

    "respond with BAD_REQUEST" when {

      "form validation fails" in new Fixture {

        when(controller.dataCacheConnector.fetch(any(), any())(any(), any())).thenReturn(Future.successful(None))

        val newRequest = requestWithUrlEncodedBody(
          "isUK" -> "*"
        )
        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("err.summary"))

      }

    }

  }
}