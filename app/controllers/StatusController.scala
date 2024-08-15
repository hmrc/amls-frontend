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

import cats.data.OptionT
import cats.implicits._
import connectors._
import models.businessmatching.BusinessActivity.{MoneyServiceBusiness, TrustAndCompanyServices}
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.responsiblepeople.ResponsiblePerson
import models.status._
import models.{FeeResponse, ReadStatusResponse}

import java.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import play.twirl.api.HtmlFormat
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BusinessName}
import views.html.status.YourRegistrationView
import views.html.status.components._

import javax.inject.{Inject, Singleton}
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
                                 authAction: AuthAction,
                                 val ds: CommonPlayDependencies,
                                 val feeResponseService: FeeResponseService,
                                 val cc: MessagesControllerComponents,
                                 val notificationConnector: AmlsNotificationConnector,
                                 feeInformation: FeeInformation,
                                 registrationStatus: RegistrationStatus,
                                 applicationIncomplete: ApplicationIncomplete,
                                 applicationDeregistered: ApplicationDeregistered,
                                 applicationRenewalSubmissionReady: ApplicationRenewalSubmissionReady,
                                 applicationRenewalDue: ApplicationRenewalDue,
                                 applicationSubmissionReady: ApplicationSubmissionReady,
                                 applicationPending: ApplicationPending,
                                 applicationRejected: ApplicationRejected,
                                 applicationRevoked: ApplicationRevoked,
                                 applicationExpired: ApplicationExpired,
                                 applicationWithdrawn: ApplicationWithdrawn,
                                 applicationRenewalSubmitted: ApplicationRenewalSubmitted,
                                 applicationRenewalIncomplete: ApplicationRenewalIncomplete,
                                 withdrawOrDeregisterInformation: WithdrawOrDeregisterInformation,
                                 tradeInformationNoActivities: TradeInformationNoActivities,
                                 tradeInformationOneActivity: TradeInformationOneActivity,
                                 tradeInformation: TradeInformation,
                                 tradeInformationFindOut: TradeInformationFindOut,
                                 view: YourRegistrationView) extends AmlsBaseController(ds, cc) {

  def get(fromDuplicateSubmission: Boolean = false): Action[AnyContent] = authAction.async {
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
        case _ => Future.successful(Redirect(controllers.routes.LandingController.get()))
      }
  }

  def getFeeResponse(mlrRegNumber: Option[String], submissionStatus: SubmissionStatus, accountTypeId: (String, String))
                    (implicit headerCarrier: HeaderCarrier): Future[Option[FeeResponse]] = {

    (mlrRegNumber, submissionStatus) match {
      case (Some(mlNumber), (SubmissionReadyForReview | SubmissionDecisionApproved)) => feeResponseService.getFeeResponse(mlNumber, accountTypeId)
      case _ => Future.successful(None)
    }
  }

  def newSubmission: Action[AnyContent] = authAction.async {
    implicit request => {
      val redirect = for {
        amlsRegNumber <- OptionT.fromOption[Future](request.amlsRefNumber)
        _ <- OptionT.liftF(enrolmentsService.deEnrol(amlsRegNumber, request.groupIdentifier))
        _ <- OptionT.liftF(dataCache.remove(request.credId))
      } yield Redirect(controllers.routes.LandingController.start(true))

      redirect getOrElse InternalServerError("New submission failed")
    }
  }

  private def getPage(amlsRefNumber: Option[String], credId: String, accountTypeId: (String, String),
                      groupIdentifier: Option[String], fromDuplicateSubmission: Boolean)
                     (implicit request: Request[AnyContent]): Future[Result] = {
    for {
      refNo <- enrolmentsService.amlsRegistrationNumber(amlsRefNumber, groupIdentifier)
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
                                  (implicit request: Request[AnyContent]): Future[Result] = {
    statusInfo match {
      case (NotCompleted, _) | (SubmissionReady, _) | (SubmissionReadyForReview, _) =>
        getInitialSubmissionPage(mlrRegNumber, statusInfo._1, businessNameOption, feeResponse, fromDuplicateSubmission, accountTypeId, cacheId, activities, unreadNotifications)
      case (SubmissionDecisionApproved, _) | (SubmissionDecisionRejected, _) |
           (SubmissionDecisionRevoked, _) | (SubmissionDecisionExpired, _) |
           (SubmissionWithdrawn, _) | (DeRegistered, _) =>
        Future.successful(getDecisionPage(mlrRegNumber, statusInfo, businessNameOption, responsiblePeople, activities, accountTypeId, unreadNotifications))
      case (ReadyForRenewal(_), _) | (RenewalSubmitted(_), _) =>
        getRenewalFlowPage(mlrRegNumber, statusInfo, businessNameOption, responsiblePeople, activities, cacheId, unreadNotifications)
      case (_, _) => Future.successful(
        Ok(view(
          regNo = mlrRegNumber.getOrElse(""),
          businessName = businessNameOption,
          yourRegistrationInfo = Some(applicationIncomplete(businessNameOption)),
          unreadNotifications = unreadNotifications,
          registrationStatus = registrationStatus(
            amlsRegNo = mlrRegNumber,
            status = statusInfo._1
          ),
          feeInformation = None
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
        Ok(view(
          regNo = mlrRegNumber.getOrElse(""),
          businessName = businessNameOption,
          yourRegistrationInfo = Some(applicationIncomplete(businessNameOption)),
          unreadNotifications = unreadNotifications,
          registrationStatus = registrationStatus(
            amlsRegNo = mlrRegNumber,
            status = status
          ),
          feeInformation = None
        ))
      )
      case SubmissionReady => {
        Future.successful(
          Ok(view(
            regNo = mlrRegNumber.getOrElse(""),
            businessName = businessNameOption,
            yourRegistrationInfo = Some(applicationSubmissionReady(controllers.routes.RegistrationProgressController.get(), businessNameOption)),
            unreadNotifications = unreadNotifications,
            registrationStatus = registrationStatus(
              amlsRegNo = mlrRegNumber,
              status = status
            ),
            feeInformation = None
          ))
        )
      }
      case _ =>
        Future.successful(
          Ok(view(
            regNo = mlrRegNumber.getOrElse(""),
            businessName = businessNameOption,
            yourRegistrationInfo = Some(applicationPending()),
            unreadNotifications = unreadNotifications,
            displayContactLink = true,
            registrationStatus = registrationStatus(
              amlsRegNo = mlrRegNumber,
              status = status,
              canOrCannotTradeInformation = canOrCannotTradeInformation(activities)),
            feeInformation = Some(feeInformation(status)),
            withdrawOrDeregisterInformation = withdrawOrDeregisterInformation(status))))
    }
  }

  private def getDecisionPage(mlrRegNumber: Option[String],
                              statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                              businessNameOption: Option[String],
                              responsiblePeople: Option[Seq[ResponsiblePerson]],
                              maybeActivities: Option[BusinessActivities],
                              accountTypeId: (String, String),
                              unreadNotifications: Int)(implicit request: Request[AnyContent]): Result = {
    statusInfo match {
      case (SubmissionDecisionApproved, statusDtls) => {
        val endDate = statusDtls.fold[Option[LocalDate]](None)(_.currentRegYearEndDate)

        Ok {
          view(
            regNo = mlrRegNumber.getOrElse(""),
            businessName = businessNameOption,
            unreadNotifications = unreadNotifications,
            registrationStatus = registrationStatus(
              amlsRegNo = mlrRegNumber,
              status = statusInfo._1,
              canOrCannotTradeInformation = canOrCannotTradeInformation(maybeActivities),
              endDate = endDate),
            feeInformation = Some(feeInformation(statusInfo._1)),
            withdrawOrDeregisterInformation = withdrawOrDeregisterInformation(statusInfo._1))
        }
      }

      case (SubmissionDecisionRejected, _) =>
        Ok {
          view(
            regNo = "",
            businessName = businessNameOption,
            yourRegistrationInfo = Some(applicationRejected(businessNameOption)),
            displayCheckOrUpdateLink = false,
            unreadNotifications = unreadNotifications,
            registrationStatus = registrationStatus(
              status = statusInfo._1),
            feeInformation = None)
        }
      case (SubmissionDecisionRevoked, _) =>
        Ok {
          view(
            regNo = "",
            businessName = businessNameOption,
            yourRegistrationInfo = Some(applicationRevoked(businessNameOption)),
            displayCheckOrUpdateLink = false,
            unreadNotifications = unreadNotifications,
            registrationStatus = registrationStatus(
              status = statusInfo._1),
            feeInformation = None)
        }
      case (SubmissionDecisionExpired, _) =>
        Ok {
          view(
            regNo = "",
            businessName = businessNameOption,
            yourRegistrationInfo = Some(applicationExpired(businessNameOption)),
            displayCheckOrUpdateLink = false,
            unreadNotifications = unreadNotifications,
            registrationStatus = registrationStatus(
              status = statusInfo._1),
            feeInformation = None)
        }
      case (SubmissionWithdrawn, _) => {
        Ok {
          view(
            regNo = "",
            businessName = businessNameOption,
            yourRegistrationInfo = Some(applicationWithdrawn(businessNameOption)),
            displayCheckOrUpdateLink = false,
            unreadNotifications = unreadNotifications,
            registrationStatus = registrationStatus(
              status = statusInfo._1),
            feeInformation = None)
        }
      }
      case (DeRegistered, _) =>
        val deregistrationDate = for {
          info <- statusInfo._2
          date <- info.deRegistrationDate
        } yield date

        Ok {
          view(
            regNo = "",
            businessName = businessNameOption,
            yourRegistrationInfo = Some(applicationDeregistered(businessNameOption)),
            displayCheckOrUpdateLink = false,
            unreadNotifications = unreadNotifications,
            registrationStatus = registrationStatus(
              status = statusInfo._1,
              endDate = deregistrationDate),
            feeInformation = None)
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
                                (implicit request: Request[AnyContent]): Future[Result] = {

    statusInfo match {
      case (RenewalSubmitted(renewalDate), _) =>
        Future.successful(
          Ok(
            view(
              regNo = mlrRegNumber.getOrElse(""),
              businessName = businessNameOption,
              yourRegistrationInfo = Some(applicationRenewalSubmitted()),
              unreadNotifications = unreadNotifications,
              registrationStatus = registrationStatus(
                amlsRegNo = mlrRegNumber,
                status = statusInfo._1,
                endDate = renewalDate),
              feeInformation = Some(feeInformation(statusInfo._1)))
          )
        )
      case (ReadyForRenewal(renewalDate), _) => {
        renewalService.getRenewal(cacheId) flatMap {
          case Some(renewal) =>
            renewalService.isRenewalComplete(renewal, cacheId) flatMap { complete =>
              if (complete) {
                Future.successful(
                  Ok(
                    view(
                      regNo = mlrRegNumber.getOrElse(""),
                      businessName = businessNameOption,
                      yourRegistrationInfo = Some(applicationRenewalSubmissionReady(businessNameOption)),
                      unreadNotifications = unreadNotifications,
                      registrationStatus = registrationStatus(
                        amlsRegNo = mlrRegNumber,
                        status = statusInfo._1,
                        endDate = renewalDate),
                      feeInformation = Some(feeInformation(statusInfo._1)))
                  )
                )
              } else {
                Future.successful(
                  Ok(
                    view(
                      regNo = mlrRegNumber.getOrElse(""),
                      businessName = businessNameOption,
                      yourRegistrationInfo = Some(applicationRenewalIncomplete(businessNameOption)),
                      unreadNotifications = unreadNotifications,
                      registrationStatus = registrationStatus(
                        amlsRegNo = mlrRegNumber,
                        status = statusInfo._1,
                        endDate = renewalDate),
                      feeInformation = Some(feeInformation(statusInfo._1)))
                  )
                )
              }
            }
          case _ => Future.successful(
            Ok {
              view(
                regNo = mlrRegNumber.getOrElse(""),
                businessName = businessNameOption,
                yourRegistrationInfo = Some(applicationRenewalDue(businessNameOption, renewalDate)),
                unreadNotifications = unreadNotifications,
                registrationStatus = registrationStatus(
                  amlsRegNo = mlrRegNumber,
                  status = statusInfo._1,
                  endDate = renewalDate),
                feeInformation = Some(feeInformation(statusInfo._1)),
                withdrawOrDeregisterInformation = withdrawOrDeregisterInformation(statusInfo._1))
            })
        }
      }
      case _ => Future.successful(InternalServerError("Post: An UnknowException has occurred: StatusController"))
    }
  }

  private def getBusinessName(credId: String, safeId: Option[String], accountTypeId: (String, String))(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    BusinessName.getName(credId, safeId, accountTypeId)(hc, ec, dataCache, amlsConnector)

  def hasMsb(activities: Option[BusinessActivities]): Boolean = {
    activities.fold(false)(_.businessActivities.contains(MoneyServiceBusiness))
  }

  def hasTcsp(activities: Option[BusinessActivities]): Boolean = {
    activities.fold(false)(_.businessActivities.contains(TrustAndCompanyServices))
  }

  def hasOther(activities: Option[BusinessActivities]): Boolean = {
    activities.fold(false)(ba => (ba.businessActivities -- Set(MoneyServiceBusiness, TrustAndCompanyServices)).nonEmpty)
  }

  def canOrCannotTradeInformation(activities: Option[BusinessActivities]): HtmlFormat.Appendable =
    (hasMsb(activities), hasTcsp(activities), hasOther(activities)) match {
      case (false, false, true) => tradeInformationNoActivities()
      case (true, _, false) | (_, true, false) => tradeInformationOneActivity()
      case (true, _, true) | (_, true, true) => tradeInformation()
      case (_, _, _) => tradeInformationFindOut()
    }

  def countUnreadNotifications(amlsRefNo: Option[String], safeId: Option[String], accountTypeId: (String, String))(implicit headerCarrier: HeaderCarrier): Future[Int] = {
    val notifications = (amlsRefNo, safeId) match {
      case (Some(ref), _) => notificationConnector.fetchAllByAmlsRegNo(ref, accountTypeId)
      case (None, Some(id)) => notificationConnector.fetchAllBySafeId(id, accountTypeId)
      case (_, _) => Future.successful(Seq())
    }

    notifications.map(_.count(!_.isRead))
  }
}


