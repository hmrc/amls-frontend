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

package controllers.testonly

import org.joda.time.LocalDate
import config.BusinessCustomerSessionCache
import connectors.cache.MongoCacheConnector
import connectors.{AmlsConnector, DataCacheConnector, TestOnlyStubConnector}
import controllers.DefaultBaseController
import javax.inject.{Inject, Singleton}
import models.businessmatching.HighValueDealing
import models.tradingpremises._
import play.api.libs.json.Json
import services.UpdateMongoCacheService
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthAction
import views.html.submission.duplicate_submission

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TestOnlyController @Inject()(implicit val dataCacheConnector: DataCacheConnector,
                                   val mongoCacheConnector: MongoCacheConnector,
                                   implicit val testOnlyStubConnector: TestOnlyStubConnector,
                                   val stubsService: UpdateMongoCacheService,
                                   val amlsConnector: AmlsConnector,
                                   val authAction: AuthAction,
                                   val customerCache: BusinessCustomerSessionCache) extends DefaultBaseController {


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
                Redirect(controllers.routes.LandingController.get())
              }
            }
          case _ => Future.successful(BadRequest)
        }
  }


  def duplicateEnrolment = authAction.async {
    implicit request =>
      Future.successful(Ok(views.html.submission.duplicate_enrolment()))
  }

  def duplicateSubmission = authAction.async {
    implicit request =>
      Future.successful(Ok(duplicate_submission("There's an error")))
  }

  def wrongCredentials = authAction.async {
    implicit request =>
      Future.successful(Ok(views.html.submission.wrong_credential_type()))
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
      Future.successful(Ok(views.html.confirmation.payment_failure("confirmation.payment.failed.reason.failure", 100, "X123456789")))
  }

  def paymentSuccessful = authAction.async {
    implicit request =>
      Future.successful(Ok(views.html.confirmation.payment_confirmation("Company Name", "X123456789")))
  }

  def paymentSuccessfulTransitionalRenewal = authAction.async {
    implicit request =>
      Future.successful(Ok(views.html.confirmation.payment_confirmation_transitional_renewal("Company Name", "X123456789")))
  }

  def confirmationBacs = authAction.async {
    implicit request =>
      Future.successful(Ok(views.html.confirmation.confirmation_bacs("Company Name")))
  }

  def confirmationBacsTransitionalRenewal = authAction.async {
    implicit request =>
      Future.successful(Ok(views.html.confirmation.confirmation_bacs_transitional_renewal("Company Name")))
  }

  def populateTP = authAction.async {
    implicit request =>
      val c = (1 until 1625) map { i =>
          TradingPremises(
            Some(RegisteringAgentPremises(false)),
            Some(YourTradingPremises(s"Test $i", Address(s"Trading Premises $i", "Line 2", None, None, "RE1 1ER"), Some(true), Some(LocalDate.now()))),
            Some(LimitedLiabilityPartnership),
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasChanged = true,
            hasAccepted = true
          )
      }

      dataCacheConnector.save(request.credId, TradingPremises.key, c) map { _ => Redirect(controllers.routes.StatusController.get())}
  }

}
