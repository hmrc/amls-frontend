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

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.RemoveResponsiblePersonFormProvider
import generators.ResponsiblePersonGenerator
import models.Country
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import models.status._
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import uk.gov.hmrc.domain.Nino
import services.cache.Cache
import utils.{AmlsSpec, StatusConstants}
import views.html.responsiblepeople.RemoveResponsiblePersonView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class RemoveResponsiblePersonControllerSpec
    extends AmlsSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with ScalaCheckPropertyChecks
    with NinoUtil
    with ResponsiblePersonGenerator
    with Injecting {

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[RemoveResponsiblePersonView]
    val controller = new RemoveResponsiblePersonController(
      dataCacheConnector = mock[DataCacheConnector],
      statusService = mock[StatusService],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[RemoveResponsiblePersonFormProvider],
      view = view,
      error = errorView
    )
  }

  "RemoveResponsiblePersonController" when {
    "get is called" when {
      "the submission status is NotCompleted" must {
        "respond with OK when the index is valid" in new Fixture {

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(NotCompleted))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(
              Future.successful(Some(Seq(ResponsiblePerson(Some(PersonName("firstName", None, "lastName"))))))
            )

          val result = controller.get(1)(request)

          status(result) must be(OK)

        }
        "respond with NOT_FOUND when the index is out of bounds" in new Fixture {

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(NotCompleted))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

          val result = controller.get(100)(request)

          status(result) must be(NOT_FOUND)

        }
      }

      "the submission status is Renewal amendment" must {
        "respond with OK when the index is valid" in new Fixture {

          val p = mock[ResponsiblePerson]
          when(p.isComplete).thenReturn(true)
          when(p.personName).thenReturn(Some(PersonName("firstName", None, "lastName")))
          when(p.lineId).thenReturn(Some(4444))

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(RenewalSubmitted(None)))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(p))))

          val result = controller.get(1)(request)

          status(result) must be(OK)
          val contentString = contentAsString(result)
          val doc           = Jsoup.parse(contentString)
          doc.getElementsMatchingOwnText(messages("lbl.day")).hasText must be(true)

        }
      }

      "the submission status is Renewal"                    must {
        "respond with OK when the index is valid" in new Fixture {

          val p = mock[ResponsiblePerson]
          when(p.isComplete).thenReturn(true)
          when(p.personName).thenReturn(Some(PersonName("firstName", None, "lastName")))
          when(p.lineId).thenReturn(Some(4444))

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(ReadyForRenewal(None)))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(p))))

          val result = controller.get(1)(request)

          status(result) must be(OK)
          val contentString = contentAsString(result)
          val doc           = Jsoup.parse(contentString)
          doc.getElementsMatchingOwnText(messages("lbl.day")).hasText must be(true)

        }
      }
      "the submission status is SubmissionDecisionApproved" must {
        "respond with OK when the index is valid" in new Fixture {

          val p = mock[ResponsiblePerson]
          when(p.isComplete).thenReturn(true)
          when(p.personName).thenReturn(Some(PersonName("firstName", None, "lastName")))
          when(p.lineId).thenReturn(Some(4444))

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(p))))

          val result = controller.get(1)(request)

          status(result) must be(OK)
          val contentString = contentAsString(result)
          val doc           = Jsoup.parse(contentString)
          doc.getElementsMatchingOwnText(messages("lbl.day")).hasText must be(true)

        }
        "respond with NOT_FOUND when the index is out of bounds" in new Fixture {

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

          val result = controller.get(100)(request)

          status(result) must be(NOT_FOUND)

        }
        "respond with OK without showing endDate form when RP does not have lineId" in new Fixture {

          val rp = ResponsiblePerson(
            Some(PersonName("firstName", None, "lastName"))
          )

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(rp))))

          val result = controller.get(1)(request)

          status(result) must be(OK)

          contentAsString(result) must not include messages("responsiblepeople.remove.responsible.person.enddate.lbl")
        }
      }
      "the submission status is SubmissionReadyForReview"   must {
        "respond with OK without showing endDate form when RP does not have lineId" in new Fixture {

          val rp = ResponsiblePerson(
            Some(PersonName("firstName", None, "lastName"))
          )

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(rp))))

          val result = controller.get(1)(request)

          status(result) must be(OK)

          contentAsString(result) must not include messages("responsiblepeople.remove.responsible.person.enddate.lbl")

        }

        "respond with OK without showing endDate form when RP does have lineId" in new Fixture {

          val p = mock[ResponsiblePerson]
          when(p.isComplete).thenReturn(true)
          when(p.personName).thenReturn(Some(PersonName("firstName", None, "lastName")))
          when(p.lineId).thenReturn(Some(4444))

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(p))))

          val result = controller.get(1)(request)

          status(result) must be(OK)

          contentAsString(result) must not include messages("responsiblepeople.remove.responsible.person.enddate.lbl")

        }

        "redirect to start of RP flow where RP is not completed and has a lineId" in new Fixture {

          val p = mock[ResponsiblePerson]
          when(p.isComplete).thenReturn(false)
          when(p.personName).thenReturn(Some(PersonName("firstName", None, "lastName")))
          when(p.lineId).thenReturn(Some(4444))

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(p))))

          val result = controller.get(1)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.WhatYouNeedController.get(1).url))
        }
      }
    }

    "remove is called" must {
      "respond with SEE_OTHER" when {
        "removing a responsible person from an application with status NotCompleted" in new Fixture {

          val emptyCache = Cache.empty

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(NotCompleted))

          val result = controller.remove(1)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url)
          )

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](
            any(),
            any(),
            meq(
              Seq(
                CompleteResponsiblePeople2,
                CompleteResponsiblePeople3
              )
            )
          )(any())
        }

        "removing a responsible person from an application with status SubmissionReady" in new Fixture {

          val emptyCache = Cache.empty

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.remove(1)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url)
          )

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](
            any(),
            any(),
            meq(
              Seq(
                CompleteResponsiblePeople2,
                CompleteResponsiblePeople3
              )
            )
          )(any())
        }

        "removing a responsible person from an application with status SubmissionReady and redirect to your answers page" in new Fixture {

          val emptyCache = Cache.empty

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.remove(1)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url)
          )

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](
            any(),
            any(),
            meq(
              Seq(
                CompleteResponsiblePeople2,
                CompleteResponsiblePeople3
              )
            )
          )(any())
        }

        "removing a responsible person with lineId from an application with status SubmissionReadyForReview" in new Fixture {

          val emptyCache = Cache.empty

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(
              Future.successful(
                Some(Seq(CompleteResponsiblePeople1, CompleteResponsiblePeople2, CompleteResponsiblePeople3))
              )
            )
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = controller.remove(1)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url)
          )

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](
            any(),
            any(),
            meq(
              Seq(
                CompleteResponsiblePeople1.copy(status = Some(StatusConstants.Deleted), hasChanged = true),
                CompleteResponsiblePeople2,
                CompleteResponsiblePeople3
              )
            )
          )(any())
        }

        "removing a responsible person without lineId from an application with status SubmissionReadyForReview" in new Fixture {

          val emptyCache = Cache.empty

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(
              Future.successful(
                Some(
                  Seq(
                    CompleteResponsiblePeople1.copy(lineId = None),
                    CompleteResponsiblePeople2,
                    CompleteResponsiblePeople3
                  )
                )
              )
            )
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = controller.remove(1)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url)
          )

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](
            any(),
            any(),
            meq(
              Seq(
                CompleteResponsiblePeople2,
                CompleteResponsiblePeople3
              )
            )
          )(any())
        }

        "removing a responsible person from an application with status SubmissionDecisionApproved" in new Fixture {

          val emptyCache = Cache.empty
          val newRequest = FakeRequest(POST, routes.RemoveResponsiblePersonController.remove(1).url)
            .withFormUrlEncodedBody(
              "dateRequired"  -> "true",
              "endDate.day"   -> "1",
              "endDate.month" -> "1",
              "endDate.year"  -> "2006"
            )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url)
          )

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](
            any(),
            any(),
            meq(
              Seq(
                CompleteResponsiblePeople1.copy(
                  status = Some(StatusConstants.Deleted),
                  hasChanged = true,
                  endDate = Some(ResponsiblePersonEndDate(LocalDate.of(2006, 1, 1)))
                ),
                CompleteResponsiblePeople2,
                CompleteResponsiblePeople3
              )
            )
          )(any())
        }

        "removing a new incomplete responsible person from an application with status SubmissionDecisionApproved" in new Fixture {

          val emptyCache = Cache.empty
          val newRequest = FakeRequest(POST, routes.RemoveResponsiblePersonController.remove(1).url)
            .withFormUrlEncodedBody("dateRequired" -> "false")

          val people = Seq(
            responsiblePersonGen.sample.get
              .copy(lineId = None, positions = Some(positionsGen.sample.get.copy(startDate = None)))
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(people)))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1)(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url)
          )

          val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])
          verify(controller.dataCacheConnector)
            .save[Seq[ResponsiblePerson]](any(), meq(ResponsiblePerson.key), captor.capture())(any())

          captor.getValue mustBe Seq.empty[ResponsiblePerson]
        }

        "removing a responsible person from an application with no date" in new Fixture {
          val emptyCache = Cache.empty

          val newRequest = FakeRequest(POST, routes.RemoveResponsiblePersonController.remove(1).url)
            .withFormUrlEncodedBody(
              "dateRequired" -> "false"
            )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(CompleteResponsiblePeople1.copy(lineId = None)))))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1)(newRequest)
          status(result) must be(SEE_OTHER)

        }

      }

      "respond with BAD_REQUEST" when {
        "removing a responsible person from an application with no date" in new Fixture {
          val emptyCache = Cache.empty

          val newRequest = FakeRequest(POST, routes.RemoveResponsiblePersonController.remove(1).url)
            .withFormUrlEncodedBody(
              "dateRequired"  -> "true",
              "endDate.day"   -> "",
              "endDate.month" -> "",
              "endDate.year"  -> ""
            )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1)(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(messages("error.required.rp.all"))

        }

        s"removing a responsible person from an application given a year which is below ${RemoveResponsiblePersonFormProvider.minDate
            .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}" in new Fixture {
          val emptyCache = Cache.empty

          val belowMinDate = RemoveResponsiblePersonFormProvider.minDate.minusDays(1)

          val newRequest = FakeRequest(POST, routes.RemoveResponsiblePersonController.remove(1).url)
            .withFormUrlEncodedBody(
              "dateRequired"  -> "true",
              "endDate.day"   -> belowMinDate.getDayOfMonth.toString,
              "endDate.month" -> belowMinDate.getMonthValue.toString,
              "endDate.year"  -> belowMinDate.getYear.toString
            )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1)(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(messages("error.invalid.year.post1900"))
        }

        "removing a rp from an application with future date" in new Fixture {
          val emptyCache = Cache.empty

          val futureDate = LocalDate.now().plusDays(1)

          val newRequest = FakeRequest(POST, routes.RemoveResponsiblePersonController.remove(1).url)
            .withFormUrlEncodedBody(
              "dateRequired"  -> "true",
              "endDate.day"   -> futureDate.getDayOfMonth.toString,
              "endDate.month" -> futureDate.getMonthValue.toString,
              "endDate.year"  -> futureDate.getYear.toString
            )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1)(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(messages("error.invalid.rp.date.future"))

        }

        "removing a responsible person from an application with end date before position start date" in new Fixture {

          val emptyCache = Cache.empty

          val startDate = LocalDate.of(1999, 5, 1)

          val position   = Positions(Set(InternalAccountant), Some(PositionStartDate(startDate)))
          val peopleList = Seq(CompleteResponsiblePeople1.copy(positions = Some(position)))

          val newRequest = FakeRequest(POST, routes.RemoveResponsiblePersonController.remove(1).url)
            .withFormUrlEncodedBody(
              "dateRequired"  -> "true",
              "endDate.day"   -> "15",
              "endDate.month" -> "1",
              "endDate.year"  -> "1998"
            )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(peopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1)(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(
            messages(
              "error.expected.rp.date.after.start",
              personName.titleName,
              startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            )
          )
        }
      }
    }
  }

  private val residence               = UKResidence(Nino(nextNino))
  private val residenceCountry        = Country("United Kingdom", "GB")
  private val residenceNationality    = Country("United Kingdom", "GB")
  private val currentPersonAddress    = PersonAddressUK("Line 1", Some("Line 2"), None, None, "AA111AA")
  private val currentAddress          = ResponsiblePersonCurrentAddress(currentPersonAddress, Some(ZeroToFiveMonths))
  private val additionalPersonAddress = PersonAddressUK("Line 1", Some("Line 2"), None, None, "AA11AA")
  private val additionalAddress       = ResponsiblePersonAddress(additionalPersonAddress, Some(ZeroToFiveMonths))
  // scalastyle:off magic.number
  val personName                      = PersonName("firstName", Some("middleName"), "lastName")
  val legalName                       = PreviousName(Some(true), Some("firstName"), Some("middleName"), Some("lastName"))
  val legalNameChangeDate             = LocalDate.of(1990, 2, 24)
  val knownBy                         = KnownBy(Some(true), Some("knownByName"))
  val personResidenceType             = PersonResidenceType(residence, Some(residenceCountry), Some(residenceNationality))
  val saRegistered                    = SaRegisteredYes("0123456789")
  val contactDetails                  = ContactDetails("07000000000", "test@test.com")
  val addressHistory                  = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
  val vatRegistered                   = VATRegisteredNo
  val training                        = TrainingYes("test")
  val experienceTraining              = ExperienceTrainingYes("Some training")

  // scalastyle:off magic.number
  val positions =
    Positions(Set(BeneficialOwner, InternalAccountant), Some(PositionStartDate(LocalDate.of(2005, 3, 15))))

  val CompleteResponsiblePeople1 = ResponsiblePerson(
    personName = Some(personName),
    legalName = Some(legalName),
    legalNameChangeDate = Some(legalNameChangeDate),
    knownBy = Some(knownBy),
    personResidenceType = Some(personResidenceType),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(contactDetails),
    addressHistory = Some(addressHistory),
    positions = Some(positions),
    saRegistered = Some(saRegistered),
    vatRegistered = Some(vatRegistered),
    experienceTraining = Some(experienceTraining),
    training = Some(training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some("test")
  )
  val CompleteResponsiblePeople2 = ResponsiblePerson(
    personName = Some(personName),
    legalName = Some(legalName),
    legalNameChangeDate = Some(legalNameChangeDate),
    knownBy = Some(knownBy),
    personResidenceType = Some(personResidenceType),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(contactDetails),
    addressHistory = Some(addressHistory),
    positions = Some(positions),
    saRegistered = Some(saRegistered),
    vatRegistered = Some(vatRegistered),
    experienceTraining = Some(experienceTraining),
    training = Some(training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some("test")
  )
  val CompleteResponsiblePeople3 = ResponsiblePerson(
    personName = Some(personName),
    legalName = Some(legalName),
    legalNameChangeDate = Some(legalNameChangeDate),
    knownBy = Some(knownBy),
    personResidenceType = Some(personResidenceType),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(contactDetails),
    addressHistory = Some(addressHistory),
    positions = Some(positions),
    saRegistered = Some(saRegistered),
    vatRegistered = Some(vatRegistered),
    experienceTraining = Some(experienceTraining),
    training = Some(training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some("test")
  )

  val ResponsiblePeopleList = Seq(CompleteResponsiblePeople1, CompleteResponsiblePeople2, CompleteResponsiblePeople3)
}
