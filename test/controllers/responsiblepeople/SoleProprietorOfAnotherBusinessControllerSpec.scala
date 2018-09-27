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
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{PersonName, ResponsiblePerson, SoleProprietorOfAnotherBusiness, VATRegisteredNo}
import models.status.{NotCompleted, SubmissionStatus}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class SoleProprietorOfAnotherBusinessControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]

    val submissionStatus: SubmissionStatus = NotCompleted

    val controller = new SoleProprietorOfAnotherBusinessController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      statusService = mock[StatusService])
  }

  val emptyCache = CacheMap("", Map.empty)

  val personName = Some(PersonName("firstname", None, "lastname"))
  val soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true))

  "SoleProprietorOfAnotherBusinessController" when {
    "get is called" when {
      "application status is PostSubmission" when {
        "adding a new Responsible Person" must {
          "display empty Sole Proprietor view" in new Fixture {

            when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName, lineId = None)))))

            when(controller.statusService.isPreSubmission(any(), any(), any()))
              .thenReturn(Future.successful(false))

            val result = controller.get(1)(request)

            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("soleProprietorOfAnotherBusiness-true").hasAttr("checked") must be(false)
            document.getElementById("soleProprietorOfAnotherBusiness-false").hasAttr("checked") must be(false)
          }
        }

        "updating existing Responsible Person if there is some VAT data" must {
          "redirect to VATRegisteredController" in new Fixture {

            val mockCacheMap = mock[CacheMap]

            when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = personName, vatRegistered = Some(VATRegisteredNo), lineId = Some(44444))))))

            when(mockDataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(mockCacheMap))

            when(controller.statusService.isPreSubmission(any(), any(), any()))
              .thenReturn(Future.successful(false))

            val result = controller.get(1)(request)

            status(result) must be(SEE_OTHER)

            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(1).url))
          }
        }

        "updating existing Responsible Person if there is no any VAT data" must {
          "redirect to SelfAssessmentController" in new Fixture {

            val mockCacheMap = mock[CacheMap]

            when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName, soleProprietorOfAnotherBusiness = None, vatRegistered = None, lineId = Some(4444))))))

            when(mockDataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(mockCacheMap))

            when(controller.statusService.isPreSubmission(any(), any(), any()))
              .thenReturn(Future.successful(false))

            val result = controller.get(1)(request)

            status(result) must be(SEE_OTHER)

            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1).url))
          }
        }

        "display page and prepopulate data from save4later" in new Fixture {

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName, soleProprietorOfAnotherBusiness = soleProprietorOfAnotherBusiness)))))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.isPreSubmission(any(), any(), any()))
            .thenReturn(Future.successful(false))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("soleProprietorOfAnotherBusiness-true").hasAttr("checked") must be(true)
          document.getElementById("soleProprietorOfAnotherBusiness-false").hasAttr("checked") must be(false)
        }

        "display page Not Found" when {
          "neither soleProprietorOfAnotherBusiness nor name is set" in new Fixture {

            when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))
            when(controller.statusService.isPreSubmission(any(), any(), any()))
              .thenReturn(Future.successful(false))

            val result = controller.get(1)(request)
            status(result) must be(NOT_FOUND)
          }
        }
      }

      "application status is PreSubmission" when {
        "adding a new Responsible Person" must {
          "display the Sole Proprietor view" in new Fixture {

            when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName, lineId = None)))))

            when(controller.statusService.isPreSubmission(any(), any(), any()))
              .thenReturn(Future.successful(true))

            val result = controller.get(1)(request)

            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("soleProprietorOfAnotherBusiness-true").hasAttr("checked") must be(false)
            document.getElementById("soleProprietorOfAnotherBusiness-false").hasAttr("checked") must be(false)
          }
        }

        "adding a new Responsible Person" must {
          "display page" in new Fixture {

            when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName)))))

            when(controller.statusService.isPreSubmission(any(), any(), any()))
              .thenReturn(Future.successful(true))

            val result = controller.get(1)(request)

            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("soleProprietorOfAnotherBusiness-true").hasAttr("checked") must be(false)
            document.getElementById("soleProprietorOfAnotherBusiness-false").hasAttr("checked") must be(false)
          }
        }

        "display page and prepopulate data from save4later" in new Fixture {

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName, soleProprietorOfAnotherBusiness = soleProprietorOfAnotherBusiness)))))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.isPreSubmission(any(), any(), any()))
            .thenReturn(Future.successful(true))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("soleProprietorOfAnotherBusiness-true").hasAttr("checked") must be(true)
          document.getElementById("soleProprietorOfAnotherBusiness-false").hasAttr("checked") must be(false)
        }

        "display page Not Found" when {
          "neither soleProprietorOfAnotherBusiness nor name is set" in new Fixture {

            when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))
            when(controller.statusService.isPreSubmission(any(), any(), any()))
              .thenReturn(Future.successful(true))

            val result = controller.get(1)(request)
            status(result) must be(NOT_FOUND)
          }
        }
      }
    }

    "post is called" when {
      "soleProprietorOfAnotherBusiness is set to true" must {
        "go to VATRegisteredController" in new Fixture {

          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody(
            "soleProprietorOfAnotherBusiness" -> "true",
            "personName" -> "Person Name")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(vatRegistered = Some(VATRegisteredNo))))))

          when(mockDataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(1).url))

          verify(controller.dataCacheConnector).save(
            any(),
            meq(Seq(ResponsiblePerson(
              soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true)), vatRegistered = Some(VATRegisteredNo)
            )))
          )(any(), any(), any())

        }
      }

      "soleProprietorOfAnotherBusiness is set to false" when {
        "edit is true" must {
          "go to DetailedAnswersController" in new Fixture {
            val mockCacheMap = mock[CacheMap]
            val newRequest = request.withFormUrlEncodedBody(
              "soleProprietorOfAnotherBusiness" -> "false",
              "personName" -> "Person Name")

            when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

            when(mockDataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(1,true,Some(flowFromDeclaration))(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
          }
        }

        "edit is false" must {
          "go to RegisteredForSelfAssessmentController" in new Fixture {
            val mockCacheMap = mock[CacheMap]
            val newRequest = request.withFormUrlEncodedBody(
              "soleProprietorOfAnotherBusiness" -> "false",
              "personName" -> "Person Name")

            when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

            when(mockDataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(1)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1).url))

            verify(controller.dataCacheConnector).save(
              any(),
              meq(Seq(ResponsiblePerson(soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false)))))
            )(any(), any(), any())
          }
        }
      }

      "respond with BAD_REQUEST" when {
        "given an invalid form" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "soleProprietorOfAnotherBusiness" -> "",
            "personName" -> "Person Name")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName)))))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "respond with NOT_FOUND" when {
        "ResponsiblePeople model cannot be found with given index" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "soleProprietorOfAnotherBusiness" -> "true",
            "personName" -> "Person Name")

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.post(1)(newRequest)
          status(result) must be(NOT_FOUND)

        }
      }
    }
  }
}