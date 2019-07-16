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
import models.Country
import models.autocomplete.NameValuePair
import models.businessdetails.{BusinessDetails, CorrespondenceAddress, CorrespondenceAddressNonUk}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.AutoCompleteService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{AmlsSpec, AuthorisedFixture}

import scala.collection.JavaConversions._
import scala.concurrent.Future

class CorrespondenceAddressNonUkControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new CorrespondenceAddressNonUkController (
      dataConnector = mock[DataCacheConnector],
      authConnector = self.authConnector,
      auditConnector = mock[AuditConnector],
      autoCompleteService = mock[AutoCompleteService]
    )

    when {
      controller.autoCompleteService.getCountries
    } thenReturn Some(Seq(
      NameValuePair("United Kingdom", "UK"),
      NameValuePair("Albania", "AL")
    ))

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)
  }

  "CorrespondenceAddressNonUkController" should {

    "respond to a get request correctly with different form values" when {

      "data exists in the keystore" in new Fixture {

        val correspondenceAddress = CorrespondenceAddress(None,
          Some(CorrespondenceAddressNonUk("Name Test", "Test", "Test", "Test", Some("test"), None, Country("Albania", "AL"))))
        val businessDetails = BusinessDetails(None, None, None, None, None,None, None, None, None, Some(correspondenceAddress))

        when(controller.dataConnector.fetch[BusinessDetails](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.get(false)(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))

        page.getElementById("yourName").`val` must be("Name Test")
        page.getElementById("businessName").`val` must be("Test")
        page.getElementById("addressLineNonUK1").`val` must be("Test")
        page.getElementById("addressLineNonUK2").`val` must be("Test")
        page.getElementById("addressLineNonUK3").`val` must be("test")
        page.getElementById("addressLineNonUK4").`val` must be("")
        page.getElementById("country").getElementsByAttribute("selected").`val` must be("AL")
      }

      "no data exists in the keystore" in new Fixture {

        when(controller.dataConnector.fetch[BusinessDetails](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(false)(request)
        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title must include(Messages("businessdetails.correspondenceaddress.title"))

      }
    }

    "respond to a post request correctly" when {

      val emptyCache = CacheMap("", Map.empty)

      "a valid form request is sent in the body" in new Fixture {

        val address = CorrespondenceAddressNonUk("Test", "Test", "old line 1", "old line 2", Some("old line 3"), None, Country("Albania", "AL"))

        val fetchResult = Future.successful(Some(BusinessDetails(None,None, None, None, None, None, None, None, None, Some(CorrespondenceAddress(None, Some(address))))))

        val newRequest = request.withFormUrlEncodedBody(
          "yourName" -> "Name",
          "businessName" -> "Business Name",
          "addressLineNonUK1" -> "Add Line 1",
          "addressLineNonUK2" -> "Add Line 2",
          "addressLineNonUK3" -> "",
          "addressLineNonUK4" -> "",
          "country" -> "AL"
        )

        when(controller.dataConnector.fetch[BusinessDetails](any())
          (any(), any(), any())).thenReturn(fetchResult)

        when(controller.dataConnector.save[BusinessDetails](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))

        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(controller.auditConnector).sendEvent(captor.capture())(any(), any())

        captor.getValue match {
          case d: DataEvent =>
            d.detail("addressLine1") mustBe "Add Line 1"
            d.detail("addressLine2") mustBe "Add Line 2"
            d.detail("country") mustBe "Albania"
        }
      }

      "a valid form request is sent in the body when editing" in new Fixture {

        val address = CorrespondenceAddressNonUk("Test", "Test", "old line 1", "old line 2", Some("old line 3"), None, Country("Albania", "AL"))

        val fetchResult = Future.successful(Some(BusinessDetails(None,None, None, None, None, None, None, None, None, Some(CorrespondenceAddress(None, Some(address))))))

        val newRequest = request.withFormUrlEncodedBody(
          "yourName" -> "Name",
          "businessName" -> "Business Name",
          "addressLineNonUK1" -> "Add Line 1",
          "addressLineNonUK2" -> "Add Line 2",
          "addressLineNonUK3" -> "",
          "addressLineNonUK4" -> "",
          "country" -> "AL"
        )

        when(controller.dataConnector.fetch[BusinessDetails](any())
          (any(), any(), any())).thenReturn(fetchResult)

        when(controller.dataConnector.save[BusinessDetails](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(edit = true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))

        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(controller.auditConnector).sendEvent(captor.capture())(any(), any())

        captor.getValue match {
          case d: DataEvent =>
            d.detail("addressLine1") mustBe "Add Line 1"
            d.detail("addressLine2") mustBe "Add Line 2"
            d.detail("originalLine1") mustBe "old line 1"
            d.detail("originalLine2") mustBe "old line 2"
            d.detail("originalLine3") mustBe "old line 3"
            d.detail("originalCountry") mustBe "Albania"
        }
      }

      "fail submission on invalid address" in new Fixture {

        val fetchResult = Future.successful(None)

        val newRequest = request.withFormUrlEncodedBody(
          "yourName" -> "Name",
          "businessName" -> "Business Name",
          "addressLineNonUK1" -> "Add Line 1 & 3",
          "addressLineNonUK2" -> "Add Line 2 *",
          "addressLineNonUK3" -> "$$$",
          "addressLineNonUK4" -> "##",
          "country" -> "AL"
        )

        when(controller.dataConnector.fetch[BusinessDetails](any())
          (any(), any(), any())).thenReturn(fetchResult)

        when(controller.dataConnector.save[BusinessDetails](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(BAD_REQUEST)

        val document: Document  = Jsoup.parse(contentAsString(result))
        val errorCount = 4
        val elementsWithError : Elements = document.getElementsByClass("error-notification")
        elementsWithError.size() must be(errorCount)
        for (ele: Element <- elementsWithError) {
          ele.html() must include(Messages("err.text.validation"))
        }
      }

      "an invalid form request is sent in the body" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "yourName" -> "Name",
          "businessName" -> "Business Name",
          "invalid" -> "AL"
        )

        val result = controller.post(false)(newRequest)
        status(result) must be(BAD_REQUEST)

      }
    }
  }
}
