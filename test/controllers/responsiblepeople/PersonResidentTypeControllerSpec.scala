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

import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.scalatest.mock.MockitoSugar
import utils.AmlsSpec
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino

import scala.concurrent.Future

class PersonResidentTypeControllerSpec extends AmlsSpec with MockitoSugar with NinoUtil {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new PersonResidentTypeController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)
  val mockCacheMap = mock[CacheMap]

  "PersonResidentTypeController" when {

    "get" must {

      "return OK" when {

        val personName = PersonName("firstname", None, "lastname")
        val nino = nextNino
        val residenceTypeUK = UKResidence(Nino(nino))
        val residenceTypeNonUK = NonUKResidence

        "without pre-populated data" in new Fixture {

          val responsiblePeople = ResponsiblePerson(Some(personName))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("isUKResidence-true").hasAttr("checked") must be(false)
          document.getElementById("isUKResidence-false").hasAttr("checked") must be(false)
          document.select("input[name=nino]").`val` must be("")

        }

        "with pre-populated data (uk)" in new Fixture {
          val responsiblePeople = ResponsiblePerson(
            personName = Some(personName),
            personResidenceType = Some(PersonResidenceType(
              isUKResidence = residenceTypeUK,
              countryOfBirth = Some(Country("United Kingdom", "GB")),
              nationality = Some(Country("United Kingdom", "GB"))))
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=isUKResidence]").`val` must be("true")
          document.select("input[name=nino]").`val` must be(nino)

        }

        "with pre-populated data (non uk)" in new Fixture {
          val responsiblePeople = ResponsiblePerson(
            personName = Some(personName),
            personResidenceType = Some(PersonResidenceType(
              isUKResidence = residenceTypeNonUK,
              countryOfBirth = Some(Country("United Kingdom", "GB")),
              nationality = Some(Country("United Kingdom", "GB"))))
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("isUKResidence-true").hasAttr("checked") must be(false)
          document.getElementById("isUKResidence-false").hasAttr("checked") must be(true)
        }

      }

      "return NOT_FOUND" when {
        "neither RP personName nor residenceType is found" in new Fixture {
          val responsiblePeople = ResponsiblePerson()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(0)(request)

          status(result) must be(NOT_FOUND)
          val document: Document = Jsoup.parse(contentAsString(result))

        }
      }

    }

    "post" must {
      "submit with a valid form" which {

        "goes to CountryOfBirthController" when {
          "uk residence" in new Fixture   {

            val newRequest = request.withFormUrlEncodedBody(
              "isUKResidence" -> "true",
              "nino" -> nextNino,
              "countryOfBirth" -> "GB",
              "nationality" -> "GB"
            )

            val responsiblePeople = ResponsiblePerson(
              personResidenceType = Some(
                PersonResidenceType(
                  UKResidence(Nino(nextNino)),
                  Some(Country("UK", "UK")),
                  None
                )
              )
            )

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(1)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.CountryOfBirthController.get(1).url))
          }
        }

        "goes to PersonUKPassportController" when {
          "non uk residence" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "isUKResidence" -> "false",
              "nino" -> nextNino,
              "countryOfBirth" -> "GB",
              "nationality" -> "GB"
            )

            val responsiblePeople = ResponsiblePerson(
              personResidenceType = Some(
                PersonResidenceType(
                  NonUKResidence,
                  Some(Country("UK", "UK")),
                  None
                )
              )
            )

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(1)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.PersonUKPassportController.get(1).url))
          }

          "in edit mode" when {
            "residence type is changed from uk residence to non uk residence" in new Fixture {

              val newRequest = request.withFormUrlEncodedBody(
                "isUKResidence" -> "false"
              )

              val responsiblePeople = ResponsiblePerson(
                personResidenceType = Some(
                  PersonResidenceType(
                    UKResidence(Nino(nextNino)),
                    Some(Country("UK", "UK")),
                    None
                  )
                )
              )

              when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
                .thenReturn(Some(Seq(responsiblePeople)))

              when(controller.dataCacheConnector.fetchAll(any(), any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))

              val result = controller.post(1, true)(newRequest)
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.PersonUKPassportController.get(1, true).url))
            }
          }
        }

        "goes to DetailedAnswersController" when {
          "in edit mode" when {
            "uk residence" in new Fixture {

              val newRequest = request.withFormUrlEncodedBody(
                "isUKResidence" -> "true",
                "nino" -> nextNino,
                "countryOfBirth" -> "GB",
                "nationality" -> "GB"
              )

              val responsiblePeople = ResponsiblePerson(
                personResidenceType = Some(
                  PersonResidenceType(
                    NonUKResidence,
                    Some(Country("UK", "UK")),
                    None
                  )
                )
              )

              when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
                .thenReturn(Some(Seq(responsiblePeople)))

              when(controller.dataCacheConnector.fetchAll(any(), any()))
                .thenReturn(Future.successful(Some(mockCacheMap)))

              when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))

              val result = controller.post(1, true,Some(flowFromDeclaration))(newRequest)
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
            }
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

          val responsiblePeople = ResponsiblePerson()

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)

          val captor = ArgumentCaptor.forClass(classOf[List[ResponsiblePerson]])
          verify(controller.dataCacheConnector).save(any(), captor.capture())(any(), any(), any())

          captor.getValue must have size 1

          (for {
            person <- captor.getValue.headOption
            residence <- person.personResidenceType
            nino <- residence.isUKResidence match {
              case UKResidence(n) => Some(n)
              case _ => None
            }
          } yield nino.toString) foreach {
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

          val responsiblePeople = ResponsiblePerson()

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)

          val captor = ArgumentCaptor.forClass(classOf[List[ResponsiblePerson]])
          verify(controller.dataCacheConnector).save(any(), captor.capture())(any(), any(), any())

          captor.getValue must have size 1

          (for {
            person <- captor.getValue.headOption
            residence <- person.personResidenceType
            nino <- residence.isUKResidence match {
              case UKResidence(n) => Some(n)
              case _ => None
            }
          } yield nino.toString) foreach {
            _ mustBe testNino
          }

        }

        "removes data from uk passport and no uk passport" when {
          "data is changed from not uk resident to uk resident when edit is true" in new Fixture {

            val nino = nextNino

            val countryCode = "GB"

            val responsiblePeople = ResponsiblePerson(
              personResidenceType = Some(
                PersonResidenceType(
                  NonUKResidence,
                  Some(Country(countryCode, countryCode)),
                  Some(Country(countryCode, countryCode))
                )
              ),
              ukPassport = Some(UKPassportNo),
              nonUKPassport = Some(NonUKPassportYes("22654321"))
            )

            val newRequest = request.withFormUrlEncodedBody(
              "isUKResidence" -> "true",
              "nino" -> nino,
              "countryOfBirth" -> countryCode,
              "nationality" -> countryCode
            )


            val personName = PersonName("firstname", None, "lastname")

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(1, true)(newRequest)
            status(result) must be(SEE_OTHER)

            verify(controller.dataCacheConnector)
              .save[Seq[ResponsiblePerson]](any(), meq(Seq(responsiblePeople.copy(
              personResidenceType = Some(PersonResidenceType(
                UKResidence(Nino(nino)),
                Some(Country(countryCode, countryCode)),
                Some(Country(countryCode, countryCode))
              )),
              ukPassport = None,
              nonUKPassport = None,
              hasChanged = true
            ))))(any(), any(), any())
          }
        }
      }

      "respond with BAD_REQUEST" when {
        "invalid form is submitted" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "ukPassportNumber" -> "12346464688"
          )
          val responsiblePeople = ResponsiblePerson(Some(PersonName("firstname", None, "lastname")))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
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

          val responsiblePeople = ResponsiblePerson()

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(10, false)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }
    }


  }
}
