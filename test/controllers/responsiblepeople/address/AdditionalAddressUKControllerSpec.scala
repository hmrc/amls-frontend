/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.autocomplete.NameValuePair
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.AutoCompleteService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.responsiblepeople.address.additional_address_UK

import scala.collection.JavaConversions._
import scala.concurrent.Future

class AdditionalAddressUKControllerSpec extends AmlsSpec with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val auditConnector = mock[AuditConnector]
    val autoCompleteService = mock[AutoCompleteService]
    lazy val view = app.injector.instanceOf[additional_address_UK]
    val additionalAddressUKController = new AdditionalAddressUKController(
      dataCacheConnector = mockDataCacheConnector,
      auditConnector = auditConnector,
      authAction = SuccessfulAuthAction,
      autoCompleteService = autoCompleteService,
      ds = commonDependencies,
      cc = mockMcc,
      additional_address_UK = view,
      error = errorView
    )

    when {
      auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)

    when {
      autoCompleteService.getCountries
    } thenReturn Some(Seq(
      NameValuePair("United Kingdom", "UK"),
      NameValuePair("Spain", "ES")
    ))
  }

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "AdditionalAddressController" when {

    val pageTitle = Messages("responsiblepeople.additional_address_country.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname"))


    "get is called" must {

      "display the persons page when no existing data in mongoCache" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalAddressUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))


        val result = additionalAddressUKController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
        document.select("input[name=addressLine1]").`val` must be("")
        document.select("input[name=addressLine2]").`val` must be("")
        document.select("input[name=addressLine3]").`val` must be("")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=postcode]").`val` must be("")
      }

      "display the previous home address with UK fields populated" in new Fixture {

        val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
        val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
        val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

        when(additionalAddressUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressUKController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
        document.select("input[name=addressLine1]").`val` must be("Line 1")
        document.select("input[name=addressLine2]").`val` must be("Line 2")
        document.select("input[name=addressLine3]").`val` must be("Line 3")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=postcode]").`val` must be("AA1 1AA")
      }

      "display 404 NotFound" when {

        "person cannot be found" in new Fixture {

          val responsiblePeople = ResponsiblePerson()

          when(additionalAddressUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = additionalAddressUKController.get(RecordId)(request)
          status(result) must be(NOT_FOUND)
        }

      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {

        "all the mandatory UK parameters are supplied" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(additionalAddressUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressUKController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "Line 1"
              d.detail("addressLine2") mustBe "Line 2"
              d.detail("postCode") mustBe "AA1 1AA"
          }
        }
      }
      "respond with BAD_REQUEST" when {

        "the default fields for UK are not supplied" in new Fixture {

          val requestWithMissingParams = requestWithUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "",
            "addressLine2" -> "",
            "postCode" -> ""
          )

          when(additionalAddressUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressUKController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLine1]").html() must include(Messages("error.required.address.line1"))
          document.select("a[href=#addressLine2]").html() must include(Messages("error.required.address.line2"))
          document.select("a[href=#postcode]").html() must include(Messages("error.required.postcode"))
        }

        "given an invalid uk address" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line *1",
            "addressLine2" -> "Line &2",
            "postCode" -> "AA1 1AA"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName,addressHistory = Some(history))

          when(additionalAddressUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressUKController.post(RecordId)(requestWithParams)
          status(result) must be(BAD_REQUEST)

          val document: Document  = Jsoup.parse(contentAsString(result))
          document.title mustBe s"Error: $pageTitle"
          val errorCount = 2
          val elementsWithError : Elements = document.getElementsByClass("error-notification")
          elementsWithError.size() must be(errorCount)
          val elements = elementsWithError.map(_.text())
          elements.get(0) must include(Messages("error.required.enter.addresslineone.regex"))
          elements.get(1) must include(Messages("error.required.enter.addresslinetwo.regex"))
        }
      }

      "go to DetailedAnswers" when {
        "edit is true"  in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "New line 1",
            "addressLine2" -> "New line 2",
            "postCode" -> "TE1 1ET"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(additionalAddressUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressUKController.post(RecordId, edit = true, Some(flowFromDeclaration))(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(RecordId, Some(flowFromDeclaration)).url))

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(auditConnector).sendEvent(captor.capture())(any(), any())

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


      "go to TimeAtAdditionalAddress page" when {
        "edit is false" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "true",
          "addressLine1" -> "Line 1",
          "addressLine2" -> "Line 2",
          "postCode" -> "AA1 1AA"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, None)
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName, addressHistory = Some(history))

          when(additionalAddressUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressUKController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtAdditionalAddressController.get(RecordId).url))
        }

        "edit is true and time at address does not exist" in new Fixture {
          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, None)
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(additionalAddressUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressUKController.post(RecordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtAdditionalAddressController.get(RecordId, true).url))
        }
      }
    }
  }
}
