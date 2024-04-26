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

package controllers

import audit.ServiceEntrantEvent
import config.ApplicationConfig
import connectors.DataCacheConnector
import forms.mappings.Constraints
import models._
import models.amp.Amp
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessdetails.BusinessDetails
import models.businessmatching.BusinessMatching
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePerson
import models.status._
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{AuthEnrolmentsService, LandingService, StatusService}
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.http.HeaderCarrier
import services.cache.Cache
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import utils.{AuthAction, ControllerHelper}
import views.html.Start

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class LandingController @Inject()(val landingService: LandingService,
                                  val enrolmentsService: AuthEnrolmentsService,
                                  val auditConnector: AuditConnector,
                                  val cacheConnector: DataCacheConnector,
                                  authAction: AuthAction,
                                  val ds: CommonPlayDependencies,
                                  val statusService: StatusService,
                                  val mcc: MessagesControllerComponents,
                                  implicit override val messagesApi: MessagesApi,
                                  val config: ApplicationConfig,
                                  parser: BodyParsers.Default,
                                  start: Start,
                                  headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter) extends AmlsBaseController(ds, mcc) with I18nSupport with MessagesRequestHelper with Logging with Constraints {

  private lazy val unauthorisedUrl = URLEncoder.encode(ReturnLocation(controllers.routes.AmlsController.unauthorised_role)(appConfig).absoluteUrl, "utf-8")

  def signoutUrl = s"${appConfig.logoutUrl}?continue=$unauthorisedUrl"

  private def isAuthorised(implicit headerCarrier: HeaderCarrier) =
    headerCarrier.authorization.isDefined


  /**
    * allowRedirect allows us to configure whether or not the start page is *always* shown,
    * regardless of the user's auth status
    */
  def start(allowRedirect: Boolean = true): Action[AnyContent] = messagesAction(parser).async {
    implicit request: MessagesRequest[AnyContent] =>
      if (isAuthorised && allowRedirect) {
        Future.successful(Redirect(controllers.routes.LandingController.get))
      } else {
        Future.successful(Ok(start()))
      }
  }

  def get() = authAction.async {
    implicit request =>
      request.credentialRole match {
        case Some(User) => getWithAmendments(request.amlsRefNumber, request.credId, request.accountTypeId)
        case _ => Future.successful(Redirect(signoutUrl))
      }
  }

  def getWithAmendments(amlsRegistrationNumber: Option[String], credId: String, accountTypeId: (String, String))
                       (implicit request: Request[_]) = {

    amlsRegistrationNumber match {
      case Some(mlrNumber) => landingService.cacheMap(credId) flatMap {
        //enrolment exists
        case Some(c) =>
          // $COVERAGE-OFF$
          logger.debug("getWithAmendments:AMLSReference:" + amlsRegistrationNumber)
          // $COVERAGE-ON$
          lazy val fixEmpties = for {
            c1 <- fixEmptyRecords[TradingPremises](credId, c, TradingPremises.key)
            c2 <- fixEmptyRecords[ResponsiblePerson](credId, c1, ResponsiblePerson.key)
          } yield c2

          //there is data in S4l
          fixEmpties flatMap { cacheMap =>
            if (dataHasChanged(cacheMap)) {
              // $COVERAGE-OFF$
              logger.debug("Data has changed in getWithAmendments()")
              // $COVERAGE-ON$
              cacheMap.getEntry[SubmissionRequestStatus](SubmissionRequestStatus.key) collect {
                case SubmissionRequestStatus(true, _) => refreshAndRedirect(mlrNumber, Some(cacheMap), credId, accountTypeId)
              } getOrElse landingService.setAltCorrespondenceAddress(mlrNumber, Some(cacheMap), accountTypeId, credId) flatMap { _ =>
                preFlightChecksAndRedirect(amlsRegistrationNumber, accountTypeId, credId)
              }
            } else {
              // $COVERAGE-OFF$
              logger.debug("Data has not changed route in getWithAmendments()")
              // $COVERAGE-ON$
              refreshAndRedirect(mlrNumber, Some(cacheMap), credId, accountTypeId)
            }
          }
        case _ => {
          // $COVERAGE-OFF$
          logger.debug("Data with amlsRegistration number route in getWithAmendments()" + amlsRegistrationNumber)
          // $COVERAGE-ON$
          refreshAndRedirect(mlrNumber, None, credId, accountTypeId)
        }
      }

      case _ => {
        // $COVERAGE-OFF$
        logger.debug("No Enrolement exists getWithAmendments()")
        // $COVERAGE-ON$
        getWithoutAmendments(amlsRegistrationNumber, credId, accountTypeId)
      } //no enrolment exists
    }
  }

  private def refreshAndRedirect(amlsRegistrationNumber: String, maybeCache: Option[Cache], credId: String, accountTypeId: (String, String))
                                (implicit headerCarrier: HeaderCarrier): Future[Result] = {

    maybeCache match {
      case Some(c) if c.getEntry[DataImport](DataImport.key).isDefined =>
        // $COVERAGE-OFF$
        logger.debug("[AMLSLandingController][refreshAndRedirect]: dataImport is defined")
        // $COVERAGE-ON$
        Future.successful(Redirect(controllers.routes.StatusController.get()))

      case _ =>
        // $COVERAGE-OFF$
        logger.debug(s"[AMLSLandingController][refreshAndRedirect]: calling refreshCache with ${amlsRegistrationNumber}")
        // $COVERAGE-ON$
        landingService.refreshCache(amlsRegistrationNumber, credId, accountTypeId) flatMap {
          _ => {
            preFlightChecksAndRedirect(Option(amlsRegistrationNumber), accountTypeId, credId)
          }
        }
    }
  }

  private def preFlightChecksAndRedirect(amlsRegistrationNumber: Option[String], accountTypeId: (String, String), cacheId: String)
                                        (implicit headerCarrier: HeaderCarrier): Future[Result] = {

    val loginEvent = for {
      dupe <- cacheConnector.fetch[SubscriptionResponse](cacheId, SubscriptionResponse.key).recover { case _ => None } map {
        case Some(x) => x.previouslySubmitted.contains(true)
        case _ => false
      }
      //below to be called logic to decide if the Login Events Page should be displayed or not
      redirectToEventPage <- hasIncompleteRedressScheme(amlsRegistrationNumber, accountTypeId, cacheId)
    } yield (redirectToEventPage, dupe)

    loginEvent.map {
      case (true, false) => Redirect(controllers.routes.LoginEventController.get)
      case (_, true) => Redirect(controllers.routes.StatusController.get(true))
      case (_, false) => Redirect(controllers.routes.StatusController.get(false))
    }
  }

  def getWithoutAmendments(amlsRegistrationNumber: Option[String], credId: String, accountTypeId: (String, String))
                          (implicit request: Request[_]) = {

    // $COVERAGE-OFF$
    logger.debug("getWithoutAmendments:AMLSReference:" + amlsRegistrationNumber.getOrElse("Amls registration number not available"))
    // $COVERAGE-ON$

    implicit val hc = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)

    landingService.cacheMap(credId) flatMap {
      case Some(cache) =>
        preApplicationComplete(cache, amlsRegistrationNumber, accountTypeId, credId)
      case None => {
        for {
          reviewDetails <- landingService.reviewDetails
        } yield (reviewDetails, amlsRegistrationNumber) match {
          case (Some(rd), None) =>
            // $COVERAGE-OFF$
            logger.debug("LandingController:getWithoutAmendments: " + rd)
            // $COVERAGE-ON$
            landingService.updateReviewDetails(rd, credId) map { _ => {
              auditConnector.sendExtendedEvent(ServiceEntrantEvent(rd.businessName, rd.utr.getOrElse(""), rd.safeId))

              (rd.businessAddress.postcode, rd.businessAddress.country) match {
                  case (Some(postcode), Country("United Kingdom", "GB")) =>
                    if (postcode.matches(postcodeRegex)) {
                      Redirect(controllers.businessmatching.routes.BusinessTypeController.get)
                    } else {
                      Redirect(controllers.businessmatching.routes.ConfirmPostCodeController.get)
                    }
                  case (_, Country("United Kingdom", "GB")) => Redirect(controllers.businessmatching.routes.ConfirmPostCodeController.get)
                  case (_, country) if !country.isUK && !country.isEmpty => Redirect(controllers.businessmatching.routes.BusinessTypeController.get)
                  case (_, _) => Redirect(controllers.businessmatching.routes.ConfirmPostCodeController.get)
                }
              }
            }
          case (None, None) =>
            // $COVERAGE-OFF$
            logger.debug("LandingController:getWithoutAmendments - (None, None)")
            // $COVERAGE-ON$
            Future.successful(Redirect(Call("GET", appConfig.businessCustomerUrl)))
          case (_, Some(amlsRef)) =>
            // $COVERAGE-OFF$
            logger.debug("LandingController:getWithoutAmendments: " + amlsRef)
            // $COVERAGE-ON$
            Future.successful(Redirect(controllers.routes.StatusController.get()))
        }
        }.flatMap(identity)
    }
  }

  private def preApplicationComplete(cache: Cache, amlsRegistrationNumber: Option[String], accountTypeId: (String, String), cacheId: String)
                                    (implicit headerCarrier: HeaderCarrier): Future[Result] = {

    val deleteAndRedirect = () => cacheConnector.remove(cacheId) map { _ =>
      // $COVERAGE-OFF$
      logger.debug(s"[AMLSLandingController][preApplicationComplete]: removed cache and redirect to landingController.get")
      // $COVERAGE-ON$
      Redirect(controllers.routes.LandingController.get)
    }

    cache.getEntry[BusinessMatching](BusinessMatching.key) map { bm =>
      // $COVERAGE-OFF$
      logger.debug(s"[AMLSLandingController][preApplicationComplete]: found BusinessMatching key")
      // $COVERAGE-ON$
      (bm.isCompleteLanding, cache.getEntry[BusinessDetails](BusinessDetails.key)) match {
        case (true, Some(abt)) =>
          landingService.setAltCorrespondenceAddress(abt, cacheId) flatMap { _ =>
            // $COVERAGE-OFF$
            logger.debug(s"[AMLSLandingController][preApplicationComplete]: landingService.setAltCorrespondenceAddress returned")
            // $COVERAGE-ON$
            //below to be called logic to decide if the Login Events Page should be displayed or not (second place below)
            val redirectToEventPage: Future[Boolean] = hasIncompleteRedressScheme(amlsRegistrationNumber, accountTypeId, cacheId)
            redirectToEventPage.map {
              case true =>
                // $COVERAGE-OFF$
                logger.debug(s"[AMLSLandingController][preApplicationComplete]: redirecting to LoginEvent")
                // $COVERAGE-ON$
                Redirect(controllers.routes.LoginEventController.get)
              case _ =>
                // $COVERAGE-OFF$
                logger.debug(s"[AMLSLandingController][preApplicationComplete]: has complete RPs - redirecting to status")
                // $COVERAGE-ON$
                Redirect(controllers.routes.StatusController.get())
            }
          }

        case (true, _) =>
          // $COVERAGE-OFF$
          logger.debug(s"[AMLSLandingController][preApplicationComplete]: bm.isComplete is true but no cache Entry for BusinessDetails - redirecting to status")
          // $COVERAGE-ON$
          Future.successful(Redirect(controllers.routes.StatusController.get()))

        case (false, _) =>
          // $COVERAGE-OFF$
          logger.debug(s"[AMLSLandingController][preApplicationComplete]: bm.isComplete is false")
          // $COVERAGE-ON$
          deleteAndRedirect()
      }
    } getOrElse deleteAndRedirect()
  }

  private def hasIncompleteRedressScheme(amlsRegistrationNumber: Option[String], accountTypeId: (String, String), cacheId: String)
                                        (implicit headerCarrier: HeaderCarrier): Future[Boolean] = {

    // $COVERAGE-OFF$
    logger.debug("[AMLSLandingController][hasIncompleteRedressScheme]: calling statusService.getDetailedStatus")
    // $COVERAGE-ON$
    statusService.getDetailedStatus(amlsRegistrationNumber, accountTypeId, cacheId).flatMap {
      case (SubmissionDecisionRejected |
            SubmissionDecisionRevoked |
            DeRegistered |
            SubmissionDecisionExpired |
            SubmissionWithdrawn, _) =>
        // $COVERAGE-OFF$
        logger.debug("[AMLSLandingController][hasIncompleteRedressScheme]: status is negative, returning false")
        // $COVERAGE-ON$
        Future.successful(false)
      case _ =>
        // $COVERAGE-OFF$
        logger.debug("[AMLSLandingController][hasIncompleteRedressScheme]: status is positive, will call the landingService.cachMap")
        // $COVERAGE-ON$
        landingService.cacheMap(cacheId).map {
          cache =>
            // $COVERAGE-OFF$
            logger.debug("[AMLSLandingController][hasIncompleteRedressScheme]: checking cacheMap for incomplete redress scheme")
            // $COVERAGE-ON$

            val hasInvalidRedressScheme = for {
              eab <- cache.map(_.getEntry[Eab](Eab.key))
            } yield ControllerHelper.hasInvalidRedressScheme(eab)

            // $COVERAGE-OFF$
            logger.debug(s"[AMLSLandingController][hasIncompleteRedressScheme]: eab.isRedressInvalid = ${hasInvalidRedressScheme.contains(true)}")
            // $COVERAGE-ON$
            hasInvalidRedressScheme.contains(true)
        }
    }
  }

  private def dataHasChanged(cache: Cache) = {
    Seq(
      cache.getEntry[Asp](Asp.key).fold(false) {
        _.hasChanged
      },
      cache.getEntry[Amp](Amp.key).fold(false) {
        _.hasChanged
      },
      cache.getEntry[BusinessDetails](BusinessDetails.key).fold(false) {
        _.hasChanged
      },
      cache.getEntry[Seq[BankDetails]](BankDetails.key).fold(false) {
        _.exists(_.hasChanged)
      },
      cache.getEntry[BusinessActivities](BusinessActivities.key).fold(false) {
        _.hasChanged
      },
      cache.getEntry[BusinessMatching](BusinessMatching.key).fold(false) {
        _.hasChanged
      },
      cache.getEntry[Eab](Eab.key).fold(false) {
        _.hasChanged
      },
      cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key).fold(false) {
        _.hasChanged
      },
      cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key).fold(false) {
        _.exists(_.hasChanged)
      },
      cache.getEntry[Supervision](Supervision.key).fold(false) {
        _.hasChanged
      },
      cache.getEntry[Tcsp](Tcsp.key).fold(false) {
        _.hasChanged
      },
      cache.getEntry[Seq[TradingPremises]](TradingPremises.key).fold(false) {
        _.exists(_.hasChanged)
      },
      cache.getEntry[Hvd](Hvd.key).fold(false) {
        _.hasChanged
      },
      cache.getEntry[Renewal](Renewal.key).fold(false) {
        _.hasChanged
      }
    ).exists(identity)
  }


  private def fixEmptyRecords[T](credId: String, cache: Cache, key: String)
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