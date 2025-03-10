/*
 * Copyright 2024 HM Revenue & Customs
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
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, TransmittingMoney}
import models.businessmatching._
import models.businessmatching.updateservice.UpdateService
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => MsbModel}
import models.tcsp.Tcsp
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.businessmatching.{BusinessMatchingService, ServiceFlow}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{DependencyMocks, FutureAssertions}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ServiceFlowSpec extends PlaySpec with Matchers with MockitoSugar with ScalaFutures with FutureAssertions {

  trait Fixture extends DependencyMocks {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val businessMatchingService = mock[BusinessMatchingService]
    val service                 = new ServiceFlow(businessMatchingService)
    val businessMatching        = mock[BusinessMatching]
    mockCacheGetEntry(Some(businessMatching), BusinessMatching.key)

    when(businessMatching.msbServices) thenReturn Some(
      BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))
    )

    val msbModel = mock[MsbModel]
    when(msbModel.isComplete(any(), any(), any())) thenReturn false
    mockCacheGetEntry[MsbModel](Some(msbModel), MsbModel.key)

    val hvdModel = mock[Hvd]
    when(hvdModel.isComplete) thenReturn false
    mockCacheGetEntry[Hvd](Some(hvdModel), Hvd.key)

    val tcspModel = mock[Tcsp]
    when(tcspModel.isComplete) thenReturn false
    mockCacheGetEntry[Tcsp](Some(tcspModel), Tcsp.key)

    val eabModel = mock[Eab]
    when(eabModel.isComplete) thenReturn false
    mockCacheGetEntry[Eab](Some(eabModel), Eab.key)

    val aspModel = mock[Asp]
    when(aspModel.isComplete) thenReturn false
    mockCacheGetEntry[Asp](Some(aspModel), Asp.key)

    def setUpActivities(activities: Set[BusinessActivity]) = when {
      businessMatchingService.getAdditionalBusinessActivities(any())(any())
    } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](Future.successful(activities))

    mockCacheFetch(Some(UpdateService(inNewServiceFlow = true)), Some(UpdateService.key))
  }

  "isNewService" when {
    "called" must {
      "return true if the service appears in the additionalBusinessActivities collection" in new Fixture {
        setUpActivities(Set(AccountancyServices))
        whenReady(service.isNewActivity("credId", AccountancyServices))(_ mustBe true)
      }

      "return false if the service does not appear in the additionaBusinessActivities collection" in new Fixture {
        setUpActivities(Set(AccountancyServices))
        whenReady(service.isNewActivity("credId", HighValueDealing))(_ mustBe false)
      }
    }
  }
}
