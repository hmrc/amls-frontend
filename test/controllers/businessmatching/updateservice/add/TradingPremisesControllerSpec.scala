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

import cats.data.OptionT
import cats.implicits._
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.flowmanagement.AddServiceFlowModel
import models.status.SubmissionDecisionApproved
import org.mockito.Matchers.{eq => eqTo, any}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class TradingPremisesControllerSpec extends GenericTestHelper with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val authContext: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    mockCacheFetch(Some(AddServiceFlowModel(Some(HighValueDealing))))
    mockApplicationStatus(SubmissionDecisionApproved)

    val controller = new TradingPremisesController(
      self.authConnector,
      mockCacheConnector,
      mockStatusService
    )
  }

  "TradingPremisesController" when {

    "get is called" must {
      "return OK with trading_premises view" in new Fixture {
        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(
          Messages(
            "businessmatching.updateservice.tradingpremises.header",
            Messages(s"businessmatching.registerservices.servicename.lbl.${BusinessActivities.getValue(HighValueDealing)}")
          ))
      }
    }

    "post is called" must {

      "with a valid request" must {
        "redirect" when {
          "request equals Yes" in new Fixture {

            mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key), AddServiceFlowModel())

            val result = controller.post()(request.withFormUrlEncodedBody(
              "tradingPremisesNewActivities" -> "true"
            ))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.WhichTradingPremisesController.get(0).url)
          }
        }

        "when request equals No" when {
          "progress to the 'new service information' page" when {
            "an activity that generates a section has been chosen" in new Fixture {
              mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key), AddServiceFlowModel(Some(HighValueDealing)))

              val result = controller.post()(request.withFormUrlEncodedBody(
                "tradingPremisesNewActivities" -> "false"
              ))

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(routes.NewServiceInformationController.get().url)
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
