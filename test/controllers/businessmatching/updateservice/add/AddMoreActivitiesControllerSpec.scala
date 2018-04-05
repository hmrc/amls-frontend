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
import controllers.businessmatching.updateservice.ChangeServicesController
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AddMoreActivitiesControllerSpec extends GenericTestHelper with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val bmService = mock[BusinessMatchingService]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[BusinessMatchingService].to(bmService))
      .build()

    val controller = app.injector.instanceOf[AddMoreActivitiesController]

    val BusinessActivitiesModel = BusinessActivities(Set(BillPaymentServices, TelephonePaymentService))
    val bm = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

    val bmEmpty = Some(BusinessMatching())

    mockCacheGetEntry[BusinessMatching](Some(bm), BusinessMatching.key)

    when {
      bmService.preApplicationComplete(any(), any(), any())
    } thenReturn Future.successful(false)

  }

  "AddMoreActivitiesController" when {

    "get is called" must {
      "return OK with add_more_activities view" in new Fixture {
        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.addmoreactivities.title"))

      }

      "return INTERNAL_SERVER_ERROR if activites cannot be retrieved" in new Fixture {
fail()
      }

      "return OK and display existing submitted Activities" in new Fixture {
        fail()
      }

      "return OK and display existing newly added Activities" in new Fixture {
        fail()
      }
    }

//    "post is called" must {
//
//      "with a valid request" must {
//        "redirect to WhichTradingPremises" when {
//          "request equals Yes" in new Fixture {
//
//          }
//        }
//        "when request equals No" when {
//          "progress to the 'new service information' page" when {
//            "fit and proper is not required" in new Fixture {
//
//            }
//          }
//
//          "progress to the 'fit and proper' page" when {
//            "fit and proper requirement is introduced" in new Fixture {
//
//            }
//          }
//        }
//        "redirect to TradingPremises" when {
//          "request equals No" when {
//            "there are more activities through which to iterate" in new Fixture {
//
//            }
//          }
//        }
//      }
//
//      "on invalid request" must {
//
//        "return badRequest" in new Fixture {
//
//        }
//
//      }
//
//      "return NOT_FOUND" when {
//        "status is pre-submission" in new Fixture {
//
//        }
//      }
//
//      "return INTERNAL_SERVER_ERROR" when {
//
//        "activities cannot be retrieved" in new Fixture {
//
//        }
//
//      }
//
//    }
  }

}
