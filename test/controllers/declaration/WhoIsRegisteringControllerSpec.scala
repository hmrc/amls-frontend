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

package controllers.declaration

import connectors.{AmlsConnector, DataCacheConnector}
import generators.ResponsiblePersonGenerator
import models.ReadStatusResponse
import models.declaration.release7.RoleWithinBusinessRelease7
import models.declaration.{AddPerson, WhoIsRegistering}
import models.renewal.Renewal
import models.responsiblepeople._
import models.status._
import org.joda.time.{LocalDate, LocalDateTime}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthorisedFixture, StatusConstants}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class WhoIsRegisteringControllerSpec extends GenericTestHelper with MockitoSugar with ResponsiblePersonGenerator {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val controller = new WhoIsRegisteringController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val amlsConnector = mock[AmlsConnector]
      override val statusService: StatusService = mock[StatusService]
      override private[controllers] val renewalService = mock[RenewalService]
    }

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

    def run(status: SubmissionStatus, renewal: Option[Renewal] = None, people: Seq[ResponsiblePeople] = responsiblePeople)(block: Unit => Any) = {
      when {
        controller.renewalService.getRenewal(any(), any(), any())
      } thenReturn Future.successful(renewal)

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(status))

      when(cacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
        .thenReturn(Some(people))

//      when(cacheMap.getEntry[WhoIsRegistering](WhoIsRegistering.key))
//        .thenReturn(None)

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(people)))

      when(controller.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      block()
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "WhoIsRegisteringController" must {

    "Get Option:" must {

      "load the who is registering page" when {
        "status is pending" in new Fixture {
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
          run(RenewalSubmitted(None)) { _ =>
            val result = controller.get()(request)
            status(result) must be(OK)

            val htmlValue = Jsoup.parse(contentAsString(result))

            contentAsString(result) must include(Messages("submit.amendment.application"))
          }
        }


        "status is renewal" in new Fixture {
          run(ReadyForRenewal(None), Some(mock[Renewal])) { _ =>
            val result = controller.get(request)
            status(result) must be(OK)

            val htmlValue = Jsoup.parse(contentAsString(result))
            contentAsString(result) must include(Messages("declaration.renewal.who.is.registering.heading"))
          }
        }
      }
    }

    "Post" must {

      "successfully redirect next page when user selects the option 'Someone else'" when {
        "status is pending" in new Fixture {
          run(SubmissionReadyForReview) { _ =>
            val newRequest = request.withFormUrlEncodedBody("person" -> "-1")

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AddPersonController.getWithAmendment().url))
          }
        }

        "status is pre-submission" in new Fixture {
          run(SubmissionReady) { _ =>
            val newRequest = request.withFormUrlEncodedBody("person" -> "-1")
            val result = controller.post()(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AddPersonController.get().url))
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
          val newRequest = request.withFormUrlEncodedBody("person" -> "person-1")
          val result = controller.post()(newRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)

          val expectedAddPersonModel = AddPerson(name.firstName, name.middleName, name.lastName, RoleWithinBusinessRelease7(Set(models.declaration.release7.InternalAccountant)))
          verify(controller.dataCacheConnector).save[AddPerson](eqTo(AddPerson.key), eqTo(expectedAddPersonModel))(any(), any(), any())
        }

      }

      "successfully redirect next page when user selects one of the responsible person from the options" in new Fixture {
        run(NotCompleted) { _ =>
          val newRequest = request.withFormUrlEncodedBody("person" -> "person-0")
          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DeclarationController.get().url))

          verify(controller.dataCacheConnector).save[AddPerson](eqTo(AddPerson.key), any())(any(), any(), any())
        }
      }

      "show error when invalid data is posted" in new Fixture {
        run(SubmissionReady) { _ =>
          val newRequest = request.withFormUrlEncodedBody()
          val result = controller.post()(newRequest)

          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("declaration.who.is.registering.text"))
          contentAsString(result) must include(Messages("submit.registration"))
        }
      }

      "redirect to the declaration page" when {
        "status is pending" in new Fixture {
          run(SubmissionReadyForReview) { _ =>
            val newRequest = request.withFormUrlEncodedBody("person" -> "person-0")
            val result = controller.post()(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) mustBe Some(routes.DeclarationController.getWithAmendment().url)

            verify(controller.dataCacheConnector).save[AddPerson](eqTo(AddPerson.key), any())(any(), any(), any())
          }
        }

        "status is pre-submission" in new Fixture {
          run(SubmissionReady) { _ =>
            val newRequest = request.withFormUrlEncodedBody("person" -> "person-0")
            val result = controller.post()(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)

            verify(controller.dataCacheConnector).save[AddPerson](eqTo(AddPerson.key), any())(any(), any(), any())
          }
        }
      }
    }
  }
}
