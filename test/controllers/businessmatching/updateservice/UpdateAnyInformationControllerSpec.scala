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

package controllers.businessmatching.updateservice

import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.add.UpdateServicesSummaryController
import models.businessmatching.updateservice.UpdateService
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}
import views.Fixture
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class UpdateAnyInformationControllerSpec extends AmlsSpec {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    val mockBusinessMatchingService = mock[BusinessMatchingService]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[BusinessMatchingService].to(mockBusinessMatchingService))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[UpdateAnyInformationController]

    mockCacheFetch(Some(UpdateService(inNewServiceFlow = true)))
    mockCacheSave[UpdateService]

    when {
      controller.statusService.isPreSubmission(any(),any(),any())
    } thenReturn Future.successful(false)
  }

  "UpdateAnyInformationController" when {

    "get is called" must {
      "return OK with change_services view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("summary.updateinformation"))

      }

      "respond with OK with update_any_information" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include(Messages("summary.updateinformation"))

        verify(mockCacheConnector).save[UpdateService](
          eqTo(UpdateService.key),
          eqTo(UpdateService(inNewServiceFlow = false)))(any(), any(), any())
      }

      "respond with NOT_FOUND" when {
        "status is pre-submission" in new Fixture {

          when {
            controller.statusService.isPreSubmission(any(),any(),any())
          } thenReturn Future.successful(true)

          val result = controller.get()(request)

          status(result) mustBe NOT_FOUND

        }
      }
    }

    "post is called" must {
      "redirect to RegistrationProgressController" when {
        "yes is selected" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody(
            "updateAnyInformation" -> "true"
          ))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)

        }
      }
      "redirect to WhoIsRegisteringController" when {
        "no is selected" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody(
            "updateAnyInformation" -> "false"
          ))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.declaration.routes.WhoIsRegisteringController.get().url)

        }
      }
      "respond with BAD_REQUEST" when {
        "an invalid form is submitted" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody(
            "updateAnyInformation" -> ""
          ))

          status(result) mustBe BAD_REQUEST

        }
      }
    }

  }
}
