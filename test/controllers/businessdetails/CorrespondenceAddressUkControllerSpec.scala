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

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.businessdetails.CorrespondenceAddressUKFormProvider
import models.businessdetails.{BusinessDetails, CorrespondenceAddress, CorrespondenceAddressIsUk, CorrespondenceAddressUk}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.AutoCompleteService
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import services.cache.Cache
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.AmlsSpec
import views.html.businessdetails.CorrespondenceAddressUKView

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class CorrespondenceAddressUkControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[CorrespondenceAddressUKView]
    val controller = new CorrespondenceAddressUkController(
      dataConnector = mock[DataCacheConnector],
      auditConnector = mock[AuditConnector],
      autoCompleteService = mock[AutoCompleteService],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[CorrespondenceAddressUKFormProvider],
      view = view
    )

    when {
      controller.autoCompleteService.formOptions
    } thenReturn Seq(
      SelectItem(text = "United Kingdom", value = Some("UK")),
      SelectItem(text = "Albania", value = Some("AL"))
    )

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)
  }

  "CorrespondenceAddressController" should {

    "respond to a get request correctly with different form values" when {

      "data exists in the keystore" in new Fixture {

        val correspondenceAddress = CorrespondenceAddress(
          Some(CorrespondenceAddressUk("Name Test", "Test", "Test", Some("Test"), Some("test"), None, "POSTCODE")),
          None
        )
        val businessDetails       = BusinessDetails(
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          Some(CorrespondenceAddressIsUk(true)),
          Some(correspondenceAddress)
        )

        when(controller.dataConnector.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        val result = controller.get(false)(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))

        page.getElementById("yourName").`val`     must be("Name Test")
        page.getElementById("businessName").`val` must be("Test")
        page.getElementById("addressLine1").`val` must be("Test")
        page.getElementById("addressLine2").`val` must be("Test")
        page.getElementById("addressLine3").`val` must be("test")
        page.getElementById("addressLine4").`val` must be("")
        page.getElementById("postCode").`val`     must be("POSTCODE")
      }

      "no data exists in the keystore" in new Fixture {

        when(controller.dataConnector.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(false)(request)
        status(result)                             must be(OK)
        Jsoup.parse(contentAsString(result)).title must include(messages("businessdetails.correspondenceaddress.title"))

      }
    }

    "respond to a post request correctly" when {

      val emptyCache = Cache.empty

      "a valid form request is sent in the body" in new Fixture {

        val address =
          CorrespondenceAddressUk("Test", "Test", "old line 1", Some("old line 2"), Some("old line 3"), None, "AA1 1AA")

        val fetchResult = Future.successful(
          Some(
            BusinessDetails(
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              Some(CorrespondenceAddressIsUk(true)),
              Some(CorrespondenceAddress(Some(address), None))
            )
          )
        )

        val newRequest = FakeRequest(POST, routes.CorrespondenceAddressUkController.post().url).withFormUrlEncodedBody(
          "yourName"     -> "Name",
          "businessName" -> "Business Name",
          "isUK"         -> "true",
          "addressLine1" -> "Add Line 1",
          "addressLine2" -> "Add Line 2",
          "addressLine3" -> "",
          "addressLine4" -> "",
          "postCode"     -> "AA1 1AA"
        )

        when(controller.dataConnector.fetch[BusinessDetails](any(), any())(any())).thenReturn(fetchResult)

        when(controller.dataConnector.save[BusinessDetails](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get.url))

        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(controller.auditConnector).sendEvent(captor.capture())(any(), any())

        captor.getValue match {
          case d: DataEvent =>
            d.detail("addressLine1") mustBe "Add Line 1"
            d.detail("addressLine2") mustBe "Add Line 2"
            d.detail("postCode") mustBe "AA1 1AA"
        }
      }

      "a valid form request is sent in the body when editing" in new Fixture {

        val address =
          CorrespondenceAddressUk("Test", "Test", "old line 1", Some("old line 2"), Some("old line 3"), None, "AA1 1AA")

        val fetchResult = Future.successful(
          Some(
            BusinessDetails(
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              Some(CorrespondenceAddressIsUk(true)),
              Some(CorrespondenceAddress(Some(address), None))
            )
          )
        )

        val newRequest =
          FakeRequest(POST, routes.CorrespondenceAddressUkController.post(true).url).withFormUrlEncodedBody(
            "yourName"     -> "Name",
            "businessName" -> "Business Name",
            "isUK"         -> "true",
            "addressLine1" -> "Add Line 1",
            "addressLine2" -> "Add Line 2",
            "addressLine3" -> "",
            "addressLine4" -> "",
            "postCode"     -> "AA1 1AA"
          )

        when(controller.dataConnector.fetch[BusinessDetails](any(), any())(any())).thenReturn(fetchResult)

        when(controller.dataConnector.save[BusinessDetails](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(edit = true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get.url))

        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(controller.auditConnector).sendEvent(captor.capture())(any(), any())

        captor.getValue match {
          case d: DataEvent =>
            d.detail("addressLine1") mustBe "Add Line 1"
            d.detail("addressLine2") mustBe "Add Line 2"
            d.detail("postCode") mustBe "AA1 1AA"
            d.detail("originalLine1") mustBe "old line 1"
            d.detail("originalLine2") mustBe "old line 2"
            d.detail("originalLine3") mustBe "old line 3"
            d.detail("originalPostCode") mustBe "AA1 1AA"
        }
      }

      "fail submission on invalid address" in new Fixture {

        val fetchResult = Future.successful(None)

        val newRequest = FakeRequest(POST, routes.CorrespondenceAddressUkController.post().url).withFormUrlEncodedBody(
          "yourName"     -> "Name",
          "businessName" -> "Business Name",
          "isUK"         -> "true",
          "addressLine1" -> "Add Line 1 & 3",
          "addressLine2" -> "Add Line 2 *",
          "addressLine3" -> "$$$",
          "addressLine4" -> "##",
          "postCode"     -> "AA1 1AA"
        )

        when(controller.dataConnector.fetch[BusinessDetails](any(), any())(any())).thenReturn(fetchResult)

        when(controller.dataConnector.save[BusinessDetails](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(BAD_REQUEST)

        val document: Document          = Jsoup.parse(contentAsString(result))
        val errorCount                  = 4
        val elementsWithError: Elements = document.select(".govuk-error-summary__list li")
        elementsWithError.size() must be(errorCount)

        elementsWithError.asScala.map(_.text()) must contain allOf (messages("error.text.validation.address.line1"),
        messages("error.text.validation.address.line2"),
        messages("error.text.validation.address.line3"),
        messages("error.text.validation.address.line4"))
      }

      "an invalid form request is sent in the body" in new Fixture {

        val newRequest = FakeRequest(POST, routes.CorrespondenceAddressUkController.post().url).withFormUrlEncodedBody(
          "yourName"     -> "Name",
          "businessName" -> "Business Name",
          "invalid"      -> "AA1 1AA"
        )

        val result = controller.post(false)(newRequest)
        status(result) must be(BAD_REQUEST)
      }
    }
  }
}
