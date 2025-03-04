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
import forms.responsiblepeople.SelfAssessmentRegisteredFormProvider
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{PersonName, ResponsiblePerson, SaRegisteredYes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import utils.AmlsSpec
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import views.html.responsiblepeople.SelfAssessmentRegisteredView

import scala.concurrent.Future

class RegisteredForSelfAssessmentControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  val recordId = 1

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[SelfAssessmentRegisteredView]
    val controller = new RegisteredForSelfAssessmentController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[SelfAssessmentRegisteredFormProvider],
      view = view,
      error = errorView
    )
  }

  val emptyCache = Cache.empty
  val personName = Some(PersonName("firstname", None, "lastname"))

  "RegisteredForSelfAssessmentController" when {

    "get is called" must {
      "load the page with an empty form" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName)))))

        val result = controller.get(recordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.getElementById("saRegistered-true").hasAttr("checked")  must be(false)
        document.getElementById("saRegistered-false").hasAttr("checked") must be(false)
      }

      "display the page with pre populated data" in new Fixture {

        val utr = "0123456789"

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(
            Future.successful(
              Some(Seq(ResponsiblePerson(personName = personName, saRegistered = Some(SaRegisteredYes(utr)))))
            )
          )

        val result = controller.get(recordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.getElementById("saRegistered-true").hasAttr("checked")  must be(true)
        document.getElementById("saRegistered-false").hasAttr("checked") must be(false)
        document.getElementById("utrNumber").`val`                       must be(utr)
      }

      "respond with NOT_FOUND when there is no responsiblePeople data" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(recordId)(request)
        status(result) must be(NOT_FOUND)
      }
    }

    "post is called" must {

      "respond with BAD_REQUEST when given invalid data" in new Fixture {
        val newRequest = FakeRequest(POST, routes.RegisteredForSelfAssessmentController.post(1).url)
          .withFormUrlEncodedBody(
            "saRegistered" -> "test"
          )
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName)))))

        val result = controller.post(recordId)(newRequest)
        status(result) must be(BAD_REQUEST)

      }

      "respond with NOT_FOUND when the index is out of bounds" in new Fixture {
        val newRequest = FakeRequest(POST, routes.RegisteredForSelfAssessmentController.post(1).url)
          .withFormUrlEncodedBody(
            "saRegistered" -> "true",
            "utrNumber"    -> "0123456789"
          )

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

        val result = controller.post(99)(newRequest)
        status(result) must be(NOT_FOUND)
      }

      "when edit is false" must {
        "redirect to the ExperienceTrainingController" in new Fixture {
          val newRequest = FakeRequest(POST, routes.RegisteredForSelfAssessmentController.post(1).url)
            .withFormUrlEncodedBody(
              "saRegistered" -> "true",
              "utrNumber"    -> "0123456789"
            )
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

          when(controller.dataCacheConnector.save[ResponsiblePerson](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(recordId, edit = false)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.ExperienceTrainingController.get(recordId).url))
        }
      }
      "when edit is true"  must {
        "on post with valid data in edit mode" in new Fixture {
          val newRequest = FakeRequest(POST, routes.RegisteredForSelfAssessmentController.post(1).url)
            .withFormUrlEncodedBody(
              "saRegistered" -> "true",
              "utrNumber"    -> "0123456789"
            )
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

          when(controller.dataCacheConnector.save[ResponsiblePerson](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(recordId, true, Some(flowFromDeclaration))(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(routes.DetailedAnswersController.get(recordId, Some(flowFromDeclaration)).url)
          )
        }
      }
    }
  }
}
