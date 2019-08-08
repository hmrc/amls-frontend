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
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import utils.{AmlsSpec, AuthorisedFixture}

import scala.collection.JavaConversions._
import scala.concurrent.Future

class CorrespondenceAddressIsUkControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val mockDataConnector = mock[DataCacheConnector]

    val controller = new CorrespondenceAddressIsUkController (
      dataConnector = mock[DataCacheConnector],
      auditConnector = mock[AuditConnector],
      authAction = SuccessfulAuthAction
    )

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)

    val correspondenceAddressUK = CorrespondenceAddress(Some(
      CorrespondenceAddressUk("test", "test", "line1", "line2", Some("line3"), Some("line4"), "AA1 1AA")), None)
    val correspondenceAddressNonUk = CorrespondenceAddress(None, Some(
      CorrespondenceAddressNonUk("name", "name", "line1", "line2", Some("line3"), Some("line4"), Country("Hong Kong", "HK"))))

    val businessDetails = BusinessDetails(None, None, None, None, None, None, None, None, Some(CorrespondenceAddressIsUk(true)), Some(correspondenceAddressUK))
  }

  "CorrespondenceAddressIsUkController" should {
    "respond to a get request correctly" when {
      "load IsUK page" in new Fixture {

        when(controller.dataConnector.fetch[BusinessDetails](any(), meq(BusinessDetails.key))
          (any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("businessdetails.correspondenceaddress.isuk.title"))
      }

      "load isUk result when there is already an isUK answer in BusinessDetails" in new Fixture {

        when(controller.dataConnector.fetch[BusinessDetails](any(), meq(BusinessDetails.key))
          (any(), any())).thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.get()(request)
 
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("isUK-true").hasAttr("checked") must be(true)
        htmlValue.getElementById("isUK-false").hasAttr("checked") must be(false)
      }

      "load isUk result when there is already an nonUk answer in BusinessDetails" in new Fixture {

        when(controller.dataConnector.fetch[BusinessDetails](any(), meq(BusinessDetails.key))
          (any(), any())).thenReturn(Future.successful(Some(businessDetails.copy(
          correspondenceAddressIsUk = Some(CorrespondenceAddressIsUk(false)),
          correspondenceAddress = Some(correspondenceAddressNonUk)))))

        val result = controller.get()(request)

        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("isUK-true").hasAttr("checked") must be(false)
        htmlValue.getElementById("isUK-false").hasAttr("checked") must be(true)
      }
    }

    "respond to a post request correctly" when {

      val emptyCache = CacheMap("", Map.empty)

      "when isUk is true" in new Fixture {

        val fetchResult = Future.successful(Some(BusinessDetails(None,None, None, None, None, None, None, None, None, None, false, false)))

        val newRequest = request.withFormUrlEncodedBody("isUK" -> "true")

        when(controller.dataConnector.fetch[BusinessDetails](any(), any())(any(), any()))
          .thenReturn(fetchResult)

        when(controller.dataConnector.save[BusinessDetails](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.CorrespondenceAddressUkController.get().url))
      }

      "when isUk is false" in new Fixture {

        val fetchResult = Future.successful(Some(BusinessDetails(None,None, None, None, None, None, None, None, None, None, false, false)))

        val newRequest = request.withFormUrlEncodedBody("isUK" -> "false")

        when(controller.dataConnector.fetch[BusinessDetails](any(), any())(any(), any()))
          .thenReturn(fetchResult)

        when(controller.dataConnector.save[BusinessDetails](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.CorrespondenceAddressNonUkController.get().url))
      }

      "isUk is not entered" in new Fixture {

        val fetchResult = Future.successful(None)

        val newRequest = request.withFormUrlEncodedBody( )

        when(controller.dataConnector.fetch[BusinessDetails](any(), any()) (any(), any())).thenReturn(fetchResult)

        when(controller.dataConnector.save[BusinessDetails](any(), any(), any()) (any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(BAD_REQUEST)

        val document: Document  = Jsoup.parse(contentAsString(result))
        val errorCount = 1
        val elementsWithError : Elements = document.getElementsByClass("error-notification")
        elementsWithError.size() must be(errorCount)
        for (ele: Element <- elementsWithError) {
          ele.html() must include(Messages("businessdetails.correspondenceaddress.isuk.error"))
        }
      }
    }
  }
}
