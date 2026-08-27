/*
 * Copyright 2026 HM Revenue & Customs
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

import models.amp.Amp
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessdetails.BusinessDetails
import models.businessmatching.BusinessMatching
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import services.cache.Cache
import services.encryption.CryptoService

import javax.inject.{Inject, Singleton}

@Singleton
class DataChangeChecker {

  def dataHasChanged(cache: Cache): Boolean =
    Seq(
      cache.getEntry[Asp](Asp.key).fold(false)(_.hasChanged),
      cache.getEntry[Amp](Amp.key).fold(false)(_.hasChanged),
      cache.getEntry[BusinessDetails](BusinessDetails.key).fold(false)(_.hasChanged),
      cache.getEntry[Seq[BankDetails]](BankDetails.key).fold(false)(_.exists(_.hasChanged)),
      cache.getEntry[BusinessActivities](BusinessActivities.key).fold(false)(_.hasChanged),
      cache.getEntry[BusinessMatching](BusinessMatching.key).fold(false)(_.hasChanged),
      cache.getEntry[Eab](Eab.key).fold(false)(_.hasChanged),
      cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key).fold(false)(_.hasChanged),
      cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key).fold(false)(_.exists(_.hasChanged)),
      cache.getEntry[Supervision](Supervision.key).fold(false)(_.hasChanged),
      cache.getEntry[Tcsp](Tcsp.key).fold(false)(_.hasChanged),
      cache.getEntry[Seq[TradingPremises]](TradingPremises.key).fold(false)(_.exists(_.hasChanged)),
      cache.getEntry[Hvd](Hvd.key).fold(false)(_.hasChanged),
      cache.getEntry[Renewal](Renewal.key).fold(false)(_.hasChanged)
    ).exists(identity)
}
