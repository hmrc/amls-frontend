package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{AmlsDataCacheConnector, DataCacheConnector}
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models._
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{AnyContent, Request, Result}
import services.BusinessCustomerService
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

trait RegisteredOfficeController extends AMLSGenericController {
  def optionsIsRegisteredOffice(implicit lang: Lang) = Seq(
    Messages("registeredoffice.lbl.yes.same") -> "1",
    Messages("registeredoffice.lbl.yes.different") -> "2",
    Messages("registeredoffice.lbl.no") -> "3"
  )

  def dataCacheConnector: DataCacheConnector

  def businessCustomerService: BusinessCustomerService

  private val CACHE_KEY = "registeredOffice"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) = {
    val reviewBusinessDetailsFuture = businessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails]
    val cachedDataFuture = dataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](CACHE_KEY)
    for {
      reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture
      cachedData: Option[RegisteredOffice] <- cachedDataFuture
    } yield {
      val fm = cachedData.fold (registeredOfficeForm) (registeredOfficeForm.fill)
      Ok(views.html.registered_office(fm, reviewBusinessDetails, optionsIsRegisteredOffice))
    }
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) = {
    val reviewBusinessDetailsFuture = businessCustomerService.getReviewBusinessDetails[BusinessCustomerDetails]
    registeredOfficeForm.bindFromRequest().fold(
      errors => {
        for (reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture) yield {
          BadRequest(views.html.registered_office(errors, reviewBusinessDetails, optionsIsRegisteredOffice))
        }
      },
      registeredOffice => {
        val futureFutureResult: Future[Future[Result]] =
          for (reviewBusinessDetails: BusinessCustomerDetails <- reviewBusinessDetailsFuture) yield {
            val regOfficeSave4Later = RegisteredOfficeSave4Later(reviewBusinessDetails.businessAddress,
               registeredOffice.isRegisteredOffice, registeredOffice.isCorrespondenceAddressSame)

            dataCacheConnector.saveDataShortLivedCache[RegisteredOfficeSave4Later](CACHE_KEY, regOfficeSave4Later) map {
              case Some(RegisteredOfficeSave4Later(_, true, false)) => Redirect(controllers.aboutthebusiness.routes.TelephoningBusinessController.get())
              case _ => NotImplemented("Not yet implemented")
            }
          }
        futureFutureResult.flatMap({ (x: Future[Result]) => x map { (y: Result) => y } })
      }
    )
  }
}

object RegisteredOfficeController extends RegisteredOfficeController {
  override def dataCacheConnector = AmlsDataCacheConnector

  override def authConnector = AMLSAuthConnector

  override def businessCustomerService = BusinessCustomerService
}