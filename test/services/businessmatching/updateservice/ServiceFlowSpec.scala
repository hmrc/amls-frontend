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

package services.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import models.asp.Asp
import models.businessmatching._
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.tcsp.Tcsp
import org.mockito.Matchers.{eq => eqTo, any}
import org.mockito.Mockito.{when, verify}
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import models.moneyservicebusiness.{MoneyServiceBusiness => MsbModel}
import org.scalatest.concurrent.ScalaFutures
import services.businessmatching.{BusinessMatchingService, NextService, ServiceFlow}
import utils.{DependencyMocks, FutureAssertions}
import models.businessmatching.updateservice.UpdateService
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ServiceFlowSpec extends PlaySpec with MustMatchers with MockitoSugar with ScalaFutures with FutureAssertions {

  trait Fixture extends DependencyMocks {

    implicit val hc = HeaderCarrier()
    implicit val ac = mock[AuthContext]

    val businessMatchingService = mock[BusinessMatchingService]

    val service = new ServiceFlow(businessMatchingService, mockCacheConnector)

    val businessMatching = mock[BusinessMatching]

    when(businessMatching.msbServices) thenReturn Some(
      BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))
    )

    mockCacheGetEntry(Some(businessMatching), BusinessMatching.key)

    val msbModel = mock[MsbModel]
    when(msbModel.isComplete(any(), any(), any())) thenReturn false
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

    mockCacheFetch(Some(UpdateService(inNewServiceFlow = true)), Some(UpdateService.key))
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

  "isNewService" when {
    "called" must {
      "return true if the service appears in the additionalBusinessActivities collection" in new Fixture {
        setUpActivities(Set(AccountancyServices))

        whenReady(service.isNewActivity(AccountancyServices))(_ mustBe true)
      }

      "return false if the service does not appear in the additionaBusinessActivities collection" in new Fixture {
        setUpActivities(Set(AccountancyServices))

        whenReady(service.isNewActivity(HighValueDealing))(_ mustBe false)
      }
    }
  }

  "inNewServiceFlow" when {
    "called" must {
      "return false if the user is not in the new service flow" in new Fixture {
        setUpActivities(Set(AccountancyServices))

        mockCacheFetch(Some(UpdateService(inNewServiceFlow = false)), Some(UpdateService.key))

        whenReady(service.inNewServiceFlow(AccountancyServices))(_ mustBe false)
      }

      "return false if the specified service does not exist in the additional business activities" in new Fixture {
        setUpActivities(Set(TrustAndCompanyServices))

        whenReady(service.inNewServiceFlow(AccountancyServices))(_ mustBe false)
      }
    }
  }

  "setInServiceFlowFlag" must {
    "update the data with the specified value" in new Fixture {
      mockCacheFetch(Some(UpdateService(inNewServiceFlow = false)), Some(UpdateService.key))
      mockCacheSave[UpdateService]

      whenReady(service.setInServiceFlowFlag(true)) { _ =>
        verify(mockCacheConnector).save[UpdateService](eqTo(UpdateService.key), eqTo(UpdateService(inNewServiceFlow = true)))(any(), any(), any())
      }
    }

    "update the value even when no value previously existed" in new Fixture {
      mockCacheFetch(None, Some(UpdateService.key))
      mockCacheSave[UpdateService]

      whenReady(service.setInServiceFlowFlag(true)) { _ =>
        verify(mockCacheConnector).save[UpdateService](eqTo(UpdateService.key), eqTo(UpdateService(inNewServiceFlow = true)))(any(), any(), any())
      }
    }
  }
}
