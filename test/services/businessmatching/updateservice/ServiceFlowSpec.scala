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

package services.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import models.asp.Asp
import models.businessmatching._
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.tcsp.Tcsp
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import models.moneyservicebusiness.{MoneyServiceBusiness => MsbModel}
import org.scalatest.concurrent.ScalaFutures
import services.businessmatching.{BusinessMatchingService, ServiceFlow, NextService}
import utils.{DependencyMocks, FutureAssertions}

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._

import scala.concurrent.Future

class ServiceFlowSpec extends PlaySpec with MustMatchers with MockitoSugar with ScalaFutures with FutureAssertions {

  trait Fixture extends DependencyMocks {

    val businessMatchingService = mock[BusinessMatchingService]

    val service = new ServiceFlow(businessMatchingService, mockCacheConnector)

    val msbModel = mock[MsbModel]
    when(msbModel.isComplete(any(), any())) thenReturn false
    mockCacheGetEntry[MsbModel](Some(msbModel), MsbModel.key)

    val hvdModel = mock[Hvd]
    when(hvdModel.isComplete) thenReturn false
    mockCacheGetEntry[Hvd](Some(hvdModel), Hvd.key)

    val tcspModel = mock[Tcsp]
    when(tcspModel.isComplete) thenReturn false
    mockCacheGetEntry[Tcsp](Some(tcspModel), Tcsp.key)

    val eabModel = mock[EstateAgentBusiness]
    when(eabModel.isComplete) thenReturn false
    mockCacheGetEntry[EstateAgentBusiness](Some(eabModel), EstateAgentBusiness.key)

    val aspModel = mock[Asp]
    when(aspModel.isComplete) thenReturn false
    mockCacheGetEntry[Asp](Some(aspModel), Asp.key)

    def setUpActivities(activities: Set[BusinessActivity]) = when {
      businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
    } thenReturn OptionT.some[Future, Set[BusinessActivity]](activities)
  }

  "getNextFlow" must {
    "return the correct state" when {
      "adding the MSB service" in new Fixture {
        setUpActivities(Set(MoneyServiceBusiness))

        service.next returnsSome NextService(controllers.msb.routes.WhatYouNeedController.get().url, MoneyServiceBusiness)
      }

      "adding the HVD service" in new Fixture {
        setUpActivities(Set(HighValueDealing))

        service.next returnsSome NextService(controllers.hvd.routes.WhatYouNeedController.get().url, HighValueDealing)
      }

      "adding the TSCP service" in new Fixture {
        setUpActivities(Set(TrustAndCompanyServices))

        service.next returnsSome NextService(controllers.tcsp.routes.WhatYouNeedController.get().url, TrustAndCompanyServices)
      }

      "adding the EAB service" in new Fixture {
        setUpActivities(Set(EstateAgentBusinessService))

        service.next returnsSome NextService(controllers.estateagentbusiness.routes.WhatYouNeedController.get().url, EstateAgentBusinessService)
      }

      "adding the Accountancy service" in new Fixture {
        setUpActivities(Set(AccountancyServices))

        service.next returnsSome NextService(controllers.asp.routes.WhatYouNeedController.get().url, AccountancyServices)
      }

      "adding two services that are incomplete" in new Fixture {
        setUpActivities(Set(TrustAndCompanyServices, AccountancyServices))

        service.next returnsSome NextService(controllers.tcsp.routes.WhatYouNeedController.get().url, TrustAndCompanyServices)
      }

      "adding two services, where the first one is complete" in new Fixture {
        setUpActivities(Set(TrustAndCompanyServices, AccountancyServices))

        when(tcspModel.isComplete) thenReturn true

        service.next returnsSome NextService(controllers.asp.routes.WhatYouNeedController.get().url, AccountancyServices)
      }

      "adding a service where the service is already complete" in new Fixture {
        setUpActivities(Set(EstateAgentBusinessService))

        when(eabModel.isComplete) thenReturn true

        service.next.returnsNone
      }
    }
  }
}
