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

package connectors

import config.ApplicationConfig

import javax.inject.Inject
import models.deregister.{DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse}
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawSubscriptionResponse}
import models.{AmendVariationRenewalResponse, _}
import play.api.Logging
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class AmlsConnector @Inject()(val http: HttpClient,
                              val appConfig: ApplicationConfig) extends Logging {

  private[connectors] val url: String = appConfig.subscriptionUrl

  private[connectors] val registrationUrl: String = s"${appConfig.amlsUrl}/amls/registration"

  private[connectors] val paymentUrl: String= s"${appConfig.amlsUrl}/amls/payment"

  def subscribe(subscriptionRequest: SubscriptionRequest, safeId: String, accountTypeId: (String, String))
               (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[SubscriptionRequest], resW: Writes[SubscriptionResponse]): Future[SubscriptionResponse] = {

    val (accountType, accountId) = accountTypeId
    val postUrl = s"$url/$accountType/$accountId/$safeId"
    val prefix = "[AmlsConnector][subscribe]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request Body: ${Json.toJson(subscriptionRequest)}")
    // $COVERAGE-ON$
    http.POST[SubscriptionRequest, SubscriptionResponse](postUrl, subscriptionRequest) map {
      response =>
        // $COVERAGE-OFF$
        logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        // $COVERAGE-ON$
        response
    }
  }

  def status(amlsRegistrationNumber: String, accountTypeId: (String, String))
            (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[ReadStatusResponse]): Future[ReadStatusResponse] = {

    val (accountType, accountId) = accountTypeId

    val getUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/status"
    val prefix = "[AmlsConnector][status]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    // $COVERAGE-ON$

    http.GET[ReadStatusResponse](getUrl) map {
      response =>
        // $COVERAGE-OFF$
        logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        // $COVERAGE-ON$
        response
    }
  }

  def view(amlsRegistrationNumber: String, accountTypeId: (String, String))
          (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[ViewResponse]): Future[ViewResponse] = {

    val (accountType, accountId) = accountTypeId

    val getUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[AmlsConnector][view]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    // $COVERAGE-ON$

    http.GET[ViewResponse](getUrl) map {
      response =>
        // $COVERAGE-OFF$
        logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        // $COVERAGE-ON$
        response
    }

  }

  def update(updateRequest: SubscriptionRequest, amlsRegistrationNumber: String, accountTypeId: (String, String))
            (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[SubscriptionRequest], resW: Writes[AmendVariationRenewalResponse]): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = accountTypeId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/update"
    val prefix = "[AmlsConnector][update]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request Body: ${Json.toJson(updateRequest)}")
    // $COVERAGE-ON$
    http.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, updateRequest) map {
      response =>
        // $COVERAGE-OFF$
        logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        // $COVERAGE-ON$
        response
    }
  }

  def variation(updateRequest: SubscriptionRequest, amlsRegistrationNumber: String, accountTypeId: (String, String))
               (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[SubscriptionRequest], resW: Writes[AmendVariationRenewalResponse]): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = accountTypeId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/variation"
    val prefix = "[AmlsConnector][variation]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request Body: ${Json.toJson(updateRequest)}")
    // $COVERAGE-ON$
    http.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, updateRequest) map {
      response =>
        // $COVERAGE-OFF$
        logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        // $COVERAGE-ON$
        response
    }
  }

  def renewal(subscriptionRequest: SubscriptionRequest, amlsRegistrationNumber: String, accountTypeId: (String, String))
             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = accountTypeId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/renewal"
    // $COVERAGE-OFF$
    val log = (msg: String) => logger.debug(s"[AmlsConnector][renewal] $msg")

    log(s"Request body: ${Json.toJson(subscriptionRequest)}")
    // $COVERAGE-ON$

    http.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, subscriptionRequest) map { response =>
      // $COVERAGE-OFF$
      log(s"Response body: ${Json.toJson(response)}")
      // $COVERAGE-ON$
      response
    }
  }

  def renewalAmendment(subscriptionRequest: SubscriptionRequest, amlsRegistrationNumber: String, accountTypeId: (String, String))
                      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = accountTypeId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/renewalAmendment"
    // $COVERAGE-OFF$
    val log = (msg: String) => logger.debug(s"[AmlsConnector][renewalAmendment] $msg")

    log(s"Request body: ${Json.toJson(subscriptionRequest)}")
    // $COVERAGE-ON$

    http.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, subscriptionRequest) map { response =>
      // $COVERAGE-OFF$
      log(s"Response body: ${Json.toJson(response)}")
      // $COVERAGE-ON$
      response
    }
  }

  def withdraw(amlsRegistrationNumber: String, request: WithdrawSubscriptionRequest, accountTypeId: (String, String))
              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[WithdrawSubscriptionResponse] = {
    
    val (accountType, accountId) = accountTypeId
    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/withdrawal"

    http.POST[WithdrawSubscriptionRequest, WithdrawSubscriptionResponse](postUrl, request)
  }

  def deregister(amlsRegistrationNumber: String, request: DeRegisterSubscriptionRequest, accountTypeId: (String, String))
                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeRegisterSubscriptionResponse] = {
    
    val (accountType, accountId) = accountTypeId
    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/deregistration"

    http.POST[DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse](postUrl, request)
  }

  def savePayment(paymentId: String, amlsRefNo: String, safeId: String, accountTypeId: (String, String))
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val (accountType, accountId) = accountTypeId
    val postUrl = s"$paymentUrl/$accountType/$accountId/$amlsRefNo/$safeId"

    // $COVERAGE-OFF$
    logger.debug(s"[AmlsConnector][savePayment]: Request to $postUrl with paymentId $paymentId")
    // $COVERAGE-ON$

    http.POSTString[HttpResponse](postUrl, paymentId)
  }

  def getPaymentByPaymentReference(paymentReference: String, accountTypeId: (String, String))
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Payment]] = {

    val (accountType, accountId) = accountTypeId
    val getUrl = s"$paymentUrl/$accountType/$accountId/payref/$paymentReference"

    // $COVERAGE-OFF$
    logger.debug(s"[AmlsConnector][getPaymentByPaymentReference]: Request to $getUrl with $paymentReference")
    // $COVERAGE-ON$

    http.GET[Payment](getUrl) map { result =>
      Some(result)
    } recover {
      case _: NotFoundException => None
    }
  }

  def getPaymentByAmlsReference(amlsRef: String, accountTypeId: (String, String))
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Payment]] = {

    val (accountType, accountId) = accountTypeId
    val getUrl = s"$paymentUrl/$accountType/$accountId/amlsref/$amlsRef"

    // $COVERAGE-OFF$
    logger.debug(s"[AmlsConnector][getPaymentByAmlsReference]: Request to $getUrl with $amlsRef")
    // $COVERAGE-ON$

    http.GET[Payment](getUrl) map { result =>
      Some(result)
    } recover {
      case _: NotFoundException => None
    }
  }

  def refreshPaymentStatus(paymentReference: String, accountTypeId: (String, String))
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PaymentStatusResult] = {

    val (accountType, accountId) = accountTypeId
    val putUrl = s"$paymentUrl/$accountType/$accountId/refreshstatus"
    // $COVERAGE-OFF$
    logger.debug(s"[AmlsConnector][refreshPaymentStatus]: Request to $putUrl with $paymentReference")
    // $COVERAGE-ON$
    http.PUT[RefreshPaymentStatusRequest, PaymentStatusResult](putUrl, RefreshPaymentStatusRequest(paymentReference))
  }

  def registrationDetails(accountTypeId: (String, String), safeId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationDetails] = {
    val getUrl = s"$registrationUrl/${accountTypeId._1}/${accountTypeId._2}/details/$safeId"
    http.GET[RegistrationDetails](getUrl)
  }

  def updateBacsStatus(accountTypeId: (String, String), ref: String, request: UpdateBacsRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val putUrl = s"$paymentUrl/${accountTypeId._1}/${accountTypeId._2}/$ref/bacs"
    http.PUT[UpdateBacsRequest, HttpResponse](putUrl, request)
  }

  def createBacsPayment(accountTypeId: (String, String), request: CreateBacsPaymentRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Payment] = {
    val postUrl = s"$paymentUrl/${accountTypeId._1}/${accountTypeId._2}/bacs"
    http.POST[CreateBacsPaymentRequest, Payment](postUrl, request)
  }
}
