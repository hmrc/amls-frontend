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

package services

import connectors.DataCacheConnector
import models.amp.Amp
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessdetails.BusinessDetails
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BMBusinessActivities, BusinessMatching, BusinessMatchingMsbServices}
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => Msb}
import models.registrationprogress.TaskRow
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.IntegrationPatience
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class SectionsProviderSpec extends AmlsSpec with IntegrationPatience {

  val mockCacheConnector        = mock[DataCacheConnector]
  implicit val mockCache: Cache = mock[Cache]

  lazy val sectionsProvider = new SectionsProvider(mockCacheConnector, appConfig)

  val credId = "123456"

  when(mockCache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
    .thenReturn(Some(BusinessMatching()))

  when(mockCache.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any()))
    .thenReturn(Some(BusinessDetails()))

  when(mockCache.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
    .thenReturn(Some(BusinessActivities()))

  when(mockCache.getEntry[Seq[BankDetails]](eqTo(BankDetails.key))(any()))
    .thenReturn(Some(Seq(BankDetails())))

  when(mockCache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any()))
    .thenReturn(Some(Seq(TradingPremises())))

  when(mockCache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any()))
    .thenReturn(Some(Seq(ResponsiblePerson())))

  when(mockCache.getEntry[Asp](eqTo(Asp.key))(any()))
    .thenReturn(None)

  when(mockCache.getEntry[Tcsp](eqTo(Tcsp.key))(any()))
    .thenReturn(None)

  when(mockCache.getEntry[Supervision](eqTo(Supervision.key))(any()))
    .thenReturn(None)

  when(mockCache.getEntry[Amp](eqTo(Amp.key))(any()))
    .thenReturn(None)

  when(mockCache.getEntry[Eab](eqTo(Eab.key))(any()))
    .thenReturn(None)

  when(mockCache.getEntry[Hvd](eqTo(Hvd.key))(any()))
    .thenReturn(None)

  when(mockCache.getEntry[Msb](eqTo(Msb.key))(any()))
    .thenReturn(None)

  "SectionsProvider" when {

    "taskRows is called" must {

      "return an empty sequence when cache connector cannot retrieve cache" in {

        when(mockCacheConnector.fetchAll(eqTo(credId)))
          .thenReturn(Future.successful(None))

        sectionsProvider.taskRows(credId).futureValue mustBe Seq.empty[TaskRow]
      }

      "return all mandatory rows even if there are no extra activities in cache " in {

        when(mockCacheConnector.fetchAll(eqTo(credId)))
          .thenReturn(Future.successful(Some(mockCache)))

        sectionsProvider.taskRows(credId).futureValue mustBe Seq(
          BusinessMatching.taskRow,
          BusinessDetails.taskRow,
          BusinessActivities.taskRow,
          BankDetails.taskRow,
          TradingPremises.taskRow,
          ResponsiblePerson.taskRow
        )
      }

      Seq(
        (AccountancyServices, Asp.taskRow),
        (TrustAndCompanyServices, Tcsp.taskRow),
        (ArtMarketParticipant, Amp.taskRow(appConfig)),
        (EstateAgentBusinessService, Eab.taskRow(appConfig)),
        (HighValueDealing, Hvd.taskRow),
        (MoneyServiceBusiness, Msb.taskRow)
      ) foreach { activity =>
        s"include ${activity._1.toString} row when it is set in the cache" in {

          when(mockCache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(
              Some(
                BusinessMatching(
                  activities = Some(BMBusinessActivities(Set(activity._1))),
                  msbServices = Some(BusinessMatchingMsbServices(Set.empty))
                )
              )
            )

          sectionsProvider.taskRows(credId).futureValue must contain(activity._2)
        }
      }

      "include supervision row when ASP and TCSP are in cache" in {

        when(mockCache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(
            Some(
              BusinessMatching(
                activities = Some(BMBusinessActivities(Set(AccountancyServices, TrustAndCompanyServices))),
                msbServices = Some(BusinessMatchingMsbServices(Set.empty))
              )
            )
          )

        when(mockCache.getEntry[Supervision](eqTo(Supervision.key))(any()))
          .thenReturn(Some(Supervision()))

        sectionsProvider.taskRows(credId).futureValue must contain(Supervision.taskRow)
      }

      "not include MSB if it is not defined in Business Matching in the cache" in {

        when(mockCache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(MoneyServiceBusiness))))))

        when(mockCache.getEntry[Msb](eqTo(Msb.key))(any()))
          .thenReturn(Some(Msb()))

        sectionsProvider.taskRows(credId).futureValue mustNot contain(Msb.taskRow)
      }
    }
  }
}
