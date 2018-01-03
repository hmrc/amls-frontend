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
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}

@Singleton
class TradingPremisesService {


  def addBusinessActivtiesToTradingPremises(
                                             indices: Seq[Int],
                                             tradingPremises: Seq[TradingPremises],
                                             activity: BusinessActivity,
                                             remove: Boolean): Seq[TradingPremises] =
    patchTradingPremisesBusinessActivities(tradingPremises){ (wdybd, index) =>
      wdybd.copy({
        if (indices contains index) {
          wdybd.activities + activity
        } else if (remove) {
          wdybd.activities - activity
        } else {
          wdybd.activities
        }
      })
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


  def patchTradingPremisesBusinessActivities(tradingPremises: Seq[TradingPremises])
                                            (fn: ((WhatDoesYourBusinessDo, Int) => WhatDoesYourBusinessDo)): Seq[TradingPremises] =
    tradingPremises.zipWithIndex map { case (tp, index) =>
      tp.whatDoesYourBusinessDoAtThisAddress(
        tp.whatDoesYourBusinessDoAtThisAddress.fold(WhatDoesYourBusinessDo(Set.empty)){ wdybd =>
          fn(wdybd, index)
        }
      ).copy(hasAccepted = true)
    }

}
