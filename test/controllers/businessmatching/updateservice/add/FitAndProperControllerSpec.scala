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

package controllers.businessmatching.updateservice.add

import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.flowmanagement.{AddBusinessTypeFlowModel, FitAndProperPageId}
import models.status.SubmissionDecisionApproved
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}

class FitAndProperControllerSpec extends AmlsSpec with MockitoSugar with ResponsiblePersonGenerator with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper = mock[AddBusinessTypeHelper]

    val controller = new FitAndProperController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector,
      router = createRouter[AddBusinessTypeFlowModel]
    )

    mockCacheFetch(Some(AddBusinessTypeFlowModel(Some(HighValueDealing))))
    mockApplicationStatus(SubmissionDecisionApproved)

  }

  //TODO Possibly need more tests

  "FitAndProperController" when {

    "get is called" must {
      "return OK with fit_and_proper view" in new Fixture {
        val result = controller.get()(request)

        status(result) must be(OK)

        contentAsString(result) must include(
          Messages("businessmatching.updateservice.fitandproper.heading")
        )
      }
    }

    "post is called" must {

      "with a valid request" must {
        "redirect" when {
          "request equals Yes" in new Fixture {

            mockCacheUpdate[AddBusinessTypeFlowModel](Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel())

            val result = controller.post()(request.withFormUrlEncodedBody(
              "passedFitAndProper" -> "true"
            ))

            status(result) mustBe SEE_OTHER

            controller.router.verify(FitAndProperPageId,
              AddBusinessTypeFlowModel(fitAndProper = Some(true), hasChanged = true))
          }

          "request equals No" when {
            "progress to the 'new service information' page" when {
              "an activity that generates a section has been chosen" in new Fixture {
                mockCacheUpdate[AddBusinessTypeFlowModel](Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel(Some(TrustAndCompanyServices)))

                val result = controller.post()(request.withFormUrlEncodedBody(
                  "passedFitAndProper" -> "false"
                ))

                status(result) mustBe SEE_OTHER

                controller.router.verify(FitAndProperPageId,
                  AddBusinessTypeFlowModel(Some(TrustAndCompanyServices), fitAndProper = Some(false), hasChanged = true))
              }
            }
          }
        }
      }

      "on invalid request" must {
        "return badRequest" in new Fixture {
          val result = controller.post()(request)

          status(result) mustBe BAD_REQUEST
        }
      }
    }
  }
}
