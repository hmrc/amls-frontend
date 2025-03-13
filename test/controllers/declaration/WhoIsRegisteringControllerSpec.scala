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

package controllers.declaration

import connectors.{AmlsConnector, DataCacheConnector}
import controllers.actions.SuccessfulAuthAction
import forms.declaration.WhoIsRegisteringFormProvider
import generators.ResponsiblePersonGenerator
import models.ReadStatusResponse
import models.declaration.release7.RoleWithinBusinessRelease7
import models.declaration.{AddPerson, WhoIsRegistering}
import models.registrationprogress.{Completed, Started, TaskRow}
import models.renewal.Renewal
import models.responsiblepeople._
import models.status._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.{RenewalService, SectionsProvider, StatusService}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks, StatusConstants}
import views.html.declaration.{WhoIsRegisteringThisRegistrationView, WhoIsRegisteringThisRenewalView, WhoIsRegisteringThisUpdateView}

import java.time.LocalDateTime
import scala.concurrent.Future

class WhoIsRegisteringControllerSpec extends AmlsSpec with MockitoSugar with ResponsiblePersonGenerator with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request: Request[AnyContentAsEmpty.type]         = addToken(authRequest)
    val mockSectionsProvider: SectionsProvider           = mock[SectionsProvider]
    lazy val view1: WhoIsRegisteringThisUpdateView       = inject[WhoIsRegisteringThisUpdateView]
    lazy val view2: WhoIsRegisteringThisRenewalView      = inject[WhoIsRegisteringThisRenewalView]
    lazy val view3: WhoIsRegisteringThisRegistrationView = inject[WhoIsRegisteringThisRegistrationView]
    val renewalService: RenewalService                   = mock[RenewalService]
    when(renewalService.isRenewalFlow(any(), any(), any())(any(), any())).thenReturn(Future.successful(false))

    val controller = new WhoIsRegisteringController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      amlsConnector = mock[AmlsConnector],
      statusService = mock[StatusService],
      renewalService = renewalService,
      cc = mockMcc,
      sectionsProvider = mockSectionsProvider,
      formProvider = inject[WhoIsRegisteringFormProvider],
      updateView = view1,
      renewalView = view2,
      registrationView = view3,
      error = errorView
    )

    val pendingReadStatusResponse: ReadStatusResponse =
      ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, None, renewalConFlag = false)

    val notCompletedReadStatusResponse: ReadStatusResponse =
      ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None, None, renewalConFlag = false)

    val cacheMap: Cache = mock[Cache]

    val responsiblePeople: Seq[ResponsiblePerson] = (for {
      p1 <- responsiblePersonGen
      p2 <- responsiblePersonGen.map(p => p.copy(status = Some(StatusConstants.Deleted)))
    } yield Seq(p1, p2)).sample.get

    def run(
      status: SubmissionStatus,
      renewal: Option[Renewal] = None,
      people: Seq[ResponsiblePerson] = responsiblePeople
    )(block: Unit => Any) = {
      when {
        controller.renewalService.getRenewal(any())
      } thenReturn Future.successful(renewal)

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(controller.statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(status))

      when(cacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
        .thenReturn(Some(people))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
        .thenReturn(Future.successful(Some(people)))

      when(controller.dataCacheConnector.save[AddPerson](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      block(())
    }
  }

  val emptyCache: Cache = Cache.empty

  "WhoIsRegisteringController" must {
    "Get" must {
      "with completed sections" must {
        val completedSections = Seq(
          TaskRow("s1", "/foo", hasChanged = true, Completed, TaskRow.completedTag),
          TaskRow("s2", "/bar", hasChanged = true, Completed, TaskRow.completedTag)
        )

        "load the who is registering page" when {
          "status is pending" in new Fixture {
            when {
              mockSectionsProvider.taskRows(any[String])(any(), any())
            }.thenReturn(Future.successful(completedSections))

            run(SubmissionReadyForReview) { _ =>
              val result = controller.get()(request)
              status(result) must be(OK)

              val htmlValue = Jsoup.parse(contentAsString(result))
              htmlValue.title mustBe messages("declaration.who.is.registering.amendment.title") + " - " + messages(
                "title.amls"
              ) + " - " + messages("title.gov")
              htmlValue.getElementById("person-0").parent().text() must include(
                responsiblePeople.head.personName.get.fullName
              )

              contentAsString(result) must include(messages("submit.amendment.application"))
            }
          }

          "status is approved" in new Fixture {
            when {
              mockSectionsProvider.taskRows(any[String])(any(), any())
            }.thenReturn(Future.successful(completedSections))

            run(SubmissionDecisionApproved) { _ =>
              val result = controller.get()(request)
              status(result) must be(OK)

              val htmlValue = Jsoup.parse(contentAsString(result))
              htmlValue.title mustBe messages("declaration.who.is.registering.amendment.title") + " - " + messages(
                "title.amls"
              ) + " - " + messages("title.gov")
              htmlValue.getElementById("person-0").parent().text() must include(
                responsiblePeople.head.personName.get.fullName
              )

              contentAsString(result) must include(messages("submit.amendment.application"))
            }
          }

          "status is pre-submission" in new Fixture {
            when {
              mockSectionsProvider.taskRows(any[String])(any(), any())
            }.thenReturn(Future.successful(completedSections))

            run(SubmissionReady) { _ =>
              val result = controller.get()(request)
              status(result) must be(OK)

              val htmlValue = Jsoup.parse(contentAsString(result))
              htmlValue.title mustBe messages("declaration.who.is.registering.title") + " - " + messages(
                "title.amls"
              ) + " - " + messages("title.gov")
              htmlValue.getElementById("person-0").parent().text() must include(
                responsiblePeople.head.personName.get.fullName
              )

              contentAsString(result) must include(messages("submit.registration"))
            }
          }

          "status is renewal amendment" in new Fixture {
            when {
              mockSectionsProvider.taskRows(any[String])(any(), any())
            }.thenReturn(Future.successful(completedSections))

            run(RenewalSubmitted(None)) { _ =>
              val result = controller.get()(request)
              status(result) must be(OK)

              contentAsString(result) must include(messages("submit.amendment.application"))
            }
          }

          "status is renewal" in new Fixture {
            when {
              mockSectionsProvider.taskRows(any[String])(any(), any())
            }.thenReturn(Future.successful(completedSections))

            run(ReadyForRenewal(None), Some(mock[Renewal])) { _ =>
              val result = controller.get()(request)
              status(result) must be(OK)

              contentAsString(result) must include(messages("declaration.renewal.who.is.registering.heading"))
            }
          }
        }
      }

      "with incomplete sections" must {
        val incompleteSections = Seq(
          TaskRow("s1", "/foo", hasChanged = true, Completed, TaskRow.completedTag),
          TaskRow("s2", "/bar", hasChanged = true, Started, TaskRow.incompleteTag)
        )

        "redirect to the RegistrationProgressController" in new Fixture {
          when {
            mockSectionsProvider.taskRows(any[String])(any(), any())
          }.thenReturn(Future.successful(incompleteSections))

          run(SubmissionReadyForReview) { _ =>
            val result = controller.get()(request)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
          }
        }
      }
    }

    "Post" must {
      "successfully redirect next page when user selects the option 'Someone else'" when {
        "status is pending" in new Fixture {
          run(SubmissionReadyForReview) { _ =>
            val newRequest = FakeRequest(POST, routes.WhoIsRegisteringController.post("registration").url)
              .withFormUrlEncodedBody("person" -> "-1")

            val result = controller.post("registration")(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AddPersonController.getWithAmendment().url))
          }
        }

        "status is pre-submission" in new Fixture {
          run(SubmissionReady) { _ =>
            val newRequest = FakeRequest(POST, routes.WhoIsRegisteringController.post("registration").url)
              .withFormUrlEncodedBody("person" -> "-1")
            val result     = controller.post("registration")(newRequest)

            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AddPersonController.get().url))
          }
        }
      }

      "select the correct person when two people have the same name" in new Fixture {

        val (name, people) = (for {
          name <- personNameGen
          p1   <- responsiblePersonWithPositionsGen(Some(Set(Director))).map(_.copy(personName = Some(name)))
          p2   <- responsiblePersonWithPositionsGen(Some(Set(InternalAccountant))).map(_.copy(personName = Some(name)))
        } yield (name, Seq(p1, p2))).sample.get

        run(NotCompleted, people = people) { _ =>
          val newRequest = FakeRequest(POST, routes.WhoIsRegisteringController.post("registration").url)
            .withFormUrlEncodedBody("person" -> "person-1")
          val result     = controller.post("registration")(newRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)

          val expectedAddPersonModel = AddPerson(
            name.firstName,
            name.middleName,
            name.lastName,
            RoleWithinBusinessRelease7(Set(models.declaration.release7.InternalAccountant))
          )
          verify(controller.dataCacheConnector)
            .save[AddPerson](any(), eqTo(AddPerson.key), eqTo(expectedAddPersonModel))(any())
        }

      }

      "successfully redirect next page when user selects one of the responsible person from the options" in new Fixture {
        run(NotCompleted) { _ =>
          val newRequest = FakeRequest(POST, routes.WhoIsRegisteringController.post("registration").url)
            .withFormUrlEncodedBody("person" -> "person-0")
          val result     = controller.post("registration")(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DeclarationController.get().url))

          verify(controller.dataCacheConnector).save[AddPerson](any(), eqTo(AddPerson.key), any())(any())
        }
      }

      "show error when invalid data is posted" in new Fixture {
        run(SubmissionReady) { _ =>
          val newRequest = FakeRequest(POST, routes.WhoIsRegisteringController.post("registration").url)
            .withFormUrlEncodedBody("" -> "")
          val result     = controller.post("registration")(newRequest)

          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(messages("declaration.who.is.registering.text"))
          contentAsString(result) must include(messages("submit.registration"))
        }
      }

      "show who is declaring this update error when invalid data is posted for update" in new Fixture {
        run(SubmissionReadyForReview) { _ =>
          val newRequest = FakeRequest(POST, routes.WhoIsRegisteringController.post("update").url)
            .withFormUrlEncodedBody("" -> "")
          val result     = controller.post("update")(newRequest)

          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include("Select who is declaring this update")
        }
      }

      "show who is declaring this renewal error when invalid data is posted for renewal" in new Fixture {
        run(SubmissionReadyForReview) { _ =>
          val newRequest = FakeRequest(POST, routes.WhoIsRegisteringController.post("renewal").url)
            .withFormUrlEncodedBody("" -> "")
          val result     = controller.post("renewal")(newRequest)

          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include("Select who is submitting this declaration")
        }
      }

      "redirect to the declaration page" when {
        "status is pending" in new Fixture {
          run(SubmissionReadyForReview) { _ =>
            val newRequest = FakeRequest(POST, routes.WhoIsRegisteringController.post("registration").url)
              .withFormUrlEncodedBody("person" -> "person-0")
            val result     = controller.post("registration")(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) mustBe Some(routes.DeclarationController.getWithAmendment().url)

            verify(controller.dataCacheConnector).save[AddPerson](any(), eqTo(AddPerson.key), any())(any())
          }
        }

        "status is pre-submission" in new Fixture {
          run(SubmissionReady) { _ =>
            val newRequest = FakeRequest(POST, routes.WhoIsRegisteringController.post("registration").url)
              .withFormUrlEncodedBody("person" -> "person-0")
            val result     = controller.post("registration")(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)

            verify(controller.dataCacheConnector).save[AddPerson](any(), eqTo(AddPerson.key), any())(any())
          }
        }
      }
    }
  }
}
