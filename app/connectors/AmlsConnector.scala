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

package connectors

import config.ApplicationConfig
import models.deregister.{DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse}
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawSubscriptionResponse}
import models._
import play.api.Logging
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmlsConnector @Inject()(val httpClient: HttpClientV2,
                              val appConfig: ApplicationConfig) extends Logging {

  private[connectors] val url: String = appConfig.subscriptionUrl

  private[connectors] val registrationUrl: String = s"${appConfig.amlsUrl}/amls/registration"

  private[connectors] val paymentUrl: String = s"${appConfig.amlsUrl}/amls/payment"

  def subscribe(subscriptionRequest: SubscriptionRequest, safeId: String, accountTypeId: (String, String))
               (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[SubscriptionRequest], resW: Writes[SubscriptionResponse]): Future[SubscriptionResponse] = {

    val (accountType, accountId) = accountTypeId
    val postUrl = url"$url/$accountType/$accountId/$safeId"
    val prefix = "[AmlsConnector][subscribe]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request Body: ${Json.toJson(subscriptionRequest)}")
    // $COVERAGE-ON$

    httpClient
      .post(postUrl)
      .withBody(Json.toJson(subscriptionRequest))
      .execute[SubscriptionResponse]
      .map {
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

    val getUrl = url"$url/$accountType/$accountId/$amlsRegistrationNumber/status"
    val prefix = "[AmlsConnector][status]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    // $COVERAGE-ON$

    httpClient
      .get(getUrl)
      .execute[ReadStatusResponse]
      .map {
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

    val getUrl = url"$url/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[AmlsConnector][view]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    // $COVERAGE-ON$

    httpClient
      .get(getUrl)
    .execute[ViewResponse]
    .map {
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

    val postUrl = url"$url/$accountType/$accountId/$amlsRegistrationNumber/update"
    val prefix = "[AmlsConnector][update]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request Body: ${Json.toJson(updateRequest)}")
    // $COVERAGE-ON$
    httpClient
      .post(postUrl)
      .withBody(Json.toJson(updateRequest))
      .execute[AmendVariationRenewalResponse]
      .map {
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

    val postUrl = url"$url/$accountType/$accountId/$amlsRegistrationNumber/variation"
    val prefix = "[AmlsConnector][variation]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request Body: ${Json.toJson(updateRequest)}")
    // $COVERAGE-ON$
    httpClient
      .post(postUrl)
      .withBody(Json.toJson(updateRequest))
      .execute[AmendVariationRenewalResponse]
      .map {
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

    val postUrl = url"$url/$accountType/$accountId/$amlsRegistrationNumber/renewal"
    // $COVERAGE-OFF$
    val log = (msg: String) => logger.debug(s"[AmlsConnector][renewal] $msg")

    log(s"Request body: ${Json.toJson(subscriptionRequest)}")
    // $COVERAGE-ON$

    httpClient
      .post(postUrl)
      .withBody(Json.toJson(subscriptionRequest))
      .execute[AmendVariationRenewalResponse]
      .map { response =>
      // $COVERAGE-OFF$
      log(s"Response body: ${Json.toJson(response)}")
      // $COVERAGE-ON$
      response
    }
  }

  def renewalAmendment(subscriptionRequest: SubscriptionRequest, amlsRegistrationNumber: String, accountTypeId: (String, String))
                      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = accountTypeId

    val postUrl = url"$url/$accountType/$accountId/$amlsRegistrationNumber/renewalAmendment"
    // $COVERAGE-OFF$
    val log = (msg: String) => logger.debug(s"[AmlsConnector][renewalAmendment] $msg")

    log(s"Request body: ${Json.toJson(subscriptionRequest)}")
    // $COVERAGE-ON$


    httpClient
      .post(postUrl)
      .withBody(Json.toJson(subscriptionRequest))
      .execute[AmendVariationRenewalResponse]
      .map { response =>
      // $COVERAGE-OFF$
      log(s"Response body: ${Json.toJson(response)}")
      // $COVERAGE-ON$
      response
    }
  }

  def withdraw(amlsRegistrationNumber: String, request: WithdrawSubscriptionRequest, accountTypeId: (String, String))
              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[WithdrawSubscriptionResponse] = {

    val (accountType, accountId) = accountTypeId
    val postUrl = url"$url/$accountType/$accountId/$amlsRegistrationNumber/withdrawal"

    httpClient
      .post(postUrl)
      .withBody(Json.toJson(request))
      .execute[WithdrawSubscriptionResponse]
  }

  def deregister(
    amlsRegistrationNumber: String,
    request: DeRegisterSubscriptionRequest,
    accountTypeId: (String, String))(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[DeRegisterSubscriptionResponse] = {

    val (accountType, accountId) = accountTypeId
    val postUrl = url"$url/$accountType/$accountId/$amlsRegistrationNumber/deregistration"

    httpClient
      .post(postUrl)
      .withBody(Json.toJson(request))
      .execute[DeRegisterSubscriptionResponse]
  }

  def savePayment(paymentId: String, amlsRefNo: String, safeId: String, accountTypeId: (String, String))
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val (accountType, accountId) = accountTypeId
    val postUrl = url"$paymentUrl/$accountType/$accountId/$amlsRefNo/$safeId"

    // $COVERAGE-OFF$
    logger.debug(s"[AmlsConnector][savePayment]: Request to $postUrl with paymentId $paymentId")
    // $COVERAGE-ON$

    httpClient
      .post(postUrl)
      .withBody(paymentId)
      .execute[HttpResponse]
  }

  def getPaymentByPaymentReference(paymentReference: String, accountTypeId: (String, String))
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Payment]] = {

    val (accountType, accountId) = accountTypeId
    val getUrl = url"$paymentUrl/$accountType/$accountId/payref/$paymentReference"

    // $COVERAGE-OFF$
    logger.debug(s"[AmlsConnector][getPaymentByPaymentReference]: Request to $getUrl with $paymentReference")
    // $COVERAGE-ON$

    httpClient
      .get(getUrl)
      .execute[Option[Payment]]
  }

  def getPaymentByAmlsReference(amlsRef: String, accountTypeId: (String, String))
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Payment]] = {

    val (accountType, accountId) = accountTypeId
    val getUrl = url"$paymentUrl/$accountType/$accountId/amlsref/$amlsRef"

    // $COVERAGE-OFF$
    logger.debug(s"[AmlsConnector][getPaymentByAmlsReference]: Request to $getUrl with $amlsRef")
    // $COVERAGE-ON$

    httpClient
      .get(getUrl)
      .execute[Option[Payment]]
  }

  def refreshPaymentStatus(paymentReference: String, accountTypeId: (String, String))
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PaymentStatusResult] = {

    val (accountType, accountId) = accountTypeId
    val putUrl = url"$paymentUrl/$accountType/$accountId/refreshstatus"
    // $COVERAGE-OFF$
    logger.debug(s"[AmlsConnector][refreshPaymentStatus]: Request to $putUrl with $paymentReference")
    // $COVERAGE-ON$
    httpClient
      .put(putUrl)
      .withBody(Json.toJson(RefreshPaymentStatusRequest(paymentReference)))
      .execute[PaymentStatusResult]
  }

  def registrationDetails(accountTypeId: (String, String), safeId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationDetails] = {
    val getUrl = url"$registrationUrl/${accountTypeId._1}/${accountTypeId._2}/details/$safeId"
    httpClient
      .get(getUrl)
      .execute[RegistrationDetails]
  }

  def updateBacsStatus(accountTypeId: (String, String), ref: String, request: UpdateBacsRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val putUrl = url"$paymentUrl/${accountTypeId._1}/${accountTypeId._2}/$ref/bacs"
    httpClient
      .put(putUrl)
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
  }

  def createBacsPayment(accountTypeId: (String, String), request: CreateBacsPaymentRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Payment] = {
    val postUrl = url"$paymentUrl/${accountTypeId._1}/${accountTypeId._2}/bacs"
    httpClient
      .post(postUrl)
      .withBody(Json.toJson(request))
      .execute[Payment]
  }
}
