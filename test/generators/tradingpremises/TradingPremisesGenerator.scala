/*
 * Copyright 2017 HM Revenue & Customs
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

import generators.{BaseGenerator, CountryGenerator}
import models.businessmatching._
import models.tradingpremises.{Address, TradingPremises, WhatDoesYourBusinessDo, YourTradingPremises}
import org.scalacheck.Gen

//noinspection ScalaStyle
trait TradingPremisesGenerator extends BaseGenerator {

  private val nameLength = 10

  val tradingPremisesAddressGen: Gen[Address] = for {
    line1 <- stringOfLengthGen(nameLength)
    line2 <- stringOfLengthGen(nameLength)
    postcode <- postcodeGen
  } yield Address(line1, line2, None, None, postcode, None)

  val yourTradingPremisesGen: Gen[YourTradingPremises] = for {
    name <- stringOfLengthGen(nameLength)
    address <- tradingPremisesAddressGen
    residential <- Gen.oneOf(Some(true), Some(false), None)
    startDate <- localDateGen
  } yield YourTradingPremises(name, address, residential, Some(startDate), None)

  val businessActivitiesGen: Gen[WhatDoesYourBusinessDo] = for {
    activities <- Gen.someOf(MoneyServiceBusiness,
      AccountancyServices,
      BillPaymentServices,
      EstateAgentBusinessService,
      HighValueDealing,
      TrustAndCompanyServices,
      TelephonePaymentService)
  } yield WhatDoesYourBusinessDo(activities.toSet, None)

  val tradingPremisesGen: Gen[TradingPremises] = for {
    ytp <- yourTradingPremisesGen
    activities <- businessActivitiesGen
  } yield TradingPremises(
    yourTradingPremises = Some(ytp),
    whatDoesYourBusinessDoAtThisAddress = Some(activities),
    hasAccepted = true,
    hasChanged = true
  )

  def tradingPremisesWithActivitiesGen(activities: BusinessActivity*): Gen[TradingPremises] = for {
    tp <- tradingPremisesGen
  } yield tp.copy(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(activities.toSet)))

}
