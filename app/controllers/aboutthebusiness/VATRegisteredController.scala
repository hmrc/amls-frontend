package controllers.aboutthebusiness

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness._
import models.businessmatching.BusinessType.{Partnership, SoleProprietor}
import models.businessmatching.{BusinessType, BusinessMatching}
import utils.ControllerHelper
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait VATRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(AboutTheBusiness(_, _, Some(data), _, _, _, _)) =>
          Ok(vat_registered(Form2[VATRegistered](data), edit))
        case _ =>
          Ok(vat_registered(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[VATRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(vat_registered(f, edit)))
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                businessType <- ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                aboutTheBusiness <- cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
              } yield {
                dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
                  aboutTheBusiness.vatRegistered(data))
                (businessType, edit) match {
                  case (Partnership, false) => Redirect(routes.RegisteredOfficeController.get())
                  case (_, false) => Redirect(routes.CorporationTaxRegisteredController.get())
                  case (_, true) => Redirect(routes.SummaryController.get())
                }
              }).getOrElse(Redirect(routes.ConfirmRegisteredOfficeController.get(edit)))
          }
      }
    }
  }
}

object VATRegisteredController extends VATRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
