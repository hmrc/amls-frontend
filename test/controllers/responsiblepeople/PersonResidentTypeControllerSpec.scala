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

package controllers.responsiblepeople

import cats.data.OptionT
import config.ApplicationConfig
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.PersonResidentTypeFormProvider
import generators.NinoGen
import models.Country
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.responsiblepeople.PersonResidentTypeService
import uk.gov.hmrc.domain.Nino
import services.cache.Cache
import utils.AmlsSpec
import views.html.responsiblepeople.PersonResidenceTypeView

import java.time.LocalDate
import scala.concurrent.Future

class PersonResidentTypeControllerSpec extends AmlsSpec with MockitoSugar with NinoGen with Injecting {

  def nextNino = ninoGen.sample.value.value
  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val mockService = mock[PersonResidentTypeService]

    val mockApplicationConfig = mock[ApplicationConfig]

    val controller = new PersonResidentTypeController(
      messagesApi,
      SuccessfulAuthAction,
      commonDependencies,
      stubMessagesControllerComponents(),
      mockService,
      inject[PersonResidentTypeFormProvider],
      inject[PersonResidenceTypeView],
      errorView
    )
  }

  val emptyCache   = Cache.empty
  val mockCacheMap = mock[Cache]

  "PersonResidentTypeController" when {

    "get" must {

      "return OK" when {

        val personName         = PersonName("firstname", None, "lastname")
        val nino               = nextNino
        val residenceTypeUK    = UKResidence(Nino(nino))
        val residenceTypeNonUK = NonUKResidence

        "without pre-populated data" in new Fixture {

          val responsiblePeople = ResponsiblePerson(Some(personName))

          when(mockService.getResponsiblePerson(any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeople)))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("isUKResidence-true").hasAttr("checked")  must be(false)
          document.getElementById("isUKResidence-false").hasAttr("checked") must be(false)
          document.select("input[name=nino]").`val`                         must be("")

        }

        "with pre-populated data (uk)" in new Fixture {
          val responsiblePeople = ResponsiblePerson(
            personName = Some(personName),
            personResidenceType = Some(
              PersonResidenceType(
                isUKResidence = residenceTypeUK,
                countryOfBirth = Some(Country("United Kingdom", "GB")),
                nationality = Some(Country("United Kingdom", "GB"))
              )
            )
          )

          when(mockService.getResponsiblePerson(any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeople)))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=isUKResidence]").`val` must be("true")
          document.select("input[name=nino]").`val`          must be(nino)

        }

        "with pre-populated data (non uk)" in new Fixture {
          val responsiblePeople = ResponsiblePerson(
            personName = Some(personName),
            personResidenceType = Some(
              PersonResidenceType(
                isUKResidence = residenceTypeNonUK,
                countryOfBirth = Some(Country("United Kingdom", "GB")),
                nationality = Some(Country("United Kingdom", "GB"))
              )
            )
          )

          when(mockService.getResponsiblePerson(any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeople)))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("isUKResidence-true").hasAttr("checked")  must be(false)
          document.getElementById("isUKResidence-false").hasAttr("checked") must be(true)
        }

      }

      "return NOT_FOUND" when {
        "neither RP personName nor residenceType is found" in new Fixture {
          val responsiblePeople = ResponsiblePerson()

          when(mockService.getResponsiblePerson(any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeople)))

          val result = controller.get(0)(request)

          status(result) must be(NOT_FOUND)
        }
      }

    }

    "post" must {
      "submit with a valid form" which {

        "goes to CountryOfBirthController" when {
          "uk residence" in new Fixture {

            val newRequest = FakeRequest(POST, routes.PersonResidentTypeController.post(1).url)
              .withFormUrlEncodedBody(
                "isUKResidence"  -> "true",
                "nino"           -> nextNino,
                "countryOfBirth" -> "GB",
                "nationality"    -> "GB"
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
            when(mockService.getCache(any(), any(), any()))
              .thenReturn(OptionT[Future, Cache](Future.successful(Some(mockCacheMap))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            val result = controller.post(1)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.CountryOfBirthController.get(1).url)
            )
          }
        }

        "goes to PersonUKPassportController" when {
          "non uk residence" in new Fixture {

            val newRequest = FakeRequest(POST, routes.PersonResidentTypeController.post(1).url)
              .withFormUrlEncodedBody(
                "isUKResidence"  -> "false",
                "nino"           -> nextNino,
                "countryOfBirth" -> "GB",
                "nationality"    -> "GB"
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

            when(mockService.getCache(any(), any(), any()))
              .thenReturn(OptionT[Future, Cache](Future.successful(Some(mockCacheMap))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            val result = controller.post(1)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.PersonUKPassportController.get(1).url)
            )
          }

          "in edit mode" when {
            "residence type is changed from uk residence to non uk residence" in new Fixture {

              val newRequest = FakeRequest(POST, routes.PersonResidentTypeController.post(1).url)
                .withFormUrlEncodedBody(
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

              when(mockService.getCache(any(), any(), any()))
                .thenReturn(OptionT[Future, Cache](Future.successful(Some(mockCacheMap))))

              when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
                .thenReturn(Some(Seq(responsiblePeople)))

              val result = controller.post(1, true)(newRequest)
              status(result)           must be(SEE_OTHER)
              redirectLocation(result) must be(
                Some(controllers.responsiblepeople.routes.PersonUKPassportController.get(1, true).url)
              )
            }
          }
        }

        "goes to DetailedAnswersController" when {
          "in edit mode" when {
            "uk residence" in new Fixture {

              val newRequest = FakeRequest(POST, routes.PersonResidentTypeController.post(1).url)
                .withFormUrlEncodedBody(
                  "isUKResidence"  -> "true",
                  "nino"           -> nextNino,
                  "countryOfBirth" -> "GB",
                  "nationality"    -> "GB"
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

              when(mockService.getCache(any(), any(), any()))
                .thenReturn(OptionT[Future, Cache](Future.successful(Some(mockCacheMap))))

              when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
                .thenReturn(Some(Seq(responsiblePeople)))

              val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
              status(result)           must be(SEE_OTHER)
              redirectLocation(result) must be(
                Some(
                  controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url
                )
              )
            }
          }
        }

        "accept the NINO to uppercase" in new Fixture {

          val testNino = nextNino

          val newRequest = FakeRequest(POST, routes.PersonResidentTypeController.post(1).url)
            .withFormUrlEncodedBody(
              "isUKResidence"  -> "true",
              "nino"           -> testNino.toLowerCase,
              "countryOfBirth" -> "GB",
              "nationality"    -> "GB"
            )

          val responsiblePeople = ResponsiblePerson()

          when(mockService.getCache(any(), any(), any()))
            .thenReturn(OptionT[Future, Cache](Future.successful(Some(mockCacheMap))))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
        }

        "accept spaces and dashes" in new Fixture {

          val testNino   = nextNino
          val spacedNino = testNino.grouped(2).mkString(" ")
          val withDashes =
            spacedNino.substring(0, 8) + "-" + spacedNino.substring(8, spacedNino.length) // ## ## ##- ## #

          val newRequest = FakeRequest(POST, routes.PersonResidentTypeController.post(1).url)
            .withFormUrlEncodedBody(
              "isUKResidence"  -> "true",
              "nino"           -> withDashes,
              "countryOfBirth" -> "GB",
              "nationality"    -> "GB"
            )

          val responsiblePeople = ResponsiblePerson()

          when(mockService.getCache(any(), any(), any()))
            .thenReturn(OptionT[Future, Cache](Future.successful(Some(mockCacheMap))))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
        }

        "removes data from uk passport and no uk passport excluding date of birth" when {
          "data is changed from not uk resident to uk resident and edit is true" in new Fixture {

            val nino = nextNino

            val countryCode = "GB"

            val dateOfBirth = DateOfBirth(LocalDate.of(2000, 1, 1))

            val responsiblePeople = ResponsiblePerson(
              personResidenceType = Some(
                PersonResidenceType(
                  NonUKResidence,
                  Some(Country(countryCode, countryCode)),
                  Some(Country(countryCode, countryCode))
                )
              ),
              ukPassport = Some(UKPassportNo),
              nonUKPassport = Some(NonUKPassportYes("22654321")),
              dateOfBirth = Some(dateOfBirth)
            )

            val newRequest = FakeRequest(POST, routes.PersonResidentTypeController.post(1).url)
              .withFormUrlEncodedBody(
                "isUKResidence"  -> "true",
                "nino"           -> nino,
                "countryOfBirth" -> countryCode,
                "nationality"    -> countryCode
              )

            when(mockService.getCache(any(), any(), any()))
              .thenReturn(OptionT[Future, Cache](Future.successful(Some(mockCacheMap))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            val result = controller.post(1, true)(newRequest)
            status(result) must be(SEE_OTHER)
          }
        }
      }

      "respond with BAD_REQUEST" when {
        "invalid form is submitted" in new Fixture {

          val newRequest        = FakeRequest(POST, routes.PersonResidentTypeController.post(1).url)
            .withFormUrlEncodedBody(
              "ukPassportNumber" -> "12346464688"
            )
          val responsiblePeople = ResponsiblePerson(Some(PersonName("firstname", None, "lastname")))

          when(mockService.getResponsiblePerson(any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeople)))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "return NOT_FOUND" when {
        "index is out of bounds" in new Fixture {

          val newRequest = FakeRequest(POST, routes.PersonResidentTypeController.post(1).url)
            .withFormUrlEncodedBody(
              "isUKResidence"  -> "true",
              "nino"           -> nextNino,
              "countryOfBirth" -> "GB",
              "nationality"    -> "GB"
            )

          val responsiblePeople = ResponsiblePerson()

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(mockService.getCache(any(), any(), any()))
            .thenReturn(OptionT[Future, Cache](Future.successful(Some(mockCacheMap))))

          val result = controller.post(10, false)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }
    }
  }
}
