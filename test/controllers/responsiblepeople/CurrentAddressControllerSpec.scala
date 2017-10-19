/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.responsiblepeople

import audit.AddressCreatedEvent
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeApplication

import scala.collection.JavaConversions._
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.AuthorisedFixture
import audit.AddressConversions._
import uk.gov.hmrc.play.audit.AuditExtensions._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class CurrentAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  implicit val hc = HeaderCarrier()
  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val currentAddressController = new CurrentAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val auditConnector = mock[AuditConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }

    when {
      currentAddressController.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)
  }
  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true))

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "CurrentAddressController" when {

    val pageTitle = Messages("responsiblepeople.wherepersonlives.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")

    val personName = Some(PersonName("firstname", None, "lastname", None, None))

    "get is called" must {

      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {
        val responsiblePeople = ResponsiblePeople()

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(40)(request)
        status(result) must be(NOT_FOUND)
      }

      "display the persons page when no existing data in save4later" in new Fixture {

        val responsiblePeople = ResponsiblePeople(personName)

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(true)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
        document.select("input[name=addressLine1]").`val` must be("")
        document.select("input[name=addressLine2]").`val` must be("")
        document.select("input[name=addressLine3]").`val` must be("")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=addressLineNonUK1]").`val` must be("")
        document.select("input[name=addressLineNonUK2]").`val` must be("")
        document.select("input[name=addressLineNonUK3]").`val` must be("")
        document.select("input[name=addressLineNonUK4]").`val` must be("")
        document.select("input[name=postcode]").`val` must be("")
        document.select("input[name=country]").`val` must be("")
        document.select("input[name=timeAtAddress][value=01]").hasAttr("checked") must be(false)
        document.select("input[name=timeAtAddress][value=02]").hasAttr("checked") must be(false)
        document.select("input[name=timeAtAddress][value=03]").hasAttr("checked") must be(false)
        document.select("input[name=timeAtAddress][value=04]").hasAttr("checked") must be(false)
      }

      "display the previous home address with UK fields populated" in new Fixture {

        val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
        val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
        val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(personName = personName,addressHistory = Some(history))

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(true)
        document.select("input[name=addressLine1]").`val` must be("Line 1")
        document.select("input[name=addressLine2]").`val` must be("Line 2")
        document.select("input[name=addressLine3]").`val` must be("Line 3")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=postcode]").`val` must be("AA1 1AA")
      }

      "display the previous home address with non-UK fields populated" in new Fixture {

        val nonukAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES"))
        val additionalAddress = ResponsiblePersonCurrentAddress(nonukAddress, Some(SixToElevenMonths))
        val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(true)
        document.select("input[name=addressLineNonUK1]").`val` must be("Line 1")
        document.select("input[name=addressLineNonUK2]").`val` must be("Line 2")
        document.select("input[name=addressLineNonUK3]").`val` must be("")
        document.select("input[name=addressLineNonUK4]").`val` must be("")
        document.select("select[name=country] > option[value=ES]").hasAttr("selected") must be(true)
      }
    }

    "post is called" must {
      "redirect to TimeAtAddressController" when {

        "all the mandatory UK parameters are supplied" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtCurrentAddressController.get(RecordId).url))

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(currentAddressController.auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "Line 1"
              d.detail("addressLine2") mustBe "Line 2"
              d.detail("postCode") mustBe "AA1 1AA"
          }
        }

        "all the mandatory non-UK parameters are supplied" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "Line 1",
            "addressLineNonUK2" -> "Line 2",
            "country" -> "ES"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtCurrentAddressController.get(RecordId).url))

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(currentAddressController.auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "Line 1"
              d.detail("addressLine2") mustBe "Line 2"
              d.detail("country") mustBe "Spain"
          }
        }

      }

      "redirect to CurrentAddressDateOfChangeController" when {
        "address changed and in approved state" in new Fixture {
          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "New line 1",
            "addressLine2" -> "New line 2",
            "postCode" -> "TE1 1ET"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history), lineId = Some(1))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = currentAddressController.post(RecordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CurrentAddressDateOfChangeController.get(RecordId,true).url))

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(currentAddressController.auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "New line 1"
              d.detail("addressLine2") mustBe "New line 2"
              d.detail("postCode") mustBe "TE1 1ET"
              d.detail("originalLine1") mustBe "Line 1"
              d.detail("originalLine2") mustBe "Line 2"
              d.detail("originalPostCode") mustBe "AA1 1AA"
          }
        }
      }

      "redirect to CurrentAddressDateOfChangeController" when {
        "address changed and in ready for renewal state" in new Fixture {
          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history), lineId = Some(1))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(ReadyForRenewal(None)))

          val result = currentAddressController.post(RecordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CurrentAddressDateOfChangeController.get(RecordId,true).url))

        }
      }

      "redirect to DetailedAnswersController" when {
        "edit is true" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, true).url))

        }
      }

      "respond with BAD_REQUEST" when {

        "given an invalid address" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line &1",
            "addressLine2" -> "Line *2",
            "postCode" -> "AA1 1AA"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithParams)
          status(result) must be(BAD_REQUEST)
          val document: Document  = Jsoup.parse(contentAsString(result))
          document.title must be(pageTitle)
          val errorCount = 2
          val elementsWithError : Elements = document.getElementsByClass("error-notification")
          elementsWithError.size() must be(errorCount)
          for (ele: Element <- elementsWithError) {
            ele.html() must include(Messages("err.text.validation"))
          }
        }

        "isUK field is not supplied" in new Fixture {

          val line1MissingRequest = request.withFormUrlEncodedBody()

          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#isUK]").html() must include(Messages("error.required.uk.or.overseas"))
        }

        "the default fields for UK are not supplied" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "",
            "addressLine2" -> "",
            "postCode" -> ""
          )

          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLine1]").html() must include(Messages("error.required.address.line1"))
          document.select("a[href=#addressLine2]").html() must include(Messages("error.required.address.line2"))
          document.select("a[href=#postcode]").html() must include(Messages("error.invalid.postcode"))
        }

        "the default fields for overseas are not supplied" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "",
            "addressLineNonUK2" -> "",
            "country" -> ""
          )

          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLineNonUK1]").html() must include(Messages("error.required.address.line1"))
          document.select("a[href=#addressLineNonUK2]").html() must include(Messages("error.required.address.line2"))
          document.select("a[href=#country]").html() must include(Messages("error.required.country"))
        }
      }

      "respond with NOT_FOUND" when {
        "given an out of bounds index" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(outOfBounds, true)(requestWithParams)

          status(result) must be(NOT_FOUND)
        }

      }

    }
  }

  it must {
    "use the correct services" in new Fixture {
      AdditionalAddressController.dataCacheConnector must be(DataCacheConnector)
      AdditionalAddressController.authConnector must be(AMLSAuthConnector)
    }
  }

}


