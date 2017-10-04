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

package controllers

import javax.inject.{Inject, Singleton}

import config.ApplicationConfig
import connectors.DataCacheConnector
import models.aboutthebusiness.{AboutTheBusiness, PreviouslyRegisteredNo, PreviouslyRegisteredYes}
import models.businessmatching.{BusinessMatching, MoneyServiceBusiness, TrustAndCompanyServices}
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import play.api.i18n.Messages
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class FeeGuidanceController @Inject()(val authConnector: AuthConnector,
                                      val dataCacheConnector: DataCacheConnector) extends BaseController with ServicesConfig {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      getBreakdownRows() map { rows =>
        val total = getTotal(rows)
        Ok(views.html.fee_guidance(total, rows))
      }
  }

  private def getBreakdownRows()(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Seq[BreakdownRow]] = {

    val submissionFee = ApplicationConfig.regFee
    val premisesFee = ApplicationConfig.premisesFee
    val peopleFee = ApplicationConfig.peopleFee

    dataCacheConnector.fetchAll map { optCacheMap =>
      (for {
        cacheMap <- optCacheMap
        responsiblepeople <- cacheMap.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
        tradingpremises <- cacheMap.getEntry[Seq[TradingPremises]](TradingPremises.key)
        aboutthebusiness <- cacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key)
        businessmatching <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
        previouslyRegistered <- aboutthebusiness.previouslyRegistered
        activities <- businessmatching.activities
      } yield {

        val submissionCount = previouslyRegistered match {
          case PreviouslyRegisteredYes(_) => 0
          case PreviouslyRegisteredNo => 1
        }

        val submissionRow = BreakdownRow(Messages("confirmation.submission"), submissionCount, Currency(submissionFee), Currency(submissionCount * submissionFee))

        val peopleCount = {
          if (activities.businessActivities.contains(MoneyServiceBusiness) || activities.businessActivities.contains(TrustAndCompanyServices)) {
            responsiblepeople.count { responsiblePerson =>
              !responsiblePerson.hasAlreadyPassedFitAndProper.contains(true)
            }
          } else {
            0
          }
        }
        val peopleRow = BreakdownRow(Messages("summary.responsiblepeople"), peopleCount, Currency(peopleFee), Currency(peopleCount * peopleFee))

        val premisesCount = tradingpremises.count(_ != TradingPremises())
        val premisesRow = BreakdownRow(Messages("summary.tradingpremises"), premisesCount, Currency(premisesFee), Currency(premisesCount * premisesFee))

        Seq(submissionRow, peopleRow, premisesRow).filter(row =>
          !row.quantity.equals(0)
        )

      }) getOrElse Seq.empty
    }

  }

  private def getTotal(breakdownRows: Seq[BreakdownRow]): Int = {
    def t(total: BigDecimal = 0, rows: Seq[BreakdownRow] = breakdownRows): BigDecimal = {
      if (rows.isEmpty) {
        total
      } else {
        t(total + rows.head.total.value, rows.tail)
      }
    }

    t().toInt
  }

}
