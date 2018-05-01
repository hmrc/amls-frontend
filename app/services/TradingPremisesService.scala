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

package services

import javax.inject.Singleton
import models.businessmatching.{BusinessActivity, MoneyServiceBusiness}
import models.tradingpremises
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}



@Singleton
class TradingPremisesService {


  def updateTradingPremises(
                             indices: Seq[Int],
                             tradingPremises: Seq[models.tradingpremises.TradingPremises],
                             activity: models.businessmatching.BusinessActivity,
                             msbServicesInput: Option[models.businessmatching.MsbServices],
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
          if ((indices contains index)) {
            tpservices.services ++ tradingpremises.MsbServices.convertServices(msbServicesInput.get.msbServices)
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
                                                   removeActivities: Set[BusinessActivity]): Seq[TradingPremises] =
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

  private def patchTradingPremisesBusinessActivities(tradingPremises: Seq[TradingPremises])
                                                    (fn: ((WhatDoesYourBusinessDo, Int) => WhatDoesYourBusinessDo)): Seq[TradingPremises] = {
    tradingPremises.zipWithIndex map { case (tp, index) =>
      val stuff: TradingPremises = tp.whatDoesYourBusinessDoAtThisAddress {
        tp.whatDoesYourBusinessDoAtThisAddress.fold(WhatDoesYourBusinessDo(Set.empty)) { wdybd =>
          fn(wdybd, index)
        }
      }
      stuff.copy(hasAccepted = true)
    }
  }

  private def patchTradingPremisesMsbSubServices(tradingPremises: Seq[TradingPremises], newMsbServices: models.businessmatching.MsbServices)
                                          (fn: ((models.tradingpremises.MsbServices, Int) => models.tradingpremises.MsbServices)): Seq[TradingPremises] = {
    tradingPremises.zipWithIndex map { case (tp, index) =>
      val stuff: TradingPremises = tp.msbServices {
        //val emptyMsbServices = Set.e
        tp.msbServices.fold(tradingpremises.MsbServices(tradingpremises.MsbServices.convertServices(newMsbServices.msbServices))) { tpservices =>
          fn(tpservices, index)
        }
      }
      stuff.copy(hasAccepted = true)
    }
  }
}

