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

package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.autocomplete.NameValuePair
import models.responsiblepeople.ResponsiblePeople._
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConversions._
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.AutoCompleteService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.AuthorisedFixture

import scala.concurrent.Future

class AdditionalAddressControllerSpec extends AmlsSpec with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val additionalAddressController = new AdditionalAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override lazy val auditConnector = mock[AuditConnector]
      override val autoCompleteService = mock[AutoCompleteService]
    }

    when {
      additionalAddressController.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)

    when {
      additionalAddressController.autoCompleteService.getCountries
    } thenReturn Some(Seq(
      NameValuePair("United Kingdom", "UK"),
      NameValuePair("Spain", "ES")
    ))
  }

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "AdditionalAddressController" when {

    val pageTitle = Messages("responsiblepeople.additional_address.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname"))


    "get is called" must {

      "display the persons page when no existing data in save4later" in new Fixture {

        val responsiblePeople = ResponsiblePeople(personName)

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))


        val result = additionalAddressController.get(RecordId)(request)
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
      }

      "display the previous home address with UK fields populated" in new Fixture {

        val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
        val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
        val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressController.get(RecordId)(request)
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

        val nonUKAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES"))
        val additionalAddress = ResponsiblePersonAddress(nonUKAddress, Some(SixToElevenMonths))
        val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(true)
        document.select("input[name=addressLineNonUK1]").`val` must be("Line 1")
        document.select("input[name=addressLineNonUK2]").`val` must be("Line 2")
        document.select("input[name=addressLineNonUK3]").`val` must be("")
        document.select("input[name=addressLineNonUK4]").`val` must be("")
        document.select("select[name=country] > option[value=ES]").hasAttr("selected") must be(true)
      }

      "display 404 NotFound" when {

        "person cannot be found" in new Fixture {

          val responsiblePeople = ResponsiblePeople()

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = additionalAddressController.get(RecordId)(request)
          status(result) must be(NOT_FOUND)
        }

      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {

        "all the mandatory UK parameters are supplied" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(additionalAddressController.auditConnector).sendEvent(captor.capture())(any(), any())

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
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(additionalAddressController.auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "Line 1"
              d.detail("addressLine2") mustBe "Line 2"
              d.detail("country") mustBe "Spain"
          }
        }

      }

      "respond with NOT_FOUND" when {
        "given an index out of bounds in edit mode" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
          when(additionalAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(outOfBounds, true)(requestWithParams)

          status(result) must be(NOT_FOUND)
        }
      }

      "respond with BAD_REQUEST" when {

        "isUK field is not supplied" in new Fixture {

          val line1MissingRequest = request.withFormUrlEncodedBody()

          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(line1MissingRequest)
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

          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithMissingParams)
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

          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLineNonUK1]").html() must include(Messages("error.required.address.line1"))
          document.select("a[href=#addressLineNonUK2]").html() must include(Messages("error.required.address.line2"))
          document.select("a[href=#country]").html() must include(Messages("error.required.country"))
        }

        "given an invalid uk address" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line *1",
            "addressLine2" -> "Line &2",
            "postCode" -> "AA1 1AA"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(personName = personName,addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)
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

        "given an invalid non uk address" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "Line *1",
            "addressLineNonUK2" -> "Line *2",
            "country" -> "ES"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)
          status(result) must be(BAD_REQUEST)

          val document: Document  = Jsoup.parse(contentAsString(result))
          val errorCount = 2
          val elementsWithError : Elements = document.getElementsByClass("error-notification")
          elementsWithError.size() must be(errorCount)
          for (ele: Element <- elementsWithError) {
            ele.html() must include(Messages("err.text.validation"))
          }
        }


      }

      "go to DetailedAnswers" when {
        "edit is true"  in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "New line 1",
            "addressLine2" -> "New line 2",
            "postCode" -> "TE1 1ET"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId, edit = true, Some(flowFromDeclaration))(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, true, Some(flowFromDeclaration)).url))

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(additionalAddressController.auditConnector).sendEvent(captor.capture())(any(), any())

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

          val requestWithParams = request.withFormUrlEncodedBody(
          "isUK" -> "true",
          "addressLine1" -> "Line 1",
          "addressLine2" -> "Line 2",
          "postCode" -> "AA1 1AA"
          )
          val responsiblePeople = ResponsiblePeople()

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtAdditionalAddressController.get(RecordId).url))
        }

        "edit is true and time at address does not exist" in new Fixture {
          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, None)
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtAdditionalAddressController.get(RecordId, true).url))
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
