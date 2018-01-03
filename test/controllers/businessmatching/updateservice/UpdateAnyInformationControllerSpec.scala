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
import models.businessmatching.updateservice.UpdateService
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.Future

class UpdateAnyInformationControllerSpec extends GenericTestHelper {

  trait TestFixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(self.authRequest)

    val statusService = mock[StatusService]

    when {
      controller.statusService.isPreSubmission(any(),any(),any())
    } thenReturn Future.successful(false)

    lazy val controller = new UpdateAnyInformationController(
      mockCacheConnector,
      self.authConnector,
      statusService
    )

    mockCacheFetch(Some(UpdateService(inNewServiceFlow = true)))
    mockCacheSave[UpdateService]
  }

  "UpdateAnyInformationController" when {
    "get is called" must {
      "respond with OK with update_any_information" in new TestFixture {
        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include(Messages("updateanyinformation.title"))

        verify(mockCacheConnector).save[UpdateService](
          eqTo(UpdateService.key),
          eqTo(UpdateService(inNewServiceFlow = false)))(any(), any(), any())
      }

      "respond with NOT_FOUND" when {
        "status is pre-submission" in new TestFixture {

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
        "yes is selected" in new TestFixture {

          val result = controller.post()(request.withFormUrlEncodedBody(
            "updateAnyInformation" -> "true"
          ))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)

        }
      }
      "redirect to WhoIsRegisteringController" when {
        "no is selected" in new TestFixture {

          val result = controller.post()(request.withFormUrlEncodedBody(
            "updateAnyInformation" -> "false"
          ))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.declaration.routes.WhoIsRegisteringController.get().url)

        }
      }
      "respond with BAD_REQUEST" when {
        "an invalid form is submitted" in new TestFixture {

          val result = controller.post()(request.withFormUrlEncodedBody(
            "updateAnyInformation" -> ""
          ))

          status(result) mustBe BAD_REQUEST

        }
      }
    }

  }
}
