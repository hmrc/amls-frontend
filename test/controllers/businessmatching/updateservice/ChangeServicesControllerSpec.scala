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

import models.businessmatching._
import models.businessmatching.updateservice.{ChangeServices, ChangeServicesAdd}
import models.flowmanagement.ChangeServicesPageId
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.Future

class ChangeServicesControllerSpec extends GenericTestHelper with MockitoSugar {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    val bmService = mock[BusinessMatchingService]

    val controller = new ChangeServicesController(
      self.authConnector,
      mockCacheConnector,
      bmService,
      createRouter[ChangeServices]
    )

    val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val bm = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

    val bmEmpty = Some(BusinessMatching())

    mockCacheGetEntry[BusinessMatching](Some(bm), BusinessMatching.key)

    when {
      bmService.preApplicationComplete(any(), any(), any())
    } thenReturn Future.successful(false)

  }

  "ChangeServicesController" when {

    "get is called" must {
      "return OK with change_services view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.changeservices.title"))

      }

      "return OK with change_services view - no activities" in new Fixture {
        mockCacheGetEntry[BusinessMatching](Some(bmEmpty), BusinessMatching.key)

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.changeservices.title"))

      }
    }

    "post is called" must {
      "redirect to RegisterServicesController" when {
        "request is add" in new Fixture {

          val result = controller.post()(request.withFormUrlEncodedBody("changeServices" -> "add"))

          status(result) must be(SEE_OTHER)
          controller.router.verify(ChangeServicesPageId, ChangeServicesAdd)
        }
      }

      "return BAD_REQUEST" when {
        "request is invalid" in new Fixture {
          val result = controller.post()(request)
          status(result) must be(BAD_REQUEST)
        }
      }

      "return Internal Server Error if the business matching model can't be obtained" in new Fixture {

        val postRequest = request.withFormUrlEncodedBody()

        mockCacheGetEntry[BusinessMatching](None, BusinessMatching.key)

        val result = controller.post()(postRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

}