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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}
import models.businessactivities.BusinessActivities
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching._
import models.businessmatching.{BusinessActivities => BMBusinessActivities}
import models.flowmanagement.AddServiceFlowModel
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tradingpremises.TradingPremises
import services.{ResponsiblePeopleService, TradingPremisesService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{RepeatingSection, StatusConstants}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UpdateServiceHelper @Inject()(val authConnector: AuthConnector,
                                    implicit val dataCacheConnector: DataCacheConnector,
                                    val tradingPremisesService: TradingPremisesService,
                                    val responsiblePeopleService: ResponsiblePeopleService
                                   ) extends RepeatingSection {

  def updateBusinessActivities(model: AddServiceFlowModel)(implicit ac: AuthContext, hc: HeaderCarrier): OptionT[Future, BusinessActivities] = {
    OptionT(dataCacheConnector.update[BusinessActivities](BusinessActivities.key) {
      case Some(dcModel) if model.activity.get.equals(AccountancyServices) =>
        dcModel.accountantForAMLSRegulations(None)
          .whoIsYourAccountant(None)
          .taxMatters(None)
          .copy(hasAccepted = true)

      case Some(model) => model
    })
  }

  def updateSupervision(implicit ac: AuthContext, hc: HeaderCarrier) = {
    OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)) flatMap { businessMatching =>
      OptionT.fromOption[Future](businessMatching.activities) flatMap { activities =>
        (OptionT(dataCacheConnector.fetch[Supervision](Supervision.key)) orElse OptionT.some(Supervision())) flatMap { supervision =>
          if (activities.businessActivities.intersect(Set(AccountancyServices, TrustAndCompanyServices)).isEmpty) {
            OptionT.liftF(dataCacheConnector.save[Supervision](Supervision.key, Supervision())) map { _ => Supervision() }
          } else {
            OptionT.some(supervision)
          }
        }
      }
    }
  }

  def updateHasAcceptedFlag(model: AddServiceFlowModel)(implicit ac: AuthContext, hc: HeaderCarrier) =
    OptionT.liftF(dataCacheConnector.save[AddServiceFlowModel](AddServiceFlowModel.key, model.copy(hasAccepted = true)))

  def updateServicesRegister(model: AddServiceFlowModel)(implicit ac: AuthContext, hc: HeaderCarrier):  OptionT[Future, ServiceChangeRegister] =
    OptionT(dataCacheConnector.update[ServiceChangeRegister](ServiceChangeRegister.key) {
      case Some(dcModel@ServiceChangeRegister(Some(activities))) =>
        dcModel.copy(addedActivities = Some(activities +  model.activity.get))
      case _ => ServiceChangeRegister(Some(Set(model.activity.get)))
    })

  def updateTradingPremises(model: AddServiceFlowModel)(implicit ac: AuthContext, hc: HeaderCarrier): OptionT[Future, Seq[TradingPremises]] = for {

    tradingPremises <- OptionT.liftF(tradingPremisesData)
    activity <- OptionT.fromOption[Future](model.activity)
    indices <- OptionT.fromOption[Future](model.tradingPremisesActivities map {
      _.index.toSeq
    }) orElse OptionT.some(Seq.empty)
    msbServices <- OptionT.fromOption[Future](model.tradingPremisesMsbServices) orElse OptionT.some(MsbServices(Set.empty[MsbService]))
    newTradingPremises <- OptionT.some[Future, Seq[TradingPremises]](tradingPremisesService.updateTradingPremises(indices, tradingPremises, activity, Some(msbServices), false))
    _ <- OptionT.liftF(dataCacheConnector.save[Seq[TradingPremises]](TradingPremises.key, newTradingPremises))
  } yield tradingPremises

  def tradingPremisesData(implicit hc: HeaderCarrier, ac: AuthContext): Future[Seq[TradingPremises]] =
    getData[TradingPremises].map {
      _.filterNot(tp => tp.status.contains(StatusConstants.Deleted) | !tp.isComplete)
    }

  def updateBusinessMatching(model: AddServiceFlowModel)(implicit hc: HeaderCarrier, ac: AuthContext): OptionT[Future, BusinessMatching] = {
    for {
      newActivity <- OptionT.fromOption[Future](model.activity)
      newMsbServices <- OptionT.fromOption[Future](model.msbServices) orElse OptionT.some(MsbServices(Set.empty[MsbService]))
      currentBusinessMatching <- OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key))
      currentActivities <- OptionT.fromOption[Future](currentBusinessMatching.activities) orElse OptionT.some(BMBusinessActivities(Set.empty[BusinessActivity]))
      newBusinessMatching <- {
        OptionT(dataCacheConnector.update[BusinessMatching](BusinessMatching.key) {
          case Some(bm) if newActivity equals MoneyServiceBusiness  =>
            val currentMsbServices = currentBusinessMatching.msbServices.getOrElse(MsbServices(Set.empty))
            bm.activities(currentActivities.copy(businessActivities = currentActivities.businessActivities + newActivity))
              .msbServices(currentMsbServices.copy(msbServices = currentMsbServices.msbServices ++ newMsbServices.msbServices))
              .copy(hasAccepted = true)
          case Some(bm) =>
            bm.activities(currentActivities.copy(businessActivities = currentActivities.businessActivities + newActivity))
              .copy(hasAccepted = true)
        })
      }
    } yield newBusinessMatching
  }

  def updateResponsiblePeople(model: AddServiceFlowModel)(implicit hc: HeaderCarrier, ac: AuthContext): OptionT[Future, Seq[ResponsiblePeople]] = {
    val indices = model.responsiblePeople.fold[Set[Int]](Set.empty)(_.index)

    OptionT(dataCacheConnector.update[Seq[ResponsiblePeople]](ResponsiblePeople.key) {
      case Some(people) if model.activity.contains(TrustAndCompanyServices) || model.activity.contains(MoneyServiceBusiness) =>
        responsiblePeopleService.updateFitAndProperFlag(people, indices)
      case Some(people) => people
    })
  }

  def clearFlowModel()(implicit hc: HeaderCarrier, ac: AuthContext): OptionT[Future, AddServiceFlowModel] =
    OptionT(dataCacheConnector.update[AddServiceFlowModel](AddServiceFlowModel.key)(_ => AddServiceFlowModel()))

}
