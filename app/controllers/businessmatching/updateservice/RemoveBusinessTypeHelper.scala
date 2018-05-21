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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.flowmanagement.RemoveBusinessTypeFlowModel
import models.responsiblepeople.ResponsiblePerson
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import models.businessmatching.{MoneyServiceBusiness, TrustAndCompanyServices, BusinessActivities => BMBusinessActivities, BusinessActivity => BMBusinessActivity, BusinessMatching => BMBusinessMatching}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessTypeHelper @Inject()(val authConnector: AuthConnector,
                                         implicit val dataCacheConnector: DataCacheConnector
                                        ) {

  def removeBusinessMatchingBusinessTypes(model: RemoveBusinessTypeFlowModel)
                                         (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, BMBusinessMatching] = {

    val emptyActivities = BMBusinessActivities(Set.empty[BMBusinessActivity])
    val setAccepted = (bm: BMBusinessMatching) => bm.copy(hasAccepted = true)

    for {
      activitiesToRemove <- OptionT.fromOption[Future](model.activitiesToRemove)
      currentBusinessMatching <- OptionT(dataCacheConnector.fetch[BMBusinessMatching](BMBusinessMatching.key))
      currentActivities <- OptionT.fromOption[Future](currentBusinessMatching.activities) orElse OptionT.some(emptyActivities)
      newBusinessMatching <- {
        OptionT(dataCacheConnector.update[BMBusinessMatching](BMBusinessMatching.key) {

          case Some(bm) =>
            val newBm = bm.activities(currentActivities.copy(businessActivities = currentActivities.businessActivities -- activitiesToRemove))

            setAccepted {
              if (activitiesToRemove.contains(MoneyServiceBusiness)) {
                newBm.msbServices(None).businessAppliedForPSRNumber(None)
              } else {
                newBm
              }
            }
        })
      }
    } yield newBusinessMatching
  }

  def removeTradingPremisesBusinessTypes(model: RemoveBusinessTypeFlowModel)
                                        (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, Seq[TradingPremises]] = {

    val setAccepted = (tp: TradingPremises) => tp.copy(hasAccepted = true)

    for {
      activitiesToRemove <- OptionT.fromOption[Future](model.activitiesToRemove)
      newTradingPremises <- {
        OptionT(dataCacheConnector.update[Seq[TradingPremises]](TradingPremises.key) {
          case Some(tpList) => tpList map { tp =>
            val currentBusinessTypes = tp.whatDoesYourBusinessDoAtThisAddress.getOrElse(WhatDoesYourBusinessDo(Set.empty))
            val newBusinessTypes = currentBusinessTypes.copy(activities = currentBusinessTypes.activities -- activitiesToRemove)
            val newPremises = tp.whatDoesYourBusinessDoAtThisAddress(newBusinessTypes)

            setAccepted {
              if (activitiesToRemove.contains(MoneyServiceBusiness)) {
                newPremises.msbServices(None)
              } else {
                newPremises
              }
            }
          }

          case _ => throw new RuntimeException("No trading premises found")
        })
      }
    } yield newTradingPremises
  }

  def removeFitAndProper(model: RemoveBusinessTypeFlowModel)
                        (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, Seq[ResponsiblePerson]] = {

    val emptyActivities = BMBusinessActivities(Set.empty[BMBusinessActivity])

    val canRemoveFitProper = (current: Set[BMBusinessActivity], removing: Set[BMBusinessActivity]) => {
      val hasTCSP = current.contains(TrustAndCompanyServices)
      val hasMSB = current.contains(MoneyServiceBusiness)

      (removing.contains(MoneyServiceBusiness) && !hasTCSP) || (removing.contains(TrustAndCompanyServices) && !hasMSB)
    }

    for {
      activitiesToRemove <- OptionT.fromOption[Future](model.activitiesToRemove)
      currentBusinessMatching <- OptionT(dataCacheConnector.fetch[BMBusinessMatching](BMBusinessMatching.key))
      currentActivities <- OptionT.fromOption[Future](currentBusinessMatching.activities) orElse OptionT.some(emptyActivities)
      newResponsiblePeople <- {
        OptionT(dataCacheConnector.update[Seq[ResponsiblePerson]](ResponsiblePerson.key) {
          case Some(rpList) if canRemoveFitProper(currentActivities.businessActivities, activitiesToRemove) =>
            rpList.map(_.hasAlreadyPassedFitAndProper(None).copy(hasAccepted = true))
          case Some(rpList) => rpList
          case _ => throw new RuntimeException("No responsible people found")
        })
      }
    } yield newResponsiblePeople
  }

  def dateOfChangeApplicable(model: RemoveBusinessTypeFlowModel)(implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, Boolean] = {
    for {
      activitiesToRemove <- OptionT.fromOption[Future](model.activitiesToRemove)
      recentlyAdded <- OptionT(dataCacheConnector.fetch[ServiceChangeRegister](ServiceChangeRegister.key)) orElse OptionT.some(ServiceChangeRegister())
      addedActivities <- OptionT.fromOption[Future](recentlyAdded.addedActivities) orElse OptionT.some(Set.empty)
    } yield {
      println(activitiesToRemove -- addedActivities)
      (activitiesToRemove -- addedActivities).nonEmpty
    }
  }
}
