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

package controllers.renewal

import connectors.DataCacheConnector
import models.registrationprogress.{Completed, Section}
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{ProgressService, RenewalService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.{ExecutionContext, Future}

class UpdateAnyInformationControllerSpec extends GenericTestHelper {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(self.authRequest)

    val cacheMap = CacheMap("", Map.empty)

    val renewalSection = Section("renewal", Completed, true, controllers.renewal.routes.SummaryController.get())

    val dataCacheConnector = mock[DataCacheConnector]
    val renewalService = mock[RenewalService]
    val progressService = mock[ProgressService]

    lazy val controller = new UpdateAnyInformationController(
      self.authConnector, dataCacheConnector, renewalService, progressService
    )

    when {
      controller.dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cacheMap))

    when {
      controller.renewalService.getSection(any(), any(), any())
    } thenReturn Future.successful(renewalSection)

    when {
      controller.progressService.sections(cacheMap)
    } thenReturn Seq.empty[Section]

  }

  "UpdateAnyInformationController" when {
    "get is called" must {

      "respond with OK with update_any_information" in new TestFixture {

        when {
          renewalService.canSubmit(renewalSection, Seq.empty[Section])
        } thenReturn true

        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include(Messages("renewal.updateanyinformation.title"))
      }

      "respond with NOT_FOUND" when {
        "renewal has not been submitted" in new TestFixture {

          when {
            renewalService.canSubmit(renewalSection, Seq.empty[Section])
          } thenReturn false

          val result = controller.get()(request)

          status(result) mustBe NOT_FOUND

        }
      }

    }

  }
}
