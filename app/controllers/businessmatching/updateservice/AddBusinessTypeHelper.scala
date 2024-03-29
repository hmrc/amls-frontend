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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import config.ApplicationConfig
import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}
import models.businessactivities.BusinessActivities
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching._
import models.businessmatching.{BusinessActivities => BMBusinessActivities}
import models.businessmatching.BusinessActivity._
import models.flowmanagement.AddBusinessTypeFlowModel
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tradingpremises.TradingPremises
import play.api.i18n.Messages
import services.{ResponsiblePeopleService, TradingPremisesService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, RepeatingSection, StatusConstants}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessTypeHelper @Inject()(authAction: AuthAction,
                                      implicit val dataCacheConnector: DataCacheConnector,
                                      val tradingPremisesService: TradingPremisesService,
                                      val responsiblePeopleService: ResponsiblePeopleService,
                                      val appConfig:ApplicationConfig) extends RepeatingSection {

  def updateBusinessActivities(credId: String, model: AddBusinessTypeFlowModel)(implicit hc: HeaderCarrier): OptionT[Future, BusinessActivities] = {
    OptionT(dataCacheConnector.update[BusinessActivities](credId, BusinessActivities.key) {
      case Some(dcModel) if model.activity.contains(AccountancyServices) =>
        dcModel.accountantForAMLSRegulations(None)
          .whoIsYourAccountant(None)
          .taxMatters(None)
          .copy(hasAccepted = true)

      case Some(dcModel) => dcModel
    })
  }

  def updateSupervision(credId: String)(implicit hc: HeaderCarrier ,ec: ExecutionContext) = {
    val emptyModel = Supervision(hasAccepted = true)

    for {
      businessMatching <- OptionT(dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key))
      activities <- OptionT.fromOption[Future](businessMatching.activities)
      supervision <- OptionT(dataCacheConnector.fetch[Supervision](credId, Supervision.key)) orElse OptionT.some(emptyModel)
      newSupervision <- if (activities.businessActivities.intersect(Set(AccountancyServices, TrustAndCompanyServices)).isEmpty) {
        OptionT.liftF(dataCacheConnector.save[Supervision](credId, Supervision.key, emptyModel)) map { _ => emptyModel }
      } else {
        OptionT.liftF(Future.successful(supervision))
      }
    } yield newSupervision
  }

  def updateHasAcceptedFlag(credId: String, model: AddBusinessTypeFlowModel)(implicit hc: HeaderCarrier ,ec: ExecutionContext) =
    OptionT.liftF(dataCacheConnector.save[AddBusinessTypeFlowModel](credId, AddBusinessTypeFlowModel.key, model.copy(hasAccepted = true)))

  def updateServicesRegister(credId: String, model: AddBusinessTypeFlowModel)(implicit hc: HeaderCarrier ,ec: ExecutionContext):  OptionT[Future, ServiceChangeRegister] = {
    for {
      activity <- OptionT.fromOption[Future](model.activity)
      updatedModel <- OptionT(dataCacheConnector.update[ServiceChangeRegister](credId, ServiceChangeRegister.key) {
        case Some(dcModel@ServiceChangeRegister(Some(activities), _)) =>
          dcModel.copy(addedActivities = Some(activities + activity))
        case _ => ServiceChangeRegister(Some(Set(activity)))
      })
    } yield updatedModel
  }

  def tradingPremisesData(credId: String)(implicit hc: HeaderCarrier ,ec: ExecutionContext): Future[Seq[TradingPremises]] =
    getData[TradingPremises](credId).map {
      _.filterNot(tp => tp.status.contains(StatusConstants.Deleted))
    }

  def updateBusinessMatching(credId: String, model: AddBusinessTypeFlowModel)(implicit hc: HeaderCarrier ,ec: ExecutionContext): OptionT[Future, BusinessMatching] = {
    for {
      newActivity <- OptionT.fromOption[Future](model.activity)
      newMsbServices <- OptionT.fromOption[Future](model.subSectors) orElse OptionT.some(BusinessMatchingMsbServices(Set.empty[BusinessMatchingMsbService]))
      currentBusinessMatching <- OptionT(dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key))
      currentActivities <- OptionT.fromOption[Future](currentBusinessMatching.activities) orElse OptionT.some(BMBusinessActivities(Set.empty[BusinessActivity]))

      newBusinessMatching <- {

        OptionT(dataCacheConnector.update[BusinessMatching](credId, BusinessMatching.key) {
          case Some(bm) if newActivity equals MoneyServiceBusiness  =>
            val currentMsbServices = currentBusinessMatching.msbServices.getOrElse(BusinessMatchingMsbServices(Set.empty))
            val newPsrNumber = model.businessAppliedForPSRNumber
            bm.activities(currentActivities.copy(businessActivities = currentActivities.businessActivities + newActivity))
              .msbServices(Some(currentMsbServices.copy(msbServices = currentMsbServices.msbServices ++ newMsbServices.msbServices)))
              .businessAppliedForPSRNumber(newPsrNumber)
              .copy(hasAccepted = true)
          case Some(bm) => {
            bm.activities(currentActivities.copy(businessActivities = currentActivities.businessActivities + newActivity))
              .copy(hasAccepted = true)
          }

        })
      }
    } yield newBusinessMatching
  }

  def isMsbOrTcsp(model: AddBusinessTypeFlowModel) = {
    model.activity.contains(TrustAndCompanyServices) || model.activity.contains(MoneyServiceBusiness)
  }

  def clearFlowModel(credId: String)(implicit hc: HeaderCarrier): OptionT[Future, AddBusinessTypeFlowModel] =
    OptionT(dataCacheConnector.update[AddBusinessTypeFlowModel](credId, AddBusinessTypeFlowModel.key)(_ => AddBusinessTypeFlowModel()))

  def prefixedActivities(businessMatching: BusinessMatching)(implicit messages: Messages) =
    businessMatching.prefixedAlphabeticalBusinessTypes(true) match {
    case Some(activities) if activities.size == 1 => Set(activities.head)
    case Some(_) => businessMatching.alphabeticalBusinessActivitiesLowerCase().getOrElse(Set.empty[String]).toSet
    case _ => Set.empty[String]
  }

  def tradingPremises(credId: String)(implicit hc: HeaderCarrier ,ec: ExecutionContext): Future[Seq[(TradingPremises, Int)]] = {
    getData[TradingPremises](credId).map {
      _.zipWithIndex.filterNot { case (tp, _) =>
        tp.status.contains(StatusConstants.Deleted) | !tp.isComplete
      }
    }
  }

  def responsiblePeople(credId: String)(implicit hc: HeaderCarrier ,ec: ExecutionContext): Future[Seq[(ResponsiblePerson, Int)]] = {
    getData[ResponsiblePerson](credId).map {
      _.zipWithIndex.filterNot { case (rp, _) =>
        rp.status.contains(StatusConstants.Deleted) | !rp.isComplete
      }
    }
  }
}
