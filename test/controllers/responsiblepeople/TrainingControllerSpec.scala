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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching._
import models.responsiblepeople.ResponsiblePeople._
import models.responsiblepeople.{PersonName, ResponsiblePeople, TrainingNo, TrainingYes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import org.mockito.Matchers.{eq => meq, _}

import scala.concurrent.Future

class TrainingControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  val recordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new TrainingController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)
  val mockCacheMap = mock[CacheMap]

  "TrainingController" when {

    val pageTitle = Messages("responsiblepeople.training.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname"))



    "get is called" must {

      "display the page with pre populated data (training is set to Yes)" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName, training = Some(TrainingYes("test")))))))

        val result = controller.get(recordId)(request)
        status(result) must be(OK)

        val page: Document = Jsoup.parse(contentAsString(result))
        page.getElementById("training-true").hasAttr("checked") must be(true)
        page.getElementById("information").`val` must be("test")
      }

      "display the page with pre populated data (training is set to No)" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName, training = Some(TrainingNo))))))

        val result = controller.get(recordId)(request)
        status(result) must be(OK)

        val page: Document = Jsoup.parse(contentAsString(result))
        page.getElementById("training-true").hasAttr("checked") must be(false)
        page.getElementById("training-false").hasAttr("checked") must be(true)
      }

      "display the page without pre populated data" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName)))))

        val result = controller.get(recordId)(request)
        status(result) must be(OK)

        val page: Document = Jsoup.parse(contentAsString(result))
        page.getElementById("training-true").hasAttr("checked") must be(false)
        page.getElementById("training-false").hasAttr("checked") must be(false)
        page.getElementById("information").`val` must be("")
      }

      "respond with NOT_FOUND when there is no personName" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

        val result = controller.get(recordId)(request)
        status(result) must be(NOT_FOUND)
      }
    }

    "post is called" when {
      "index is out of bounds, must respond with NOT_FOUND" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "training" -> "true",
          "information" -> "test"
        )

        when(controller.dataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(Some(Seq(ResponsiblePeople())))

        val result = controller.post(99)(newRequest)
        status(result) must be(NOT_FOUND)
      }

      "given valid data" when {
        "there is no cache data, must redirect to the PersonRegisteredController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "training" -> "true",
            "information" -> "test"
          )

          when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.post(recordId, false)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.PersonRegisteredController.get(recordId).url))
        }

        "edit is false" must {
          "redirect to PersonRegisteredController when training is yes" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "training" -> "true",
              "information" -> "test"
            )

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(emptyCache)))

            val result = controller.post(recordId, false)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PersonRegisteredController.get(recordId).url))
          }

          "redirect to FitAndProperController when businessActivities includes TrustAndCompanyServices" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "training" -> "true",
              "information" -> "I do not remember when I did the training"
            )

            val testCacheMap = CacheMap("", Map(

            ))

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))
            when(mockCacheMap.getEntry[BusinessMatching](meq(BusinessMatching.key))(any()))
              .thenReturn(Some(
                BusinessMatching(activities = Some(BusinessActivities(Set(TrustAndCompanyServices,HighValueDealing))))
              ))
            when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
              .thenReturn(Some(Seq(ResponsiblePeople())))

            val result = controller.post(recordId, false)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.FitAndProperController.get(recordId).url))
          }
          "redirect to FitAndProperController when businessActivities includes MoneyServiceBusiness" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "training" -> "true",
              "information" -> "I do not remember when I did the training"
            )

            val testCacheMap = CacheMap("", Map(

            ))

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))
            when(mockCacheMap.getEntry[BusinessMatching](meq(BusinessMatching.key))(any()))
              .thenReturn(Some(
                BusinessMatching(activities = Some(BusinessActivities(Set(MoneyServiceBusiness,HighValueDealing))))
              ))
            when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
              .thenReturn(Some(Seq(ResponsiblePeople())))

            val result = controller.post(recordId, false)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.FitAndProperController.get(recordId).url))
          }
        }
        "edit is true" must {
          "on post with valid data in edit mode" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "training" -> "true",
              "information" -> "I do not remember when I did the training"
            )

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(emptyCache)))

            val result = controller.post(recordId, true, Some(flowFromDeclaration))(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(recordId, true, Some(flowFromDeclaration)).url))
          }


        }
      }

      "given invalid data, must respond with BAD_REQUEST" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "training" -> "not a boolean value"
        )
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName)))))

        val result = controller.post(recordId)(newRequest)
        status(result) must be(BAD_REQUEST)
      }



    }
  }

  it must {
    "use correct services" in new Fixture {
      TrainingController.authConnector must be(AMLSAuthConnector)
      TrainingController.dataCacheConnector must be(DataCacheConnector)
    }
  }
}
