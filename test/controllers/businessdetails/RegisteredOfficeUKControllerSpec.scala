/*
 * Copyright 2023 HM Revenue & Customs
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
import models.businessdetails._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{AmlsSpec, AutoCompleteServiceMocks}
import views.html.businessdetails.registered_office_uk

import scala.collection.JavaConverters._
import scala.concurrent.Future

class RegisteredOfficeUKControllerSpec extends AmlsSpec with  MockitoSugar{

  trait Fixture extends AutoCompleteServiceMocks {
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[registered_office_uk]
    val controller = new RegisteredOfficeUKController(
      dataCacheConnector = mock[DataCacheConnector],
      statusService = mock[StatusService],
      auditConnector = mock[AuditConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      registered_office_uk = view)

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)
  }

  val emptyCache = CacheMap("", Map.empty)

  "RegisteredOfficeUKController" must {

    val ukAddress = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "AA1 1AA")

    "load the where is your registered office or main place of business place page" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessdetails.registeredoffice.where.title"))

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=isUK]").`val` must be("true")
      document.select("input[name=addressLine2]").`val` must be("")

    }

    "pre select uk when not in edit mode" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any())).
        thenReturn(Future.successful(Some(BusinessDetails(None,None, None, None, None, None, Some(ukAddress), None))))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=isUK]").`val` must be("true")
      document.select("input[name=addressLine2]").`val` must be("address line")
    }

    "pre populate where is your registered office or main place of business page with saved data" in new Fixture {

      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))
      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(BusinessDetails(None, None, None, None, None, None, Some(ukAddress), None))))

      val result = controller.get(true)(request)
      status(result) must be(OK)
      contentAsString(result) must include("305")
    }

    "successfully submit form and navigate to target page" in new Fixture {
      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))
      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(BusinessDetails(None,None, None, None, None, None, Some(ukAddress), None))))
      when (controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val newRequest = requestWithUrlEncodedBody(
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

      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))
      when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(BusinessDetails(None,None, None, None, None, None, Some(ukAddress), None))))
      when (controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val newRequest = requestWithUrlEncodedBody(
        "isUK"-> "true",
        "addressLine1"->"line1",
        "addressLine2"->"line2",
        "addressLine3"->"",
        "addressLine4"->"",
        "postCode"->"AA1 1AA")

      val result = controller.post(edit = true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get.url))

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
      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionRejected))

      when(controller.dataCacheConnector.fetch(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      when (controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val newRequest = requestWithUrlEncodedBody(
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

      elementsWithError.asScala.map(_.text()) must contain allOf(
        "Error: " + Messages("error.text.validation.address.line1"),
        "Error: " + Messages("error.text.validation.address.line2"))
    }

    "respond with BAD_REQUEST" when {

      "form validation fails" in new Fixture {

        when(controller.dataCacheConnector.fetch(any(), any())(any(), any())).thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save(any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCache))

        val newRequest = requestWithUrlEncodedBody(
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

        when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(BusinessDetails(None,None, None, None, None, None, Some(ukAddress), None))))
        when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))
        when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val newRequest = requestWithUrlEncodedBody(
          "isUK" -> "true",
          "addressLine1" -> "line1",
          "addressLine2" -> "line2",
          "addressLine3" -> "",
          "addressLine4" -> "",
          "postCode" -> "AA1 1AA")

        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.RegisteredOfficeDateOfChangeController.get.url))
      }

      "status is ready for renewal and registeredOffice has changed" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessDetails](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(BusinessDetails(None,None, None, None, None, None, Some(ukAddress), None))))
        when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))
        when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
          .thenReturn(Future.successful(ReadyForRenewal(None)))

        val newRequest = requestWithUrlEncodedBody(
          "isUK" -> "true",
          "addressLine1" -> "line1",
          "addressLine2" -> "line2",
          "addressLine3" -> "",
          "addressLine4" -> "",
          "postCode" -> "AA1 1AA")

        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.RegisteredOfficeDateOfChangeController.get.url))
      }
    }

  }
}