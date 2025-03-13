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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.address.AdditionalAddressNonUKFormProvider
import models.Country
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.AutoCompleteService
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import services.cache.Cache
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.responsiblepeople.address.AdditionalAddressNonUKView

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class AdditionalAddressNonUKControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId               = 1

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val auditConnector                   = mock[AuditConnector]
    val autoCompleteService              = mock[AutoCompleteService]
    lazy val view                        = inject[AdditionalAddressNonUKView]
    val additionalAddressNonUKController = new AdditionalAddressNonUKController(
      dataCacheConnector = mockDataCacheConnector,
      auditConnector = auditConnector,
      authAction = SuccessfulAuthAction,
      autoCompleteService = autoCompleteService,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[AdditionalAddressNonUKFormProvider],
      view = view,
      error = errorView
    )

    when {
      auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)

    when {
      autoCompleteService.formOptionsExcludeUK
    } thenReturn Seq(
      SelectItem(Some("US"), "United States"),
      SelectItem(Some("ES"), "Spain")
    )
  }

  val emptyCache  = Cache.empty
  val outOfBounds = 99

  "AdditionalAddressNonUKController" when {

    val pageTitle  = messages("responsiblepeople.additional_address_country.title", "firstname lastname") + " - " +
      messages("summary.responsiblepeople") + " - " +
      messages("title.amls") + " - " + messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {

      "display the persons page when no existing data in mongoCache" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(additionalAddressNonUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressNonUKController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title                                    must be(pageTitle)
        document.select("input[name=addressLine1]").`val` must be("")
        document.select("input[name=addressLine2]").`val` must be("")
        document.select("input[name=addressLine3]").`val` must be("")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=country]").`val`      must be("")
      }
      "display the previous home address with non-UK fields populated" in new Fixture {

        val nonUKAddress      = PersonAddressNonUK("Line 1", Some("Line 2"), None, None, Country("Spain", "ES"))
        val additionalAddress = ResponsiblePersonAddress(nonUKAddress, Some(SixToElevenMonths))
        val history           = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

        when(additionalAddressNonUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressNonUKController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=addressLine1]").`val`                              must be("Line 1")
        document.select("input[name=addressLine2]").`val`                              must be("Line 2")
        document.select("input[name=addressLine3]").`val`                              must be("")
        document.select("input[name=addressLine4]").`val`                              must be("")
        document.select("select[name=country] > option[value=ES]").hasAttr("selected") must be(true)
      }

      "display 404 NotFound" when {

        "person cannot be found" in new Fixture {

          val responsiblePeople = ResponsiblePerson()

          when(additionalAddressNonUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = additionalAddressNonUKController.get(RecordId)(request)
          status(result) must be(NOT_FOUND)
        }

      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "all the mandatory non-UK parameters are supplied" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalAddressNonUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line 1",
              "country"      -> "ES"
            )
          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(additionalAddressNonUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressNonUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressNonUKController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "Line 1"
              d.detail("country") mustBe "Spain"
          }
        }

      }
      "respond with BAD_REQUEST" when {

        "there is no country supplied" in new Fixture {

          val requestWithMissingParams = FakeRequest(POST, routes.AdditionalAddressNonUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "",
              "addressLine2" -> "",
              "country"      -> ""
            )

          when(additionalAddressNonUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressNonUKController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLine1]").html()          must include(messages("error.required.address.line1"))
          document.select("a[href=#location-autocomplete]").html() must include(messages("error.required.country"))
        }

        "the country selected is United Kingdom" in new Fixture {

          val requestWithMissingParams = FakeRequest(POST, routes.AdditionalAddressNonUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "",
              "country"      -> "GB"
            )

          val responsiblePeople = ResponsiblePerson(personName = personName)

          when(additionalAddressNonUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(additionalAddressNonUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressNonUKController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLine1]").html()          must include(messages("error.required.address.line1"))
          document.select("a[href=#location-autocomplete]").html() must include(
            messages("error.required.enter.non.uk.previous")
          )
        }

        "given an invalid non uk address" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalAddressNonUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line *1",
              "addressLine2" -> "Line &2",
              "country"      -> "ES"
            )
          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(additionalAddressNonUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressNonUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressNonUKController.post(RecordId)(requestWithParams)
          status(result) must be(BAD_REQUEST)

          val document: Document          = Jsoup.parse(contentAsString(result))
          val errorCount                  = 2
          val elementsWithError: Elements = document.getElementsByClass("govuk-error-message")
          elementsWithError.size() must be(errorCount)
          val elements = elementsWithError.asScala.map(_.text())
          elements(0) must include(messages("error.text.validation.address.line1"))
          elements(1) must include(messages("error.text.validation.address.line2"))
        }

      }

      "go to DetailedAnswers" when {
        "edit is true" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalAddressNonUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "New line 1",
              "addressLine2" -> "New line 2",
              "country"      -> "FR"
            )
          val NonUKAddress      = PersonAddressNonUK("Line 1", Some("Line 2"), None, None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonAddress(NonUKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName, addressHistory = Some(history))

          when(additionalAddressNonUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressNonUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result =
            additionalAddressNonUKController.post(RecordId, edit = true, Some(flowFromDeclaration))(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(
              controllers.responsiblepeople.routes.DetailedAnswersController
                .get(RecordId, Some(flowFromDeclaration))
                .url
            )
          )

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "New line 1"
              d.detail("addressLine2") mustBe "New line 2"
              d.detail("country") mustBe "France"
              d.detail("originalLine1") mustBe "Line 1"
              d.detail("originalLine2") mustBe "Line 2"
              d.detail("originalCountry") mustBe "Spain"
          }
        }
      }

      "go to TimeAtAdditionalAddress page" when {
        "edit is false" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AdditionalAddressNonUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "country"      -> "ES"
            )
          val NonUKAddress      = PersonAddressNonUK("Line 1", Some("Line 2"), None, None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonAddress(NonUKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName, addressHistory = Some(history))

          when(additionalAddressNonUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressNonUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressNonUKController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtAdditionalAddressController.get(RecordId).url))
        }

        "edit is true and time at address does not exist" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.AdditionalAddressNonUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "country"      -> "ES"
            )
          val NonUKAddress      = PersonAddressNonUK("Line 1", Some("Line 2"), None, None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonAddress(NonUKAddress, None)
          val history           = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName, addressHistory = Some(history))

          when(additionalAddressNonUKController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressNonUKController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressNonUKController.post(RecordId, true)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtAdditionalAddressController.get(RecordId, true).url))
        }
      }
    }
  }
}
