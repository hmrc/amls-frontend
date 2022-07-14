/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, AuthenticatorConnector, DataCacheConnector, _}
import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessActivities, BusinessMatching, MoneyServiceBusiness, TrustAndCompanyServices}
import models.responsiblepeople.ResponsiblePerson
import models.status._
import models.{FeeResponse, ReadStatusResponse}
import org.joda.time.LocalDate
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import play.twirl.api.HtmlFormat
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BusinessName}
import views.html.include.status._
import views.html.status._
import views.html.status.components.{fee_information, registration_status, withdraw_or_deregister_information}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatusController @Inject()(val landingService: LandingService,
                                 val statusService: StatusService,
                                 val enrolmentsService: AuthEnrolmentsService,
                                 val feeConnector: FeeConnector,
                                 val renewalService: RenewalService,
                                 val progressService: ProgressService,
                                 val amlsConnector: AmlsConnector,
                                 val dataCache: DataCacheConnector,
                                 val authenticator: AuthenticatorConnector,
                                 authAction: AuthAction,
                                 val ds: CommonPlayDependencies,
                                 val feeResponseService: FeeResponseService,
                                 val cc: MessagesControllerComponents,
                                 val notificationConnector: AmlsNotificationConnector,
                                 your_registration: your_registration) extends AmlsBaseController(ds, cc) {

  def get(fromDuplicateSubmission: Boolean = false) = authAction.async {
    implicit request =>
      // MUST ensure we have a cache here! If no cache go back to landing controller and refresh.
      renewalService.isCachePresent(request.credId) flatMap {
        case true => getPage(
          request.amlsRefNumber,
          request.credId,
          request.accountTypeId,
          request.groupIdentifier,
          fromDuplicateSubmission
        )
        case _ => Future.successful(Redirect(controllers.routes.LandingController.get))
      }
  }

  def getFeeResponse(mlrRegNumber: Option[String], submissionStatus: SubmissionStatus, accountTypeId: (String, String))
                    (implicit headerCarrier: HeaderCarrier): Future[Option[FeeResponse]] = {

    (mlrRegNumber, submissionStatus) match {
      case (Some(mlNumber), (SubmissionReadyForReview | SubmissionDecisionApproved)) => feeResponseService.getFeeResponse(mlNumber, accountTypeId)
      case _ => Future.successful(None)
    }
  }

  def newSubmission = authAction.async {
    implicit request => {
      val redirect = for {
        amlsRegNumber <- OptionT.fromOption[Future](request.amlsRefNumber)
        _ <- OptionT.liftF(enrolmentsService.deEnrol(amlsRegNumber, request.groupIdentifier))
        _ <- OptionT.liftF(authenticator.refreshProfile)
        _ <- OptionT.liftF(dataCache.remove(request.credId))
      } yield Redirect(controllers.routes.LandingController.start(true))

      redirect getOrElse InternalServerError("New submission failed")
    }
  }

  private def getPage(amlsRefNumber: Option[String], credId: String, accountTypeId: (String, String),
                      groupIdentifier: Option[String], fromDuplicateSubmission: Boolean)
                     (implicit request: Request[AnyContent]) = {
    for {
      refNo <- enrolmentsService.amlsRegistrationNumber(amlsRefNumber, groupIdentifier)
      _ = println(" ref no is ::"+refNo)
      statusInfo <- statusService.getDetailedStatus(refNo, accountTypeId, credId)
      statusResponse <- Future(statusInfo._2)
      maybeBusinessName <- getBusinessName(credId, statusResponse.fold(none[String])(_.safeId), accountTypeId).value
      feeResponse <- getFeeResponse(refNo, statusInfo._1, accountTypeId)
      responsiblePeople <- dataCache.fetch[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key)
      bm <- dataCache.fetch[BusinessMatching](credId, BusinessMatching.key)
      unreadNotifications <- countUnreadNotifications(refNo, statusResponse.fold(none[String])(_.safeId), accountTypeId)
      maybeActivities <- Future(bm.activities)
      page <- getPageBasedOnStatus(
        refNo,
        statusInfo,
        maybeBusinessName,
        feeResponse,
        fromDuplicateSubmission,
        responsiblePeople,
        maybeActivities,
        accountTypeId,
        credId,
        unreadNotifications)
    } yield page
  }

  private def getPageBasedOnStatus(mlrRegNumber: Option[String],
                                   statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                                   businessNameOption: Option[String],
                                   feeResponse: Option[FeeResponse],
                                   fromDuplicateSubmission: Boolean,
                                   responsiblePeople: Option[Seq[ResponsiblePerson]],
                                   activities: Option[BusinessActivities],
                                   accountTypeId: (String, String),
                                   cacheId: String,
                                   unreadNotifications: Int)
                                  (implicit request: Request[AnyContent]) = {
    statusInfo match {
      case (NotCompleted, _) | (SubmissionReady, _) | (SubmissionReadyForReview, _) =>
        println(" inside case 1")
        getInitialSubmissionPage(mlrRegNumber, statusInfo._1, businessNameOption, feeResponse, fromDuplicateSubmission, accountTypeId, cacheId, activities, unreadNotifications)
      case (SubmissionDecisionApproved, _) | (SubmissionDecisionRejected, _) |
           (SubmissionDecisionRevoked, _) | (SubmissionDecisionExpired, _) |
           (SubmissionWithdrawn, _) | (DeRegistered, _) =>
        println(" inside case 2")
        Future.successful(getDecisionPage(mlrRegNumber, statusInfo, businessNameOption, responsiblePeople, activities, accountTypeId, unreadNotifications))
      case (ReadyForRenewal(_), _) | (RenewalSubmitted(_), _) =>
        println(" inside case 3")
        getRenewalFlowPage(mlrRegNumber, statusInfo, businessNameOption, responsiblePeople, activities, cacheId, unreadNotifications)
      case (_, _) => println(" inside case _")
        Future.successful(
        Ok(your_registration(
          regNo = mlrRegNumber.getOrElse(""),
          businessName = businessNameOption,
          yourRegistrationInfo = application_incomplete(businessNameOption),
          unreadNotifications = unreadNotifications,
          registrationStatus = registration_status(
            amlsRegNo = mlrRegNumber,
            status = statusInfo._1
          ),
          feeInformation = HtmlFormat.empty
        ))
      )
    }
  }

  private def getInitialSubmissionPage(mlrRegNumber: Option[String],
                                       status: SubmissionStatus,
                                       businessNameOption: Option[String],
                                       feeResponse: Option[FeeResponse],
                                       fromDuplicateSubmission: Boolean,
                                       accountTypeId: (String, String),
                                       cacheId: String,
                                       activities: Option[BusinessActivities],
                                       unreadNotifications: Int)
                                      (implicit request: Request[AnyContent]): Future[Result] = {

    status match {
      case NotCompleted => Future.successful(
        Ok(your_registration(
          regNo = mlrRegNumber.getOrElse(""),
          businessName = businessNameOption,
          yourRegistrationInfo = application_incomplete(businessNameOption),
          unreadNotifications = unreadNotifications,
          registrationStatus = registration_status(
            amlsRegNo = mlrRegNumber,
            status = status
          ),
          feeInformation = HtmlFormat.empty
        ))
      )
      case SubmissionReady => {
        Future.successful(
          Ok(your_registration(
            regNo = mlrRegNumber.getOrElse(""),
            businessName = businessNameOption,
            yourRegistrationInfo = application_submission_ready(controllers.routes.RegistrationProgressController.get, businessNameOption),
            unreadNotifications = unreadNotifications,
            registrationStatus = registration_status(
              amlsRegNo = mlrRegNumber,
              status = status
            ),
            feeInformation = HtmlFormat.empty
          ))
        )
      }
      case _ =>
        Future.successful(
          Ok(your_registration(
            regNo = mlrRegNumber.getOrElse(""),
            businessName = businessNameOption,
            yourRegistrationInfo = application_pending(),
            unreadNotifications = unreadNotifications,
            displayContactLink = true,
            registrationStatus = registration_status(
              amlsRegNo = mlrRegNumber,
              status = status,
              canOrCannotTradeInformation = canOrCannotTradeInformation(activities)),
            feeInformation = fee_information(status),
            withdrawOrDeregisterInformation = withdraw_or_deregister_information(status))))
    }
  }

  private def getDecisionPage(mlrRegNumber: Option[String],
                              statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                              businessNameOption: Option[String],
                              responsiblePeople: Option[Seq[ResponsiblePerson]],
                              maybeActivities: Option[BusinessActivities],
                              accountTypeId: (String, String),
                              unreadNotifications: Int)(implicit request: Request[AnyContent]) = {
    statusInfo match {
      case (SubmissionDecisionApproved, statusDtls) => {
        val endDate = statusDtls.fold[Option[LocalDate]](None)(_.currentRegYearEndDate)

        Ok {
          your_registration(
            regNo = mlrRegNumber.getOrElse(""),
            businessName = businessNameOption,
            unreadNotifications = unreadNotifications,
            registrationStatus = registration_status(
              amlsRegNo = mlrRegNumber,
              status = statusInfo._1,
              canOrCannotTradeInformation = canOrCannotTradeInformation(maybeActivities),
              endDate = endDate),
            feeInformation = fee_information(statusInfo._1),
            withdrawOrDeregisterInformation = withdraw_or_deregister_information(statusInfo._1))
        }
      }

      case (SubmissionDecisionRejected, _) =>
        Ok {
          your_registration(
            regNo = "",
            businessName = businessNameOption,
            yourRegistrationInfo = application_rejected(businessNameOption),
            displayCheckOrUpdateLink = false,
            unreadNotifications = unreadNotifications,
            registrationStatus = registration_status(
              status = statusInfo._1),
            feeInformation = HtmlFormat.empty)
        }
      case (SubmissionDecisionRevoked, _) =>
        Ok {
          your_registration(
            regNo = "",
            businessName = businessNameOption,
            yourRegistrationInfo = application_revoked(businessNameOption),
            displayCheckOrUpdateLink = false,
            unreadNotifications = unreadNotifications,
            registrationStatus = registration_status(
              status = statusInfo._1),
            feeInformation = HtmlFormat.empty)
        }
      case (SubmissionDecisionExpired, _) =>
        Ok {
          your_registration(
            regNo = "",
            businessName = businessNameOption,
            yourRegistrationInfo = application_expired(businessNameOption),
            displayCheckOrUpdateLink = false,
            unreadNotifications = unreadNotifications,
            registrationStatus = registration_status(
              status = statusInfo._1),
            feeInformation = HtmlFormat.empty)
        }
      case (SubmissionWithdrawn, _) => {
        Ok {
          your_registration(
            regNo = "",
            businessName = businessNameOption,
            yourRegistrationInfo = application_withdrawn(businessNameOption),
            displayCheckOrUpdateLink = false,
            unreadNotifications = unreadNotifications,
            registrationStatus = registration_status(
              status = statusInfo._1),
            feeInformation = HtmlFormat.empty)
        }
      }
      case (DeRegistered, _) =>
        val deregistrationDate = for {
          info <- statusInfo._2
          date <- info.deRegistrationDate
        } yield date

        Ok {
          your_registration(
            regNo = "",
            businessName = businessNameOption,
            yourRegistrationInfo = application_deregistered(businessNameOption),
            displayCheckOrUpdateLink = false,
            unreadNotifications = unreadNotifications,
            registrationStatus = registration_status(
              status = statusInfo._1,
              endDate = deregistrationDate),
            feeInformation = HtmlFormat.empty)
        }
      case _ => InternalServerError("Post: An UnknowException has occurred: RegisterServicesController")
    }
  }

  private def getRenewalFlowPage(mlrRegNumber: Option[String],
                                 statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                                 businessNameOption: Option[String],
                                 responsiblePeople: Option[Seq[ResponsiblePerson]],
                                 maybeActivities: Option[BusinessActivities],
                                 cacheId: String,
                                 unreadNotifications: Int)
                                (implicit request: Request[AnyContent]) = {

    statusInfo match {
      case (RenewalSubmitted(renewalDate), _) =>
        Future.successful(
          Ok(
            your_registration(
              regNo = mlrRegNumber.getOrElse(""),
              businessName = businessNameOption,
              yourRegistrationInfo = application_renewal_submitted(),
              unreadNotifications = unreadNotifications,
              registrationStatus = registration_status(
                amlsRegNo = mlrRegNumber,
                status = statusInfo._1,
                endDate = renewalDate),
              feeInformation = fee_information(statusInfo._1))
          )
        )
      case (ReadyForRenewal(renewalDate), _) => {
        renewalService.getRenewal(cacheId) flatMap {
          case Some(renewal) =>
            renewalService.isRenewalComplete(renewal, cacheId) flatMap { complete =>
              if (complete) {
                Future.successful(
                  Ok(
                    your_registration(
                      regNo = mlrRegNumber.getOrElse(""),
                      businessName = businessNameOption,
                      yourRegistrationInfo = application_renewal_submission_ready(businessNameOption),
                      unreadNotifications = unreadNotifications,
                      registrationStatus = registration_status(
                        amlsRegNo = mlrRegNumber,
                        status = statusInfo._1,
                        endDate = renewalDate),
                      feeInformation = fee_information(statusInfo._1))
                  )
                )
              } else {
                Future.successful(
                  Ok(
                    your_registration(
                      regNo = mlrRegNumber.getOrElse(""),
                      businessName = businessNameOption,
                      yourRegistrationInfo = application_renewal_incomplete(businessNameOption),
                      unreadNotifications = unreadNotifications,
                      registrationStatus = registration_status(
                        amlsRegNo = mlrRegNumber,
                        status = statusInfo._1,
                        endDate = renewalDate),
                      feeInformation = fee_information(statusInfo._1))
                  )
                )
              }
            }
          case _ => Future.successful(
            Ok {
              your_registration(
                regNo = mlrRegNumber.getOrElse(""),
                businessName = businessNameOption,
                yourRegistrationInfo = application_renewal_due(businessNameOption, renewalDate),
                unreadNotifications = unreadNotifications,
                registrationStatus = registration_status(
                  amlsRegNo = mlrRegNumber,
                  status = statusInfo._1,
                  endDate = renewalDate),
                feeInformation = fee_information(statusInfo._1),
                withdrawOrDeregisterInformation = withdraw_or_deregister_information(statusInfo._1))
            })
        }
      }
      case _ => Future.successful(InternalServerError("Post: An UnknowException has occurred: StatusController"))
    }
  }

  private def getBusinessName(credId: String, safeId: Option[String], accountTypeId: (String, String))(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    BusinessName.getName(credId, safeId, accountTypeId)(hc, ec, dataCache, amlsConnector)

  private def hasMsb(activities: Option[BusinessActivities]) = {
    activities.fold(false)(_.businessActivities.contains(MoneyServiceBusiness))
  }

  private def hasTcsp(activities: Option[BusinessActivities]) = {
    activities.fold(false)(_.businessActivities.contains(TrustAndCompanyServices))
  }

  private def hasOther(activities: Option[BusinessActivities]) = {
    activities.fold(false)(ba => (ba.businessActivities -- Set(MoneyServiceBusiness, TrustAndCompanyServices)).nonEmpty)
  }

  private def canOrCannotTradeInformation(activities: Option[BusinessActivities])(implicit request: Request[AnyContent]) =
    (hasMsb(activities), hasTcsp(activities), hasOther(activities)) match {
      case (false, false, true) => trade_information_no_msb_or_tcsp()
      case (true, _, false) | (_, true, false) => trade_information_msb_or_tcsp_only()
      case (true, _, true) | (_, true, true) => trade_information()
      case (_, _, _) => trade_information_find_out()
    }

  def countUnreadNotifications(amlsRefNo: Option[String], safeId: Option[String], accountTypeId: (String, String))(implicit headerCarrier: HeaderCarrier) = {
    val notifications = (amlsRefNo, safeId) match {
      case (Some(ref), _) => notificationConnector.fetchAllByAmlsRegNo(ref, accountTypeId)
      case (None, Some(id)) => notificationConnector.fetchAllBySafeId(id, accountTypeId)
      case (_, _) => Future.successful(Seq())
    }

    notifications.map(_.count(!_.isRead))
  }
}


