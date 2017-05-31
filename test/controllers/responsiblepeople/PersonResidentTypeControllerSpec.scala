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

import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import play.api.test.Helpers._

import scala.concurrent.Future

class PersonResidentTypeControllerSpec extends GenericTestHelper with MockitoSugar with NinoUtil {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new PersonResidentTypeController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "PersonResidentTypeController" when {

    "get" must {

      "return OK" when {

        val personName = PersonName("firstname", None, "lastname", None, None)
        val nino = nextNino
        val residenceTypeUK = UKResidence(nino)
        val residenceTypeNonUK = NonUKResidence

        "without pre-populated data" in new Fixture {
          val responsiblePeople = ResponsiblePeople(Some(personName))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("isUKResidence-true").hasAttr("checked") must be(false)
          document.getElementById("isUKResidence-false").hasAttr("checked") must be(false)
          document.select("input[name=nino]").`val` must be("")
          document.select("input[name=countryOfBirth]").`val` must be("")
          document.getElementById("passportType-01").hasAttr("checked") must be(false)
          document.getElementById("passportType-02").hasAttr("checked") must be(false)
          document.getElementById("passportType-03").hasAttr("checked") must be(false)
          document.select("input[name=ukPassportNumber]").`val` must be("")
          document.select("input[name=nonUKPassportNumber]").`val` must be("")

        }

        "with pre-populated data (uk)" in new Fixture {
          val responsiblePeople = ResponsiblePeople(
            personName = Some(personName),
            personResidenceType = Some(PersonResidenceType(
              isUKResidence = residenceTypeUK,
              countryOfBirth = Country("United Kingdom", "GB"),
              nationality = Some(Country("United Kingdom", "GB"))))
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=isUKResidence]").`val` must be("true")
          document.select("input[name=nino]").`val` must be(nino)
          document.select("select[name=countryOfBirth] > option[value=GB]").hasAttr("selected") must be(true)

        }

        "with pre-populated data (non uk)" in new Fixture {
          val responsiblePeople = ResponsiblePeople(
            personName = Some(personName),
            personResidenceType = Some(PersonResidenceType(
              isUKResidence = residenceTypeNonUK,
              countryOfBirth = Country("United Kingdom", "GB"),
              nationality = Some(Country("United Kingdom", "GB"))))
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("isUKResidence-true").hasAttr("checked") must be(false)
          document.getElementById("isUKResidence-false").hasAttr("checked") must be(true)
          document.select("select[name=countryOfBirth] > option[value=GB]").hasAttr("selected") must be(true)

        }

      }

      "return NOT_FOUND" when {
        "neither RP personName nor residenceType is found" in new Fixture {
          val responsiblePeople = ResponsiblePeople()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(0)(request)

          status(result) must be(NOT_FOUND)
          val document: Document = Jsoup.parse(contentAsString(result))

        }
      }

    }

    "post" must {
      "submit with a valid form" which {
        "goes to NationalityController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isUKResidence" -> "true",
            "nino" -> nextNino,
            "countryOfBirth" -> "GB",
            "nationality" -> "GB"
          )

          val responsiblePeople = ResponsiblePeople()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.NationalityController.get(1).url))
        }
        "goes to DetailedAnswersController" when {
          "in edit mode" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "isUKResidence" -> "false",
              "dateOfBirth.day" -> "24",
              "dateOfBirth.month" -> "2",
              "dateOfBirth.year" -> "1990",
              "passportType" -> "03",
              "nino" -> nextNino,
              "countryOfBirth" -> "GB",
              "nationality" -> "GB"
            )

            val responsiblePeople = ResponsiblePeople()

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(1, true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url))
          }
        }

        "transforms the NINO to uppercase" in new Fixture {

          val testNino = nextNino

          val newRequest = request.withFormUrlEncodedBody(
            "isUKResidence" -> "true",
            "nino" -> testNino,
            "countryOfBirth" -> "GB",
            "nationality" -> "GB"
          )

          val responsiblePeople = ResponsiblePeople()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)

          val captor = ArgumentCaptor.forClass(classOf[List[ResponsiblePeople]])
          verify(controller.dataCacheConnector).save(any(), captor.capture())(any(), any(), any())

          captor.getValue must have size 1

          (for {
            person <- captor.getValue.headOption
            residence <- person.personResidenceType
            nino <- residence.isUKResidence match {
              case UKResidence(n) => Some(n)
              case _ => None
            }
          } yield nino) foreach {
            _ mustBe testNino
          }

        }

        "remove spaces and dashes" in new Fixture {

          val testNino = nextNino
          val spacedNino = testNino.grouped(2).mkString(" ")
          val withDashes = spacedNino.substring(0, 8) + "-" + spacedNino.substring(8, spacedNino.length) // ## ## ##- ## #

          val newRequest = request.withFormUrlEncodedBody(
            "isUKResidence" -> "true",
            "nino" -> withDashes,
            "countryOfBirth" -> "GB",
            "nationality" -> "GB"
          )

          val responsiblePeople = ResponsiblePeople()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)

          val captor = ArgumentCaptor.forClass(classOf[List[ResponsiblePeople]])
          verify(controller.dataCacheConnector).save(any(), captor.capture())(any(), any(), any())

          captor.getValue must have size 1

          (for {
            person <- captor.getValue.headOption
            residence <- person.personResidenceType
            nino <- residence.isUKResidence match {
              case UKResidence(n) => Some(n)
              case _ => None
            }
          } yield nino) foreach {
            _ mustBe testNino
          }

        }
      }

      "respond with BAD_REQUEST" when {
        "invalid form is submitted" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "ukPassportNumber" -> "12346464688"
          )
          val responsiblePeople = ResponsiblePeople(Some(PersonName("firstname", None, "lastname", None, None)))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "return NOT_FOUND" when {
        "index is out of bounds" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isUKResidence" -> "true",
            "nino" -> nextNino,
            "countryOfBirth" -> "GB",
            "nationality" -> "GB"
          )

          val responsiblePeople = ResponsiblePeople()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(10, false)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }
    }


  }
}
