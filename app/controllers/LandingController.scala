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

package controllers

import audit.ServiceEntrantEvent
import cats.data.Validated.{Invalid, Valid}
import config.{ AMLSAuthConnector, AmlsShortLivedCache, ApplicationConfig}
import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessMatching
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.{AmendVariationRenewalResponse, FormTypes, SubscriptionResponse}
import play.api.mvc.{Action, Call, Request, Result}
import services.{AuthEnrolmentsService, LandingService}
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import services.AuthService

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class LandingController @Inject()(val landingService: LandingService,
                                  val enrolmentsService: AuthEnrolmentsService,
                                  val auditConnector: AuditConnector,
                                  val authService: AuthService,
                                  val cacheConnector: DataCacheConnector,
                                  val authConnector: AuthConnector = AMLSAuthConnector
                                ) extends BaseController {

  val shortLivedCache: ShortLivedCache = AmlsShortLivedCache

  private def isAuthorised(implicit headerCarrier: HeaderCarrier) =
    headerCarrier.authorization.isDefined

  /**
    * allowRedirect allows us to configure whether or not the start page is *always* shown,
    * regardless of the user's auth status
    */
  def start(allowRedirect: Boolean = true) = Action.async {
    implicit request =>
      if (isAuthorised && allowRedirect) {
        Future.successful(Redirect(controllers.routes.LandingController.get()))
      } else {
        Future.successful(Ok(views.html.start()))
      }
  }

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        authService.validateCredentialRole flatMap {
          case true =>
            if (AmendmentsToggle.feature) {
              getWithAmendments
            } else {
              getWithoutAmendments
            }
          case _ =>
            Future.successful(Redirect(authService.signoutUrl))
        }
  }

  def getWithoutAmendments(implicit authContext: AuthContext, request: Request[_]) = {
    val amlsReferenceNumber = enrolmentsService.amlsRegistrationNumber
    landingService.cacheMap flatMap {
      case Some(cache) =>
        preApplicationComplete(cache)
      case None => {
        for {
          reviewDetails <- landingService.reviewDetails
          amlsRef <- amlsReferenceNumber
        } yield (reviewDetails, amlsRef) match {
          case (Some(rd), None) =>
            landingService.updateReviewDetails(rd) map { _ => {
              auditConnector.sendExtendedEvent(ServiceEntrantEvent(rd.businessName, rd.utr.getOrElse(""), rd.safeId))

              FormTypes.postcodeType.validate(rd.businessAddress.postcode.getOrElse("")) match {
                case Valid(_) => Redirect(controllers.businessmatching.routes.BusinessTypeController.get())
                case Invalid(_) => Redirect(controllers.businessmatching.routes.ConfirmPostCodeController.get())
              }
            }
            }
          case (None, None) =>
            Future.successful(Redirect(Call("GET", ApplicationConfig.businessCustomerUrl)))
          case (_, Some(_)) =>
            Future.successful(Redirect(controllers.routes.StatusController.get()))
        }
      }.flatMap(identity)
    }
  }

  private def preApplicationComplete(cache: CacheMap)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[Result] = {
    cache.getEntry[BusinessMatching](BusinessMatching.key) map { bm =>
      (bm.isComplete, cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)) match {
        case (true, Some(abt)) =>
          landingService.setAltCorrespondenceAddress(abt) map { _ =>
            Redirect(controllers.routes.StatusController.get())
          }

        case (true, _) => Future.successful(Redirect(controllers.routes.StatusController.get()))

        case (false, _) =>
          shortLivedCache.remove(authContext.user.oid).map { http =>
            http.status match {
              case NO_CONTENT => Redirect(controllers.routes.LandingController.get())
              case _ => throw new Exception("Cannot remove pre application data")
            }
          }
      }
    } getOrElse Future.successful(Redirect(controllers.routes.LandingController.get()))
  }

  private def refreshAndRedirect(amlsRegistrationNumber: String, cacheMap: Option[CacheMap])
                                (implicit authContext: AuthContext, headerCarrier: HeaderCarrier) = {

    landingService.refreshCache(amlsRegistrationNumber) map {
      _ => {
        val fromDuplicate = cacheMap match {
          case Some(map) => map.getEntry[SubscriptionResponse](SubscriptionResponse.key).fold(false) {
            _.previouslySubmitted.contains(true)
          }
          case _ => false
        }

        Redirect(controllers.routes.StatusController.get(fromDuplicate))
      }
    }

  }

  private def setAlCorrespondenceAddressAndRedirect(amlsRegistrationNumber: String, cacheMap: Option[CacheMap])
                                                   (implicit authContext: AuthContext, headerCarrier: HeaderCarrier) = {

    landingService.setAlCorrespondenceAddressWithRegNo(amlsRegistrationNumber, cacheMap) map {
      _ => Redirect(controllers.routes.StatusController.get())
    }
  }

  private def dataHasChanged(cacheMap: CacheMap) = {
    Seq(
      cacheMap.getEntry[Asp](Asp.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[Seq[BankDetails]](BankDetails.key).fold(false) {
        _.exists(_.hasChanged)
      },
      cacheMap.getEntry[BusinessActivities](BusinessActivities.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[BusinessMatching](BusinessMatching.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[EstateAgentBusiness](EstateAgentBusiness.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key).fold(false) {
        _.exists(_.hasChanged)
      },
      cacheMap.getEntry[Supervision](Supervision.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[Tcsp](Tcsp.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[Seq[TradingPremises]](TradingPremises.key).fold(false) {
        _.exists(_.hasChanged)
      },
      cacheMap.getEntry[Hvd](Hvd.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[Renewal](Renewal.key).fold(false) {
        _.hasChanged
      }
    ).exists(identity)
  }

  def getWithAmendments(implicit authContext: AuthContext, request: Request[_]) = {
    enrolmentsService.amlsRegistrationNumber flatMap {
      case Some(amlsRegistrationNumber) => landingService.cacheMap flatMap {
        //enrolment exists
        case Some(c) =>
          val fix = for {
            c2 <- fixEmptyRecords[ResponsiblePerson](c, ResponsiblePerson.key)
          } yield c2

          //there is data in S4l
          fix flatMap { cacheMap =>
            if (dataHasChanged(cacheMap)) {
              (cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key),
                cacheMap.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)) match {
                case (Some(_), _) => refreshAndRedirect(amlsRegistrationNumber, Some(cacheMap))
                case (_, Some(_)) => refreshAndRedirect(amlsRegistrationNumber, Some(cacheMap))
                case _ => setAlCorrespondenceAddressAndRedirect(amlsRegistrationNumber, Some(cacheMap))

              }
            } else {
              //DataHasNotChanged
              refreshAndRedirect(amlsRegistrationNumber, Some(cacheMap))
            }
          }
        case _ => refreshAndRedirect(amlsRegistrationNumber, None)
      }

      case _ => getWithoutAmendments //no enrolment exists
    }
  }

  private def fixEmptyRecords[T](cache: CacheMap, key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, f: play.api.libs.json.Format[T]) = {
    import play.api.libs.json._
    try {
      cache.getEntry[Seq[T]](key)
      Future.successful(cache)
    } catch {
      case e: JsResultException =>
        cacheConnector.save[Seq[T]](key, Seq.empty[T])
    }
  }
}
