/*
 * Copyright 2020 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}
import models.amp.Amp
import models.asp.Asp
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessActivities => BMBusinessActivities, BusinessActivity => BMBusinessActivity, BusinessMatching => BMBusinessMatching, _}
import models.estateagentbusiness.{EstateAgentBusiness}
import models.eab.Eab
import models.flowmanagement.RemoveBusinessTypeFlowModel
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => MSBSection}
import models.responsiblepeople.ResponsiblePerson
import models.tcsp.Tcsp
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessTypeHelper @Inject()(authAction: AuthAction,
                                         appConfig: ApplicationConfig,
                                         implicit val dataCacheConnector: DataCacheConnector) {

  def removeSectionData(credId: String, model: RemoveBusinessTypeFlowModel)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, Seq[CacheMap]] = {

    def removeActivities(activities: List[BMBusinessActivity]): Future[Seq[CacheMap]] = {
      activities match {
        case Nil =>
          Future.successful(Seq.empty)
        case first :: rest =>
          for {
            f <- removeActivity(first).map(Seq(_))
            r <- removeActivities(rest)
          } yield f ++ r
      }
    }

    def removeActivity(activity: BMBusinessActivity): Future[CacheMap] = {
      activity match {
         case ArtMarketParticipant =>
          dataCacheConnector.removeByKey[Amp](credId, Amp.key)
        case MoneyServiceBusiness =>
          dataCacheConnector.removeByKey[MSBSection](credId, MSBSection.key)
        case HighValueDealing =>
          dataCacheConnector.removeByKey[Hvd](credId, Hvd.key)
        case TrustAndCompanyServices =>
          dataCacheConnector.removeByKey[Tcsp](credId, Tcsp.key)
        case AccountancyServices =>
          dataCacheConnector.removeByKey[Asp](credId, Asp.key)
        case EstateAgentBusinessService =>
          //TODO AMLS-5540 - Can be removed when feature toggle for new EAB service is removed.
          if(appConfig.phase3Release2La) {
            dataCacheConnector.removeByKey[Eab](credId, Eab.key)
          } else {
            dataCacheConnector.removeByKey[EstateAgentBusiness](credId, EstateAgentBusiness.key)
          }
        case _ =>
          dataCacheConnector.fetchAllWithDefault(credId)
      }
    }

    OptionT.liftF(model.activitiesToRemove.fold(Future.successful(Seq.empty[CacheMap])) {
      activities =>
        removeActivities(activities.toList)
    })
  }

  def removeBusinessMatchingBusinessTypes(credId: String, model: RemoveBusinessTypeFlowModel)
                                         (implicit hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, BMBusinessMatching] = {

    val emptyActivities = BMBusinessActivities(Set.empty[BMBusinessActivity])
    val setAccepted = (bm: BMBusinessMatching) => bm.copy(hasAccepted = true)

    for {
      activitiesToRemove <- OptionT.fromOption[Future](model.activitiesToRemove)
      currentBusinessMatching <- OptionT(dataCacheConnector.fetch[BMBusinessMatching](credId, BMBusinessMatching.key))
      currentActivities <- OptionT.fromOption[Future](currentBusinessMatching.activities) orElse OptionT.some(emptyActivities)
      newBusinessMatching <- {
        OptionT(dataCacheConnector.update[BMBusinessMatching](credId, BMBusinessMatching.key) {

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

  def removeTradingPremisesBusinessTypes(credId: String, model: RemoveBusinessTypeFlowModel)
                                        (implicit hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, Seq[TradingPremises]] = {

    val setAccepted = (tp: TradingPremises) => tp.copy(hasAccepted = true)

    for {
      businessMatching <- OptionT(dataCacheConnector.fetch[BMBusinessMatching](credId, BMBusinessMatching.key))
      currentActivities <- OptionT.fromOption[Future](businessMatching.activities)
      activitiesToRemove <- OptionT.fromOption[Future](model.activitiesToRemove)
      newTradingPremises <- {
        OptionT(dataCacheConnector.update[Seq[TradingPremises]](credId, TradingPremises.key) {
          case Some(tpList) => tpList map { tp =>
            val currentBusinessTypes = tp.whatDoesYourBusinessDoAtThisAddress.getOrElse(WhatDoesYourBusinessDo(Set.empty))

            val newTPActivities = currentBusinessTypes.activities -- activitiesToRemove
            val newBusinessActivities = currentActivities.businessActivities -- activitiesToRemove

            val newActivities = (newTPActivities, newBusinessActivities) match {
              case (tpActivities, busActivities) if tpActivities.isEmpty && busActivities.size == 1 => busActivities
              case (tpActivities, _) => tpActivities
            }

            val newBusinessTypes = currentBusinessTypes.copy(activities = newActivities)
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

  def removeFitAndProper(credId: String, model: RemoveBusinessTypeFlowModel)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, Seq[ResponsiblePerson]] = {

    val emptyActivities = BMBusinessActivities(Set.empty[BMBusinessActivity])

    val canRemoveFitProper = (
      current: Set[BMBusinessActivity],
      removing: Set[BMBusinessActivity]
    ) => {
      val hasTCSP = current.contains(TrustAndCompanyServices)
      val hasMSB = current.contains(MoneyServiceBusiness)

      (removing.contains(MoneyServiceBusiness) && !hasTCSP) ||
      (removing.contains(TrustAndCompanyServices) && !hasMSB)
    }

    for {
      activitiesToRemove <- OptionT.fromOption[Future](model.activitiesToRemove)
      currentBusinessMatching <- OptionT(dataCacheConnector.fetch[BMBusinessMatching](credId, BMBusinessMatching.key))
      currentActivities <- OptionT.fromOption[Future](currentBusinessMatching.activities) orElse OptionT.some(emptyActivities)
      newResponsiblePeople <- {
        OptionT(dataCacheConnector.update[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key) {
          case Some(rpList) if canRemoveFitProper(currentActivities.businessActivities, activitiesToRemove) =>
            rpList.map(rp => rp.resetBasedOnApprovalFlags())
          case Some(rpList) => rpList
          case _ => throw new RuntimeException("No responsible people found")
        })
      }
    } yield newResponsiblePeople
  }

  def dateOfChangeApplicable(credId: String, activitiesToRemove: Set[BMBusinessActivity])
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, Boolean] = {
    for {
      recentlyAdded <- OptionT(dataCacheConnector.fetch[ServiceChangeRegister](credId, ServiceChangeRegister.key)) orElse OptionT.some(ServiceChangeRegister())
      addedActivities <- OptionT.fromOption[Future](recentlyAdded.addedActivities) orElse OptionT.some(Set.empty)
    } yield {
      (activitiesToRemove -- addedActivities).nonEmpty
    }
  }

  def removeFlowData(credId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): OptionT[Future, RemoveBusinessTypeFlowModel] = {
    val emptyModel = RemoveBusinessTypeFlowModel()

    OptionT.liftF(dataCacheConnector.save(credId, RemoveBusinessTypeFlowModel.key, RemoveBusinessTypeFlowModel())) map { _ => emptyModel }
  }
}
