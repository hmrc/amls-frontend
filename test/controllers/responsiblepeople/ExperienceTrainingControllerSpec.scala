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

package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, InvolvedInOtherYes}
import models.responsiblepeople.{ExperienceTrainingNo, ExperienceTrainingYes, PersonName, ResponsiblePeople}
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.responsiblepeople.ResponsiblePeople._
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
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class ExperienceTrainingControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ExperienceTrainingController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "ExperienceTrainingController" must {

    val pageTitle = Messages("responsiblepeople.experiencetraining.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")

    val personName = Some(PersonName("firstname", None, "lastname", None, None))

      "use correct services" in new Fixture {
        ExperienceTrainingController.authConnector must be(AMLSAuthConnector)
        ExperienceTrainingController.dataCacheConnector must be(DataCacheConnector)
      }

    "get" must {

      "load the page with the business activities" in new Fixture {

        val mockCacheMap = mock[CacheMap]

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName, experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training")))))))

        val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))
        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(Some(businessActivities))

        val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

        val newRequest = request.withFormUrlEncodedBody("activities[0]" -> "01")

        val RecordId = 1
        val result = controller.get(RecordId)(request)
        status(result) must be(OK)
        contentAsString(result) must include(AccountancyServices.getMessage)
        contentAsString(result) must include(BillPaymentServices.getMessage)
        contentAsString(result) must include(EstateAgentBusinessService.getMessage)
        contentAsString(result) must include(Messages("responsiblepeople.experiencetraining.title"))
      }
    }

    "on get display the page with pre populated data for the Yes Option" in new Fixture {

      val mockCacheMap = mock[CacheMap]

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))
      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))


      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName,experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training")))))))

      val result = controller.get(RecordId)(request)

      status(result) must be(OK)

      contentAsString(result) must include ("I do not remember when I did the training")
    }


    "on get display the page with pre populated data with No Data for the information" in new Fixture {

      val mockCacheMap = mock[CacheMap]

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))
      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))


      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName, experienceTraining = Some(ExperienceTrainingNo))))))
      val result = controller.get(RecordId)(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))

      contentAsString(result) must not include "I do not remember when I did the training"
      document.select("input[name=experienceTraining][value=true]").hasAttr("checked") must be(false)
      document.select("input[name=experienceTraining][value=false]").hasAttr("checked") must be(true)
    }


    "on post with valid data and training selected yes" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "experienceTraining" -> "true",
        "experienceInformation" -> "I do not remember when I did the training"
      )


      val mockCacheMap = mock[CacheMap]

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))
      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName, experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training")))))))

      when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.TrainingController.get(RecordId).url))
    }

    "on post with valid data and training selected no" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "experienceTraining" -> "false"
      )


      val mockCacheMap = mock[CacheMap]

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))
      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName, experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training")))))))

      when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.TrainingController.get(RecordId).url))
    }

    "on post with invalid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "experienceTraining" -> "not a boolean value"
      )
      val mockCacheMap = mock[CacheMap]

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))
      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName, experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training")))))))

      val result = controller.post(RecordId)(newRequest)

      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title must be(pageTitle)
    }


    "on post with valid data in edit mode" in new Fixture {

      val mockCacheMap = mock[CacheMap]

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessActivities = BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))
      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(businessActivities))

      val businessMatchingActivities = BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val newRequest = request.withFormUrlEncodedBody(
        "experienceTraining" -> "true",
        "experienceInformation" -> "I do not remember when I did the training"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(experienceTraining = Some(ExperienceTrainingYes("I do not remember when I did the training")))))))

      when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(RecordId, true, Some(flowFromDeclaration))(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, true, Some(flowFromDeclaration)).url))
    }
  }
}
