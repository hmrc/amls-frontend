/*
 * Copyright 2020 HM Revenue & Customs
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

import connectors.DataCacheConnector
import javax.inject.Inject
import models.businessmatching._
import models.businessmatching.updateservice.ServiceChangeRegister
import models.flowmanagement.ChangeSubSectorFlowModel
import models.moneyservicebusiness.MoneyServiceBusiness
import models.tradingpremises.TradingPremisesMsbServices.{convertServices, convertSingleService}
import models.tradingpremises.{TradingPremises, TradingPremisesMsbServices}
import utils.AuthAction
import scala.concurrent.{ExecutionContext, Future}

class ChangeSubSectorHelper @Inject()(authAction: AuthAction,
                                      implicit val dataCacheConnector: DataCacheConnector) {

  def requiresPSRNumber(model: ChangeSubSectorFlowModel): Boolean = {
    model.psrNumber match {
      case None => model.subSectors.getOrElse(Set.empty).contains(TransmittingMoney)
      case _ => false
    }
  }

  def createFlowModel(credId: String)(implicit ec: ExecutionContext): Future[ChangeSubSectorFlowModel] = {
    dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key).map {
      case Some(x) => ChangeSubSectorFlowModel(subSectors = x.msbServices.map(_.msbServices), psrNumber = x.businessAppliedForPSRNumber)
      case None => ChangeSubSectorFlowModel()
    }
  }

  def getOrCreateFlowModel(credId: String)(implicit ec: ExecutionContext): Future[ChangeSubSectorFlowModel] = {
    (dataCacheConnector.fetch[ChangeSubSectorFlowModel](credId: String, ChangeSubSectorFlowModel.key) map {
      case Some(x) => Future.successful(x)
      case None => createFlowModel(credId)
    }).flatMap(identity)
  }

  def updateSubSectors(credId: String, model: ChangeSubSectorFlowModel)(implicit ec: ExecutionContext) = for {
    _ <- updateServiceRegister(credId, model)
    msb <- updateMsb(credId, model)
    bm <- updateBusinessMatching(credId, model)
    tp <- updateTradingPremises(credId, model)
  } yield (msb, bm, tp)

  def updateServiceRegister(credId: String, model: ChangeSubSectorFlowModel)
                           (implicit ec: ExecutionContext): Future[ServiceChangeRegister] = {
    dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key) flatMap { maybeBm =>
      dataCacheConnector.update[ServiceChangeRegister](credId, ServiceChangeRegister.key) { maybeRegister =>
        val bmSectors = maybeBm.fold[Set[BusinessMatchingMsbService]](Set.empty) {
          _.msbServices.fold[Set[BusinessMatchingMsbService]](Set.empty)(_.msbServices)
        }

        val newSectors = model.subSectors.getOrElse(Set.empty) diff bmSectors
        val register = maybeRegister.getOrElse(ServiceChangeRegister())
        val mergedSubSectors = register.addedSubSectors.getOrElse(Set.empty) ++ newSectors

        register.copy(addedSubSectors = Some(mergedSubSectors))
      } map {
        _.getOrElse(ServiceChangeRegister())
      }
    }
  }

  def updateMsb(credId: String, model: ChangeSubSectorFlowModel)
               (implicit ec: ExecutionContext): Future[MoneyServiceBusiness] = {

    val updateCE = (msb: MoneyServiceBusiness, newSectors: Set[BusinessMatchingMsbService]) => {
      if (!newSectors.contains(CurrencyExchange)) {
        msb.copy(ceTransactionsInNext12Months = None, whichCurrencies = None)
      } else {
        msb
      }
    }

    val updateMT = (msb: MoneyServiceBusiness, newSectors: Set[BusinessMatchingMsbService]) => {
      if (!newSectors.contains(TransmittingMoney)) {
        msb.copy(
          businessUseAnIPSP = None,
          fundsTransfer = None,
          transactionsInNext12Months = None,
          sendMoneyToOtherCountry = None,
          sendTheLargestAmountsOfMoney = None,
          mostTransactions = None
        )
      } else {
        msb
      }
    }

    dataCacheConnector.fetch[MoneyServiceBusiness](credId, MoneyServiceBusiness.key) flatMap { maybeMsb =>
      val sectorDiff = model.subSectors.getOrElse(Set.empty)
      val msb = maybeMsb.getOrElse(MoneyServiceBusiness())
      val hasAccepted = msb.hasAccepted
      val updatedMsb = updateMT(updateCE(msb, sectorDiff), sectorDiff)

      if (sectorDiff.isEmpty) {
        Future.successful(msb)
      } else {
        // If the msb section is an empty section return none to avoid empty cache entry for MSB
        if (msb == MoneyServiceBusiness()) {
          Future.successful(None)
        } else {
          updateChangeFlag(credId, model) flatMap { isSectionChanged =>
            dataCacheConnector.save[MoneyServiceBusiness](credId, MoneyServiceBusiness.key, updatedMsb.copy(hasChanged = isSectionChanged)) map { _ =>
              updatedMsb.copy(hasAccepted = hasAccepted)
            }
          }
        }
      }
    }
  }

  def updateBusinessMatching(credId: String, model: ChangeSubSectorFlowModel)
                            (implicit ec: ExecutionContext): Future[BusinessMatching] = {

    val updatePsr = (bm: BusinessMatching, newSectors: Set[BusinessMatchingMsbService]) => {
      val updatedBm = bm.copy(msbServices = Some(BusinessMatchingMsbServices(model.subSectors.getOrElse(Set.empty))))

      if (!newSectors.contains(TransmittingMoney)) {
        updatedBm.copy(
          businessAppliedForPSRNumber = None
        )
      } else {
        updatedBm.businessAppliedForPSRNumber(model.psrNumber)
      }
    }

    dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key) flatMap { maybeBm =>
      val sectorDiff = model.subSectors.getOrElse(Set.empty)
      val bm = maybeBm.getOrElse(BusinessMatching())
      val hasAccepted = bm.hasAccepted
      val updatedBm = updatePsr(maybeBm.getOrElse(bm), sectorDiff)

      if (sectorDiff.isEmpty) {
        Future.successful(bm)
      } else {
        dataCacheConnector.save[BusinessMatching](credId, BusinessMatching.key, updatedBm) map { _ =>
          updatedBm.copy(hasAccepted = hasAccepted)
        }
      }
    }
  }

  def updateChangeFlag(credId: String, model: ChangeSubSectorFlowModel)(implicit ec: ExecutionContext) = {

    dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key) flatMap { maybeBm =>
      val bm = maybeBm.getOrElse(BusinessMatching())
      val msbActivitiesFromBm = bm.msbServices.getOrElse(BusinessMatchingMsbServices(Set.empty)).msbServices
      val msbActivitiesFromChangeSubsectors = model.subSectors.getOrElse(Set.empty)

      val diffMsbServices = (msbActivitiesFromBm diff msbActivitiesFromChangeSubsectors) union (msbActivitiesFromChangeSubsectors diff msbActivitiesFromBm)

      if (diffMsbServices.nonEmpty) {
        Future.successful(true)
      } else {
        Future.successful(false)
      }
    }
  }

  def updateTradingPremises(credId: String, model: ChangeSubSectorFlowModel)
                           (implicit ec: ExecutionContext): Future[Seq[TradingPremises]] = {
    if (model.subSectors.getOrElse(Set.empty).isEmpty) {
      Future.successful(Seq.empty)
    } else {
      dataCacheConnector.update[Seq[TradingPremises]](credId, TradingPremises.key) {
        case Some(tp) => tp map {
          case t if hasMsb(t) => applySubSectorsTo(t, model.subSectors.get)
          case t => t
        }
        case None => Seq.empty
      } map {
        _.getOrElse(Seq.empty)
      }
    }
  }

  private def applySubSectorsTo(t: TradingPremises, subSectors: Set[BusinessMatchingMsbService]): TradingPremises = {
    val hasAccepted = t.hasAccepted
    val s = subSectors.map(convertSingleService) intersect t.msbServices.getOrElse(TradingPremisesMsbServices(Set.empty)).services

    t.msbServices(Some(TradingPremisesMsbServices(s match {
      case l if l.isEmpty && subSectors.size == 1 => convertServices(subSectors)
      case _ => s
    }))).copy(hasAccepted = hasAccepted)
  }

  private def hasMsb(tp: TradingPremises): Boolean = tp match {
    case t if t.whatDoesYourBusinessDoAtThisAddress.isDefined
      && t.whatDoesYourBusinessDoAtThisAddress.get.activities.contains(models.businessmatching.MoneyServiceBusiness) => true
    case _ => false
  }
}
