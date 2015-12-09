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

trait RegisteredOfficeController extends AMLSGenericController {
  def optionsIsRegisteredOffice(implicit lang: Lang) = Seq(
    Messages("aboutthebusiness.registeredoffice.isregoffice.yessame") -> "1",
    Messages("aboutthebusiness.registeredoffice.isregoffice.yesdifferent") -> "2",
    Messages("aboutthebusiness.registeredoffice.isregoffice.no") -> "3"
  )

  def dataCacheConnector: DataCacheConnector

  def businessCustomerSessionCacheConnector: BusinessCustomerSessionCacheConnector

  private val CACHE_KEY = "registeredOffice"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) = {
    val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
    val cachedDataFuture = dataCacheConnector.fetchDataShortLivedCache[RegisteredOfficeSave4Later](CACHE_KEY)
    for {
      reviewBusinessDetails <- reviewBusinessDetailsFuture
      cachedData <- cachedDataFuture
    } yield {
      val fm = cachedData.fold(registeredOfficeForm)( x => registeredOfficeForm.fill(RegisteredOffice.fromRegisteredOfficeSave4Later(x)) )
      Ok(views.html.registered_office(fm, reviewBusinessDetails, optionsIsRegisteredOffice))
    }
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) = {
    val reviewBusinessDetailsFuture = businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails]
    registeredOfficeForm.bindFromRequest().fold(
      errors => {
        for (reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture) yield {
          BadRequest(views.html.registered_office(errors, reviewBusinessDetails, optionsIsRegisteredOffice))
        }
      },
      regOffice => {
        val futureFutureResult: Future[Future[Result]] =
          for {
            reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture
            result = dataCacheConnector.saveDataShortLivedCache[RegisteredOfficeSave4Later](CACHE_KEY,
              RegisteredOfficeSave4Later(reviewBusinessDetails.businessAddress,
                regOffice.isRegisteredOffice, regOffice.isCorrespondenceAddressSame))
          } yield {
            result map {
              case Some(RegisteredOfficeSave4Later(_, true, true)) => Redirect(controllers.aboutthebusiness.routes.TelephoningBusinessController.get())
              case _ => NotImplemented("Not yet implemented")
            }
          }
        futureFutureResult.flatMap({ (x: Future[Result]) => x map { (y: Result) => y } })
      }
    )
  }
}

object RegisteredOfficeController extends RegisteredOfficeController {
  override def dataCacheConnector = DataCacheConnector

  override def authConnector = AMLSAuthConnector

  override def businessCustomerSessionCacheConnector = BusinessCustomerSessionCacheConnector
}
