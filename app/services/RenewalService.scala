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
import cats.data.Validated.Valid
import cats.implicits._
import connectors.DataCacheConnector
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, ForeignExchange, TransmittingMoney}
import models.businessmatching._
import models.registrationprogress.{Completed, NotStarted, Started, TaskRow, Updated}
import models.renewal.Renewal.ValidationRules._
import models.renewal._
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.MappingUtils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RenewalService @Inject()(dataCache: DataCacheConnector) {

  def getTaskRow(credId: String)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, messages: Messages) = {

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

  def getRenewal(cacheId: String)(implicit headerCarrier: HeaderCarrier): Future[Option[Renewal]] =
    dataCache.fetch[Renewal](cacheId, Renewal.key)

  // TODO make private and update usages to new update function
  def updateRenewal(credId: String, renewal: Renewal)(implicit headerCarrier: HeaderCarrier): Future[CacheMap] =
    dataCache.save[Renewal](credId, Renewal.key, renewal)

  def isRenewalComplete(renewal: Renewal, credId: String)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

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

        case _ => standardRule.validate(renewal) match {
          case Valid(_) => true
          case _ => false
        }
      }
    }

    isComplete.getOrElse(false)
  }

  def isCachePresent(credId: String)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext) = {

    val isCache = for {
      cache <- dataCache.fetchAll(credId)
    } yield cache match {
      case Some(c) => true
      case _ => false
    }

    isCache
  }

  private def checkCompletionOfMsb(renewal: Renewal, msbServices: Option[BusinessMatchingMsbServices]) = {

    val validationRule = compileOpt {
      Seq(
        Some(msbRule),
        if (msbServices.exists(_.msbServices.contains(TransmittingMoney))) Some(moneyTransmitterRule) else None,
        if (msbServices.exists(_.msbServices.contains(CurrencyExchange))) Some(currencyExchangeRule) else None,
        if (msbServices.exists(_.msbServices.contains(ForeignExchange))) Some(foreignExchangeRule) else None,
        Some(standardRule)
      )
    }

    // Validate the renewal object using the composed chain of validation rules
    validationRule.validate(renewal) match {
      case Valid(_) => true
      case r => false
    }
  }

  private def checkCompletionOfAMP(renewal: Renewal) = {

    val validationRule = compileOpt {
      Seq(
        Some(ampRule),
        Some(standardRule)
      )
    }
    
    validationRule.validate(renewal) match {
      case Valid(_) => true
      case r => false
    }
  }

  private def checkCompletionOfAsp(renewal: Renewal) = {

    val validationRule = compileOpt {
      Seq(
        Some(aspRule),
        Some(standardRule)
      )
    }

    // Validate the renewal object using the composed chain of validation rules
    validationRule.validate(renewal) match {
      case Valid(_) => true
      case r => false
    }
  }

  private def checkCompletionOfHvd(renewal: Renewal) = {

    val validationRule = compileOpt {
      Seq(
        Some(hvdRule),
        Some(standardRule)
      )
    }

    // Validate the renewal object using the composed chain of validation rules
    validationRule.validate(renewal) match {
      case Valid(_) => true
      case r => false
    }
  }

  def canSubmit(renewalSection: TaskRow, variationSections: Seq[TaskRow]) = {
    variationSections.forall(row => row.status == Completed || row.status == Updated) &&
            !renewalSection.status.equals(Started) &&
            (variationSections :+ renewalSection).exists(_.hasChanged)
  }

  def getFirstBusinessActivityInLowercase(cacheId: String)
                                         (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Option[String]] = {
    getBusinessMatching(cacheId)
      .map(optBm => optBm.flatMap(bm => bm.alphabeticalBusinessActivitiesLowerCase())
        .flatMap(activities => if (activities.length == 1) activities.headOption else None))
  }

  def getBusinessMatching(cacheId: String)(implicit headerCarrier: HeaderCarrier): Future[Option[BusinessMatching]] =
    dataCache.fetch[BusinessMatching](cacheId, BusinessMatching.key)

  /*
   TODO:
    - Update controllers usages of updateRenewal with this
    - Make old method private
   */
  def fetchAndUpdateRenewal(credId: String, updateAction: Renewal => Renewal)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheMap]] = {
    for {
      renewal <- OptionT(getRenewal(credId))
      updatedCache <- OptionT.liftF(updateRenewal(credId, updateAction(renewal)))
    } yield {
      updatedCache
    }
  }.value
}
