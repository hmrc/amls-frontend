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
import controllers.businessmatching.updateservice.UpdateServiceHelper
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.flowmanagement.AddServiceFlowModel
import models.moneyservicebusiness.MoneyServiceBusinessTestData
import models.status.SubmissionDecisionApproved
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatDoYouDoHereControllerSpec extends GenericTestHelper with ScalaFutures with MockitoSugar with MoneyServiceBusinessTestData with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper = mock[UpdateServiceHelper]

    val controller = new WhatDoYouDoHereController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector,
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService,
      helper = mockUpdateServiceHelper,
      router = createRouter[AddServiceFlowModel]
    )

    mockCacheFetch(Some(AddServiceFlowModel(Some(HighValueDealing))))
    mockApplicationStatus(SubmissionDecisionApproved)

    mockCacheFetch(Some(AddServiceFlowModel(Some(MoneyServiceBusiness))))

    val cacheMapT = OptionT.some[Future, CacheMap](mockCacheMap)

    when {
      controller.dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(mockCacheMap))

    when {
      controller.businessMatchingService.getModel(any(), any(), any())
    } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
      activities = Some(BusinessActivities(Set(AccountancyServices)))
    ))

    when {
      controller.businessMatchingService.updateModel(any())(any(), any(), any())
    } thenReturn cacheMapT

    //    def setupModel(model: Option[BusinessMatching]) = when {
    //      controller.businessMatchingService.getModel(any(), any(), any())
    //    } thenReturn (model match {
    //      case Some(bm) => OptionT.pure[Future, BusinessMatching](bm)
    //      case _ => OptionT.none[Future, BusinessMatching]
    //    })
  }


  "SubServicesController" when {

    "get is called" must {
      "return OK with 'whatdoyoudohere' view" in new Fixture {
        val result = controller.get()(request)

        status(result) must be(OK)

        contentAsString(result) must include(
          Messages("businessmatching.updateservice.whatdoyoudohere.heading")
        )
      }
    }

    "post is called" must {

      "return a bad request when no data has been posted" in new Fixture {

        val result = controller.post()(request.withFormUrlEncodedBody())

        status(result) mustBe BAD_REQUEST
      }
    }

  }

}