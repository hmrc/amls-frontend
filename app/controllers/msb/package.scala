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

package controllers

import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatchingMsbService, CurrencyExchange, ForeignExchange, MoneyServiceBusiness}

package object msb {

  def shouldAnswerCurrencyExchangeQuestions(msbActivities: Set[BusinessMatchingMsbService], register: ServiceChangeRegister) = {
    (register.addedActivities.fold(false)(_.contains(MoneyServiceBusiness)) ||
      register.addedSubSectors.fold(false)(_.contains(CurrencyExchange))) && msbActivities.contains(CurrencyExchange)
  }

  def shouldAnswerForeignExchangeQuestions(msbActivities: Set[BusinessMatchingMsbService], register: ServiceChangeRegister) = {
    (register.addedActivities.fold(false)(_.contains(MoneyServiceBusiness)) ||
            register.addedSubSectors.fold(false)(_.contains(ForeignExchange))) && msbActivities.contains(ForeignExchange)
  }

}
