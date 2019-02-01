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
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import services.AutoCompleteService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class NationalityControllerSpec extends AmlsSpec with MockitoSugar with NinoUtil {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val autoCompleteService = mock[AutoCompleteService]

    val controller = new NationalityController (
      dataCacheConnector = mock[DataCacheConnector],
      authConnector = self.authConnector,
      autoCompleteService = autoCompleteService
    )

    when {
      controller.autoCompleteService.getCountries
    } thenReturn Some(Seq(
      NameValuePair("Country 1", "country:1"),
      NameValuePair("Country 2", "country:2")
    ))
  }

  val emptyCache = CacheMap("", Map.empty)

  "NationalityController" must {

    val personName = Some(PersonName("firstname", None, "lastname"))

    "load nationality page" in new Fixture {

      val responsiblePeople = ResponsiblePerson(personName)

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      contentAsString(result) must include(Messages("responsiblepeople.nationality.title"))

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("input[type=radio][name=nationality][value=01]").hasAttr("checked") must be(false)
      document.select("input[type=radio][name=nationality][value=02]").hasAttr("checked") must be(false)
    }

    "load Not found page" when {
      "get throws not found exception" in new Fixture {

        val responsiblePeople = ResponsiblePerson()

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controller.get(3)(request)

        status(result) must be(NOT_FOUND)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title mustBe s"${Messages("error.not-found.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

      }
    }

    "load nationality page when nationality is none" in new Fixture {

      val pResidenceType = PersonResidenceType(UKResidence(Nino(nextNino)), Some(Country("United Kingdom", "GB")), None)
      val responsiblePeople = ResponsiblePerson(personName, personResidenceType = Some(pResidenceType))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      contentAsString(result) must include(Messages("responsiblepeople.nationality.title"))

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("input[type=radio][name=nationality][value=01]").hasAttr("checked") must be(false)
      document.select("input[type=radio][name=nationality][value=02]").hasAttr("checked") must be(false)
    }

    "pre-populate UI with data from sav4later" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(
          personName,
          personResidenceType = Some(PersonResidenceType(
            NonUKResidence,
            Some(Country("United Kingdom", "GB")),
            Some(Country("France", "FR"))))
        )))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("input[type=radio][name=nationality][value=01]").hasAttr("checked") must be(false)
    }

    "fail submission on error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName)))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#nationality]").html() must include(Messages("error.required.nationality"))
    }

    "submit with valid nationality data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "nationality" -> "01"
      )

      val responsiblePeople = ResponsiblePerson()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ContactDetailsController.get(1).url))
    }

    "submit with valid nationality data (with other country)" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "nationality" -> "02",
        "otherCountry" -> "GB"
      )

      val responsiblePeople = ResponsiblePerson()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ContactDetailsController.get(1).url))
    }

    "submit with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "nationality" -> "02",
        "otherCountry" -> "GB"
      )

      val pResidenceType = PersonResidenceType(UKResidence(Nino(nextNino)), Some(Country("United Kingdom", "GB")), None)
      val responsiblePeople = ResponsiblePerson(None, personResidenceType = Some(pResidenceType))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      val prt = pResidenceType.copy(nationality = Some(Country("France", "FR")))
      val responsiblePeople1 = ResponsiblePerson(None, personResidenceType = Some(pResidenceType))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), meq(Seq(responsiblePeople1)))(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
    }

    "load NotFound page on exception" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "nationality" -> "01"
      )

      val responsiblePeople = ResponsiblePerson()

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

      when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(10, true)(newRequest)
      status(result) must be(NOT_FOUND)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title mustBe s"${Messages("error.not-found.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
    }
  }
}
