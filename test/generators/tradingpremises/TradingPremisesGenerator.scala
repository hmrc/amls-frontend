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

package generators.tradingpremises

import generators.businessmatching.BusinessActivitiesGenerator
import generators.BaseGenerator
import models.businessmatching._
import models.businessmatching.BusinessActivity._
import models.tradingpremises._

import org.scalacheck.Gen

//noinspection ScalaStyle
trait TradingPremisesGenerator extends BaseGenerator with BusinessActivitiesGenerator {

  private val nameLength = 10

  val tradingPremisesAddressGen: Gen[Address] = for {
    line1    <- stringOfLengthGen(nameLength)
    line2    <- stringOfLengthGen(nameLength)
    postcode <- postcodeGen
  } yield Address(line1, Some(line2), None, None, postcode, None)

  val yourTradingPremisesGen: Gen[YourTradingPremises] = for {
    name        <- stringOfLengthGen(nameLength)
    address     <- tradingPremisesAddressGen
    residential <- Gen.oneOf(Some(true), Some(false), None)
    startDate   <- localDateGen
  } yield YourTradingPremises(name, address, residential, Some(startDate), None)

  val whatBusinessActivitiesGen: Gen[WhatDoesYourBusinessDo] = for {
    activities <- businessActivitiesListGen
  } yield WhatDoesYourBusinessDo(activities.toSet, None)

  val whatBusinessTypesAtLeastOneGen: Gen[WhatDoesYourBusinessDo] = for {
    activities <- businessActivitiesListGen
    activity   <- singleBusinessTypeGen
  } yield WhatDoesYourBusinessDo(activities.toSet union Set(activity), None)

  val tpSubSectorGen: Gen[TradingPremisesMsbServices] = for {
    subSectors <- Gen
                    .choose(1, 3)
                    .flatMap(
                      Gen.pick(
                        _,
                        Seq(
                          models.tradingpremises.TradingPremisesMsbService.TransmittingMoney,
                          models.tradingpremises.TradingPremisesMsbService.CurrencyExchange,
                          models.tradingpremises.TradingPremisesMsbService.ChequeCashingScrapMetal,
                          models.tradingpremises.TradingPremisesMsbService.ChequeCashingNotScrapMetal
                        )
                      )
                    )
  } yield TradingPremisesMsbServices(subSectors.toSet)

  val tradingPremisesGen: Gen[TradingPremises] = for {
    ytp        <- yourTradingPremisesGen
    activities <- whatBusinessActivitiesGen
  } yield TradingPremises(
    yourTradingPremises = Some(ytp),
    whatDoesYourBusinessDoAtThisAddress = Some(activities),
    hasAccepted = true,
    hasChanged = true
  )

  val tradingPremisesWithAtLeastOneBusinessTypeGen: Gen[TradingPremises] = for {
    ytp        <- yourTradingPremisesGen
    activities <- whatBusinessTypesAtLeastOneGen
    subSectors <- tpSubSectorGen
  } yield TradingPremises(
    yourTradingPremises = Some(ytp),
    whatDoesYourBusinessDoAtThisAddress = Some(activities),
    msbServices = if (activities.activities.contains(MoneyServiceBusiness)) Some(subSectors) else None,
    hasAccepted = true,
    hasChanged = true
  )

  val fullTradingPremisesGen: Gen[TradingPremises] = for {
    ytp        <- yourTradingPremisesGen.suchThat(_.nonEmpty)
    activities <- whatBusinessActivitiesGen.suchThat(_.activities.nonEmpty)
    subSectors <- tpSubSectorGen.suchThat(_.services.nonEmpty)
  } yield TradingPremises(
    yourTradingPremises = Some(ytp),
    whatDoesYourBusinessDoAtThisAddress = Some(activities),
    msbServices = if (activities.activities.contains(MoneyServiceBusiness)) Some(subSectors) else None,
    hasAccepted = true,
    hasChanged = true
  )

  def tradingPremisesWithActivitiesGen(activities: BusinessActivity*): Gen[TradingPremises] = for {
    tp <- tradingPremisesGen
  } yield tp.copy(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(activities.toSet)))

}
