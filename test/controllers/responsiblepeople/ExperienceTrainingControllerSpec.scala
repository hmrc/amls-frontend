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
import forms.responsiblepeople.ExperienceTrainingFormProvider
import models.businessactivities.{BusinessActivities, InvolvedInOtherYes}
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{ExperienceTrainingNo, ExperienceTrainingYes, PersonName, ResponsiblePerson}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.businessmatching.RecoverActivitiesService
import services.cache.Cache
import utils.AmlsSpec
import views.html.responsiblepeople.ExperienceTrainingView

import scala.concurrent.Future

class ExperienceTrainingControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  val RecordId = 1

  def getMessage(service: BusinessActivity): String =
    messages("businessactivities.registerservices.servicename.lbl." + BusinessMatchingActivities.getValue(service))

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val dataCacheConnector = mock[DataCacheConnector]
    lazy val view          = inject[ExperienceTrainingView]
    val controller         = new ExperienceTrainingController(
      dataCacheConnector = dataCacheConnector,
      recoverActivitiesService = mock[RecoverActivitiesService],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[ExperienceTrainingFormProvider],
      view = view,
      error = errorView
    )
  }

  val emptyCache: Cache   = Cache.empty
  val mockCacheMap: Cache = mock[Cache]

  "ExperienceTrainingController" must {

    val pageTitle = messages("responsiblepeople.experiencetraining.title", "firstname lastname") + " - " +
      messages("summary.responsiblepeople") + " - " +
      messages("title.amls") + " - " + messages("title.gov")

    val personName = Some(PersonName("firstname", None, "lastname"))

    "on get load the page with the business activities" in new Fixture {

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())).thenReturn(
        Future.successful(
          Some(
            Seq(
              ResponsiblePerson(
                personName = personName,
                experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training"))
              )
            )
          )
        )
      )

      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))
      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      val businessMatchingActivities =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val RecordId = 1
      val result   = controller.get(RecordId)(request)
      status(result) must be(OK)

      contentAsString(result) must include(getMessage(AccountancyServices))
      contentAsString(result) must include(getMessage(BillPaymentServices))
      contentAsString(result) must include(getMessage(EstateAgentBusinessService))

      contentAsString(result) must include(messages("responsiblepeople.experiencetraining.title"))
    }

    "on get display the page with pre populated data for the Yes Option" in new Fixture {

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessMatchingActivities =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())).thenReturn(
        Future.successful(
          Some(
            Seq(
              ResponsiblePerson(
                personName = personName,
                experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training"))
              )
            )
          )
        )
      )

      val result = controller.get(RecordId)(request)

      status(result) must be(OK)

      contentAsString(result) must include("I do not remember when I did the training")
    }

    "on get display the page with pre populated data with No Data for the information" in new Fixture {

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessMatchingActivities =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())).thenReturn(
        Future.successful(
          Some(Seq(ResponsiblePerson(personName = personName, experienceTraining = Some(ExperienceTrainingNo))))
        )
      )
      val result = controller.get(RecordId)(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))

      contentAsString(result)                                                           must not include "I do not remember when I did the training"
      document.select("input[name=experienceTraining][value=true]").hasAttr("checked")  must be(false)
      document.select("input[name=experienceTraining][value=false]").hasAttr("checked") must be(true)
    }

    "on post with valid data and training selected yes" in new Fixture {
      val newRequest = FakeRequest(POST, routes.ExperienceTrainingController.post(1).url)
        .withFormUrlEncodedBody(
          "experienceTraining"    -> "true",
          "experienceInformation" -> "I do not remember when I did the training"
        )

      val mockCacheMap = mock[Cache]

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessMatchingActivities =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())).thenReturn(
        Future.successful(
          Some(
            Seq(
              ResponsiblePerson(
                personName = personName,
                experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training"))
              )
            )
          )
        )
      )

      when(controller.dataCacheConnector.save[ResponsiblePerson](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(RecordId)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.TrainingController.get(RecordId).url))
    }

    "on post with valid data and training selected no" in new Fixture {
      val newRequest = FakeRequest(POST, routes.ExperienceTrainingController.post(1).url)
        .withFormUrlEncodedBody(
          "experienceTraining" -> "false"
        )

      val mockCacheMap = mock[Cache]

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessMatchingActivities =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())).thenReturn(
        Future.successful(
          Some(
            Seq(
              ResponsiblePerson(
                personName = personName,
                experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training"))
              )
            )
          )
        )
      )

      when(controller.dataCacheConnector.save[ResponsiblePerson](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(RecordId)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.TrainingController.get(RecordId).url))
    }

    "on post with invalid data" in new Fixture {
      val newRequest   = FakeRequest(POST, routes.ExperienceTrainingController.post(1).url)
        .withFormUrlEncodedBody(
          "experienceTraining" -> "not a boolean value"
        )
      val mockCacheMap = mock[Cache]

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessMatchingActivities =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())).thenReturn(
        Future.successful(
          Some(
            Seq(
              ResponsiblePerson(
                personName = personName,
                experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training"))
              )
            )
          )
        )
      )

      val result = controller.post(RecordId)(newRequest)

      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title must be(s"Error: $pageTitle")
    }

    "on post with valid data in edit mode" in new Fixture {

      val mockCacheMap = mock[Cache]

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessMatchingActivities =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val newRequest = FakeRequest(POST, routes.ExperienceTrainingController.post(1).url)
        .withFormUrlEncodedBody(
          "experienceTraining"    -> "true",
          "experienceInformation" -> "I do not remember when I did the training"
        )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())).thenReturn(
        Future.successful(
          Some(
            Seq(
              ResponsiblePerson(experienceTraining =
                Some(ExperienceTrainingYes("I do not remember when I did the training"))
              )
            )
          )
        )
      )

      when(controller.dataCacheConnector.save[ResponsiblePerson](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(RecordId, true, Some(flowFromDeclaration))(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(
        Some(routes.DetailedAnswersController.get(RecordId, Some(flowFromDeclaration)).url)
      )
    }
  }
}
