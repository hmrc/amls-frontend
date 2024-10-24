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

package services

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, ForeignExchange, TransmittingMoney}
import models.businessmatching._
import models.registrationprogress._
import models.renewal._
import models.status.ReadyForRenewal
import play.api.i18n.Messages
import services.RenewalService.BusinessAndOtherActivities
import services.cache.Cache
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RenewalService @Inject()(
  dataCache: DataCacheConnector,
  statusService: StatusService
)(implicit ec: ExecutionContext) {

  def isRenewalFlow(
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    cacheId: String)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[Boolean] = {

    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) flatMap {
      case ReadyForRenewal(_) =>
        dataCache.fetch[Renewal](cacheId, Renewal.key) map {
          case Some(_) => true
          case None => false
        }
      case _ => Future.successful(false)
    }
  }

  def getTaskRow(credId: String)(implicit messages: Messages): Future[TaskRow] = {

    val notStarted = TaskRow(
      Renewal.sectionKey,
      controllers.renewal.routes.WhatYouNeedController.get.url,
      hasChanged = false,
      NotStarted,
      TaskRow.notStartedTag
    )

    this.getRenewal(credId).flatMap {
      case Some(model) =>
        isRenewalComplete(model, credId) flatMap { x =>
          if (x) {
            Future.successful(
              TaskRow(
                Renewal.sectionKey,
                controllers.renewal.routes.SummaryController.get.url,
                model.hasChanged,
                Completed,
                TaskRow.completedTag
              )
            )
          } else {
            model match {
              case Renewal(None, None, None, None, None, _, _, _, _, _, _, _, _, _, _, _, _, _) => Future.successful(notStarted)
              case _ => Future.successful(
                TaskRow(
                  Renewal.sectionKey,
                  controllers.renewal.routes.WhatYouNeedController.get.url,
                  model.hasChanged,
                  Started,
                  TaskRow.incompleteTag
                )
              )
            }
          }
        }
      case _ => Future.successful(notStarted)
    }
  }

  def getRenewal(cacheId: String): Future[Option[Renewal]] =
    dataCache.fetch[Renewal](cacheId, Renewal.key)

  // TODO make private and update usages to new update function
  def updateRenewal(credId: String, renewal: Renewal): Future[Cache] =
    dataCache.save[Renewal](credId, Renewal.key, renewal)

  def isRenewalComplete(renewal: Renewal, credId: String)(implicit ec: ExecutionContext): Future[Boolean] = {

    val isComplete = for {
      cache <- OptionT(dataCache.fetchAll(credId))
      businessMatching <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
      activities <- OptionT.fromOption[Future](businessMatching.activities)
    } yield {

      activities.businessActivities collect {
        case MoneyServiceBusiness => checkCompletionOfMsb(renewal, businessMatching.msbServices)
        case HighValueDealing => checkCompletionOfHvd(renewal)
        case AccountancyServices => checkCompletionOfAsp(renewal)
        case ArtMarketParticipant => checkCompletionOfAMP(renewal)
      } match {
        case s if s.nonEmpty => s.forall(identity)
        case _ => renewal.standardRule
      }
    }

    isComplete.getOrElse(false)
  }

  def isCachePresent(credId: String)(implicit ec: ExecutionContext): Future[Boolean] = {

    val isCache = for {
      cache <- dataCache.fetchAll(credId)
    } yield cache match {
      case Some(c) => true
      case _ => false
    }

    isCache
  }

  private def checkCompletionOfMsb(renewal: Renewal, msbServices: Option[BusinessMatchingMsbServices]): Boolean = {
    Seq(
      Some(renewal.totalThroughput.isDefined),
      if (msbServices.exists(_.msbServices.contains(TransmittingMoney))) Some(renewal.moneyTransmitterRule) else None,
      if (msbServices.exists(_.msbServices.contains(CurrencyExchange))) Some(renewal.currencyExchangeRule) else None,
      if (msbServices.exists(_.msbServices.contains(ForeignExchange))) Some(renewal.fxTransactionsInLast12Months.isDefined) else None,
      Some(renewal.standardRule)
    ).flatten.forall(identity)
  }

  private def checkCompletionOfAMP(renewal: Renewal): Boolean = {
    Seq(
      renewal.ampTurnover.isDefined,
      renewal.standardRule
    ).forall(identity)
  }

  private def checkCompletionOfAsp(renewal: Renewal): Boolean = {
    Seq(
      renewal.aspRule,
      renewal.standardRule
    ).forall(identity)
  }

  private def checkCompletionOfHvd(renewal: Renewal): Boolean = {
    Seq(
      renewal.hvdRule,
      renewal.standardRule
    ).forall(identity)
  }

  def canSubmit(renewalSection: TaskRow, variationSections: Seq[TaskRow]): Boolean = {
    variationSections.forall(row => row.status == Completed || row.status == Updated) &&
      !renewalSection.status.equals(Started) &&
      (variationSections :+ renewalSection).exists(_.hasChanged)
  }

  def getFirstBusinessActivityInLowercase(cacheId: String)
                                         (implicit ec: ExecutionContext, messages: Messages): Future[Option[String]] = {
    getBusinessMatching(cacheId)
      .map(optBm => optBm.flatMap(bm => bm.alphabeticalBusinessActivitiesLowerCase())
        .flatMap(activities => if (activities.length == 1) activities.headOption else None))
  }

  def getBusinessMatching(cacheId: String): Future[Option[BusinessMatching]] =
    dataCache.fetch[BusinessMatching](cacheId, BusinessMatching.key)

  /*
   TODO:
    - Update controllers usages of updateRenewal with this
    - Make old method private
   */
  def fetchAndUpdateRenewal(credId: String, updateAction: Renewal => Renewal): Future[Option[Cache]] = {
    for {
      renewal <- OptionT(getRenewal(credId))
      updatedCache <- OptionT.liftF(updateRenewal(credId, updateAction(renewal)))
    } yield {
      updatedCache
    }
  }.value

  def createOrUpdateRenewal(credId: String, updateAction: Renewal => Renewal, newRenewal: Renewal): Future[Cache] = {
    getRenewal(credId).flatMap { optRenewal =>
      if (optRenewal.isEmpty) {
        dataCache.save(credId, Renewal.key, newRenewal)
      } else {
        updateRenewal(credId, updateAction(optRenewal.head))
      }
    }
  }

  def updateOtherBusinessActivities(credId: String, involvedInOtherYes: InvolvedInOtherYes): Future[Option[BusinessAndOtherActivities]] = {
    getRenewal(credId)
      .map { optRenewal =>
        optRenewal.map(renewal => updateRenewal(credId, renewal.copy(involvedInOtherActivities = Some(involvedInOtherYes))))
      }
      .flatMap { _ =>
        getBusinessMatching(credId).map { optBusinessMatching =>
          optBusinessMatching.flatMap { businessMatching =>
            businessMatching.activities.map { businessActivities =>
              BusinessAndOtherActivities(businessActivities.businessActivities, involvedInOtherYes)
            }
          }
        }
      }
  }
}

object RenewalService {
  case class BusinessAndOtherActivities(businessActivities: Set[BusinessActivity], involvedInOtherYes: InvolvedInOtherYes)
}