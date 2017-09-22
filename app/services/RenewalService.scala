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

package services

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import config.ApplicationConfig
import connectors.DataCacheConnector
import models.businessmatching._
import models.moneyservicebusiness.{MoneyServiceBusiness => moneyServiceBusiness}
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.renewal._
import play.api.mvc.Call
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
              case Renewal(None, None, None, None, _, _, _, _, _, _, _, _, _, _, _) => Future.successful(notStarted)
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

    val isComplete = for {
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
      } else if (activities.contains(HighValueDealing)) {
        checkCompletionOfHvd(renewal)
      } else {
        if (ApplicationConfig.hasAcceptedToggle) {
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, _, _, _, _, _, _, _, _, true) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, _, _, _, _, _, _, _, _, true) => true
            case _ => false
          }
        } else {
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, _, _, _, _, _, _, _, _, _) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, _, _, _, _, _, _, _, _, _) => true
            case _ => false
          }
        }
      }
    }

    isComplete.getOrElse(false)

  }

  private def checkCompletionOfMsbAndHvd(renewal: Renewal, msbServices: Option[MsbServices]) = {

    val maybeCountry = renewal.customersOutsideUK.flatMap {
      case CustomersOutsideUK(Some(country)) => Some(country)
      case _ => None
    }

    val sendsMoneyToOtherCountry = renewal.sendMoneyToOtherCountry match {
      case Some(x) if x.money => true
      case _ => false
    }
    if (ApplicationConfig.hasAcceptedToggle) {
      msbServices match {
        case Some(x) if x.msbServices.contains(CurrencyExchange) & x.msbServices.contains(TransmittingMoney) => {
          sendsMoneyToOtherCountry match {
            case true =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), true) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), true) => true
                case _ => false
              }
            case false =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, Some(_), _, _, true) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, Some(_), _, _, true) => true
                case _ => false
              }
          }
        }
        case Some(x) if x.msbServices.contains(CurrencyExchange) =>
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, Some(_), _, _, true) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, Some(_), _, _, true) => true
            case _ => false
          }
        case Some(x) if x.msbServices.contains(TransmittingMoney) => {
          sendsMoneyToOtherCountry match {
            case true =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), Some(_), Some(_), _, _, Some(_), true) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), Some(_), Some(_), _, _, Some(_), true) => true
                case _ => false
              }
            case false =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), _, _, _, _, _, true) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), _, _, _, _, _, true) => true
                case _ => false
              }
          }
        }
        case _ =>
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, _, _, _, _, true) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, _, _, _, _, true) => true
            case _ => false
          }
      }
    } else {
      msbServices match {
        case Some(x) if x.msbServices.contains(CurrencyExchange) & x.msbServices.contains(TransmittingMoney) => {
          sendsMoneyToOtherCountry match {
            case true =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), _) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), _) => true
                case _ => false
              }
            case false =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, Some(_), _, _, _) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, Some(_), _, _, _) => true
                case _ => false
              }
          }
        }
        case Some(x) if x.msbServices.contains(CurrencyExchange) =>
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, Some(_), _, _, _) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, Some(_), _, _, _) => true
            case _ => false
          }
        case Some(x) if x.msbServices.contains(TransmittingMoney) => {
          sendsMoneyToOtherCountry match {
            case true =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), Some(_), Some(_), _, _, Some(_), _) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), Some(_), Some(_), _, _, Some(_), _) => true
                case _ => false
              }
            case false =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), _, _, _, _, _, _) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), _, _, _, _, _, _) => true
                case _ => false
              }
          }
        }
        case _ =>
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, _, _, _, _, _) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, _, _, _, _, _) => true
            case _ => false
          }
      }
    }
  }


  private def checkCompletionOfMsb(renewal: Renewal, msbServices: Option[MsbServices]) = {

    val maybeCountry = renewal.customersOutsideUK.flatMap {
      case CustomersOutsideUK(Some(country)) => Some(country)
      case _ => None
    }

    val sendsMoneyToOtherCountry = renewal.sendMoneyToOtherCountry match {
      case Some(x) if x.money => true
      case _ => false
    }

    if (ApplicationConfig.hasAcceptedToggle) {
      msbServices match {
        case Some(x) if x.msbServices.contains(CurrencyExchange) & x.msbServices.contains(TransmittingMoney) => {
          sendsMoneyToOtherCountry match {
            case true =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), true) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), true) => true
                case _ => false
              }
            case false =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), Some(_), Some(_), _, _, Some(_), _, _, true) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), Some(_), Some(_), _, _, Some(_), _, _, true) => true
                case _ => false
              }
          }
        }
        case Some(x) if x.msbServices.contains(CurrencyExchange) =>
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), Some(_), _, _, _, Some(_), _, _, true) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), Some(_), _, _, _, Some(_), _, _, true) => true
            case _ => false
          }
        case Some(x) if x.msbServices.contains(TransmittingMoney) => {
          sendsMoneyToOtherCountry match {
            case true =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), _, Some(_), Some(_), Some(_), _, _, Some(_), true) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), _, Some(_), Some(_), Some(_), _, _, _, true) => true
                case _ => false
              }
            case false =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), _, Some(_), _, _, _, _, _, true) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), _, Some(_), _, _, _, _, _, true) => true
                case _ => false
              }
          }
        }
        case _ =>
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), _, _, _, _, _, _, _, true) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), _, _, _, _, _, _, _, true) => true
            case _ => false
          }
      }
    } else {
      msbServices match {
        case Some(x) if x.msbServices.contains(CurrencyExchange) & x.msbServices.contains(TransmittingMoney) => {
          sendsMoneyToOtherCountry match {
            case true =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), _) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), Some(_), Some(_), Some(_), Some(_), Some(_), _, Some(_), _) => true
                case _ => false
              }
            case false =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), Some(_), Some(_), _, _, Some(_), _, _, _) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), Some(_), Some(_), _, _, Some(_), _, _, _) => true
                case _ => false
              }
          }
        }
        case Some(x) if x.msbServices.contains(CurrencyExchange) =>
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), Some(_), _, _, _, Some(_), _, _, _) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), Some(_), _, _, _, Some(_), _, _, _) => true
            case _ => false
          }
        case Some(x) if x.msbServices.contains(TransmittingMoney) => {
          sendsMoneyToOtherCountry match {
            case true =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), _, Some(_), Some(_), Some(_), _, _, Some(_), _) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), _, Some(_), Some(_), Some(_), _, _, _, _) => true
                case _ => false
              }
            case false =>
              renewal match {
                case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), _, Some(_), _, _, _, _, _, _) => true
                case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), _, Some(_), _, _, _, _, _, _) => true
                case _ => false
              }
          }
        }
        case _ =>
          renewal match {
            case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _, Some(_), _, _, _, _, _, _, _, _) => true
            case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _, Some(_), _, _, _, _, _, _, _, _) => true
            case _ => false
          }
      }
    }
  }

  private def checkCompletionOfHvd(renewal: Renewal) = {
    if (ApplicationConfig.hasAcceptedToggle) {
      renewal match {
        case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, _, _, _, _, _, true) => true
        case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), _, _, _, _, _, _, _, _, true) => true
        case _ => false
      }
    } else {
      renewal match {
        case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, _, _, _, _, _, _) => true
        case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), Some(_), Some(_), _, _, _, _, _, _, _, _, _) => true
        case _ => false
      }
    }
  }

  private def amendmentDeclarationAvailable(sections: Seq[Section]) = {
    sections.foldLeft((true, false)) { (acc, s) =>
      (acc._1 && s.status == Completed, acc._2 || s.hasChanged)
    } match {
      case (true, true) => true
      case _ => false
    }
  }

  def canSubmit(renewalSection: Section, variationSections: Seq[Section]) = {
    !renewalSection.status.equals(Started) && ((renewalSection.status == Completed && renewalSection.hasChanged) | amendmentDeclarationAvailable(variationSections))
  }

}
