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
import connectors.DataCacheConnector
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.businessmatching.updateservice.UpdateService
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AddMoreActivitiesControllerSpec extends GenericTestHelper with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {

    self => val request = addToken(authRequest)

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val authContext: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[BusinessMatchingService].to(mockBusinessMatchingService))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    mockCacheFetch[UpdateService](Some(UpdateService()), Some(UpdateService.key))
    mockCacheSave[UpdateService]

    val controller = app.injector.instanceOf[TradingPremisesController]

  }

  "AddMoreActivitiesController" when {

    "get is called" must {
      "return OK with trading_premises view" in new Fixture {


      }
      "return NOT_FOUND" when {
        "pre-submission" in new Fixture {

        }
        "there are no additional services" in new Fixture {

        }
      }
      "return INTERNAL_SERVER_ERROR if activites cannot be retrieved" in new Fixture {

      }
    }

    "post is called" must {

      "with a valid request" must {
        "redirect to WhichTradingPremises" when {
          "request equals Yes" in new Fixture {

          }
        }
        "when request equals No" when {
          "progress to the 'new service information' page" when {
            "fit and proper is not required" in new Fixture {

            }
          }

          "progress to the 'fit and proper' page" when {
            "fit and proper requirement is introduced" in new Fixture {

            }
          }
        }
        "redirect to TradingPremises" when {
          "request equals No" when {
            "there are more activities through which to iterate" in new Fixture {

            }
          }
        }
      }

      "on invalid request" must {

        "return badRequest" in new Fixture {

        }

      }

      "return NOT_FOUND" when {
        "status is pre-submission" in new Fixture {

        }
      }

      "return INTERNAL_SERVER_ERROR" when {

        "activities cannot be retrieved" in new Fixture {

        }

      }

    }
  }

}
