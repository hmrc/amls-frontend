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
    Messages("registeredoffice.lbl.yes.same") -> "true,false",
    Messages("registeredoffice.lbl.yes.different") -> "true,true",
    Messages("registeredoffice.lbl.no") -> "false,false"
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
      val fm = cachedData.fold {
        registeredOfficeForm
      } {
        registeredOfficeForm.fill
      }
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
            val registeredOfficeSave4Later = RegisteredOfficeSave4Later(reviewBusinessDetails.businessAddress,
              registeredOffice.isCorrespondenceAddressSame, registeredOffice.isRegisteredOffice)

            dataCacheConnector.saveDataShortLivedCache[RegisteredOfficeSave4Later](CACHE_KEY,
              registeredOfficeSave4Later) map { _ =>
              NotImplemented("Not implemented")
            }
          }

        val map: Future[Result] = futureFutureResult.flatMap({ (x: Future[Result]) => x map { (y: Result) => y }  })
        map

      }
    )
  }
}

object RegisteredOfficeController extends RegisteredOfficeController {
  override def dataCacheConnector = AmlsDataCacheConnector

  override def authConnector = AMLSAuthConnector

  override def businessCustomerService = BusinessCustomerService
}