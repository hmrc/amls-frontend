/*
 * Copyright 2019 HM Revenue & Customs
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
import connectors.DataCacheConnector
import generators.businessmatching.BusinessMatchingGenerator
import models.ReadStatusResponse
import models.businessmatching._
import models.registrationprogress._
import models.status.{ReadyForRenewal, RenewalSubmitted}
import org.joda.time.{LocalDate, LocalDateTime}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.Helpers._
import services.{ProgressService, RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, AmlsSpec}
import cats.implicits._
import services.businessmatching.BusinessMatchingService
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class RenewalProgressControllerSpec extends AmlsSpec with BusinessMatchingGenerator {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val dataCacheConnector = mock[DataCacheConnector]
    val progressService = mock[ProgressService]
    val renewalService = mock[RenewalService]
    val statusService = mock[StatusService]
    val businessMatchingService = mock[BusinessMatchingService]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[ProgressService].to(progressService))
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .bindings(bind[RenewalService].to(renewalService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[StatusService].to(statusService))
      .overrides(bind[BusinessMatchingService].to(businessMatchingService))
      .build()

    val controller = app.injector.instanceOf[RenewalProgressController]

    val cacheMap = mock[CacheMap]

    val defaultSection = Section("A new section", NotStarted, hasChanged = false, mock[Call])

    val renewalSection = Section("renewal", NotStarted, hasChanged = false, mock[Call])

    when {
      dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cacheMap))

    when {
      progressService.sections(eqTo(cacheMap))
    } thenReturn Seq(defaultSection)

    when {
      renewalService.getSection(any(), any(), any())
    } thenReturn Future.successful(renewalSection)

    val businessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val bm = Some(businessMatchingGen.sample.get.copy(activities = Some(businessActivitiesModel)))

    when {
      cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn bm

    val renewalDate = LocalDate.now().plusDays(15)

    val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
      Some(renewalDate), false)

    when(businessMatchingService.getAdditionalBusinessActivities(any(), any(), any()))
      .thenReturn(OptionT.none[Future, Set[BusinessActivity]])

    when {
      progressService.sectionsFromBusinessActivities(any(), any())(any())
    } thenReturn Set(defaultSection)

  }

  "The Renewal Progress Controller" must {

    "load the page when status is ReadyForRenewal" in new Fixture {

      when(statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

      val BusinessActivitiesModelWithoutTCSPOrMSB = BusinessActivities(Set(TelephonePaymentService))
      val bmWithoutTCSPOrMSB = Some(BusinessMatching(activities = Some(businessActivitiesModel)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(bmWithoutTCSPOrMSB)

      val result = controller.get()(request)

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.select(".page-header").text() must include(Messages("renewal.progress.title"))

    }

    "redirect to the registration progress controller when status is renewal submitted" in new Fixture {

      when(statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful((RenewalSubmitted(Some(renewalDate)), Some(readStatusResponse))))

      val BusinessActivitiesModelWithoutTCSPOrMSB = BusinessActivities(Set(TelephonePaymentService))
      val bmWithoutTCSPOrMSB = Some(bm.get.copy(activities = Some(businessActivitiesModel)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(bmWithoutTCSPOrMSB)

      val result = controller.get()(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get.url)

    }

    "load the page when status is ReadyForRenewal and one of the section is modified" in new Fixture  {

      when(statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

      val BusinessActivitiesModelWithoutTCSPOrMSB = BusinessActivities(Set(TelephonePaymentService))
      val bmWithoutTCSPOrMSB = Some(bm.get.copy(activities = Some(businessActivitiesModel)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(bmWithoutTCSPOrMSB)

      val sections = Seq(
        Section("supervision", Completed, true,  controllers.supervision.routes.SummaryController.get()),
        Section("businessmatching", Completed, true,  controllers.businessmatching.routes.SummaryController.get())
      )

      when(controller.progressService.sections(cacheMap))
        .thenReturn(sections)

      when(controller.renewals.canSubmit(any(),any()))
        .thenReturn(true)

      val result = controller.get()(request)

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.select(".page-header").text() must include(Messages("renewal.progress.title"))
      html.select(".progress-step_changed").size() must be(1)
      html.select("button[name=submit]").hasAttr("disabled") must be(false)

      val elements = html.getElementsMatchingOwnText(Messages("progress.visuallyhidden.view.amend"))
      elements.size() must be(1)

    }

    "display all the available sections from a normal variation progress page" in new Fixture {
      when(statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

      val result = controller.get()(request)
      val html = Jsoup.parse(contentAsString(result))

      val element = html.select(".progress-step--details")
      element.text must include("A new section")
      element.size mustBe 1
    }

    "respond with InternalServerError when no sections are returned" in new Fixture {
      when(statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

      when {
        dataCacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(None)

      val result = controller.get()(request)
      status(result) mustBe 500

    }

  }

  "POST" must {
    "redirect to correct page" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()

      when(controller.progressService.getSubmitRedirect(any(), any(), any()))
        .thenReturn(Future.successful(Some(controllers.routes.FeeGuidanceController.get())))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.routes.FeeGuidanceController.get().url))
    }
  }

}
