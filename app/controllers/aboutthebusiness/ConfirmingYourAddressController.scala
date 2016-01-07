package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models._
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

trait ConfirmingYourAddressController extends AMLSGenericController {
  def dataCacheConnector: DataCacheConnector

  def businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector

  private val CACHE_KEY = "registeredOffice"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) = {
    val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
    val cachedDataFuture = dataCacheConnector.fetchDataShortLivedCache[ConfirmingYourAddressSave4Later](CACHE_KEY)
    for {
      reviewBusinessDetails <- reviewBusinessDetailsFuture
      cachedData <- cachedDataFuture
    } yield {
      val fm = cachedData.fold(confirmingYourAddressForm)( x => confirmingYourAddressForm.fill(ConfirmingYourAddress.fromConfirmingYourAddressSave4Later(x)) )
      Ok(views.html.registered_office(fm, reviewBusinessDetails))
    }
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) = {
    val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
    confirmingYourAddressForm.bindFromRequest().fold(
      errors => {
        for (reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture) yield {
          BadRequest(views.html.registered_office(errors, reviewBusinessDetails))
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
              case Some(ConfirmingYourAddressSave4Later(_, true)) => Redirect(controllers.aboutthebusiness.routes.TelephoningBusinessController.get())
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
