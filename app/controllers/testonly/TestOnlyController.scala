/*
 * Copyright 2018 HM Revenue & Customs
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

import config.{AmlsShortLivedCache, BusinessCustomerSessionCache}
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.BaseController
import javax.inject.{Inject, Singleton}
import models.UpdateSave4LaterResponse
import play.api.libs.json.Json
import services.UpdateSave4LaterService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.status.status_submitted

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TestOnlyController @Inject()(val authConnector: AuthConnector,
                                   implicit val dataCacheConnector: DataCacheConnector,
                                   val stubsService: UpdateSave4LaterService) extends BaseController {
//  override protected def authConnector: AuthConnector = AMLSAuthConnector
//  val stubsService: UpdateSave4LaterService = UpdateSave4LaterService


  def dropSave4Later = Authorised.async {
    implicit user =>
      implicit request =>
        removeCacheData map { _ =>
          Ok("Cache successfully cleared")
        }
  }

  def removeCacheData(implicit user: AuthContext,  hc: HeaderCarrier) = {
    BusinessCustomerSessionCache.remove()
    AmlsShortLivedCache.remove(user.user.oid)
  }

  def updateSave4Later(fileName:String)  = Authorised.async {
    implicit user =>
      implicit request =>

        stubsService.getSaveForLaterData(fileName) flatMap {
          case Some(data) => {
            removeCacheData flatMap { _ =>
              stubsService.update(data) map { _ =>
//                Ok(controllers.routes.LandingController.get())
                Ok
              }
            }
          }
          case _ => Future.successful(BadRequest)
        }
  }


  def duplicateEnrolment = Authorised.async {
    implicit user => implicit request =>
      Future.successful(Ok(views.html.submission.duplicate_enrolment()))
  }

  def duplicateSubmission = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(status_submitted("XML498749237483", Some("An example business"), None, true)))
  }

  def wrongCredentials = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.submission.wrong_credential_type()))
  }

  def getPayment(ref: String) = Authorised.async {
    implicit authContext => implicit request =>
      AmlsConnector.getPaymentByPaymentReference(ref) map {
        case Some(p) => Ok(Json.toJson(p))
        case _ => Ok(s"The payment for $ref was not found")
      }
  }

  def companyName = Authorised.async {
    implicit authContext => implicit request =>
      AmlsConnector.registrationDetails("XJ0000100093742") map { details =>
        Ok(details.companyName)
      } recover {
        case _ => Ok("Failed to fetch registration details")
      }
  }

  def paymentFailure = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.confirmation.payment_failure("confirmation.payment.failed.reason.failure", 100, "X123456789")))
  }

  def paymentSuccessful = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.confirmation.payment_confirmation("Company Name", "X123456789")))
  }

  def paymentSuccessfulTransitionalRenewal = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.confirmation.payment_confirmation_transitional_renewal("Company Name", "X123456789")))
  }

  def confirmationBacs = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.confirmation.confirmation_bacs("Company Name")))
  }

  def confirmationBacsTransitionalRenewal = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.confirmation.confirmation_bacs_transitional_renewal("Company Name")))
  }

}
