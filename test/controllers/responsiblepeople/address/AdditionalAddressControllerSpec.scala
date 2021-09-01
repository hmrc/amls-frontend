/*
 * Copyright 2021 HM Revenue & Customs
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
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, OptionValues}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.AutoCompleteService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.responsiblepeople.address.additional_address

import scala.concurrent.Future

class AdditionalAddressControllerSpec extends AmlsSpec with MockitoSugar with BeforeAndAfter with OptionValues {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  before {
    reset(mockDataCacheConnector)
  }

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val auditConnector = mock[AuditConnector]
    val autoCompleteService = mock[AutoCompleteService]
    lazy val view = app.injector.instanceOf[additional_address]
    val additionalAddressController = new AdditionalAddressController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      autoCompleteService = autoCompleteService,
      ds = commonDependencies,
      cc = mockMcc,
      additional_address = view,
      error = errorView
    )

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

    val pageTitle = Messages("responsiblepeople.additional_address.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname"))


    "get is called" must {

      "display the persons page when no existing data in mongoCache" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))


        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(false)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
      }

      "display the additionalAddressPage when existing data in mongoCache for UK address" in new Fixture {

        val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
        val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
        val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePerson(personName, addressHistory = Some(history))

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))


        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(true)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
      }

      "display the additionalAddressPage when existing data in mongoCache for NonUK address" in new Fixture {
        val NonUKAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("Norway", "NW"))
        val additionalAddress = ResponsiblePersonAddress(NonUKAddress, Some(ZeroToFiveMonths))
        val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePerson(personName, addressHistory = Some(history))

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))


        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(false)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(true)
      }

      "display 404 NotFound" when {

        "additionalAddress cannot be found" in new Fixture {

          val responsiblePeople = ResponsiblePerson()

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = additionalAddressController.get(RecordId)(request)
          status(result) must be(NOT_FOUND)
        }

      }
    }

    "post is called" must {
      "respond with BAD_REQUEST" when {
        "isUK field is not supplied" in new Fixture {
          val line1MissingRequest = requestWithUrlEncodedBody()

          val responsiblePeople = ResponsiblePerson()

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#isUK]").html() must include(Messages(s"error.required.uk.or.overseas.address.previous", ""))
        }
      }

      "go to AdditionalAddressUK" when {
        "isUK is true"  in new Fixture {
          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "true")

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId, edit = true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.AdditionalAddressUKController.get(RecordId, true).url))
        }
      }

      "go to AdditionalAddressNonUK page" when {
        "isUk is false" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
          "isUK" -> "false")

          val responsiblePeople = ResponsiblePerson()

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressNonUKController.get(RecordId).url))
        }
      }

      "redirect to AdditionalAddressNonUK and wipe old address" when {
        "changed the answer from yes to no" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "false")

          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val currentAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val additionalAddress = ResponsiblePersonAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress), additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressNonUKController.get(RecordId).url))

          val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])
          verify(additionalAddressController.dataCacheConnector).save[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key), captor.capture())(any(), any())
          captor.getValue.head.isComplete mustBe false
          captor.getValue.head.addressHistory.value.additionalAddress mustBe Some(ResponsiblePersonAddress(PersonAddressNonUK("", "", None, None, Country("", "")), None))
        }
      }

      "redirect to AdditionalAddressUkController and wipe old address" when {
        "changed the answer from no to yes" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "isUK" -> "true")

          val ukAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("", ""))
          val currentAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val additionalAddress = ResponsiblePersonAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress), additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressUKController.get(RecordId).url))

          val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])
          verify(additionalAddressController.dataCacheConnector).save[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key), captor.capture())(any(), any())
          captor.getValue.head.isComplete mustBe false
          captor.getValue.head.addressHistory.value.additionalAddress mustBe Some(ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), None))
        }
      }
    }
  }
}
