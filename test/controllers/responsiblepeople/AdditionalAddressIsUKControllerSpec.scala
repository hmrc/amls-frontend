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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.Country
import models.autocomplete.NameValuePair
import models.responsiblepeople.ResponsiblePerson._
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

class AdditionalAddressIsUKControllerSpec extends AmlsSpec with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val auditConnector = mock[AuditConnector]
    val autoCompleteService = mock[AutoCompleteService]

    val additionalAddressController = new AdditionalAddressIsUKController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      auditConnector = auditConnector,
      autoCompleteService = autoCompleteService
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

    val emptyCache = CacheMap("", Map.empty)
    val outOfBounds = 99
    val mockCacheMap = mock[CacheMap]

  }

  "AdditionalAddressIsUKController" when {

    val pageTitle = Messages("responsiblepeople.additional_address_IsUK.title") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname"))

    "display the AdditionalAddressIsUK page" in new Fixture {

      val responsiblePeople = ResponsiblePerson(personName)

      when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))


      val result = additionalAddressController.get(RecordId)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.title must be(pageTitle)
      document.select("input[name=isUK][value=true]").hasAttr("checked") must be(true)
      document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
    }

    "successfully submit form and navigate to AdditionalAddressUKController when true and edit false" in new Fixture {
      when(mockDataCacheConnector.save[ResponsiblePerson](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

      val newRequest = request.withFormUrlEncodedBody(
        "isUK" -> "true"
      )

      val result = additionalAddressController.post(0)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.AdditionalAddressUKController.get(0).url))
    }

    "successfully submit form and navigate to AdditionalAddressNonUKController when false and edit false" in new Fixture {
      when(mockDataCacheConnector.save[ResponsiblePerson](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

      val newRequest = request.withFormUrlEncodedBody(
        "isUK" -> "false"
      )

      val result = additionalAddressController.post(0)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.AdditionalAddressNonUKController.get(0).url))
    }

    "successfully submit form and navigate to AdditionalAddressUKController when true and edit true and UKAddressSaved" in new Fixture {
      val requestWithParams = request.withFormUrlEncodedBody(
        "isUK" -> "true"
      )
      val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
      val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
      val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
      val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

      when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
      when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId, edit = true, Some(flowFromDeclaration))(requestWithParams)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.AdditionalAddressUKController.get(RecordId, true, Some(flowFromDeclaration)).url))
    }

    "successfully submit form and navigate to AdditionalAddressNonUKController when false and edit true and NonUKAddresSaved" in new Fixture {
      val requestWithParams = request.withFormUrlEncodedBody(
        "isUK" -> "false"
      )
      val UKAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("ES", "Spain"))
      val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
      val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
      val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

      when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
      when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId, edit = true, Some(flowFromDeclaration))(requestWithParams)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.AdditionalAddressNonUKController.get(RecordId, true, Some(flowFromDeclaration)).url))
    }

    "display 404 NotFound" when {

      "person cannot be found" in new Fixture {

        val responsiblePeople = ResponsiblePerson()

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(NOT_FOUND)
      }

    }

    "respond with NOT_FOUND" when {
      "given an index out of bounds in edit mode" in new Fixture {

        val requestWithParams = request.withFormUrlEncodedBody(
          "isUK" -> "true"
        )

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))
        when(additionalAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
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
    }
    "edit is true" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "isUK" -> "true"
      )
      val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
      val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
      val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
      val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

      when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
      when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId, edit = true, Some(flowFromDeclaration))(requestWithParams)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, Some(flowFromDeclaration)).url))
    }
  }
}

