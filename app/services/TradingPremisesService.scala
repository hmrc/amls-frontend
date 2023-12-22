/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessActivity, BusinessMatchingMsbServices}
import models.businessmatching.BusinessActivity.MoneyServiceBusiness
import models.tradingpremises
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import play.api.libs.json.Format
import typeclasses.MongoKey
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TradingPremisesService @Inject()(val cacheConnector: DataCacheConnector) {

  def updateTradingPremises(
                             indices: Seq[Int],
                             tradingPremises: Seq[TradingPremises],
                             activity: BusinessActivity,
                             msbServicesInput: Option[BusinessMatchingMsbServices],
                             remove: Boolean): Seq[TradingPremises] = {

    val updatedTradingPremises: Seq[TradingPremises] = {
      patchTradingPremisesBusinessActivities(tradingPremises) { (wdybd, index) =>
        wdybd.copy(
          if (indices contains index) {
            wdybd.activities + activity
          } else if (remove) {
            wdybd.activities - activity
          } else {
            wdybd.activities
          }
        )
      }
    }

    if(msbServicesInput.isDefined) {
      patchTradingPremisesMsbSubServices(updatedTradingPremises, msbServicesInput.get) { (tpservices, index) =>
        tpservices.copy(
          if (indices contains index) {
            tpservices.services ++ tradingpremises.TradingPremisesMsbServices.convertServices(msbServicesInput.get.msbServices)
          } else {
            tpservices.services
          }
        )
      }
    } else {
      updatedTradingPremises
    }
  }

  def removeBusinessActivitiesFromTradingPremises(
                                                   tradingPremises: Seq[TradingPremises],
                                                   existingActivities: Set[BusinessActivity],
                                                   removeActivities: Set[BusinessActivity]): Seq[TradingPremises] = {
    patchTradingPremisesBusinessActivities(tradingPremises) { (wdybd, index) =>
      wdybd.copy({
        wdybd.activities diff removeActivities match {
          case remainingActivities if remainingActivities.nonEmpty => remainingActivities
          case _ => Set(existingActivities.head)
        }
      })
    } map { tp =>
      if(removeActivities contains MoneyServiceBusiness) {
        tp.copy(msbServices = None)
      } else {
        tp
      }
    }
  }

  def addTradingPremises(credId: String, newTradingPremises: TradingPremises)(implicit hc: HeaderCarrier, formats: Format[TradingPremises],
                                                                              key: MongoKey[TradingPremises], ec: ExecutionContext): Future[Int] = {

    val futureFetchedTradingPremises =
      cacheConnector
        .fetch[Seq[TradingPremises]](credId, key())
        .map(_.fold(Seq.empty[TradingPremises])(identity))

    futureFetchedTradingPremises.flatMap { fetchedTradingPremises =>
      if (!fetchedTradingPremises.lastOption.contains(newTradingPremises) && !fetchedTradingPremises.lastOption.exists(_.notEmpty)) {
        cacheConnector
          .save[Seq[TradingPremises]](credId, key(), fetchedTradingPremises :+ newTradingPremises)
          .map(_ => fetchedTradingPremises.size + 1)
      } else {
        Future.successful(fetchedTradingPremises.size)
      }
    }
  }

  private def patchTradingPremisesBusinessActivities(tradingPremises: Seq[TradingPremises])
                                                    (fn: (WhatDoesYourBusinessDo, Int) => WhatDoesYourBusinessDo): Seq[TradingPremises] = {
    tradingPremises.zipWithIndex map { case (tp, index) =>
      val premise: TradingPremises = tp.whatDoesYourBusinessDoAtThisAddress {
        tp.whatDoesYourBusinessDoAtThisAddress.fold(WhatDoesYourBusinessDo(Set.empty)) { wdybd =>
          fn(wdybd, index)
        }
      }
      premise.copy(hasAccepted = true)
    }
  }

  private def patchTradingPremisesMsbSubServices(tradingPremises: Seq[TradingPremises], newMsbServices: models.businessmatching.BusinessMatchingMsbServices)
                                          (fn: (models.tradingpremises.TradingPremisesMsbServices, Int) => models.tradingpremises.TradingPremisesMsbServices): Seq[TradingPremises] = {
    tradingPremises.zipWithIndex map { case (tp, index) =>
      tp match {
        case t if t.whatDoesYourBusinessDoAtThisAddress.isDefined => {
          t.whatDoesYourBusinessDoAtThisAddress match {
            case Some(x) if x.activities.contains(MoneyServiceBusiness) =>
              val services = tp.msbServices.fold(tradingpremises.TradingPremisesMsbServices(tradingpremises.TradingPremisesMsbServices.convertServices(newMsbServices.msbServices))) { tpservices =>
                fn(tpservices, index)
              }
              tp.msbServices(Some(services)).copy(hasAccepted = true)
            case _ => tp
          }
        }
        case _ => tp
      }
    }
  }
}

