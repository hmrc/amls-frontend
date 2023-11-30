/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.testonly

import org.joda.time.LocalDate
import config.BusinessCustomerSessionCache
import connectors.cache.MongoCacheConnector
import connectors.{AmlsConnector, DataCacheConnector, TestOnlyStubConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}

import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessActivity.HighValueDealing
import models.tradingpremises.BusinessStructure.LimitedLiabilityPartnership
import models.tradingpremises._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UpdateMongoCacheService
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthAction
import views.html.confirmation._
import views.html.ErrorView
import views.html.submission.{DuplicateEnrolmentView, DuplicateSubmissionView, WrongCredentialTypeView}

import scala.concurrent.Future

@Singleton
class TestOnlyController @Inject()(implicit val dataCacheConnector: DataCacheConnector,
                                   val mongoCacheConnector: MongoCacheConnector,
                                   implicit val testOnlyStubConnector: TestOnlyStubConnector,
                                   val stubsService: UpdateMongoCacheService,
                                   val amlsConnector: AmlsConnector,
                                   val authAction: AuthAction,
                                   val ds: CommonPlayDependencies,
                                   val customerCache: BusinessCustomerSessionCache,
                                   val cc: MessagesControllerComponents,
                                   duplicateEnrolment: DuplicateEnrolmentView,
                                   duplicateSubmission: DuplicateSubmissionView,
                                   wrongCredentialType: WrongCredentialTypeView,
                                   paymentFailureView: PaymentFailureView,
                                   paymentConfirmationView: PaymentConfirmationView,
                                   paymentConfirmationTransitionalRenewalView: PaymentConfirmationTransitionalRenewalView,
                                   confirmationBacsView: ConfirmationBacsView,
                                   errorView: ErrorView
                                  ) extends AmlsBaseController(ds, cc) {


  def dropMongoCache = authAction.async {
      implicit request =>
        removeCacheData(request.credId) map { _ =>
          Ok("Cache successfully cleared")
        }
  }

  def removeCacheData(credId: String)(implicit hc: HeaderCarrier) = for {
    _ <- customerCache.remove()
    _ <- mongoCacheConnector.remove(credId)
    response <- testOnlyStubConnector.clearState()
  } yield response

  def updateMongo(fileName:String)  = authAction.async {
      implicit request =>
        stubsService.getMongoCacheData(fileName) flatMap {
          case Some(data) =>
            removeCacheData(request.credId) flatMap { _ =>
              stubsService.update(request.credId, data) map { _ =>
                Redirect(controllers.routes.LandingController.get)
              }
            }
          case _ => Future.successful(BadRequest)
        }
  }


  def duplicateEnrolment: Action[AnyContent] = authAction {
    implicit request =>
      Ok(duplicateEnrolment(appConfig.contactFrontendReportUrl))
  }

  def duplicateSubmission: Action[AnyContent] = authAction {
    implicit request =>
      Ok(duplicateSubmission(appConfig.contactFrontendReportUrl))
  }

  def wrongCredentials: Action[AnyContent] = authAction {
    implicit request =>
      Ok(wrongCredentialType(appConfig.contactFrontendReportUrl))
  }

  def getPayment(ref: String) = authAction.async {
    implicit request =>
      amlsConnector.getPaymentByPaymentReference(ref, request.accountTypeId) map {
        case Some(p) => Ok(Json.toJson(p))
        case _ => Ok(s"The payment for $ref was not found")
      }
  }

  def companyName = authAction.async {
    implicit request =>
      amlsConnector.registrationDetails(request.accountTypeId, "XJ0000100093742") map { details =>
        Ok(details.companyName)
      } recover {
        case _ => Ok("Failed to fetch registration details")
      }
  }

  def paymentFailure = authAction.async {
    implicit request =>
      Future.successful(Ok(paymentFailureView("confirmation.payment.failed.reason.failure", 100, "X123456789")))
  }

  def paymentSuccessful = authAction.async {
    implicit request =>
      Future.successful(Ok(paymentConfirmationView("Company Name", "X123456789")))
  }

  def paymentSuccessfulTransitionalRenewal = authAction.async {
    implicit request =>
      Future.successful(Ok(paymentConfirmationTransitionalRenewalView("Company Name", "X123456789")))
  }

  def confirmationBacs = authAction.async {
    implicit request =>
      Future.successful(Ok(confirmationBacsView("Company Name")))
  }

  def confirmationBacsTransitionalRenewal = confirmationBacs

  def populateTP = authAction.async {
    implicit request =>
      val c = (1 until 1625) map { i =>
          TradingPremises(
            Some(RegisteringAgentPremises(false)),
            Some(YourTradingPremises(s"Test $i", Address(s"Trading Premises $i", None, None, None, "RE1 1ER"), Some(true), Some(LocalDate.now()))),
            Some(LimitedLiabilityPartnership),
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasChanged = true,
            hasAccepted = true
          )
      }

      dataCacheConnector.save(request.credId, TradingPremises.key, c) map { _ => Redirect(controllers.routes.StatusController.get())}
  }

  def error: Action[AnyContent] = authAction { implicit request =>
    Ok(errorView("Error title", "Error heading", "This is the main body of the error"))
  }
}
