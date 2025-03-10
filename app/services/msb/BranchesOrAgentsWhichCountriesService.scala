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

package services.msb

import connectors.DataCacheConnector
import models.moneyservicebusiness.{BranchesOrAgents, BranchesOrAgentsHasCountries, BranchesOrAgentsWhichCountries, MoneyServiceBusiness}
import play.api.mvc.Result

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BranchesOrAgentsWhichCountriesService @Inject() (val dataCacheConnector: DataCacheConnector)(implicit
  ec: ExecutionContext
) {

  def fetchBranchesOrAgents(credId: String): Future[Option[BranchesOrAgentsWhichCountries]] =
    dataCacheConnector.fetch[MoneyServiceBusiness](credId, MoneyServiceBusiness.key).map { optMsb =>
      for {
        msb      <- optMsb
        boa      <- msb.branchesOrAgents
        branches <- boa.branches
      } yield branches
    }

  def fetchAndSaveBranchesOrAgents(
    credId: String,
    data: BranchesOrAgentsWhichCountries,
    redirect: Result
  ): Future[Result] =
    for {
      msb <- dataCacheConnector.fetch[MoneyServiceBusiness](credId, MoneyServiceBusiness.key)
      _   <- dataCacheConnector.save[MoneyServiceBusiness](
               credId,
               MoneyServiceBusiness.key,
               msb.branchesOrAgents(
                 BranchesOrAgents.update(
                   msb.branchesOrAgents.getOrElse(BranchesOrAgents(BranchesOrAgentsHasCountries(false), None)),
                   data
                 )
               )
             )
    } yield redirect
}
