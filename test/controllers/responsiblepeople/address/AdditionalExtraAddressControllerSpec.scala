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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.Country
import models.autocomplete.NameValuePair
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AutoCompleteService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class AdditionalExtraAddressControllerSpec extends AmlsSpec with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val autoCompleteService = mock[AutoCompleteService]

    val additionalExtraAddressController = new AdditionalExtraAddressController (
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      autoCompleteService = autoCompleteService
    )

    when {
      autoCompleteService.getCountries
    } thenReturn Some(Seq(
      NameValuePair("Country 1", "country:1"),
      NameValuePair("Country 2", "country:2")
    ))
  }

  val personName = Some(PersonName("firstname", None, "lastname"))

  val emptyCache = CacheMap("", Map.empty)

  val mockCacheMap = mock[CacheMap]

  "AdditionalExtraAddressController" when {


    "get is called" must {

      "display the address country page" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(false)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
      }

      "display the address page with pre-populated data for UK address" in new Fixture {

        val ukAddress = PersonAddressUK(
          "existingAddressLine1",
          "existingAddressLine1",
          Some("existingAddressLine3"),
          Some("existingAddressLine4"),
          "AA11AA"
        )

        val responsiblePeople = ResponsiblePerson(
          personName = personName,
          addressHistory = Some(ResponsiblePersonAddressHistory(
            currentAddress = None,
            additionalAddress = None,
            additionalExtraAddress = Some(ResponsiblePersonAddress(ukAddress, None))
          ))
        )

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(true)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
      }

      "display the address page with pre-populated data for non-UK address" in new Fixture {

        val ukAddress = PersonAddressNonUK(
          "existingAddressLine1",
          "existingAddressLine1",
          Some("existingAddressLine3"),
          Some("existingAddressLine4"),
          Country("Spain", "ES")
        )

        val responsiblePeople = ResponsiblePerson(
          personName = personName,
          addressHistory = Some(ResponsiblePersonAddressHistory(
            currentAddress = None,
            additionalAddress = None,
            additionalExtraAddress = Some(ResponsiblePersonAddress(ukAddress, None))
          ))
        )

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(false)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(true)
      }
    }

    "respond with NOT_FOUND" when {
      "address cannot be found" in new Fixture {
        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

        val result = additionalExtraAddressController.get(RecordId)(request)
        status(result) must be(NOT_FOUND)
      }
    }
  }

  "post is called" when {

    "form is valid" must {
      "go to AdditionalExtraAddressUKController" when {
        "user selected Yes" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true"
          )

          val responsiblePeople = ResponsiblePerson(personName)

          when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.AdditionalExtraAddressUKController.get(RecordId).url))
        }
      }

      "go to AdditionalExtraAddressNonUKController" when {
        "user selected No" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "false"
          )

          val responsiblePeople = ResponsiblePerson(personName)

          when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.AdditionalExtraAddressNonUKController.get(RecordId).url))
        }
      }
    }

    "respond with BAD_REQUEST" when {
      "form is invalid" in new Fixture {
        val requestWithParams = request.withFormUrlEncodedBody()

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(additionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
        status(result) must be(BAD_REQUEST)
      }
    }

    "process form as valid" when {
      "isUK is defined and false" in new Fixture {
        val requestWithParams = request.withFormUrlEncodedBody(
          "isUK" -> "false"
        )

        val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.AdditionalExtraAddressNonUKController.get(RecordId).url))
      }

      "isUK is defined and true" in new Fixture {
        val requestWithParams = request.withFormUrlEncodedBody(
          "isUK" -> "true"
        )

        val result = additionalExtraAddressController.post(RecordId)(requestWithParams)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.AdditionalExtraAddressUKController.get(RecordId).url))
      }
    }
  }
}
