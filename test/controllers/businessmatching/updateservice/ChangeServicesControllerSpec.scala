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

import connectors.DataCacheConnector
import generators.ResponsiblePersonGenerator
import models.businessmatching._
import models.responsiblepeople.ResponsiblePeople
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.{ExecutionContext, Future}

class ChangeServicesControllerSpec extends GenericTestHelper with MockitoSugar{

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val authContext: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[ChangeServicesController]

    val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val bm = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

    when(mockCacheConnector.fetchAll(any(), any()))
      .thenReturn(Future.successful(Some(mockCacheMap)))

    when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
      .thenReturn(bm)


  }

  "ChangeServicesController" when {

    "get is called" must {
      "return OK with change_services view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("changeservices.title"))

      }

      "return OK with change_services view - no activities" in new Fixture {

        val bmEmpty = Some(BusinessMatching())

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(bmEmpty)


        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("changeservices.title"))

      }
    }

    "post is called" must {
      "redirect to WhichFitAndProperController" when {
        "request is false" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody("changeServices" -> "add"))

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessmatching.routes.RegisterServicesController.get().url))
        }
      }

      "return BAD_REQUEST" when {
        "request is invalid" in new Fixture {

          val result = controller.post()(request)

          status(result) must be(BAD_REQUEST)

        }
      }
    }

  }

}
