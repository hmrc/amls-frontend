package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import controllers.auth.AmlsRegime
import forms.AboutTheBusinessForms._
import models._
import play.api.mvc.Result
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait ConfirmingYourAddressController extends FrontendController with Actions {
  def dataCacheConnector: DataCacheConnector

  def businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector

  private val CACHE_KEY = "registeredOffice"

  def get(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
    val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
    val cachedDataFuture = dataCacheConnector.fetchDataShortLivedCache[ConfirmingYourAddressSave4Later](CACHE_KEY)
    for {
      reviewBusinessDetails <- reviewBusinessDetailsFuture
      cachedData <- cachedDataFuture
    } yield {
      val fm = cachedData.fold(confirmingYourAddressForm)( x => confirmingYourAddressForm.fill(ConfirmingYourAddress.fromConfirmingYourAddressSave4Later(x)) )
      Ok(views.html.confirming_your_address(fm, reviewBusinessDetails))
    }
  }

  def post(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
    val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
    confirmingYourAddressForm.bindFromRequest().fold(
      errors => {
        for (reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture) yield {
          BadRequest(views.html.confirming_your_address(errors, reviewBusinessDetails))
        }
      },
      regOffice => {
        val futureFutureResult: Future[Future[Result]] =
          for {
            reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture
            result = dataCacheConnector.saveDataShortLivedCache[ConfirmingYourAddressSave4Later](CACHE_KEY,
              ConfirmingYourAddressSave4Later(reviewBusinessDetails.businessAddress, regOffice.isRegOfficeOrMainPlaceOfBusiness))
          } yield {
            result map {
              case Some(ConfirmingYourAddressSave4Later(_, true)) => Redirect(controllers.aboutthebusiness.routes.ContactingYouController.get())
              case _ => NotImplemented("Not yet implemented")
            }
          }
        futureFutureResult.flatMap({ (x: Future[Result]) => x map { (y: Result) => y } })
      }
    )
  }
}

object ConfirmingYourAddressController extends ConfirmingYourAddressController {
  override def dataCacheConnector = DataCacheConnector
  override def authConnector = AMLSAuthConnector
  override def businessCustomerSessionCacheConnector = BusinessCustomerSessionCacheConnector
}
