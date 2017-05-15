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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeApplication

import scala.collection.JavaConversions._
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class TimeAtCurrentAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val timeAtAddressController = new TimeAtCurrentAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]

    }
  }
  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true))

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "TimeAtAddressController" when {

    val personName = Some(PersonName("firstname", None, "lastname", None, None))

    "get is called" must {

      "display the page" when {

       "with existing data" in new Fixture {

         val personName = Some(PersonName("firstname", None, "lastname", None, None))

         val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
         val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, Some(ZeroToFiveMonths))
         val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
         val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

          when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAddressController.get(RecordId)(request)
          status(result) must be(OK)

         val document: Document = Jsoup.parse(contentAsString(result))

         document.select("input[type=radio][name=timeAtAddress][value=01]").hasAttr("checked") must be(true)
         document.select("input[type=radio][name=timeAtAddress][value=02]").hasAttr("checked") must be(false)
         document.select("input[type=radio][name=timeAtAddress][value=03]").hasAttr("checked") must be(false)
         document.select("input[type=radio][name=timeAtAddress][value=04]").hasAttr("checked") must be(false)
        }

        "timeAtAddress does not exist" in new Fixture {

          val responsiblePeople = ResponsiblePeople(personName)

          when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAddressController.get(RecordId)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=timeAtAddress][value=01]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=02]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=03]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=04]").hasAttr("checked") must be(false)
        }


      }

      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {

        val responsiblePeople = ResponsiblePeople()

        when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = timeAtAddressController.get(outOfBounds)(request)
        status(result) must be(NOT_FOUND)
      }

    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "a time at address has been selected" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> "04"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = timeAtAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
        }
      }

      "respond with BAD_REQUEST" when {
        "no time has been selected" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> ""
          )

          when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = timeAtAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with NOT_FOUND" when {
        "given an out of bounds index" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> "01"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = timeAtAddressController.post(outOfBounds, true)(requestWithParams)

          status(result) must be(NOT_FOUND)
        }
      }

      "when the service status is not yet submitted" when {

        "edit mode is on" when {
          "time at address is less than 1 year" must {
            "redirect to the AdditionalAddressController" when {
              "there is no additional address already saved" in new Fixture {

                val requestWithParams = request.withFormUrlEncodedBody(
                  "timeAtAddress" -> "01"
                )
                val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
                val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
                val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
                val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

                when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
                when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionReadyForReview))

                val result = timeAtAddressController.post(RecordId, true)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(RecordId, true).url))
              }
            }
            "redirect to the DetailedAnswersController" when {
              "there is an additional address already saved" in new Fixture {

                val requestWithParams = request.withFormUrlEncodedBody(
                  "timeAtAddress" -> "01"
                )
                val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
                val currentAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
                val additionalAddress = ResponsiblePersonAddress(ukAddress, Some(ZeroToFiveMonths))
                val history = ResponsiblePersonAddressHistory(
                  currentAddress = Some(currentAddress),
                  additionalAddress = Some(additionalAddress)
                )
                val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

                when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
                when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionReadyForReview))

                val result = timeAtAddressController.post(RecordId, true)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, true).url))

              }
            }
          }
          "time at address is OneToThreeYears" must {
            "redirect to the correct location" in new Fixture {

              val requestWithParams = request.withFormUrlEncodedBody(
                "timeAtAddress" -> "03"
              )
              val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
              val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
              val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
              val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

              when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))
              when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = timeAtAddressController.post(RecordId, true)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, true).url))
            }
          }
          "time at address is ThreeYearsPlus" must {
            "redirect to the correct location" in new Fixture {

              val requestWithParams = request.withFormUrlEncodedBody(
                "timeAtAddress" -> "04"
              )
              val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
              val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
              val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
              val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

              when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))

              when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = timeAtAddressController.post(RecordId, true)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, true).url))
            }
          }
        }

        "when edit mode is off" when {

          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          "time at address is less than 1 year" must {
            "redirect to the correct location" in new Fixture {

              val requestWithParams = request.withFormUrlEncodedBody(
                "timeAtAddress" -> "01"
              )

              when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))
              when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = timeAtAddressController.post(RecordId)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(RecordId).url))
            }
          }
          "time at address is OneToThreeYears" must {
            "redirect to the correct location" in new Fixture {

              val requestWithParams = request.withFormUrlEncodedBody(
                "timeAtAddress" -> "03"
              )

              when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))
              when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = timeAtAddressController.post(RecordId)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(RecordId).url))
            }
          }
          "time at address is ThreeYearsPlus" must {
            "redirect to the correct location" in new Fixture {

              val requestWithParams = request.withFormUrlEncodedBody(
                "timeAtAddress" -> "04"
              )

              when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))
              when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = timeAtAddressController.post(RecordId)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(RecordId).url))
            }
          }
        }
      }

      "when the service status is approved" when {
        "the responsible person has previously been submitted and therefore has a lineID" when {
          "editing an existing address and the address has not changed" when {
            "time at address is less than 1 year" must {
              "redirect to the additional address controller" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "timeAtAddress" -> "01"
                )

                val originalResponsiblePeople = ResponsiblePeople(
                  addressHistory = Some(ResponsiblePersonAddressHistory(
                    currentAddress = Some(
                      ResponsiblePersonCurrentAddress(PersonAddressUK("line1", "line2", None, None, "AB1 2CD"), Some(ZeroToFiveMonths), None)
                    )
                  )),
                  lineId = Some(1)
                )

                when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = timeAtAddressController.post(RecordId, true)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(1, true).url))
              }
            }
            "time at address is more than 1 year" must {
              "redirect to the detailed answers controller" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "timeAtAddress" -> "03"
                )

                val originalResponsiblePeople = ResponsiblePeople(
                  addressHistory = Some(ResponsiblePersonAddressHistory(
                    currentAddress = Some(ResponsiblePersonCurrentAddress(PersonAddressUK("line1", "line2", None, None, "AB1 2CD"), Some(OneToThreeYears), None)
                    )
                  )),
                  lineId = Some(1)
                )

                when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = timeAtAddressController.post(RecordId, true)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1, true).url))
              }
            }
          }
        }
        "the responsible person has not previously been submitted and therefore has not got a lineID" when {
          "adding a new responsible person (therefore edit is false)" when {

            val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
            val originalResponsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            "time at address is less than 1 year" must {
              "redirect to the additional address controller" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "timeAtAddress" -> "01"
                )

                when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = timeAtAddressController.post(RecordId, false)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(1, false).url))
              }
            }
            "time at address is more than 1 year" must {
              "redirect to the PositionWithinBusinessController" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "timeAtAddress" -> "03"
                )

                when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = timeAtAddressController.post(RecordId, false)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(1, false).url))
              }
            }
          }
          "editing a previously added responsible person (therefore edit is true)" when {
            "time at address is less than 1 year" must {
              "redirect to the additional address controller" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "timeAtAddress" -> "01"
                )

                val originalResponsiblePeople = ResponsiblePeople(
                  addressHistory = Some(
                    ResponsiblePersonAddressHistory(
                      currentAddress = Some(
                        ResponsiblePersonCurrentAddress(PersonAddressUK("line1", "line2", None, None, "AB1 2CD"), Some(ZeroToFiveMonths), None)
                      )
                    )),
                  lineId = None
                )

                when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = timeAtAddressController.post(RecordId, true)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(1, true).url))
              }
            }
            "time at address is more than 1 year" must {
              "redirect to the DetailedAnswersController" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "timeAtAddress" -> "03"
                )

                val originalResponsiblePeople = ResponsiblePeople(
                  addressHistory = Some(ResponsiblePersonAddressHistory(
                    currentAddress = Some(ResponsiblePersonCurrentAddress(PersonAddressUK("line1", "line2", None, None, "AB1 2CD"), Some(OneToThreeYears), None)
                    )
                  )),
                  lineId = None
                )

                when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(timeAtAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = timeAtAddressController.post(RecordId, true)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1, true).url))
              }
            }
          }
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


