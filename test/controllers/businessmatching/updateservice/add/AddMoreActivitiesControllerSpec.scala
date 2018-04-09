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

import connectors.DataCacheConnector
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.flowmanagement.AddServiceFlowModel
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
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

import scala.concurrent.{ExecutionContext, Future}

class AddMoreActivitiesControllerSpec extends GenericTestHelper with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    implicit val authContext: AuthContext = mockAuthContext
    implicit val ec: ExecutionContext = mockExecutionContext

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[BusinessMatchingService].to(mockBusinessMatchingService))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[AddMoreActivitiesController]

    val BusinessActivitiesModel = BusinessActivities(Set(BillPaymentServices, TelephonePaymentService))
    val bm = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

    val bmEmpty = Some(BusinessMatching())

    mockCacheGetEntry[BusinessMatching](Some(bm), BusinessMatching.key)

    when {
      mockBusinessMatchingService.preApplicationComplete(any(), any(), any())
    } thenReturn Future.successful(false)

  }

  "AddMoreActivitiesController" when {

    "get is called" must {
      "return OK with add_more_activities view" in new Fixture {
        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.addmoreactivities.title"))
      }
    }

    "post is called" must {
      "with a valid request" must {
        "progress to the 'select Activivtes' page" when {
          "request equals Yes" in new Fixture {

            mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key), AddServiceFlowModel())

            val result = controller.post()(request.withFormUrlEncodedBody(
              "addmoreactivities" -> "true"
            ))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.SelectActivitiesController.get().url)
          }
        }

        "when request equals 'No'" must {
          " progress to the 'registration progress' page " when {
            "if no activity that generates a section has been chosen" when {

              "an activity that generates a section has been chosen" in new Fixture {
                mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key), AddServiceFlowModel(Some(BillPaymentServices)))

                val result = controller.post()(request.withFormUrlEncodedBody(
                  "addmoreactivities" -> "false"
                ))

                status(result) mustBe SEE_OTHER
                redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
              }
            }
          }

          " progress to the 'registration progress' page " when {
            "if an activity that generates a section has been chosen" when {

              "an activity that generates a section has been chosen" in new Fixture {
                mockCacheUpdate[AddServiceFlowModel](Some(AddServiceFlowModel.key), AddServiceFlowModel(Some(HighValueDealing)))

                val result = controller.post()(request.withFormUrlEncodedBody(
                  "addmoreactivities" -> "false"
                ))

                status(result) mustBe SEE_OTHER
                redirectLocation(result) mustBe Some(routes.NewServiceInformationController.get().url)
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
