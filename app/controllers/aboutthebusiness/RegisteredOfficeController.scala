package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{AmlsDataCacheConnector, DataCacheConnector}
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models.RegisteredOffice
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

trait RegisteredOfficeController extends AMLSGenericController {

  def dataCacheConnector: DataCacheConnector

  private val CACHE_KEY = "registeredOffice"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[RegisteredOffice](CACHE_KEY) map {
      case Some(cachedData) => Ok(views.html.registeredOffice(registeredOfficeForm.fill(cachedData)))
      case _ => Ok(views.html.registeredOffice(registeredOfficeForm))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    registeredOfficeForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.registeredOffice(errors))),
      registeredOffice => {
        dataCacheConnector.saveDataShortLivedCache[RegisteredOffice](CACHE_KEY, registeredOffice) map { _ =>
          NotImplemented
        }
      })
}

object RegisteredOfficeController extends RegisteredOfficeController {
  override def dataCacheConnector = AmlsDataCacheConnector
  override def authConnector = AMLSAuthConnector
}