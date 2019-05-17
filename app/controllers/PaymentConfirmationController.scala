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
import models.{FeeResponse, ReadStatusResponse}
import play.api.Logger
import services.{AuthEnrolmentsService, FeeResponseService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BusinessName
import views.html.confirmation._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PaymentConfirmationController @Inject()(
                                        val authConnector: AuthConnector,
                                        private[controllers] implicit val dataCacheConnector: DataCacheConnector,
                                        private[controllers] implicit val amlsConnector: AmlsConnector,
                                        private[controllers] implicit val statusService: StatusService,
                                        private[controllers] val feeResponseService: FeeResponseService,
                                        private[controllers] val authEnrolmentsService: AuthEnrolmentsService,
                                        private[controllers] val auditConnector: AuditConnector
                                      ) extends BaseController {

  val prefix = "[PaymentConfirmationController]"

  def paymentConfirmation(reference: String) = Authorised.async {
    implicit authContext =>
      implicit request =>

        def companyName(maybeStatus: Option[ReadStatusResponse]): OptionT[Future, String] =
          maybeStatus.fold[OptionT[Future, String]](OptionT.some("")) { r => BusinessName.getName(r.safeId) }

        val msgFromPaymentStatus = Map[String, String](
          "Failed" -> "confirmation.payment.failed.reason.failure",
          "Cancelled" -> "confirmation.payment.failed.reason.cancelled"
        )

        val paymentStatusFromQueryString = request.queryString.get("paymentStatus").map(_.head)

        val isPaymentSuccessful = !request.queryString.contains("paymentStatus")

        val result = for {
          (status, detailedStatus) <- OptionT.liftF(statusService.getDetailedStatus)
          businessName <- companyName(detailedStatus) orElse OptionT.some("")
          renewalData <- OptionT.liftF(dataCacheConnector.fetch[Renewal](Renewal.key))
          paymentStatus <- OptionT.liftF(amlsConnector.refreshPaymentStatus(reference))
          payment <- OptionT(amlsConnector.getPaymentByPaymentReference(reference))
          businessDetails <- OptionT(dataCacheConnector.fetch[BusinessDetails](BusinessDetails.key))
          _ <- doAudit(paymentStatus.currentStatus)
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

  private def doAudit(paymentStatus: PaymentStatus)(implicit hc: HeaderCarrier, ac: AuthContext) = {
    for {
      fees <- OptionT(retrieveFeeResponse)
      payRef <- OptionT.fromOption[Future](fees.paymentReference)
      result <- OptionT.liftF(auditConnector.sendEvent(PaymentConfirmationEvent(fees.amlsReferenceNumber, payRef, paymentStatus)))
    } yield result
  }

  private def retrieveFeeResponse(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[FeeResponse]] = {
    Logger.debug(s"[$prefix][retrieveFeeResponse] - Begin...)")
    (for {
      amlsRegistrationNumber <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
      fees <- OptionT(feeResponseService.getFeeResponse(amlsRegistrationNumber))
    } yield fees).value
  }
}