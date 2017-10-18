/*
 * Copyright 2017 HM Revenue & Customs
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

import config.{ApplicationConfig, WSHttp}
import models.deregister.{DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse}
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawSubscriptionResponse}
import models.{AmendVariationRenewalResponse, _}
import play.api.Logger
import play.api.libs.json.{JsObject, Json, Reads, Writes}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._
import play.api.http.Status.OK

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost, HttpPut, HttpResponse, NotFoundException }

trait AmlsConnector {

  private[connectors] def httpPost: HttpPost

  private[connectors] def httpGet: HttpGet

  private[connectors] def httpPut: HttpPut

  private[connectors] def url: String

  private[connectors] def registrationUrl: String

  private[connectors] def paymentUrl: String

  def subscribe
  (subscriptionRequest: SubscriptionRequest, safeId: String)
  (implicit
   headerCarrier: HeaderCarrier,
   ec: ExecutionContext,
   reqW: Writes[SubscriptionRequest],
   resW: Writes[SubscriptionResponse],
   ac: AuthContext
  ): Future[SubscriptionResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$safeId"
    val prefix = "[AmlsConnector][subscribe]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(subscriptionRequest)}")
    httpPost.POST[SubscriptionRequest, SubscriptionResponse](postUrl, subscriptionRequest) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  def status(amlsRegistrationNumber: String)
            (implicit
             headerCarrier: HeaderCarrier,
             ec: ExecutionContext,
             reqW: Writes[ReadStatusResponse],
             ac: AuthContext): Future[ReadStatusResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val getUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/status"
    val prefix = "[AmlsConnector][status]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")

    httpGet.GET[ReadStatusResponse](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  def view(amlsRegistrationNumber: String)
          (implicit
           headerCarrier: HeaderCarrier,
           ec: ExecutionContext,
           reqW: Writes[ViewResponse],
           ac: AuthContext
          ): Future[ViewResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val getUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[AmlsConnector][view]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")

    httpGet.GET[ViewResponse](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }

  }

  def update(updateRequest: SubscriptionRequest,amlsRegistrationNumber: String)
            (implicit
             headerCarrier: HeaderCarrier,
             ec: ExecutionContext,
             reqW: Writes[SubscriptionRequest],
             resW: Writes[AmendVariationRenewalResponse],
             ac: AuthContext): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/update"
    val prefix = "[AmlsConnector][update]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(updateRequest)}")
    httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, updateRequest) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  def variation(updateRequest: SubscriptionRequest,amlsRegistrationNumber: String)
               (implicit
                headerCarrier: HeaderCarrier,
                ec: ExecutionContext,
                reqW: Writes[SubscriptionRequest],
                resW: Writes[AmendVariationRenewalResponse],
                ac: AuthContext): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/variation"
    val prefix = "[AmlsConnector][variation]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(updateRequest)}")
    httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, updateRequest) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  def renewal(subscriptionRequest: SubscriptionRequest, amlsRegistrationNumber: String)
             (implicit headerCarrier: HeaderCarrier,
             ec: ExecutionContext,
             authContext: AuthContext
             ): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/renewal"
    val log = (msg: String) => Logger.debug(s"[AmlsConnector][renewal] $msg")

    log(s"Request body: ${Json.toJson(subscriptionRequest)}")

    httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, subscriptionRequest) map { response =>
      log(s"Response body: ${Json.toJson(response)}")
      response
    }
  }

  def renewalAmendment(subscriptionRequest: SubscriptionRequest, amlsRegistrationNumber: String)
             (implicit headerCarrier: HeaderCarrier,
              ec: ExecutionContext,
              authContext: AuthContext
             ): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/renewalAmendment"
    val log = (msg: String) => Logger.debug(s"[AmlsConnector][renewalAmendment] $msg")

    log(s"Request body: ${Json.toJson(subscriptionRequest)}")

    httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, subscriptionRequest) map { response =>
      log(s"Response body: ${Json.toJson(response)}")
      response
    }
  }

  def withdraw(amlsRegistrationNumber: String, request: WithdrawSubscriptionRequest)
              (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[WithdrawSubscriptionResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/withdrawal"

    httpPost.POST[WithdrawSubscriptionRequest, WithdrawSubscriptionResponse](postUrl, request)
  }

  def deregister(amlsRegistrationNumber: String, request: DeRegisterSubscriptionRequest)
                (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[DeRegisterSubscriptionResponse] = {
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/deregistration"

    httpPost.POST[DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse](postUrl, request)
  }

  def savePayment(paymentId: String, amlsRefNo: String, safeId: String)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[HttpResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val postUrl = s"$paymentUrl/$accountType/$accountId/$amlsRefNo/$safeId"

    Logger.debug(s"[AmlsConnector][savePayment]: Request to $postUrl with paymentId $paymentId")

    httpPost.POSTString[HttpResponse](postUrl, paymentId)
  }

  def getPaymentByPaymentReference(paymentReference: String)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[Option[Payment]] = {
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val getUrl = s"$paymentUrl/$accountType/$accountId/payref/$paymentReference"

    Logger.debug(s"[AmlsConnector][getPaymentByPaymentReference]: Request to $getUrl with $paymentReference")

    httpGet.GET[Payment](getUrl) map { result =>
      Some(result)
    } recover {
      case _: NotFoundException => None
    }
  }

  def getPaymentByAmlsReference(amlsRef: String)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[Option[Payment]] = {
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val getUrl = s"$paymentUrl/$accountType/$accountId/amlsref/$amlsRef"

    Logger.debug(s"[AmlsConnector][getPaymentByAmlsReference]: Request to $getUrl with $amlsRef")

    httpGet.GET[Payment](getUrl) map { result =>
      Some(result)
    } recover {
      case _: NotFoundException => None
    }
  }

  def refreshPaymentStatus(paymentReference: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[PaymentStatusResult] = {
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val putUrl = s"$paymentUrl/$accountType/$accountId/refreshstatus"

    Logger.debug(s"[AmlsConnector][refreshPaymentStatus]: Request to $putUrl with $paymentReference")

    httpPut.PUT[RefreshPaymentStatusRequest, PaymentStatusResult](putUrl, RefreshPaymentStatusRequest(paymentReference))
  }

  def registrationDetails(safeId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[RegistrationDetails] = {
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val getUrl = s"$registrationUrl/$accountType/$accountId/details/$safeId"

    httpGet.GET[RegistrationDetails](getUrl)
  }

  def updateBacsStatus(ref: String, request: UpdateBacsRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val putUrl = s"$paymentUrl/$accountType/$accountId/$ref/bacs"

    httpPut.PUT[UpdateBacsRequest, HttpResponse](putUrl, request)
  }

  def createBacsPayment(request: CreateBacsPaymentRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier, ac: AuthContext): Future[Payment] = {
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val postUrl = s"$paymentUrl/$accountType/$accountId/bacs"

    httpPost.POST[CreateBacsPaymentRequest, Payment](postUrl, request)
  }

}

object AmlsConnector extends AmlsConnector {
  override private[connectors] val httpPost = WSHttp
  override private[connectors] val httpGet = WSHttp
  override private[connectors] val httpPut = WSHttp
  override private[connectors] def url = ApplicationConfig.subscriptionUrl
  override private[connectors] def registrationUrl = s"${ApplicationConfig.amlsUrl}/amls/registration"
  override private[connectors] def paymentUrl = s"${ApplicationConfig.amlsUrl}/amls/payment"
}
