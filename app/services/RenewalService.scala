package services

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import models.businessmatching._
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.renewal.{InvolvedInOtherNo, InvolvedInOtherYes, Renewal}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RenewalService @Inject()(dataCache: DataCacheConnector) {

  def getSection(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext) = {

    val notStarted = Section("renewal", NotStarted, hasChanged = false, controllers.renewal.routes.WhatYouNeedController.get())

    this.getRenewal flatMap {
      case Some(model) =>
        isRenewalComplete(model) flatMap { x =>
          if (x) {
            Future.successful(Section("renewal", Completed, model.hasChanged, controllers.renewal.routes.SummaryController.get()))
          } else {
            model match {
              case Renewal(None, None, None, None, _, _, _, _, _, _, _, _, _) => Future.successful(notStarted)
              case _ => Future.successful(Section("renewal", Started, model.hasChanged, controllers.renewal.routes.WhatYouNeedController.get()))
            }
          }
        }
      case _ => Future.successful(notStarted)
    }
  }

  def getRenewal(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext) =
    dataCache.fetch[Renewal](Renewal.key)

  def updateRenewal(renewal: Renewal)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext) =
    dataCache.save[Renewal](Renewal.key, renewal)

  def isRenewalComplete(renewal: Renewal)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext) = {

    val test = for {
      cache <- OptionT(dataCache.fetchAll)
      bm <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
      ba <- OptionT.fromOption[Future](bm.activities)
    } yield {

      val activities = ba.businessActivities
      val msbServices = bm.msbServices

      if (activities.contains(MoneyServiceBusiness) && activities.contains(HighValueDealing)) {
        checkCompletionOfMsbAndHvd(renewal, msbServices)
      } else if (activities.contains(MoneyServiceBusiness)) {
        checkCompletionOfMsb(renewal, msbServices)
      } else if(activities.contains(HighValueDealing)) {
        checkCompletionOfHvd(renewal)
      } else {
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), None, None, None, None, None, None, None, None, _) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), None, None, None, None, None, None, None, None, _) => true
            case _ => false
          }
        }
    }

    test.getOrElse(false)

  }

  private def checkCompletionOfMsbAndHvd(renewal: Renewal, msbServices: Option[MsbServices]) = {

    msbServices match {
      case Some(x) if x.msbServices.contains(CurrencyExchange) =>
        renewal match {
          case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _) => true
          case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _) => true
          case _ => false
        }
      case _ =>
        renewal match {
        case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), None, Some(_), Some(_), Some(_), None, _) => true
        case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), None, Some(_), Some(_), Some(_), None, _) => true
        case _ => false
      }
    }
  }

  private def checkCompletionOfMsb(renewal: Renewal, msbServices: Option[MsbServices]) = {

    msbServices match {
      case Some(x) if x.msbServices.contains(CurrencyExchange) =>
        renewal match {
          case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), None, None, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _) => true
          case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), None, None, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _) => true
          case _ => false
        }
      case _ =>
        renewal match {
          case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), None, None, Some(_), None, Some(_), Some(_), Some(_), None, _) => true
          case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), None, None, Some(_), None, Some(_), Some(_), Some(_), None, _) => true
          case _ => false
        }
    }
  }

  private def checkCompletionOfHvd(renewal: Renewal) = {
    renewal match {
      case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), None, None, None, None, None, None, _) => true
      case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), None, None, None, None, None, None, _) => true
      case _ => false
    }
  }
}
