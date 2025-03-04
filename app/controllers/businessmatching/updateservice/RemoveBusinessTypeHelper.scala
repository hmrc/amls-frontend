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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import models.amp.Amp
import models.asp.Asp
import models.businessmatching.BusinessActivity._
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessActivities => BMBusinessActivities, BusinessActivity => BMBusinessActivity, BusinessMatching => BMBusinessMatching}
import models.eab.Eab
import models.flowmanagement.RemoveBusinessTypeFlowModel
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => MSBSection}
import models.responsiblepeople.ResponsiblePerson
import models.tcsp.Tcsp
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import services.cache.Cache

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessTypeHelper @Inject() (implicit val dataCacheConnector: DataCacheConnector) {

  def removeSectionData(credId: String, model: RemoveBusinessTypeFlowModel)(implicit
    ec: ExecutionContext
  ): OptionT[Future, Seq[Cache]] = {

    def removeActivities(activities: List[BMBusinessActivity]): Future[Seq[Cache]] =
      activities match {
        case Nil           =>
          Future.successful(Seq.empty)
        case first :: rest =>
          for {
            f <- removeActivity(first).map(Seq(_))
            r <- removeActivities(rest)
          } yield f ++ r
      }

    def removeActivity(activity: BMBusinessActivity): Future[Cache] =
      activity match {
        case ArtMarketParticipant       =>
          dataCacheConnector.removeByKey(credId, Amp.key)
        case MoneyServiceBusiness       =>
          dataCacheConnector.removeByKey(credId, MSBSection.key)
        case HighValueDealing           =>
          dataCacheConnector.removeByKey(credId, Hvd.key)
        case TrustAndCompanyServices    =>
          dataCacheConnector.removeByKey(credId, Tcsp.key)
        case AccountancyServices        =>
          dataCacheConnector.removeByKey(credId, Asp.key)
        case EstateAgentBusinessService =>
          dataCacheConnector.removeByKey(credId, Eab.key)
        case _                          =>
          dataCacheConnector.fetchAllWithDefault(credId)
      }

    OptionT.liftF(model.activitiesToRemove.fold(Future.successful(Seq.empty[Cache])) { activities =>
      removeActivities(activities.toList)
    })
  }

  def removeBusinessMatchingBusinessTypes(credId: String, model: RemoveBusinessTypeFlowModel)(implicit
    ec: ExecutionContext
  ): OptionT[Future, BMBusinessMatching] = {

    val emptyActivities = BMBusinessActivities(Set.empty[BMBusinessActivity])
    val setAccepted     = (bm: BMBusinessMatching) => bm.copy(hasAccepted = true)

    for {
      activitiesToRemove      <- OptionT.fromOption[Future](model.activitiesToRemove)
      currentBusinessMatching <- OptionT(dataCacheConnector.fetch[BMBusinessMatching](credId, BMBusinessMatching.key))
      currentActivities       <-
        OptionT.fromOption[Future](currentBusinessMatching.activities) orElse OptionT.some(emptyActivities)
      newBusinessMatching     <-
        OptionT(dataCacheConnector.update[BMBusinessMatching](credId, BMBusinessMatching.key) {

          case Some(bm) =>
            val newBm = bm.activities(
              currentActivities.copy(businessActivities = currentActivities.businessActivities -- activitiesToRemove)
            )

            setAccepted {
              if (activitiesToRemove.contains(MoneyServiceBusiness)) {
                newBm.msbServices(None).businessAppliedForPSRNumber(None)
              } else {
                newBm
              }
            }

          case _ => None
        })
    } yield newBusinessMatching
  }

  def removeTradingPremisesBusinessTypes(credId: String, model: RemoveBusinessTypeFlowModel)(implicit
    ec: ExecutionContext
  ): OptionT[Future, Seq[TradingPremises]] = {

    val setAccepted = (tp: TradingPremises) => tp.copy(hasAccepted = true)

    for {
      businessMatching   <- OptionT(dataCacheConnector.fetch[BMBusinessMatching](credId, BMBusinessMatching.key))
      currentActivities  <- OptionT.fromOption[Future](businessMatching.activities)
      activitiesToRemove <- OptionT.fromOption[Future](model.activitiesToRemove)
      newTradingPremises <-
        OptionT(dataCacheConnector.update[Seq[TradingPremises]](credId, TradingPremises.key) {
          case Some(tpList) =>
            tpList map { tp =>
              val currentBusinessTypes =
                tp.whatDoesYourBusinessDoAtThisAddress.getOrElse(WhatDoesYourBusinessDo(Set.empty))

              val newTPActivities       = currentBusinessTypes.activities -- activitiesToRemove
              val newBusinessActivities = currentActivities.businessActivities -- activitiesToRemove

              val newActivities = (newTPActivities, newBusinessActivities) match {
                case (tpActivities, busActivities) if tpActivities.isEmpty && busActivities.size == 1 => busActivities
                case (tpActivities, _)                                                                => tpActivities
              }

              val newBusinessTypes = currentBusinessTypes.copy(activities = newActivities)
              val newPremises      = tp.whatDoesYourBusinessDoAtThisAddress(newBusinessTypes)

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
    } yield newTradingPremises
  }

  def removeFitAndProper(credId: String, model: RemoveBusinessTypeFlowModel)(implicit
    ec: ExecutionContext
  ): OptionT[Future, Seq[ResponsiblePerson]] = {

    val emptyActivities = BMBusinessActivities(Set.empty[BMBusinessActivity])

    val canRemoveFitProper = (current: Set[BMBusinessActivity], removing: Set[BMBusinessActivity]) => {
      val hasTCSP = current.contains(TrustAndCompanyServices)
      val hasMSB  = current.contains(MoneyServiceBusiness)

      (removing.contains(MoneyServiceBusiness) && !hasTCSP) ||
      (removing.contains(TrustAndCompanyServices) && !hasMSB)
    }

    for {
      activitiesToRemove      <- OptionT.fromOption[Future](model.activitiesToRemove)
      currentBusinessMatching <- OptionT(dataCacheConnector.fetch[BMBusinessMatching](credId, BMBusinessMatching.key))
      currentActivities       <-
        OptionT.fromOption[Future](currentBusinessMatching.activities) orElse OptionT.some(emptyActivities)
      newResponsiblePeople    <-
        OptionT(dataCacheConnector.update[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key) {
          case Some(rpList) if canRemoveFitProper(currentActivities.businessActivities, activitiesToRemove) =>
            rpList.map(rp => rp.resetBasedOnApprovalFlags())
          case Some(rpList)                                                                                 => rpList
          case _                                                                                            => throw new RuntimeException("No responsible people found")
        })
    } yield newResponsiblePeople
  }

  def dateOfChangeApplicable(credId: String, activitiesToRemove: Set[BMBusinessActivity])(implicit
    ec: ExecutionContext
  ): OptionT[Future, Boolean] =
    for {
      recentlyAdded   <- OptionT(
                           dataCacheConnector.fetch[ServiceChangeRegister](credId, ServiceChangeRegister.key)
                         ) orElse OptionT.some(ServiceChangeRegister())
      addedActivities <- OptionT.fromOption[Future](recentlyAdded.addedActivities) orElse OptionT.some(Set.empty)
    } yield (activitiesToRemove -- addedActivities).nonEmpty

  def removeFlowData(credId: String)(implicit ec: ExecutionContext): OptionT[Future, RemoveBusinessTypeFlowModel] = {
    val emptyModel = RemoveBusinessTypeFlowModel()
    OptionT.liftF(dataCacheConnector.save(credId, RemoveBusinessTypeFlowModel.key, RemoveBusinessTypeFlowModel())) map {
      _ => emptyModel
    }
  }
}
