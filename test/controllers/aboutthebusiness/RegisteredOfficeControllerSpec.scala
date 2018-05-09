/*
 * Copyright 2018 HM Revenue & Customs
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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.aboutthebusiness._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}

import scala.collection.JavaConversions._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.AuthorisedFixture

import scala.concurrent.Future

class RegisteredOfficeControllerSpec extends AmlsSpec with  MockitoSugar{

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new RegisteredOfficeController () {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
      override val auditConnector = mock[AuditConnector]
    }

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)
  }
  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true))

  val emptyCache = CacheMap("", Map.empty)

  "RegisteredOfficeController" must {

    "use correct services" in new Fixture {
      RegisteredOfficeController.authConnector must be(AMLSAuthConnector)
      RegisteredOfficeController.dataCacheConnector must be(DataCacheConnector)
    }

    val ukAddress = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "AA1 1AA")

    "load the where is your registered office or main place of business place page" in new Fixture {

       when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("aboutthebusiness.registeredoffice.title"))

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=isUK]").`val` must be("true")
      document.select("input[name=addressLine2]").`val` must be("")

    }

    "pre select uk when not in edit mode" in new Fixture {

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any())).
        thenReturn(Future.successful(Some(AboutTheBusiness(None,None, None, None, None, Some(ukAddress), None))))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=isUK]").`val` must be("true")
      document.select("input[name=addressLine2]").`val` must be("address line")
    }

    "pre populate where is your registered office or main place of business page with saved data" in new Fixture {

      when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionDecisionRejected))
      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(AboutTheBusiness(None, None, None, None, None, Some(ukAddress), None))))

      val result = controller.get(true)(request)
      status(result) must be(OK)
      contentAsString(result) must include("305")
    }

    "successfully submit form and navigate to target page" in new Fixture {
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))
      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(AboutTheBusiness(None,None, None, None, None, Some(ukAddress), None))))
      when (controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val newRequest = request.withFormUrlEncodedBody(
        "isUK"-> "true",
        "addressLine1"->"line1",
        "addressLine2"->"line2",
        "addressLine3"->"",
        "addressLine4"->"",
        "postCode"->"AA1 1AA")

      val result = controller.post()(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ContactingYouController.get().url))

      val captor = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(controller.auditConnector).sendEvent(captor.capture())(any(), any())

      captor.getValue match {
        case d: DataEvent =>
          d.detail("addressLine1") mustBe "line1"
          d.detail("addressLine2") mustBe "line2"
          d.detail("postCode") mustBe "AA1 1AA"
      }
    }

    "successfully submit form and navigate to summary page after edit" in new Fixture {

      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))
      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(AboutTheBusiness(None,None, None, None, None, Some(ukAddress), None))))
      when (controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val newRequest = request.withFormUrlEncodedBody(
        "isUK"-> "true",
        "addressLine1"->"line1",
        "addressLine2"->"line2",
        "addressLine3"->"",
        "addressLine4"->"",
        "postCode"->"AA1 1AA")

      val result = controller.post(edit = true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))

      val captor = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(controller.auditConnector).sendEvent(captor.capture())(any(), any())

      captor.getValue match {
        case d: DataEvent =>
          d.detail("addressLine1") mustBe "line1"
          d.detail("addressLine2") mustBe "line2"
          d.detail("postCode") mustBe "AA1 1AA"
          d.detail("originalLine1") mustBe "305"
          d.detail("originalLine2") mustBe "address line"
          d.detail("originalLine3") mustBe "address line2"
          d.detail("originalPostCode") mustBe "AA1 1AA"
      }
    }

    "fail submission on invalid address" in new Fixture {
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))

      when(controller.dataCacheConnector.fetch(any())(any(), any(), any()))
        .thenReturn(Future.successful(None))
      when (controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val newRequest = request.withFormUrlEncodedBody(
        "isUK"-> "true",
        "addressLine1"->"line1 &",
        "addressLine2"->"line2 *",
        "addressLine3"->"",
        "addressLine4"->"",
        "postCode"->"AA1 1AA")

      val result = controller.post()(newRequest)
      val document: Document  = Jsoup.parse(contentAsString(result))
      val errorCount = 2
      val elementsWithError : Elements = document.getElementsByClass("error-notification")
      elementsWithError.size() must be(errorCount)
      for (ele: Element <- elementsWithError) {
        ele.html() must include(Messages("err.text.validation"))
      }
    }

    "respond with BAD_REQUEST" when {

      "form validation fails" in new Fixture {

        when(controller.dataCacheConnector.fetch(any())(any(), any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save(any(), any())(any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val newRequest = request.withFormUrlEncodedBody(
          "isUK" -> "true",
          "addressLine2" -> "line2",
          "addressLine3" -> "",
          "addressLine4" -> "",
          "postCode" -> "AA1 1AA")
        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("err.summary"))

      }

    }

    "go to the date of change page" when {
      "the submission has been approved and registeredOffice has changed" in new Fixture {

        when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(AboutTheBusiness(None,None, None, None, None, Some(ukAddress), None))))
        when(controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))
        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val newRequest = request.withFormUrlEncodedBody(
          "isUK" -> "true",
          "addressLine1" -> "line1",
          "addressLine2" -> "line2",
          "addressLine3" -> "",
          "addressLine4" -> "",
          "postCode" -> "AA1 1AA")

        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.RegisteredOfficeDateOfChangeController.get().url))
      }

      "status is ready for renewal and registeredOffice has changed" in new Fixture {

        when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(AboutTheBusiness(None,None, None, None, None, Some(ukAddress), None))))
        when(controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))
        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(ReadyForRenewal(None)))

        val newRequest = request.withFormUrlEncodedBody(
          "isUK" -> "true",
          "addressLine1" -> "line1",
          "addressLine2" -> "line2",
          "addressLine3" -> "",
          "addressLine4" -> "",
          "postCode" -> "AA1 1AA")

        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.RegisteredOfficeDateOfChangeController.get().url))
      }
    }

  }
}