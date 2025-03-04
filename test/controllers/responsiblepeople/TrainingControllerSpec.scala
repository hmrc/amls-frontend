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

import config.ApplicationConfig
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.TrainingFormProvider
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{PersonName, ResponsiblePerson, TrainingNo, TrainingYes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils._
import views.html.responsiblepeople.TrainingView

import scala.concurrent.Future

class TrainingControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  val recordId = 1

  trait Fixture extends DependencyMocks { self =>
    val request = addToken(authRequest)

    lazy val mockApplicationConfig = mock[ApplicationConfig]

    lazy val controller = new TrainingController(
      messagesApi,
      mockCacheConnector,
      SuccessfulAuthAction,
      commonDependencies,
      mockMcc,
      inject[TrainingFormProvider],
      inject[TrainingView],
      errorView
    )

  }

  val emptyCache   = Cache.empty
  val mockCacheMap = mock[Cache]

  "TrainingController" when {

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {

      "display the page with pre populated data (training is set to Yes)" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(
            Future.successful(
              Some(Seq(ResponsiblePerson(personName = personName, training = Some(TrainingYes("test")))))
            )
          )

        val result = controller.get(recordId)(request)
        status(result) must be(OK)

        val page: Document = Jsoup.parse(contentAsString(result))
        page.getElementById("training-true").hasAttr("checked") must be(true)
        page.getElementById("information").`val`                must be("test")
      }

      "display the page with pre populated data (training is set to No)" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(
            Future.successful(Some(Seq(ResponsiblePerson(personName = personName, training = Some(TrainingNo)))))
          )

        val result = controller.get(recordId)(request)
        status(result) must be(OK)

        val page: Document = Jsoup.parse(contentAsString(result))
        page.getElementById("training-true").hasAttr("checked")  must be(false)
        page.getElementById("training-false").hasAttr("checked") must be(true)
      }

      "display the page without pre populated data" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = personName)))))

        val result = controller.get(recordId)(request)
        status(result) must be(OK)

        val page: Document = Jsoup.parse(contentAsString(result))
        page.getElementById("training-true").hasAttr("checked")  must be(false)
        page.getElementById("training-false").hasAttr("checked") must be(false)
        page.getElementById("information").`val`                 must be("")
      }

      "respond with NOT_FOUND when there is no personName" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

        val result = controller.get(recordId)(request)
        status(result) must be(NOT_FOUND)
      }
    }

    "post is called" when {
      "index is out of bounds, must respond with NOT_FOUND" in new Fixture {

        val newRequest = FakeRequest(POST, routes.TrainingController.post(1).url)
          .withFormUrlEncodedBody(
            "training"    -> "true",
            "information" -> "test"
          )

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(Some(Seq(ResponsiblePerson())))

        val result = controller.post(99)(newRequest)
        status(result) must be(NOT_FOUND)
      }

      "given valid data" when {
        "edit is false" must {
          "redirect to FitAndProperNoticeController" in new Fixture {
            val newRequest = FakeRequest(POST, routes.TrainingController.post(1).url)
              .withFormUrlEncodedBody(
                "training"    -> "true",
                "information" -> "I do not remember when I did the training"
              )

            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
              .thenReturn(Some(Seq(ResponsiblePerson())))

            when(controller.dataCacheConnector.save(any(), any(), any())(any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(recordId, false)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.FitAndProperNoticeController.get(recordId).url))
          }
        }
        "edit is true"  must {
          "redirect to DetailedAnswersController" in new Fixture {

            val newRequest = FakeRequest(POST, routes.TrainingController.post(1).url)
              .withFormUrlEncodedBody(
                "training"    -> "true",
                "information" -> "I do not remember when I did the training"
              )

            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(emptyCache)))

            val result = controller.post(recordId, true, Some(flowFromDeclaration))(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(routes.DetailedAnswersController.get(recordId, Some(flowFromDeclaration)).url)
            )
          }
        }
      }

      "given invalid data, must respond with BAD_REQUEST" in new Fixture {
        val newRequest = FakeRequest(POST, routes.TrainingController.post(1).url)
          .withFormUrlEncodedBody(
            "training" -> "not a boolean value"
          )
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = personName)))))

        val result = controller.post(recordId)(newRequest)
        status(result) must be(BAD_REQUEST)
      }
    }
  }
}
