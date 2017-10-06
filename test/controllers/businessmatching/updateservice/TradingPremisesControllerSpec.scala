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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.businessmatching.updateservice.{NewActivitiesAtTradingPremisesYes, UpdateService}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class TradingPremisesControllerSpec extends GenericTestHelper with BusinessMatchingGenerator {

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

  "TradingPremisesController" when {

    "get is called" must {
      "return OK with trading_premises view" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)

        when {
          controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(
          Messages(
            "businessmatching.updateservice.tradingpremises.header",
            Messages(s"businessmatching.registerservices.servicename.lbl.${BusinessActivities.getValue(HighValueDealing)}")
          ))
      }
      "return NOT_FOUND" when {
        "pre-submission" in new Fixture {

          mockApplicationStatus(NotCompleted)

          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

          val result = controller.get()(request)
          status(result) must be(NOT_FOUND)

        }
        "there are no additional services" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)

          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set.empty)

          val result = controller.get()(request)
          status(result) must be(NOT_FOUND)

        }
      }
      "return INTERNAL_SERVER_ERROR if activites cannot be retrieved" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)

        when {
          controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
        } thenReturn OptionT.none[Future, Set[BusinessActivity]]

        val result = controller.get()(request)
        status(result) must be(INTERNAL_SERVER_ERROR)

      }
    }

    "post is called" must {

      "on valid request" must {

        "redirect to WhichTradingPremises" when {
          "request equals Yes" in new Fixture {

            mockApplicationStatus(SubmissionDecisionApproved)

            when {
              controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
            } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

            val result = controller.post()(request.withFormUrlEncodedBody(
              "tradingPremisesNewActivities" -> "true",
              "businessActivities" -> "04"
            ))

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessmatching.updateservice.routes.WhichTradingPremisesController.get(0).url))

          }
        }
        "redirect to CurrentTradingPremises" when {
          "request equals No" when {
            "there are no more activities through which to iterate" in new Fixture {

              mockApplicationStatus(SubmissionDecisionApproved)

              when {
                controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
              } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

              val result = controller.post()(request.withFormUrlEncodedBody(
                "tradingPremisesNewActivities" -> "false"
              ))

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.businessmatching.updateservice.routes.CurrentTradingPremisesController.get().url))

            }
          }
        }
        "redirect to TradingPremises" when {
          "request equals No" when {
            "there are more activities through which to iterate" in new Fixture {

              mockApplicationStatus(SubmissionDecisionApproved)

              when {
                controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
              } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing, MoneyServiceBusiness))

              val result = controller.post()(request.withFormUrlEncodedBody(
                "tradingPremisesNewActivities" -> "false"
              ))

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.businessmatching.updateservice.routes.TradingPremisesController.get(1).url))

            }
          }
        }
      }

      "on invalid request" must {

        "return badRequest" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)

          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

          val result = controller.post()(request)

          status(result) must be(BAD_REQUEST)

        }

      }

      "return NOT_FOUND" when {
        "status is pre-submission" in new Fixture {

          mockApplicationStatus(NotCompleted)

          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set.empty)

          val result = controller.post()(request.withFormUrlEncodedBody(
            "tradingPremisesNewActivities" -> "false"
          ))

          status(result) must be(NOT_FOUND)

        }
        "there are no additional business activities" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)

          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set.empty)

          val result = controller.post(3)(request.withFormUrlEncodedBody(
            "tradingPremisesNewActivities" -> "false"
          ))

          status(result) must be(NOT_FOUND)

        }
      }

      "return INTERNAL_SERVER_ERROR" when {

        "activities cannot be retrieved" in new Fixture {
          mockApplicationStatus(SubmissionDecisionApproved)

          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.none[Future, Set[BusinessActivity]]

          val result = controller.post()(request.withFormUrlEncodedBody(
            "tradingPremisesNewActivities" -> "false"
          ))

          status(result) must be(INTERNAL_SERVER_ERROR)

        }

      }

    }
  }

  it must {
    "save result to s4l" in new Fixture {

      mockApplicationStatus(SubmissionDecisionApproved)

      when {
        controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
      } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

      val result = controller.post()(request.withFormUrlEncodedBody(
        "tradingPremisesNewActivities" -> "true",
        "businessActivities" -> "04"
      ))

      status(result) must be(SEE_OTHER)

      verify(
        controller.dataCacheConnector
      ).save[UpdateService](any(), eqTo(
        UpdateService(Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)))
      ))(any(),any(),any())
    }
  }
}
