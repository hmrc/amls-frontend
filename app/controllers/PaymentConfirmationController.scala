/*
 * Copyright 2021 HM Revenue & Customs
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

import audit.PaymentConfirmationEvent
import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import javax.inject.{Inject, Singleton}
import models.businessdetails.{BusinessDetails, PreviouslyRegisteredYes}
import models.confirmation.Currency
import models.payments._
import models.renewal.Renewal
import models.status._
import models.ReadStatusResponse
import play.api.mvc.MessagesControllerComponents
import models.ReadStatusResponse
import services.{AuthEnrolmentsService, FeeResponseService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, BusinessName, FeeHelper}
import views.html.confirmation._

import scala.concurrent.Future

@Singleton
class PaymentConfirmationController @Inject()(authAction: AuthAction,
                                              val ds: CommonPlayDependencies,
                                              private[controllers] implicit val dataCacheConnector: DataCacheConnector,
                                              private[controllers] implicit val amlsConnector: AmlsConnector,
                                              private[controllers] implicit val statusService: StatusService,
                                              private[controllers] val feeResponseService: FeeResponseService,
                                              private[controllers] val enrolmentService: AuthEnrolmentsService,
                                              private[controllers] val auditConnector: AuditConnector,
                                              val cc: MessagesControllerComponents,
                                              val feeHelper: FeeHelper,
                                              payment_confirmation_renewal: payment_confirmation_renewal,
                                              payment_confirmation_amendvariation: payment_confirmation_amendvariation,
                                              payment_confirmation_transitional_renewal: payment_confirmation_transitional_renewal,
                                              payment_confirmation: payment_confirmation,
                                              payment_failure: payment_failure
                                             ) extends AmlsBaseController(ds, cc) {

  val prefix = "[PaymentConfirmationController]"

  def paymentConfirmation(reference: String) = authAction.async {
      implicit request =>

        def companyName(maybeStatus: Option[ReadStatusResponse]): OptionT[Future, String] =
          maybeStatus.fold[OptionT[Future, String]](OptionT.some("")) { r => BusinessName.getName(request.credId, r.safeId, request.accountTypeId) }

        val msgFromPaymentStatus = Map[String, String](
          "Failed" -> "confirmation.payment.failed.reason.failure",
          "Cancelled" -> "confirmation.payment.failed.reason.cancelled"
        )

        val paymentStatusFromQueryString = request.queryString.get("paymentStatus").map(_.head)

        val isPaymentSuccessful = !request.queryString.contains("paymentStatus")

        val result = for {
          (status, detailedStatus) <- OptionT.liftF(statusService.getDetailedStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
          businessName <- companyName(detailedStatus) orElse OptionT.some("")
          renewalData <- OptionT.liftF(dataCacheConnector.fetch[Renewal](request.credId, Renewal.key))
          paymentStatus <- OptionT.liftF(amlsConnector.refreshPaymentStatus(reference, request.accountTypeId))
          payment <- OptionT(amlsConnector.getPaymentByPaymentReference(reference, request.accountTypeId))
          businessDetails <- OptionT(dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key))
          _ <- doAudit(paymentStatus.currentStatus, request.amlsRefNumber, request.accountTypeId, request.groupIdentifier)
        } yield if (isPaymentSuccessful) {
          (status, businessDetails.previouslyRegistered) match {
            case (ReadyForRenewal(_), _) if renewalData.isDefined =>
              Ok(payment_confirmation_renewal(businessName, reference))
            case (SubmissionReadyForReview | SubmissionDecisionApproved | RenewalSubmitted(_) | ReadyForRenewal(_), _) =>
              Ok(payment_confirmation_amendvariation(businessName, reference))
            case (_, Some(PreviouslyRegisteredYes(_))) =>
              Ok(payment_confirmation_transitional_renewal(businessName, reference))
            case _ => Ok(payment_confirmation(businessName, reference))
          }
        } else {
          Ok(payment_failure(
            msgFromPaymentStatus(paymentStatusFromQueryString.getOrElse(paymentStatus.currentStatus.toString)),
            Currency(payment.amountInPence.toDouble / 100), reference))
        }

        result getOrElse InternalServerError("There was a problem trying to show the confirmation page")
  }

  private def doAudit(paymentStatus: PaymentStatus, amlsRegistrationNumber: Option[String], accountTypeId: (String, String), groupIdentifier: Option[String])
                     (implicit hc: HeaderCarrier) = {
    for {
      fees <- OptionT(feeHelper.retrieveFeeResponse(amlsRegistrationNumber, accountTypeId, groupIdentifier, prefix))
      payRef <- OptionT.fromOption[Future](fees.paymentReference)
      result <- OptionT.liftF(auditConnector.sendEvent(PaymentConfirmationEvent(fees.amlsReferenceNumber, payRef, paymentStatus)))
    } yield result
  }
}