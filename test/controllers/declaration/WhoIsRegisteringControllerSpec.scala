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
import models.ReadStatusResponse
import models.declaration.{AddPerson, WhoIsRegistering}
import models.renewal.Renewal
import models.responsiblepeople._
import models.status._
import org.joda.time.{LocalDate, LocalDateTime}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, StatusConstants}

import scala.concurrent.Future

class WhoIsRegisteringControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new  WhoIsRegisteringController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val amlsConnector = mock[AmlsConnector]
      override val statusService: StatusService = mock[StatusService]
      override private[controllers] val renewalService = mock[RenewalService]
    }

    val pendingReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None,
      None, false, safeId = "ABCDE1234567890")
    val notCompletedReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None,
      None, false, safeId = "ABCDE1234567890")
    when {
      controller.renewalService.getRenewal(any(), any(), any())
    } thenReturn Future.successful(None)
  }

  val emptyCache = CacheMap("", Map.empty)

  "WhoIsRegisteringController" must {

    val personName = PersonName("firstName", Some("middleName"), "lastName", None, Some("name"))
    val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
    val rp = ResponsiblePeople (
      personName = Some(personName),
      positions = Some(positions),
      status = None
    )
    val rp1 = ResponsiblePeople(
      personName = Some(personName),
      positions = Some(positions),
      status = Some(StatusConstants.Deleted)
    )
    val responsiblePeoples = Seq(rp, rp1)


    "Get Option:" must {

      "load the who is registering page" when {
        "status is pending" in new Fixture {

          val mockCacheMap = mock[CacheMap]

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(responsiblePeoples))

          when(mockCacheMap.getEntry[WhoIsRegistering](WhoIsRegistering.key))
            .thenReturn(None)

          val result = controller.get()(request)
          status(result) must be(OK)

          val htmlValue = Jsoup.parse(contentAsString(result))
          htmlValue.title mustBe Messages("declaration.who.is.registering.amendment.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
          htmlValue.getElementById("person-firstNamelastName").`val`() must be("firstNamelastName")

          contentAsString(result) must include(Messages("submit.amendment.application"))
        }

        "status is approved" in new Fixture {

          val mockCacheMap = mock[CacheMap]

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(responsiblePeoples))

          when(mockCacheMap.getEntry[WhoIsRegistering](WhoIsRegistering.key))
            .thenReturn(None)

          val result = controller.get()(request)
          status(result) must be(OK)

          val htmlValue = Jsoup.parse(contentAsString(result))
          htmlValue.title mustBe Messages("declaration.who.is.registering.amendment.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
          htmlValue.getElementById("person-firstNamelastName").`val`() must be("firstNamelastName")

          contentAsString(result) must include(Messages("submit.amendment.application"))
        }

        "status is pre-submission" in new Fixture {

          val mockCacheMap = mock[CacheMap]

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(responsiblePeoples))

          when(mockCacheMap.getEntry[WhoIsRegistering](WhoIsRegistering.key))
            .thenReturn(None)

          val result = controller.get()(request)
          status(result) must be(OK)

          val htmlValue = Jsoup.parse(contentAsString(result))
          htmlValue.title mustBe Messages("declaration.who.is.registering.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
          htmlValue.getElementById("person-firstNamelastName").`val`() must be("firstNamelastName")

          contentAsString(result) must include(Messages("submit.registration"))
        }


        "status is renewal amendment" in new Fixture {

          val mockCacheMap = mock[CacheMap]

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(RenewalSubmitted(None)))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(responsiblePeoples))

          when(mockCacheMap.getEntry[WhoIsRegistering](WhoIsRegistering.key))
            .thenReturn(None)

          val result = controller.get()(request)
          status(result) must be(OK)

          val htmlValue = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include(Messages("submit.amendment.application"))
        }


        "status is renewal" in new Fixture {

          val mockCacheMap = mock[CacheMap]

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(ReadyForRenewal(None)))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(responsiblePeoples))

          when(mockCacheMap.getEntry[WhoIsRegistering](WhoIsRegistering.key))
            .thenReturn(None)
          when {
            controller.renewalService.getRenewal(any(), any(), any())
          } thenReturn Future.successful(Some(mock[Renewal]))

          val result = controller.getWithRenewal(request)
          status(result) must be(OK)

          val htmlValue = Jsoup.parse(contentAsString(result))

          contentAsString(result) must include(Messages("declaration.renewal.who.is.registering.heading"))
        }
      }
    }

    "Post" must {

      "successfully redirect next page when user selects the option 'Someone else'" when {
        "status is pending" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("person" -> "-1")

          val mockCacheMap = mock[CacheMap]

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(responsiblePeoples))

          when(controller.dataCacheConnector.save[AddPerson](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AddPersonController.getWithAmendment().url))
        }
        "status is pre-submission" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("person" -> "-1")

          val mockCacheMap = mock[CacheMap]

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(responsiblePeoples))

          when(controller.dataCacheConnector.save[AddPerson](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AddPersonController.get().url))
        }
      }


      "successfully redirect next page when user selects one of the responsible person from the options" in new Fixture {

        when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(NotCompleted))

        val newRequest = request.withFormUrlEncodedBody("person" -> "dfsf")

        val mockCacheMap = mock[CacheMap]

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(responsiblePeoples))

        when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.DeclarationController.get().url))
      }


      "on post invalid data show error" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(responsiblePeoples)))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("declaration.who.is.registering.text"))
          contentAsString(result) must include(Messages("submit.registration"))

        }

      "redirect to the declaration page" when {
        "status is pending" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("person" -> "dfsf")
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(responsiblePeoples))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(routes.DeclarationController.getWithAmendment().url)
        }
        "status is pre-submission" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("person" -> "dfsf")
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(responsiblePeoples))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(controller.statusService.getStatus(any(),any(),any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)
        }
      }
    }
  }

}

class WhoIsRegisteringControllerWithoutAmendmentsSpec extends GenericTestHelper with MockitoSugar {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.amendments" -> false) )

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new  WhoIsRegisteringController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val amlsConnector = mock[AmlsConnector]
      override val statusService: StatusService = mock[StatusService]
      override private[controllers] val renewalService = mock[RenewalService]
    }

    val pendingReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None,
      None, false, safeId = "ABCDE1234567890")
    val notCompletedReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None,
      None, false, safeId = "ABCDE1234567890")
    val personName = PersonName("firstName", Some("middleName"), "lastName", None, Some("name"))
    val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
    val rp = ResponsiblePeople(
      personName = Some(personName),
      positions = Some(positions)
    )

    val rp1 = ResponsiblePeople(
      personName = Some(personName),
      positions = Some(positions),
      status = Some(StatusConstants.Deleted)
    )
    val responsiblePeoples = Seq(rp, rp1)

    when {
      controller.renewalService.getRenewal(any(), any(), any())
    } thenReturn Future.successful(None)

  }

  val emptyCache = CacheMap("", Map.empty)

  "WhoIsRegisteringController" must {
    "load the who is registering page" when {
      "status is pending" in new Fixture {

        val mockCacheMap = mock[CacheMap]

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(Some(responsiblePeoples))

        when(mockCacheMap.getEntry[WhoIsRegistering](WhoIsRegistering.key))
          .thenReturn(None)

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe Messages("declaration.who.is.registering.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov")
        htmlValue.getElementById("person-firstNamelastName").`val`() must be("firstNamelastName")

        contentAsString(result) must include(Messages("submit.registration"))
      }
    }

    "redirect to the declaration page" when {
      "status is pending" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("person" -> "dfsf")
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(responsiblePeoples))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)
      }
    }
    "successfully redirect next page when user selects the option 'Someone else'" when {
      "status is pending" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("person" -> "-1")

        val mockCacheMap = mock[CacheMap]

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(responsiblePeoples))

        when(controller.dataCacheConnector.save[AddPerson](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        when(controller.dataCacheConnector.save[WhoIsRegistering](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.AddPersonController.get().url))
      }
    }
  }
}