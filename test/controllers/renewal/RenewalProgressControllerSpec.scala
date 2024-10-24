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

package controllers.renewal

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import generators.businessmatching.BusinessMatchingGenerator
import models.ReadStatusResponse
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.registrationprogress._
import models.responsiblepeople.{ResponsiblePeopleValues, ResponsiblePerson}
import models.status.{ReadyForRenewal, RenewalSubmitted}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.cache.Cache
import services.{ProgressService, RenewalService, SectionsProvider, StatusService}
import utils.{AmlsSpec, AuthAction}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class RenewalProgressControllerSpec extends AmlsSpec with BusinessMatchingGenerator with ResponsiblePeopleValues {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val dataCacheConnector = mock[DataCacheConnector]
    val progressService = mock[ProgressService]
    val renewalService = mock[RenewalService]
    val statusService = mock[StatusService]
    val sectionsProvider = mock[SectionsProvider]
    val businessMatchingService = mock[BusinessMatchingService]
    val authAction = SuccessfulAuthAction

    lazy val app = new GuiceApplicationBuilder()
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .overrides(bind[ProgressService].to(progressService))
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .bindings(bind[RenewalService].to(renewalService))
      .overrides(bind[StatusService].to(statusService))
      .overrides(bind[SectionsProvider].to(sectionsProvider))
      .overrides(bind[BusinessMatchingService].to(businessMatchingService))
      .overrides(bind[AuthAction].to(authAction))
      .build()

    val controller = app.injector.instanceOf[RenewalProgressController]

    val cacheMap = mock[Cache]

    val defaultTaskRow = TaskRow("A new section", "/foo", false, NotStarted, TaskRow.notStartedTag)

    val renewalTaskRow = defaultTaskRow.copy(msgKey = "renewal")

    when {
      dataCacheConnector.fetchAll(any())
    } thenReturn Future.successful(Some(cacheMap))

    when {
      sectionsProvider.taskRowsForRenewal(eqTo(cacheMap))(any())
    } thenReturn Seq(defaultTaskRow)

    when {
      renewalService.getTaskRow(any())(any())
    } thenReturn Future.successful(renewalTaskRow)

    val businessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val bm = Some(businessMatchingGen.sample.get.copy(activities = Some(businessActivitiesModel)))

    when {
      cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn bm

    val renewalDate = LocalDate.now().plusDays(15)

    val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
      Some(renewalDate), false)

    when(businessMatchingService.getAdditionalBusinessActivities(any())(any()))
      .thenReturn(OptionT.none[Future, Set[BusinessActivity]])

    when {
      sectionsProvider.taskRowsFromBusinessActivities(any(), any())(any(), any())
    } thenReturn Seq(defaultTaskRow)

    when(cacheMap.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any()))
      .thenReturn(Some(Seq(completeResponsiblePerson)))
  }

  "The Renewal Progress Controller" must {

    "load the page when status is ReadyForRenewal" in new Fixture {

      when(statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

      val bmWithoutTCSPOrMSB = Some(BusinessMatching(activities = Some(businessActivitiesModel)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(bmWithoutTCSPOrMSB)

      val result = controller.get()(request)

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.getElementsByTag("h1").text() must include(messages("renewal.progress.title"))
    }

    "redirect to the registration progress controller when status is renewal submitted" in new Fixture {

      when(statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful((RenewalSubmitted(Some(renewalDate)), Some(readStatusResponse))))

      val bmWithoutTCSPOrMSB = Some(bm.get.copy(activities = Some(businessActivitiesModel)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(bmWithoutTCSPOrMSB)

      val result = controller.get()(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)

    }

    "load the page when status is ReadyForRenewal and one of the section is modified" in new Fixture  {

      when(statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

      val bmWithoutTCSPOrMSB = Some(bm.get.copy(activities = Some(businessActivitiesModel)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(bmWithoutTCSPOrMSB)

      val sections = Seq(
        TaskRow("supervision", controllers.supervision.routes.SummaryController.get().url, false, Completed, TaskRow.completedTag),
        TaskRow("asp", controllers.asp.routes.SummaryController.get.url, true, Completed, TaskRow.updatedTag)
      )

      when(controller.sectionsProvider.taskRowsForRenewal(eqTo(cacheMap))(any()))
        .thenReturn(sections)

      when(controller.renewals.canSubmit(any(),any()))
        .thenReturn(true)

      val result = controller.get()(request)

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.getElementsByTag("h1").text() must include(messages("renewal.progress.title"))
      html.select("button[name=submit]").hasAttr("disabled") must be(false)

      val elements = html.getElementsMatchingOwnText(messages("progress.visuallyhidden.view.updated"))
      elements.size() must be(1)

    }

    "display all the available sections from a normal variation progress page" in new Fixture {
      when(statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

      status(controller.get()(request)) mustBe OK
    }

    "respond with InternalServerError when no sections are returned" in new Fixture {
      when(statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

      when {
        dataCacheConnector.fetchAll(any())
      } thenReturn Future.successful(None)

      val result = controller.get()(request)
      status(result) mustBe 500

    }

  }

  "POST" must {
    "redirect to correct page" in new Fixture {

      val newRequest = FakeRequest(POST, routes.RenewalProgressController.post().url).withFormUrlEncodedBody("" -> "")

      when(controller.progressService.getSubmitRedirect(any[Option[String]](), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(controllers.declaration.routes.WhoIsRegisteringController.get)))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
    }
  }
}
