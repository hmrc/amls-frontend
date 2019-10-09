/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.flowmanagement.{AddBusinessTypeFlowModel, WhatDoYouDoHerePageId}
import models.moneyservicebusiness.MoneyServiceBusinessTestData
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatDoYouDoHereControllerSpec extends AmlsSpec with MoneyServiceBusinessTestData with BusinessMatchingGenerator {

  sealed trait Fixture extends DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper = mock[AddBusinessTypeHelper]

    val controller = new WhatDoYouDoHereController(
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService,
      helper = mockUpdateServiceHelper,
      router = createRouter[AddBusinessTypeFlowModel],
      cc = mockMcc
    )

    val cacheMapT = OptionT.some[Future, CacheMap](mockCacheMap)

    when {
      controller.businessMatchingService.getModel(any())(any(), any())
    } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
      activities = Some(BusinessActivities(Set(AccountancyServices)))
    ))

    when {
      controller.businessMatchingService.updateModel(any(), any())(any(), any())
    } thenReturn cacheMapT
  }


  "WhatDoYouDoHereController" when {

    "get is called" must {
      "return OK with 'whatdoyoudohere' view" in new Fixture {
        mockCacheFetch(Some(AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
          subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))))))
        val result = controller.get()(request)

        status(result) must be(OK)

        contentAsString(result) must include(
          Messages("businessmatching.updateservice.whatdoyoudohere.heading")
        )
      }
    }

    "post is called" must {

      "return a bad request when no data has been posted" in new Fixture {

        val result = controller.post()(requestWithUrlEncodedBody("" -> ""))

        status(result) mustBe BAD_REQUEST
      }

      "update the tradingPremisesMsbServices when not all activities are selected" in new Fixture {
        mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
          subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingScrapMetal))),
          hasChanged = true))

        val result = controller.post()(requestWithUrlEncodedBody(
          "msbServices[]" -> "01"
        ))

        status(result) mustBe SEE_OTHER
        controller.router.verify("internalId", WhatDoYouDoHerePageId,
          AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingScrapMetal))),
            tradingPremisesMsbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
            hasChanged = true))
      }

      "update the tradingPremisesMsbServices when all activities are selected" in new Fixture {
        mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
          subSectors = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))),
          hasChanged = true)
        )

        val result = controller.post()(requestWithUrlEncodedBody(
          "msbServices[]" -> "03",
          "msbServices[]" -> "04"
        ))

        status(result) mustBe SEE_OTHER
        controller.router.verify("internalId", WhatDoYouDoHerePageId,
          AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))),
            tradingPremisesMsbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))),
            hasChanged = true))
      }
    }

  }

}