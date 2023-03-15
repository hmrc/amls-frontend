/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.declaration

import connectors.{AmlsConnector, DataCacheConnector}
import controllers.actions.SuccessfulAuthAction
import forms.InvalidForm
import generators.ResponsiblePersonGenerator
import jto.validation.{Path, ValidationError}
import models.ReadStatusResponse
import models.declaration.release7.RoleWithinBusinessRelease7
import models.declaration.{AddPerson, WhoIsRegistering}
import models.registrationprogress.{Completed, Section, Started}
import models.renewal.Renewal
import models.responsiblepeople._
import models.status._
import org.joda.time.{LocalDate, LocalDateTime}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import utils.{AmlsSpec, DependencyMocks, StatusConstants}
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.test.Helpers._
import services.{RenewalService, SectionsProvider, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.declaration.{who_is_registering_this_registration, who_is_registering_this_renewal, who_is_registering_this_update}

import scala.concurrent.Future

class WhoIsRegisteringControllerSpec extends AmlsSpec with MockitoSugar with ResponsiblePersonGenerator {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)
    val mockSectionsProvider = mock[SectionsProvider]
    lazy val view1 = app.injector.instanceOf[who_is_registering_this_update]
    lazy val view2 = app.injector.instanceOf[who_is_registering_this_renewal]
    lazy val view3 = app.injector.instanceOf[who_is_registering_this_registration]
    val controller = new WhoIsRegisteringController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      amlsConnector = mock[AmlsConnector],
      statusService = mock[StatusService],
      renewalService = mock[RenewalService],
      cc = mockMcc,
      sectionsProvider = mockSectionsProvider,
      who_is_registering_this_update = view1,
      who_is_registering_this_renewal = view2,
      who_is_registering_this_registration = view3,
      error = errorView
    )

    val pendingReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None,
      None, false)

    val notCompletedReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None,
      None, false)

    val cacheMap = mock[CacheMap]

    val responsiblePeople = (for {
      p1 <- responsiblePersonGen
      p2 <- responsiblePersonGen.map(p => p.copy(status = Some(StatusConstants.Deleted)))
    } yield {
      Seq(p1, p2)
    }).sample.get

    def run(status: SubmissionStatus, renewal: Option[Renewal] = None, people: Seq[ResponsiblePerson] = responsiblePeople)(block: Unit => Any) = {
      when {
        controller.renewalService.getRenewal(any())(any())
      } thenReturn Future.successful(renewal)

      when(controller.dataCacheConnector.fetchAll(any())(any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(controller.statusService.getStatus(Some(any()), any(), any())(any(), any()))
        .thenReturn(Future.successful(status))

      when(cacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
        .thenReturn(Some(people))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(people)))

      when(controller.dataCacheConnector.save[AddPerson](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      block(())
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "WhoIsRegisteringController" must {
    "Get" must {
      "with completed sections" must {
        val completedSections = Seq(
          Section("s1", Completed, true, mock[Call]),
          Section("s2", Completed, true, mock[Call])
        )

        "load the who is registering page" when {
          "status is pending" in new Fixture {
            when {
              mockSectionsProvider.sections(any[String])(any(), any())
            }.thenReturn(Future.successful(completedSections))

            run(SubmissionReadyForReview) { _ =>
              val result = controller.get()(request)
              status(result) must be(OK)

              val htmlValue = Jsoup.parse(contentAsString(result))
              htmlValue.title mustBe Messages("declaration.who.is.registering.amendment.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
              htmlValue.getElementById("person-0").parent().text() must include(responsiblePeople.head.personName.get.fullName)

              contentAsString(result) must include(Messages("submit.amendment.application"))
            }
          }

          "status is approved" in new Fixture {
            when {
              mockSectionsProvider.sections(any[String])(any(), any())
            }.thenReturn(Future.successful(completedSections))

            run(SubmissionDecisionApproved) { _ =>
              val result = controller.get()(request)
              status(result) must be(OK)

              val htmlValue = Jsoup.parse(contentAsString(result))
              htmlValue.title mustBe Messages("declaration.who.is.registering.amendment.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
              htmlValue.getElementById("person-0").parent().text() must include(responsiblePeople.head.personName.get.fullName)

              contentAsString(result) must include(Messages("submit.amendment.application"))
            }
          }

          "status is pre-submission" in new Fixture {
            when {
              mockSectionsProvider.sections(any[String])(any(), any())
            }.thenReturn(Future.successful(completedSections))

            run(SubmissionReady) { _ =>
              val result = controller.get()(request)
              status(result) must be(OK)

              val htmlValue = Jsoup.parse(contentAsString(result))
              htmlValue.title mustBe Messages("declaration.who.is.registering.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
              htmlValue.getElementById("person-0").parent().text() must include(responsiblePeople.head.personName.get.fullName)

              contentAsString(result) must include(Messages("submit.registration"))
            }
          }

          "status is renewal amendment" in new Fixture {
            when {
              mockSectionsProvider.sections(any[String])(any(), any())
            }.thenReturn(Future.successful(completedSections))

            run(RenewalSubmitted(None)) { _ =>
              val result = controller.get()(request)
              status(result) must be(OK)

              contentAsString(result) must include(Messages("submit.amendment.application"))
            }
          }


          "status is renewal" in new Fixture {
            when {
              mockSectionsProvider.sections(any[String])(any(), any())
            }.thenReturn(Future.successful(completedSections))

            run(ReadyForRenewal(None), Some(mock[Renewal])) { _ =>
              val result = controller.get()(request)
              status(result) must be(OK)

              contentAsString(result) must include(Messages("declaration.renewal.who.is.registering.heading"))
            }
          }
        }
      }

      "with incomplete sections" must {
        val incompleteSections = Seq(
          Section("s1", Completed, true, mock[Call]),
          Section("s2", Started, true, mock[Call])
        )

        "redirect to the RegistrationProgressController" in new Fixture {
          when {
            mockSectionsProvider.sections(any[String])(any(), any())
          }.thenReturn(Future.successful(incompleteSections))

          run(SubmissionReadyForReview) { _ =>
            val result = controller.get()(request)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get.url)
          }
        }
      }
    }

    "Post" must {
      "successfully redirect next page when user selects the option 'Someone else'" when {
        "status is pending" in new Fixture {
          run(SubmissionReadyForReview) { _ =>
            val newRequest = requestWithUrlEncodedBody("person" -> "-1")

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AddPersonController.getWithAmendment().url))
          }
        }

        "status is pre-submission" in new Fixture {
          run(SubmissionReady) { _ =>
            val newRequest = requestWithUrlEncodedBody("person" -> "-1")
            val result = controller.post()(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AddPersonController.get.url))
          }
        }
      }

      "select the correct person when two people have the same name" in new Fixture {

        val (name, people) = (for {
          name <- personNameGen
          p1 <- responsiblePersonWithPositionsGen(Some(Set(Director))).map(_.copy(personName = Some(name)))
          p2 <- responsiblePersonWithPositionsGen(Some(Set(InternalAccountant))).map(_.copy(personName = Some(name)))
        } yield (name, Seq(p1, p2))).sample.get

        run(NotCompleted, people = people) { _ =>
          val newRequest = requestWithUrlEncodedBody("person" -> "person-1")
          val result = controller.post()(newRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.DeclarationController.get.url)

          val expectedAddPersonModel = AddPerson(name.firstName, name.middleName, name.lastName, RoleWithinBusinessRelease7(Set(models.declaration.release7.InternalAccountant)))
          verify(controller.dataCacheConnector).save[AddPerson](any(), eqTo(AddPerson.key), eqTo(expectedAddPersonModel))(any(), any())
        }

      }

      "successfully redirect next page when user selects one of the responsible person from the options" in new Fixture {
        run(NotCompleted) { _ =>
          val newRequest = requestWithUrlEncodedBody("person" -> "person-0")
          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DeclarationController.get.url))

          verify(controller.dataCacheConnector).save[AddPerson](any(), eqTo(AddPerson.key), any())(any(), any())
        }
      }

      "show error when invalid data is posted" in new Fixture {
        run(SubmissionReady) { _ =>
          val newRequest = requestWithUrlEncodedBody("" -> "")
          val result = controller.post()(newRequest)

          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("declaration.who.is.registering.text"))
          contentAsString(result) must include(Messages("submit.registration"))
        }
      }

      "show who is declaring this update error when invalid data is posted for update" in new Fixture {
        run(SubmissionReadyForReview) { _ =>
          val newRequest = requestWithUrlEncodedBody("" -> "")
          val result = controller.post()(newRequest)

          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Select who is declaring this update")
        }
      }

      "show who is declaring this update error when status is where there is no add person view shown" in new Fixture {
        val invalidForm = InvalidForm(Map.empty, Seq.empty)

        val actualForm = controller.updateFormErrors(invalidForm, NotCompleted, true)

        val expectedErrors = Seq((Path("person"), Seq(ValidationError(Seq("Select who is registering this business")))))
        actualForm.errors must be(expectedErrors)
      }

      "show who is declaring this update error when status is RenewalSubmitted" in new Fixture {
        val invalidForm = InvalidForm(Map.empty, Seq.empty)

        val actualForm = controller.updateFormErrors(invalidForm, RenewalSubmitted(Some(LocalDate.now)), true)

        val expectedErrors = Seq((Path("person"), Seq(ValidationError(Seq("Select who is declaring this update")))))
        actualForm.errors must be(expectedErrors)
      }

      "show who is declaring this renewal error when there is variation and status is SubmissionReadyForReview or SubmissionDecisionApproved or ReadyForRenewal" in new Fixture {
        val invalidForm = InvalidForm(Map.empty, Seq.empty)

        val actualForm = controller.updateFormErrors(invalidForm, SubmissionReadyForReview, true)

        val expectedErrors = Seq((Path("person"), Seq(ValidationError(Seq("Select who is declaring this renewal")))))
        actualForm.errors must be(expectedErrors)
      }

      "show who is declaring this update error when there is variation and status is SubmissionReadyForReview or SubmissionDecisionApproved or ReadyForRenewal" in new Fixture {
        val invalidForm = InvalidForm(Map.empty, Seq.empty)

        val actualForm = controller.updateFormErrors(invalidForm, SubmissionReadyForReview, false)

        val expectedErrors = Seq((Path("person"), Seq(ValidationError(Seq("Select who is declaring this update")))))
        actualForm.errors must be(expectedErrors)
      }

      "redirect to the declaration page" when {
        "status is pending" in new Fixture {
          run(SubmissionReadyForReview) { _ =>
            val newRequest = requestWithUrlEncodedBody("person" -> "person-0")
            val result = controller.post()(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) mustBe Some(routes.DeclarationController.getWithAmendment().url)

            verify(controller.dataCacheConnector).save[AddPerson](any(), eqTo(AddPerson.key), any())(any(), any())
          }
        }

        "status is pre-submission" in new Fixture {
          run(SubmissionReady) { _ =>
            val newRequest = requestWithUrlEncodedBody("person" -> "person-0")
            val result = controller.post()(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) mustBe Some(routes.DeclarationController.get.url)

            verify(controller.dataCacheConnector).save[AddPerson](any(), eqTo(AddPerson.key), any())(any(), any())
          }
        }
      }
    }
  }
}
