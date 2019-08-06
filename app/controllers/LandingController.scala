/*
 * Copyright 2019 HM Revenue & Customs
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

import java.net.URLEncoder

import audit.ServiceEntrantEvent
import cats.data.Validated.{Invalid, Valid}
import config.ApplicationConfig
import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}
import models._
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessdetails.BusinessDetails
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
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, ControllerHelper}

import scala.concurrent.Future

@Singleton
class LandingController @Inject()(val landingService: LandingService,
                                  val enrolmentsService: AuthEnrolmentsService,
                                  val auditConnector: AuditConnector,
                                  val authService: AuthService,
                                  val cacheConnector: DataCacheConnector,
                                  authAction: AuthAction,
                                  val statusService: StatusService) extends DefaultBaseController{


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

  private lazy val unauthorisedUrl = URLEncoder.encode(ReturnLocation(controllers.routes.AmlsController.unauthorised_role()).absoluteUrl, "utf-8")
  def signoutUrl = s"${ApplicationConfig.logoutUrl}?continue=$unauthorisedUrl"

  def get() = authAction.async {
      implicit request =>
        request.credentialRole match {
          case Some(uk.gov.hmrc.auth.core.User) => getWithAmendments(request.amlsRefNumber, request.credId, request.accountTypeId)
          case _ => Future.successful(Redirect(signoutUrl))
        }
  }

  def getWithoutAmendments(amlsRegistrationNumber: Option[String], credId: String, accountTypeId: (String, String))
                          (implicit request: Request[_]) = {

    Logger.debug("getWithoutAmendments:AMLSReference:" + amlsRegistrationNumber.getOrElse("Amls registration number not available"))

    landingService.cacheMap(credId) flatMap {
      case Some(cache) =>
        preApplicationComplete(cache, amlsRegistrationNumber, accountTypeId, credId)
      case None => {
        for {
          reviewDetails <- landingService.reviewDetails
        } yield (reviewDetails, amlsRegistrationNumber) match {
          case (Some(rd), None) =>
            Logger.debug("LandingController:getWithoutAmendments: " + rd)
            landingService.updateReviewDetails(rd, credId) map { _ => {
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
          case (_, Some(amlsRef)) =>
            Logger.debug("LandingController:getWithoutAmendments: " + amlsRef)
            Future.successful(Redirect(controllers.routes.StatusController.get()))
        }
      }.flatMap(identity)
    }
  }

  private def hasIncompleteResponsiblePeople(amlsRegistrationNumber: Option[String], accountTypeId: (String, String), cacheId: String)
                                            (implicit headerCarrier: HeaderCarrier): Future[Boolean] = {
    Logger.debug("[AMLSLandingController][hasIncompleteResponsiblePeople]: calling statusService.getDetailedStatus")
    statusService.getDetailedStatus(amlsRegistrationNumber, accountTypeId, cacheId).flatMap {
      case (SubmissionDecisionRejected |
            SubmissionDecisionRevoked |
            DeRegistered |
            SubmissionDecisionExpired |
            SubmissionWithdrawn, _) =>
        Logger.debug("[AMLSLandingController][hasIncompleteResponsiblePeople]: status is negative, returning false")
        Future.successful(false)
      case _ =>
        Logger.debug("[AMLSLandingController][hasIncompleteResponsiblePeople]: status is positive, will call the landingService.cachMap")
        landingService.cacheMap(cacheId).map {
          cache =>
            Logger.debug("[AMLSLandingController][hasIncompleteResponsiblePeople]: checking cacheMap for InComplete ResponsiblePeople")
            val hasIncompleteRps: Option[Boolean] = for {
              rps <- cache.map(_.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key))
            } yield ControllerHelper.hasIncompleteResponsiblePerson(rps)
            Logger.debug(s"[AMLSLandingController][hasIncompleteResponsiblePeople]: Rps.isComplete = ${hasIncompleteRps.contains(true)}")
            hasIncompleteRps.contains(true)
        }
    }
  }

  private def preApplicationComplete(cache: CacheMap, amlsRegistrationNumber: Option[String], accountTypeId: (String, String), cacheId: String)
                                    (implicit headerCarrier: HeaderCarrier): Future[Result] = {

    val deleteAndRedirect = () => cacheConnector.remove(cacheId) map { _ =>
      Logger.debug(s"[AMLSLandingController][preApplicationComplete]: removed cache and redirect to landingController.get")
      Redirect(controllers.routes.LandingController.get())
    }

    cache.getEntry[BusinessMatching](BusinessMatching.key) map { bm =>
      Logger.debug(s"[AMLSLandingController][preApplicationComplete]: found BusinessMatching key")
      (bm.isComplete, cache.getEntry[BusinessDetails](BusinessDetails.key)) match {
        case (true, Some(abt)) =>
          landingService.setAltCorrespondenceAddress(abt, cacheId) flatMap { _ =>
            Logger.debug(s"[AMLSLandingController][preApplicationComplete]: landingService.setAltCorrespondenceAddress returned")
            val result: Future[Boolean] = hasIncompleteResponsiblePeople(amlsRegistrationNumber, accountTypeId, cacheId)
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
          Logger.debug(s"[AMLSLandingController][preApplicationComplete]: bm.isComplete is true but no cache Entry for BusinessDetails - redirecting to status")
          Future.successful(Redirect(controllers.routes.StatusController.get()))

        case (false, _) =>
          Logger.debug(s"[AMLSLandingController][preApplicationComplete]: bm.isComplete is false")
          deleteAndRedirect()
      }
    } getOrElse deleteAndRedirect()
  }

  private def preFlightChecksAndRedirect(amlsRegistrationNumber: Option[String], accountTypeId: (String, String), cacheId: String)(implicit headerCarrier: HeaderCarrier):  Future[Result] = {
    val loginEvent = for {
      dupe <- cacheConnector.fetch[SubscriptionResponse](cacheId, SubscriptionResponse.key).recover { case _ => None } map {
        case Some(x) => x.previouslySubmitted.contains(true)
        case _ => false
      }
      incomplete <- hasIncompleteResponsiblePeople(amlsRegistrationNumber, accountTypeId, cacheId)
    } yield (incomplete, dupe)

    loginEvent.map {
      case (true, false) => Redirect(controllers.routes.LoginEventController.get())
      case (_, true) => Redirect(controllers.routes.StatusController.get(true))
      case (_, false) => Redirect(controllers.routes.StatusController.get(false))
    }
  }

  private def refreshAndRedirect(amlsRegistrationNumber: String, maybeCacheMap: Option[CacheMap], credId: String, accountTypeId: (String, String))
                                (implicit headerCarrier: HeaderCarrier): Future[Result] = {
    maybeCacheMap match {
      case Some(c) if c.getEntry[DataImport](DataImport.key).isDefined =>
        Logger.debug("[AMLSLandingController][refreshAndRedirect]: dataImport is defined")
        Future.successful(Redirect(controllers.routes.StatusController.get()))

      case _ =>
        Logger.debug(s"[AMLSLandingController][refreshAndRedirect]: calling refreshCache with ${amlsRegistrationNumber}")
        landingService.refreshCache(amlsRegistrationNumber, credId, accountTypeId) flatMap {
          _ => {
            preFlightChecksAndRedirect(Option(amlsRegistrationNumber), accountTypeId, credId)
          }
        }
    }
  }

  private def dataHasChanged(cacheMap: CacheMap) = {
    Seq(
      cacheMap.getEntry[Asp](Asp.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[BusinessDetails](BusinessDetails.key).fold(false) {
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

  def getWithAmendments(amlsRegistrationNumber: Option[String], credId: String, accountTypeId: (String, String))
                       (implicit request: Request[_]) = {

    amlsRegistrationNumber match {
      case Some(mlrNumber) => landingService.cacheMap(credId) flatMap {
        //enrolment exists
        case Some(c) =>
          Logger.debug("getWithAmendments:AMLSReference:" + amlsRegistrationNumber)
          lazy val fixEmpties = for {
            c1 <- fixEmptyRecords[TradingPremises](credId, c, TradingPremises.key)
            c2 <- fixEmptyRecords[ResponsiblePerson](credId, c1, ResponsiblePerson.key)
          } yield c2

          //there is data in S4l
          fixEmpties flatMap { cacheMap =>
            if (dataHasChanged(cacheMap)) {
              Logger.debug("Data has changed in getWithAmendments()")
              cacheMap.getEntry[SubmissionRequestStatus](SubmissionRequestStatus.key) collect {
                case SubmissionRequestStatus(true, _) => refreshAndRedirect(mlrNumber, Some(cacheMap), credId, accountTypeId)
              } getOrElse landingService.setAltCorrespondenceAddress(mlrNumber, Some(cacheMap), accountTypeId, credId) flatMap { _=>
                preFlightChecksAndRedirect(amlsRegistrationNumber, accountTypeId, credId)
              }
            } else {
              Logger.debug("Data has not changed route in getWithAmendments()")
              refreshAndRedirect(mlrNumber, Some(cacheMap), credId, accountTypeId)
            }
          }
        case _ => {
          Logger.debug("Data with amlsRegistration number route in getWithAmendments()" + amlsRegistrationNumber)
          refreshAndRedirect(mlrNumber, None, credId, accountTypeId)
        }
      }

      case _ => {
        Logger.debug("No Enrolement exists getWithAmendments()")
        getWithoutAmendments(amlsRegistrationNumber, credId, accountTypeId)
      } //no enrolment exists
    }
  }

  private def fixEmptyRecords[T](credId: String, cache: CacheMap, key: String)
                                (implicit hc: HeaderCarrier, f: play.api.libs.json.Format[T]) = {

    import play.api.libs.json._

    try {
      cache.getEntry[Seq[T]](key)
      Future.successful(cache)
    } catch {
      case _: JsResultException =>
        cacheConnector.save[Seq[T]](credId, key, Seq.empty[T])
    }
  }
}