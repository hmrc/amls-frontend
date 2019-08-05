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

package connectors

import config.{AppConfig, WSHttp}
import javax.inject.Inject
import models.deregister.{DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse}
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.withdrawal.{WithdrawSubscriptionRequest, WithdrawSubscriptionResponse}
import models.{AmendVariationRenewalResponse, _}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
// Coverage has been turned off for these types until we remove the deprecated methods
class AmlsConnector @Inject()(val httpPost: WSHttp,
                              val httpGet: WSHttp,
                              val httpPut: WSHttp,
                              val appConfig: AppConfig) {

  private[connectors] val url: String = appConfig.subscriptionUrl

  private[connectors] val registrationUrl: String = s"${appConfig.amlsUrl}/amls/registration"

  private[connectors] val paymentUrl: String= s"${appConfig.amlsUrl}/amls/payment"

  @deprecated("to be removed when new auth completely implemented")
  def subscribe(subscriptionRequest: SubscriptionRequest, safeId: String)
  (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[SubscriptionRequest], resW: Writes[SubscriptionResponse], ac: AuthContext): Future[SubscriptionResponse] = {

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

  def subscribe(subscriptionRequest: SubscriptionRequest, safeId: String, accountTypeId: (String, String))
               (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[SubscriptionRequest], resW: Writes[SubscriptionResponse]): Future[SubscriptionResponse] = {

    val (accountType, accountId) = accountTypeId

    val postUrl = s"$url/$accountType/$accountId/$safeId"
    val prefix = "[AmlsConnector][subscribe]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(subscriptionRequest)}")
    httpPost.POST[SubscriptionRequest, SubscriptionResponse](postUrl, subscriptionRequest) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  @deprecated("to be removed when new auth completely implemented")
  def status(amlsRegistrationNumber: String)
            (implicit
             headerCarrier: HeaderCarrier,
             ec: ExecutionContext,
             reqW: Writes[ReadStatusResponse],
             ac: AuthContext): Future[ReadStatusResponse] = {

    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
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

  def status(amlsRegistrationNumber: String, accountTypeId: (String, String))
            (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[ReadStatusResponse]): Future[ReadStatusResponse] = {

    val (accountType, accountId) = accountTypeId

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

    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
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

  @deprecated("to be removed when new auth completely implemented")
  def update(updateRequest: SubscriptionRequest,amlsRegistrationNumber: String)
            (implicit
             headerCarrier: HeaderCarrier,
             ec: ExecutionContext,
             reqW: Writes[SubscriptionRequest],
             resW: Writes[AmendVariationRenewalResponse],
             ac: AuthContext): Future[AmendVariationRenewalResponse] = {

    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
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

  def update(updateRequest: SubscriptionRequest, amlsRegistrationNumber: String, accountTypeId: (String, String))
            (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[SubscriptionRequest], resW: Writes[AmendVariationRenewalResponse]): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = accountTypeId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/update"
    val prefix = "[AmlsConnector][update]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(updateRequest)}")
    httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, updateRequest) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  @deprecated("to be removed when new auth completely implemented")
  def variation(updateRequest: SubscriptionRequest, amlsRegistrationNumber: String)
               (implicit
                headerCarrier: HeaderCarrier,
                ec: ExecutionContext,
                reqW: Writes[SubscriptionRequest],
                resW: Writes[AmendVariationRenewalResponse],
                ac: AuthContext): Future[AmendVariationRenewalResponse] = {

    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
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

  def variation(updateRequest: SubscriptionRequest, amlsRegistrationNumber: String, accountTypeId: (String, String))
               (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, reqW: Writes[SubscriptionRequest], resW: Writes[AmendVariationRenewalResponse]): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = accountTypeId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/variation"
    val prefix = "[AmlsConnector][variation]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(updateRequest)}")
    httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, updateRequest) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  @deprecated("to be removed when new auth completely implemented")
  def renewal(subscriptionRequest: SubscriptionRequest, amlsRegistrationNumber: String)
             (implicit headerCarrier: HeaderCarrier,
             ec: ExecutionContext,
             authContext: AuthContext
             ): Future[AmendVariationRenewalResponse] = {

    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/renewal"
    val log = (msg: String) => Logger.debug(s"[AmlsConnector][renewal] $msg")

    log(s"Request body: ${Json.toJson(subscriptionRequest)}")

    httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, subscriptionRequest) map { response =>
      log(s"Response body: ${Json.toJson(response)}")
      response
    }
  }

  def renewal(subscriptionRequest: SubscriptionRequest, amlsRegistrationNumber: String, accountTypeId: (String, String))
             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = accountTypeId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/renewal"
    val log = (msg: String) => Logger.debug(s"[AmlsConnector][renewal] $msg")

    log(s"Request body: ${Json.toJson(subscriptionRequest)}")

    httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, subscriptionRequest) map { response =>
      log(s"Response body: ${Json.toJson(response)}")
      response
    }
  }

  @deprecated("to be removed when auth migration complete")
  def renewalAmendment(subscriptionRequest: SubscriptionRequest, amlsRegistrationNumber: String)
             (implicit headerCarrier: HeaderCarrier,
              ec: ExecutionContext,
              authContext: AuthContext
             ): Future[AmendVariationRenewalResponse] = {

    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/renewalAmendment"
    val log = (msg: String) => Logger.debug(s"[AmlsConnector][renewalAmendment] $msg")

    log(s"Request body: ${Json.toJson(subscriptionRequest)}")

    httpPost.POST[SubscriptionRequest, AmendVariationRenewalResponse](postUrl, subscriptionRequest) map { response =>
      log(s"Response body: ${Json.toJson(response)}")
      response
    }
  }

  def renewalAmendment(subscriptionRequest: SubscriptionRequest, amlsRegistrationNumber: String, accountTypeId: (String, String))
                      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[AmendVariationRenewalResponse] = {

    val (accountType, accountId) = accountTypeId

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

    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/withdrawal"

    httpPost.POST[WithdrawSubscriptionRequest, WithdrawSubscriptionResponse](postUrl, request)
  }

  def deregister(amlsRegistrationNumber: String, request: DeRegisterSubscriptionRequest)
                (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[DeRegisterSubscriptionResponse] = {
    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/deregistration"

    httpPost.POST[DeRegisterSubscriptionRequest, DeRegisterSubscriptionResponse](postUrl, request)
  }

  @deprecated("to be removed after new auth changes implemented")
  def savePayment(paymentId: String, amlsRefNo: String, safeId: String)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[HttpResponse] = {

    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val postUrl = s"$paymentUrl/$accountType/$accountId/$amlsRefNo/$safeId"

    Logger.debug(s"[AmlsConnector][savePayment]: Request to $postUrl with paymentId $paymentId")

    httpPost.POSTString[HttpResponse](postUrl, paymentId)
  }

  def savePayment(paymentId: String, amlsRefNo: String, safeId: String, accountTypeId: (String, String))
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val (accountType, accountId) = accountTypeId
    val postUrl = s"$paymentUrl/$accountType/$accountId/$amlsRefNo/$safeId"

    Logger.debug(s"[AmlsConnector][savePayment]: Request to $postUrl with paymentId $paymentId")

    httpPost.POSTString[HttpResponse](postUrl, paymentId)
  }

  @deprecated("to be removed after new auth changes implemented")
  def getPaymentByPaymentReference(paymentReference: String)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[Option[Payment]] = {
    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val getUrl = s"$paymentUrl/$accountType/$accountId/payref/$paymentReference"

    Logger.debug(s"[AmlsConnector][getPaymentByPaymentReference]: Request to $getUrl with $paymentReference")

    httpGet.GET[Payment](getUrl) map { result =>
      Some(result)
    } recover {
      case _: NotFoundException => None
    }
  }

  def getPaymentByPaymentReference(paymentReference: String, accountTypeId: (String, String))
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Payment]] = {

    val (accountType, accountId) = accountTypeId
    val getUrl = s"$paymentUrl/$accountType/$accountId/payref/$paymentReference"

    Logger.debug(s"[AmlsConnector][getPaymentByPaymentReference]: Request to $getUrl with $paymentReference")

    httpGet.GET[Payment](getUrl) map { result =>
      Some(result)
    } recover {
      case _: NotFoundException => None
    }
  }

  @deprecated("to be removed after new auth changes implemented")
  def getPaymentByAmlsReference(amlsRef: String)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[Option[Payment]] = {
    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val getUrl = s"$paymentUrl/$accountType/$accountId/amlsref/$amlsRef"

    Logger.debug(s"[AmlsConnector][getPaymentByAmlsReference]: Request to $getUrl with $amlsRef")

    httpGet.GET[Payment](getUrl) map { result =>
      Some(result)
    } recover {
      case _: NotFoundException => None
    }
  }

  def getPaymentByAmlsReference(amlsRef: String, accountTypeId: (String, String))
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Payment]] = {

    val (accountType, accountId) = accountTypeId
    val getUrl = s"$paymentUrl/$accountType/$accountId/amlsref/$amlsRef"

    Logger.debug(s"[AmlsConnector][getPaymentByAmlsReference]: Request to $getUrl with $amlsRef")

    httpGet.GET[Payment](getUrl) map { result =>
      Some(result)
    } recover {
      case _: NotFoundException => None
    }
  }

  def refreshPaymentStatus(paymentReference: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[PaymentStatusResult] = {
    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val putUrl = s"$paymentUrl/$accountType/$accountId/refreshstatus"

    Logger.debug(s"[AmlsConnector][refreshPaymentStatus]: Request to $putUrl with $paymentReference")

    httpPut.PUT[RefreshPaymentStatusRequest, PaymentStatusResult](putUrl, RefreshPaymentStatusRequest(paymentReference))
  }

  def registrationDetails(safeId: String)(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[RegistrationDetails] = {
    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
    val (accountType, accountId) = ConnectorHelper.accountTypeAndId
    val getUrl = s"$registrationUrl/$accountType/$accountId/details/$safeId"

    httpGet.GET[RegistrationDetails](getUrl)
  }

  def registrationDetails(safeId: String, accountTypeId: (String, String))(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationDetails] = {
    val (accountType, accountId) = accountTypeId
    val getUrl = s"$registrationUrl/$accountType/$accountId/details/$safeId"

    httpGet.GET[RegistrationDetails](getUrl)
  }

  def updateBacsStatus(ref: String, request: UpdateBacsRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    //TODO - deprecated by AuthAction.accountTypeAndId after new auth changes
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
