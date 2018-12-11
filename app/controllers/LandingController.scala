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
import config.{AMLSAuthConnector, AmlsShortLivedCache, ApplicationConfig}
import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}
import models._
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
import models.status._
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.Logger
import play.api.mvc.{Action, Call, Request, Result}
import services.{AuthEnrolmentsService, AuthService, LandingService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper

import scala.concurrent.Future
import scala.util.{Success, Try}

@Singleton
class LandingController @Inject()(val landingService: LandingService,
                                  val enrolmentsService: AuthEnrolmentsService,
                                  val auditConnector: AuditConnector,
                                  val authService: AuthService,
                                  val cacheConnector: DataCacheConnector,
                                  val authConnector: AuthConnector = AMLSAuthConnector,
                                  val statusService: StatusService
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
    Logger.debug("getWithoutAmendments:AMLSReference:" + amlsReferenceNumber)
    landingService.cacheMap flatMap {
      case Some(cache) =>
        preApplicationComplete(cache)
      case None => {
        for {
          reviewDetails <- landingService.reviewDetails
          amlsRef <- amlsReferenceNumber
        } yield (reviewDetails, amlsRef) match {
          case (Some(rd), None) =>
            Logger.debug("LandingController:getWithoutAmendments: " + rd)
            landingService.updateReviewDetails(rd) map { _ => {
              auditConnector.sendExtendedEvent(ServiceEntrantEvent(rd.businessName, rd.utr.getOrElse(""), rd.safeId))

              FormTypes.postcodeType.validate(rd.businessAddress.postcode.getOrElse("")) match {
                case Valid(_) => Redirect(controllers.businessmatching.routes.BusinessTypeController.get())
                case Invalid(_) => Redirect(controllers.businessmatching.routes.ConfirmPostCodeController.get())
              }
            }
            }
          case (None, None) =>
            Logger.debug("LandingController:getWithoutAmendments - (None, None)")
            Future.successful(Redirect(Call("GET", ApplicationConfig.businessCustomerUrl)))
          case (_, Some(_)) =>
            Logger.debug("LandingController:getWithoutAmendments: " + amlsRef)
            Future.successful(Redirect(controllers.routes.StatusController.get()))
        }
      }.flatMap(identity)
    }
  }

  private def hasIncompleteResponsiblePeople()(implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[Boolean] = {

    statusService.getDetailedStatus.flatMap {
      case (SubmissionDecisionRejected |
            SubmissionDecisionRevoked |
            DeRegistered |
            SubmissionDecisionExpired |
            SubmissionWithdrawn, _) =>
        Future.successful(false)
      case _ =>
        landingService.cacheMap.map {
          cache =>
            val hasIncompleteRps: Option[Boolean] = for {
              rps <- cache.map(_.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key))
            } yield ControllerHelper.hasIncompleteResponsiblePerson(rps)
            hasIncompleteRps.contains(true)
        }
    }
  }

  private def preApplicationComplete(cache: CacheMap)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[Result] = {

    val deleteAndRedirect = () => cacheConnector.remove map { _ =>
      Logger.debug(s"[AMLSLandingController][preApplicationComplete]: removed cache and redirect to landingController.get")
      Redirect(controllers.routes.LandingController.get())
    }

    cache.getEntry[BusinessMatching](BusinessMatching.key) map { bm =>
      Logger.debug(s"[AMLSLandingController][preApplicationComplete]: found BusinessMatching key")
      (bm.isComplete, cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)) match {
        case (true, Some(abt)) =>
          landingService.setAltCorrespondenceAddress(abt) flatMap { _ =>
            Logger.debug(s"[AMLSLandingController][preApplicationComplete]: landingService.setAltCorrespondenceAddress returned")
            val result: Future[Boolean] = hasIncompleteResponsiblePeople()
            result.map {
              case true =>
                Logger.debug(s"[AMLSLandingController][preApplicationComplete]: has Incomplete RPs - redirecting to LoginEvent")
                Redirect(controllers.routes.LoginEventController.get())
              case _ =>
                Logger.debug(s"[AMLSLandingController][preApplicationComplete]: has complete RPs - redirecting to status")
                Redirect(controllers.routes.StatusController.get())
            }
          }

        case (true, _) =>
          Logger.debug(s"[AMLSLandingController][preApplicationComplete]: bm.isComplete is true but no cache Entry for AboutTheBusiness - redirecting to status")
          Future.successful(Redirect(controllers.routes.StatusController.get()))

        case (false, _) =>
          Logger.debug(s"[AMLSLandingController][preApplicationComplete]: bm.isComplete is false")
          deleteAndRedirect()
      }
    } getOrElse deleteAndRedirect()
  }

  private def refreshAndRedirect(amlsRegistrationNumber: String, maybeCacheMap: Option[CacheMap])
                                (implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[Result] = {
    maybeCacheMap match {
      case Some(c) if c.getEntry[DataImport](DataImport.key).isDefined =>
        Logger.debug("[AMLSLandingController][refreshAndRedirect]: dataImport is defined")
        Future.successful(Redirect(controllers.routes.StatusController.get()))

      case _ =>
        Logger.debug(s"[AMLSLandingController][refreshAndRedirect]: calling refreshCache with ${amlsRegistrationNumber}")
        landingService.refreshCache(amlsRegistrationNumber) flatMap {
          _ => {
            Try {
              val fromDuplicate = maybeCacheMap match {
                case Some(map) => map.getEntry[SubscriptionResponse](SubscriptionResponse.key).fold(false) {
                  _.previouslySubmitted.contains(true)
                }
                case _ => false
              }

              val result: Future[Boolean] = hasIncompleteResponsiblePeople()
              result.map {
                case true if fromDuplicate == false =>
                  Logger.debug(s"[AMLSLandingController][refreshAndRedirect]: fromDuplicate = $fromDuplicate and redirecting to LoginEvent")
                  Redirect(controllers.routes.LoginEventController.get())
                case _ =>
                  Logger.debug(s"[AMLSLandingController][refreshAndRedirect]: fromDuplicate = $fromDuplicate and redirecting to StatusController")
                  Redirect(controllers.routes.StatusController.get(fromDuplicate))
              }
            }
          } match {
            case Success(r) =>
              Logger.debug("[AMLSLandingController][refreshAndRedirect]: redirect is successful()")
              r
            case _ =>
              Logger.debug(s"[AMLSLandingController][refreshAndRedirect]: refresh cache returned _ and redirecting to StatusController")
              Future.successful(Redirect(controllers.routes.StatusController.get()))
          }
        }
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
          Logger.debug("getWithAmendments:AMLSReference:" + amlsRegistrationNumber)
          lazy val fixEmpties = for {
            c1 <- fixEmptyRecords[TradingPremises](c, TradingPremises.key)
            c2 <- fixEmptyRecords[ResponsiblePerson](c1, ResponsiblePerson.key)
          } yield c2

          //there is data in S4l
          fixEmpties flatMap { cacheMap =>
            if (dataHasChanged(cacheMap)) {
              Logger.debug("Data has changed in getWithAmendments()")
              cacheMap.getEntry[SubmissionRequestStatus](SubmissionRequestStatus.key) collect {
                case SubmissionRequestStatus(true, _) => refreshAndRedirect(amlsRegistrationNumber, Some(cacheMap))
              } getOrElse landingService.setAltCorrespondenceAddress(amlsRegistrationNumber, Some(cacheMap)) map { _=>
                Redirect(controllers.routes.StatusController.get())
              }
            } else {
              //DataHasNotChanged
              Logger.debug("Data has not changed route in getWithAmendments()")
              refreshAndRedirect(amlsRegistrationNumber, Some(cacheMap))
            }
          }
        case _ => {
          Logger.debug("Data with amlsRegistration number route in getWithAmendments()" + amlsRegistrationNumber)
          refreshAndRedirect(amlsRegistrationNumber, None)
        }
      }

      case _ => {
        Logger.debug("No Enrolement exists getWithAmendments()")
        getWithoutAmendments
      } //no enrolment exists
    }
  }

  private def fixEmptyRecords[T](cache: CacheMap, key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, f: play.api.libs.json.Format[T]) = {
    import play.api.libs.json._
    try {
      cache.getEntry[Seq[T]](key)
      Future.successful(cache)
    } catch {
      case _: JsResultException =>
        cacheConnector.save[Seq[T]](key, Seq.empty[T])
    }
  }
}